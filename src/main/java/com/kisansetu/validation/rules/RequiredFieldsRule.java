package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import java.util.ArrayList;
import java.util.List;

public class RequiredFieldsRule implements RowRule {

    @Override
    public List<FieldError> check(ClaimRow row) {
        List<FieldError> errors = new ArrayList<>();
        for (String field : Fields.REQUIRED) {
            if (!row.has(field)) {
                errors.add(new FieldError(row.rowNo(), field, "REQUIRED_MISSING",
                        "Required value is missing", "Fill in the '" + field + "' column"));
            }
        }
        return errors;
    }
}
