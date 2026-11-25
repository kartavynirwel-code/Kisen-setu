package com.kisansetu.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ValidationResult(int totalRows, int errorRows, List<FieldError> errors) {

    /** Sorts the errors by row (stable) and counts the distinct rows that have at least one. */
    public static ValidationResult of(int totalRows, List<FieldError> errors) {
        List<FieldError> sorted = new ArrayList<>(errors);
        sorted.sort(Comparator.comparingInt(FieldError::rowNo));
        Set<Integer> rows = new HashSet<>();
        sorted.forEach(e -> rows.add(e.rowNo()));
        return new ValidationResult(totalRows, rows.size(), List.copyOf(sorted));
    }

    public ValidationResult with(List<FieldError> more) {
        List<FieldError> all = new ArrayList<>(errors);
        all.addAll(more);
        return of(totalRows, all);
    }

    public int validRows() {
        return totalRows - errorRows;
    }

    public boolean hasError(int rowNo) {
        return errors.stream().anyMatch(e -> e.rowNo() == rowNo);
    }
}
