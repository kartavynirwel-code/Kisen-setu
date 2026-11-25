package com.kisansetu.upload;

import com.kisansetu.validation.Fields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maps the header row of an uploaded file to column positions. Header names are matched case-insensitively. */
final class HeaderMapper {

    private HeaderMapper() {
    }

    /** "KCC Account No" and "kcc-account-no" both become "kcc_account_no"; a leading BOM is dropped. */
    static String normalize(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    /** Returns field name to column index. Unknown columns are ignored. */
    static Map<String, Integer> map(List<String> headers) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalize(headers.get(i));
            if (!Fields.ALL.contains(key)) {
                continue;
            }
            if (columns.containsKey(key)) {
                throw new InvalidUploadException("Column '" + key + "' appears more than once");
            }
            columns.put(key, i);
        }
        List<String> missing = Fields.REQUIRED.stream().filter(f -> !columns.containsKey(f)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidUploadException("Missing required column(s): " + String.join(", ", missing));
        }
        return columns;
    }
}
