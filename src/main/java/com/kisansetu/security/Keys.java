package com.kisansetu.security;

import java.util.Base64;

/** Decodes base64 secrets from configuration and refuses weak or malformed ones. Never echoes the secret. */
public final class Keys {

    private Keys() {
    }

    public static byte[] decode(String base64, int minBytes, String name) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(name + " is not valid base64");
        }
        if (bytes.length < minBytes) {
            throw new IllegalStateException(name + " must be at least " + minBytes + " bytes (base64 of random bytes)");
        }
        return bytes;
    }
}
