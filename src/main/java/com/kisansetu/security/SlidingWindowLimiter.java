package com.kisansetu.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** In-memory rate limiter: at most {@code maxEvents} per key within the window. Single-instance only. */
public final class SlidingWindowLimiter {

    private static final int SWEEP_THRESHOLD = 10_000;

    private final int maxEvents;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> events = new HashMap<>();

    public SlidingWindowLimiter(int maxEvents, Duration window, Clock clock) {
        this.maxEvents = maxEvents;
        this.window = window;
        this.clock = clock;
    }

    /** Records an event and returns true if it is allowed; returns false (recording nothing) if over the limit. */
    public synchronized boolean tryAcquire(String key) {
        prune(key);
        Deque<Instant> queue = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (queue.size() >= maxEvents) {
            return false;
        }
        queue.addLast(clock.instant());
        return true;
    }

    /** True if the key has already used up its allowance (records nothing). */
    public synchronized boolean isBlocked(String key) {
        prune(key);
        Deque<Instant> queue = events.get(key);
        return queue != null && queue.size() >= maxEvents;
    }

    public synchronized void record(String key) {
        prune(key);
        events.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(clock.instant());
    }

    public synchronized void reset(String key) {
        events.remove(key);
    }

    private void prune(String key) {
        Instant cutoff = clock.instant().minus(window);
        Deque<Instant> queue = events.get(key);
        if (queue != null) {
            while (!queue.isEmpty() && !queue.peekFirst().isAfter(cutoff)) {
                queue.removeFirst();
            }
            if (queue.isEmpty()) {
                events.remove(key);
            }
        }
        if (events.size() > SWEEP_THRESHOLD) {
            events.values().removeIf(q -> q.isEmpty() || !q.peekLast().isAfter(cutoff));
        }
    }
}
