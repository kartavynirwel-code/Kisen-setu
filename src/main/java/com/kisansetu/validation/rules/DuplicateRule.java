package com.kisansetu.validation.rules;

import com.kisansetu.validation.BatchRule;
import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Duplicate / conflict detection inside one file. Errors are reported on the later row and point to the first one.
 * Rows whose Aadhaar is malformed do not take part in the Aadhaar checks, so one typo does not cascade.
 * Checks against already-stored records are added in a later phase, together with persistence.
 */
public class DuplicateRule implements BatchRule {

    private record Seen(String value, int rowNo) {
    }

    @Override
    public List<FieldError> check(List<ClaimRow> rows) {
        List<FieldError> errors = new ArrayList<>();
        Map<String, Seen> kccByAadhaar = new HashMap<>();
        Map<String, Seen> aadhaarByKcc = new HashMap<>();
        Map<String, Integer> claimKeys = new HashMap<>();

        for (ClaimRow row : rows) {
            String kcc = row.get(Fields.KCC_ACCOUNT_NO);
            String aadhaar = row.get(Fields.AADHAAR);

            if (!kcc.isBlank() && AadhaarRule.isWellFormed(aadhaar)) {
                Seen kccOwner = kccByAadhaar.putIfAbsent(aadhaar, new Seen(kcc, row.rowNo()));
                if (kccOwner != null && !kccOwner.value().equals(kcc)) {
                    errors.add(new FieldError(row.rowNo(), Fields.AADHAAR, "AADHAAR_CONFLICT",
                            "Aadhaar " + AadhaarRule.mask(aadhaar) + " is already used with a different KCC account in row "
                                    + kccOwner.rowNo(),
                            "Check which KCC account the Aadhaar really belongs to"));
                }
                Seen aadhaarOwner = aadhaarByKcc.putIfAbsent(kcc, new Seen(aadhaar, row.rowNo()));
                if (aadhaarOwner != null && !aadhaarOwner.value().equals(aadhaar)) {
                    errors.add(new FieldError(row.rowNo(), Fields.KCC_ACCOUNT_NO, "KCC_CONFLICT",
                            "KCC account is already used with a different Aadhaar in row " + aadhaarOwner.rowNo(),
                            "Check the KCC account number and the Aadhaar for both rows"));
                }
            }

            if (!kcc.isBlank() && row.has(Fields.CLAIM_TYPE) && row.has(Fields.FINANCIAL_YEAR)) {
                String key = kcc + "|" + row.get(Fields.CLAIM_TYPE) + "|" + row.get(Fields.FINANCIAL_YEAR);
                Integer firstRow = claimKeys.putIfAbsent(key, row.rowNo());
                if (firstRow != null) {
                    errors.add(new FieldError(row.rowNo(), Fields.KCC_ACCOUNT_NO, "DUPLICATE_CLAIM",
                            "Duplicate claim: same KCC account, claim type and financial year as row " + firstRow,
                            "Remove the duplicate row"));
                }
            }
        }
        return errors;
    }
}
