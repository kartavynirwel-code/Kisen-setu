package com.kisansetu.validation;

import java.util.List;

/** A check that needs to see all rows of the file at once (e.g. duplicates). */
public interface BatchRule {

    List<FieldError> check(List<ClaimRow> rows);
}
