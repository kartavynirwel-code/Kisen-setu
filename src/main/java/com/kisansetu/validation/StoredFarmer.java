package com.kisansetu.validation;

/** What the database already knows about a farmer, as far as conflict checks are concerned. */
public record StoredFarmer(String kccAccountNo, String aadhaarHash) {
}
