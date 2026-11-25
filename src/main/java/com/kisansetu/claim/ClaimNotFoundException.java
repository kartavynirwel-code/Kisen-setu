package com.kisansetu.claim;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(Long id) {
        super("Claim " + id + " not found");
    }
}
