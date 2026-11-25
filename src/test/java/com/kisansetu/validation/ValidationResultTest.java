package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

    private static FieldError error(int row, String code) {
        return new FieldError(row, "f", code, "m", null);
    }

    @Test
    void countsDistinctErrorRowsAndSortsByRow() {
        ValidationResult r = ValidationResult.of(10, List.of(error(5, "B"), error(2, "A"), error(5, "C")));
        assertThat(r.errorRows()).isEqualTo(2);
        assertThat(r.validRows()).isEqualTo(8);
        assertThat(r.errors().get(0).rowNo()).isEqualTo(2);
    }

    @Test
    void withAddsErrorsAndRecounts() {
        ValidationResult r = ValidationResult.of(10, List.of(error(2, "A")));
        ValidationResult merged = r.with(List.of(error(2, "B"), error(9, "C")));
        assertThat(merged.errors().size()).isEqualTo(3);
        assertThat(merged.errorRows()).isEqualTo(2);
        assertThat(merged.hasError(9)).isTrue();
        assertThat(merged.hasError(3)).isFalse();
    }
}
