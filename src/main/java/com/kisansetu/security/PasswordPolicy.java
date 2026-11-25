package com.kisansetu.security;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    static final int MIN_LENGTH = 12;
    static final int MAX_BYTES = 72; // BCrypt ignores everything after 72 bytes

    private PasswordPolicy() {
    }

    public static void check(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("Password must be at most " + MAX_BYTES + " bytes");
        }
    }
}
