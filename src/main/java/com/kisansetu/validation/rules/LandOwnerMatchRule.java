package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.NameMatcher;
import com.kisansetu.validation.RowRule;
import java.util.List;

/** Cross-check: the owner on the land record should be the farmer (or, commonly, the farmer's father). */
public class LandOwnerMatchRule implements RowRule {

    static final double THRESHOLD = 0.85;

    @Override
    public List<FieldError> check(ClaimRow row) {
        if (!row.has(Fields.LAND_OWNER_NAME) || !row.has(Fields.FULL_NAME)) {
            return List.of();
        }
        String owner = row.get(Fields.LAND_OWNER_NAME);
        boolean matchesFarmer = NameMatcher.matches(owner, row.get(Fields.FULL_NAME), THRESHOLD);
        boolean matchesFather = row.has(Fields.FATHER_NAME)
                && NameMatcher.matches(owner, row.get(Fields.FATHER_NAME), THRESHOLD);
        if (matchesFarmer || matchesFather) {
            return List.of();
        }
        return List.of(new FieldError(row.rowNo(), Fields.LAND_OWNER_NAME, "LAND_OWNER_MISMATCH",
                "Land record owner '" + owner + "' matches neither the farmer nor the father's name",
                "Confirm the land record, or attach co-owner / succession proof"));
    }
}
