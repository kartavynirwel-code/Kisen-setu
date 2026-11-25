package com.kisansetu.validation;

import com.kisansetu.validation.rules.AadhaarRule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Checks rows against records already saved by earlier uploads: the same claim filed twice, an Aadhaar that
 * belongs to another KCC account, a KCC account that belongs to another Aadhaar.
 */
public final class StoredConflictChecker {

    private StoredConflictChecker() {
    }

    public static List<FieldError> check(List<ClaimRow> rows, Function<String, String> aadhaarHasher,
                                         Collection<StoredFarmer> farmers, Collection<StoredClaim> claims) {
        Map<String, StoredFarmer> byKcc = new HashMap<>();
        Map<String, StoredFarmer> byHash = new HashMap<>();
        for (StoredFarmer farmer : farmers) {
            byKcc.put(farmer.kccAccountNo(), farmer);
            byHash.put(farmer.aadhaarHash(), farmer);
        }
        Map<String, StoredClaim> claimByKey = new HashMap<>();
        for (StoredClaim claim : claims) {
            claimByKey.put(key(claim.kccAccountNo(), claim.claimType(), claim.financialYear()), claim);
        }

        List<FieldError> errors = new ArrayList<>();
        for (ClaimRow row : rows) {
            String kcc = row.get(Fields.KCC_ACCOUNT_NO);
            String aadhaar = row.get(Fields.AADHAAR);
            if (kcc.isBlank()) {
                continue;
            }
            if (AadhaarRule.isWellFormed(aadhaar)) {
                String hash = aadhaarHasher.apply(aadhaar);
                StoredFarmer sameKcc = byKcc.get(kcc);
                if (sameKcc != null && !sameKcc.aadhaarHash().equals(hash)) {
                    errors.add(new FieldError(row.rowNo(), Fields.KCC_ACCOUNT_NO, "KCC_CONFLICT_STORED",
                            "This KCC account is already registered with a different Aadhaar",
                            "Check the KCC account number and the Aadhaar"));
                }
                StoredFarmer sameAadhaar = byHash.get(hash);
                if (sameAadhaar != null && !sameAadhaar.kccAccountNo().equals(kcc)) {
                    errors.add(new FieldError(row.rowNo(), Fields.AADHAAR, "AADHAAR_CONFLICT_STORED",
                            "Aadhaar " + AadhaarRule.mask(aadhaar) + " is already registered under a different KCC account",
                            "Check which KCC account the Aadhaar really belongs to"));
                }
            }
            StoredClaim existing = claimByKey.get(key(kcc, row.get(Fields.CLAIM_TYPE), row.get(Fields.FINANCIAL_YEAR)));
            if (existing != null) {
                errors.add(new FieldError(row.rowNo(), Fields.KCC_ACCOUNT_NO, "DUPLICATE_CLAIM_STORED",
                        "This claim was already saved in batch " + existing.batchId(),
                        "Remove the row, or ask a bank admin to correct the saved claim"));
            }
        }
        return errors;
    }

    private static String key(String kcc, String type, String year) {
        return kcc + "|" + type + "|" + year;
    }
}
