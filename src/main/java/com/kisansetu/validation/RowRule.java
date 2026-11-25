package com.kisansetu.validation;

import java.util.List;

/** A check that looks at one row at a time. Must skip (not fail) on blank values; RequiredFieldsRule reports those. */
public interface RowRule {

    List<FieldError> check(ClaimRow row);
}
