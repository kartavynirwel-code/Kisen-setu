package com.kisansetu.validation;

import java.util.HashMap;
import java.util.Map;

/** One data row of an uploaded file: a row number (as shown in the sheet) and field values. */
public final class ClaimRow {

    private final int rowNo;
    private final Map<String, String> values = new HashMap<>();

    public ClaimRow(int rowNo, Map<String, String> values) {
        this.rowNo = rowNo;
        values.forEach((k, v) -> this.values.put(k, v == null ? "" : v));
    }

    public int rowNo() {
        return rowNo;
    }

    /** Returns the value, or an empty string if the field is absent. */
    public String get(String field) {
        return values.getOrDefault(field, "");
    }

    public boolean has(String field) {
        return !get(field).isBlank();
    }

    public void put(String field, String value) {
        values.put(field, value == null ? "" : value);
    }
}
