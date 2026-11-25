package com.kisansetu.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SlidingWindowLimiterTest {

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-18T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void allowsUpToTheLimitThenBlocks() {
        MutableClock clock = new MutableClock();
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(3, Duration.ofMinutes(10), clock);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.isBlocked("a")).isTrue();
    }

    @Test
    void keysAreIndependent() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(1, Duration.ofMinutes(10), new MutableClock());
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }

    @Test
    void allowanceReturnsAfterTheWindow() {
        MutableClock clock = new MutableClock();
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(2, Duration.ofMinutes(10), clock);
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");
        assertThat(limiter.tryAcquire("a")).isFalse();

        clock.advance(Duration.ofMinutes(10));

        assertThat(limiter.isBlocked("a")).isFalse();
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void failuresCanBeRecordedAndResetOnSuccess() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(2, Duration.ofMinutes(10), new MutableClock());
        limiter.record("u");
        assertThat(limiter.isBlocked("u")).isFalse();
        limiter.record("u");
        assertThat(limiter.isBlocked("u")).isTrue();
        limiter.reset("u");
        assertThat(limiter.isBlocked("u")).isFalse();
    }
}
