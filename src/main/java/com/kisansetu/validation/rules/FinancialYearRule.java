package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FinancialYearRule implements RowRule {

    private static final Pattern FORMAT = Pattern.compile("(\\d{4})-(\\d{4})");

    @Override
    public List<FieldError> check(ClaimRow row) {
        String value = row.get(Fields.FINANCIAL_YEAR);
        if (value.isBlank()) {
            return List.of();
        }
        Matcher m = FORMAT.matcher(value);
        if (m.matches() && Integer.parseInt(m.group(2)) == Integer.parseInt(m.group(1)) + 1) {
            return List.of();
        }
        return List.of(new FieldError(row.rowNo(), Fields.FINANCIAL_YEAR, "FINANCIAL_YEAR_INVALID",
                "Financial year must be two consecutive years, e.g. 2025-2026", "Use the format 2025-2026"));
    }
}
