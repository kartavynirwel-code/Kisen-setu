package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.NameMatcher;
import com.kisansetu.validation.RowRule;
import java.util.List;

/** Cross-check: farmer name on the claim vs. name on the KYC record. */
public class KycNameMatchRule implements RowRule {

    static final double THRESHOLD = 0.88;

    @Override
    public List<FieldError> check(ClaimRow row) {
        if (!row.has(Fields.FULL_NAME) || !row.has(Fields.KYC_NAME)) {
            return List.of();
        }
        double similarity = NameMatcher.similarity(row.get(Fields.FULL_NAME), row.get(Fields.KYC_NAME));
        if (similarity >= THRESHOLD) {
            return List.of();
        }
        return List.of(new FieldError(row.rowNo(), Fields.KYC_NAME, "NAME_MISMATCH_KYC",
                "Name '" + row.get(Fields.FULL_NAME) + "' does not match KYC name '" + row.get(Fields.KYC_NAME)
                        + "' (" + Math.round(similarity * 100) + "% similar)",
                "Correct the spelling, or update the KYC record if the name has changed"));
    }
}
