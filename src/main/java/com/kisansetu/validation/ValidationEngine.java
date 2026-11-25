package com.kisansetu.validation;

import java.util.ArrayList;
import java.util.List;

/** Runs every row rule on every row, then every batch rule on the whole file. Expects already-normalized rows. */
public class ValidationEngine {

    private final List<RowRule> rowRules;
    private final List<BatchRule> batchRules;

    public ValidationEngine(List<RowRule> rowRules, List<BatchRule> batchRules) {
        this.rowRules = List.copyOf(rowRules);
        this.batchRules = List.copyOf(batchRules);
    }

    public ValidationResult validate(List<ClaimRow> rows) {
        List<FieldError> errors = new ArrayList<>();
        for (ClaimRow row : rows) {
            for (RowRule rule : rowRules) {
                errors.addAll(rule.check(row));
            }
        }
        for (BatchRule rule : batchRules) {
            errors.addAll(rule.check(rows));
        }
        return ValidationResult.of(rows.size(), errors);
    }
}
