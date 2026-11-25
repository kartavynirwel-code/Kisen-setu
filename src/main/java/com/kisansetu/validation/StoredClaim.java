package com.kisansetu.validation;

public record StoredClaim(String kccAccountNo, String claimType, String financialYear, Long batchId) {
}
