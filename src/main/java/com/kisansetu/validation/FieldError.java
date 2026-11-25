package com.kisansetu.validation;

/** One validation problem found in one row. */
public record FieldError(int rowNo, String field, String code, String message, String suggestion) {
}
