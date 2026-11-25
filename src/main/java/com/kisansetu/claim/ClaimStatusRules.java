package com.kisansetu.claim;

import com.kisansetu.domain.enums.ClaimStatus;

public final class ClaimStatusRules {

    static final int MAX_REASON = 500;

    private ClaimStatusRules() {
    }

    /** Returns the reason to store: required for REJECTED, always null otherwise. */
    public static String reasonToStore(ClaimStatus status, String reason) {
        if (status == null) {
            throw new InvalidRequestException("Status is required");
        }
        if (status != ClaimStatus.REJECTED) {
            return null;
        }
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidRequestException("A rejection reason is required when a claim is rejected");
        }
        if (trimmed.length() > MAX_REASON) {
            throw new InvalidRequestException("The rejection reason must be at most " + MAX_REASON + " characters");
        }
        return trimmed;
    }
}
