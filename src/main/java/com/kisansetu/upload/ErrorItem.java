package com.kisansetu.upload;

import com.kisansetu.domain.ValidationError;

public record ErrorItem(int row, String field, String code, String message, String suggestion) {

    static ErrorItem from(ValidationError error) {
        return new ErrorItem(error.getRowNo(), error.getFieldName(), error.getErrorCode(), error.getMessage(),
                error.getSuggestion());
    }
}
