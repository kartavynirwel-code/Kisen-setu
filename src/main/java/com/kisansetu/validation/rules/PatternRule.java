package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.RowRule;
import java.util.List;
import java.util.regex.Pattern;

/** Generic "value must match this pattern" rule. The value is never echoed in the message (it may be personal data). */
public class PatternRule implements RowRule {

    private final String field;
    private final String code;
    private final Pattern pattern;
    private final String message;
    private final String suggestion;

    public PatternRule(String field, String code, String regex, String message, String suggestion) {
        this.field = field;
        this.code = code;
        this.pattern = Pattern.compile(regex);
        this.message = message;
        this.suggestion = suggestion;
    }

    @Override
    public List<FieldError> check(ClaimRow row) {
        String value = row.get(field);
        if (value.isBlank() || pattern.matcher(value).matches()) {
            return List.of();
        }
        return List.of(new FieldError(row.rowNo(), field, code, message, suggestion));
    }
}
