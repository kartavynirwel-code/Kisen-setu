package com.kisansetu.security;

/**
 * Keyed hash (HMAC-SHA256) of an Aadhaar number. Used only to find duplicates without decrypting;
 * a plain hash would be easy to brute-force because Aadhaar numbers have a small search space.
 */
public final class AadhaarHasher {

    private final byte[] key;

    public AadhaarHasher(byte[] key) {
        if (key.length < 32) {
            throw new IllegalArgumentException("HMAC key must be at least 32 bytes");
        }
        this.key = key.clone();
    }

    public String hash(String aadhaar) {
        return Hmac.sha256Hex(key, aadhaar);
    }
}
