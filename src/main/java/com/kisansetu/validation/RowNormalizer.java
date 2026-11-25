package com.kisansetu.validation;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-formats a row in place so that harmless formatting differences do not cause rejections:
 * whitespace, casing, date formats, currency symbols, country codes, financial-year shorthand.
 * Values that cannot be fixed safely are left as they are for the validation rules to report.
 */
public class RowNormalizer {

    private static final List<String> NAME_FIELDS = List.of(
            Fields.FULL_NAME, Fields.FATHER_NAME, Fields.KYC_NAME, Fields.LAND_OWNER_NAME);
    private static final List<String> PLACE_FIELDS = List.of(
            Fields.VILLAGE, Fields.DISTRICT, Fields.STATE);
    private static final List<String> DATE_FIELDS = List.of(
            Fields.DATE_OF_BIRTH, Fields.KYC_DOB);
    private static final List<String> NUMBER_FIELDS = List.of(
            Fields.AREA_HECTARES, Fields.LOAN_AMOUNT, Fields.CLAIM_AMOUNT);

    private static final Pattern FINANCIAL_YEAR = Pattern.compile("^(\\d{4})\\s*[-/\\u2013]\\s*(\\d{2}|\\d{4})$");

    public void normalize(ClaimRow row) {
        for (String field : Fields.ALL) {
            row.put(field, row.get(field).trim().replaceAll("\\s+", " "));
        }
        NAME_FIELDS.forEach(f -> row.put(f, titleCase(row.get(f))));
        PLACE_FIELDS.forEach(f -> row.put(f, titleCase(row.get(f))));
        DATE_FIELDS.forEach(f -> DateParser.parse(row.get(f)).map(LocalDate::toString).ifPresent(d -> row.put(f, d)));
        NUMBER_FIELDS.forEach(f -> row.put(f, stripNumber(row.get(f))));

        row.put(Fields.AADHAAR, row.get(Fields.AADHAAR).replaceAll("[\\s-]", ""));
        row.put(Fields.MOBILE, cleanMobile(row.get(Fields.MOBILE)));
        row.put(Fields.IFSC, upperNoSpaces(row.get(Fields.IFSC)));
        row.put(Fields.KCC_ACCOUNT_NO, upperNoSpaces(row.get(Fields.KCC_ACCOUNT_NO)));
        row.put(Fields.BANK_ACCOUNT_NO, row.get(Fields.BANK_ACCOUNT_NO).replaceAll("[\\s-]", ""));
        row.put(Fields.LAND_RECORD_ID, upperNoSpaces(row.get(Fields.LAND_RECORD_ID)));
        row.put(Fields.CLAIM_TYPE, upperNoSpaces(row.get(Fields.CLAIM_TYPE)));
        row.put(Fields.FINANCIAL_YEAR, normalizeFinancialYear(row.get(Fields.FINANCIAL_YEAR)));
    }

    static String titleCase(String text) {
        if (text.isBlank()) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    static String upperNoSpaces(String text) {
        return text.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    static String stripNumber(String text) {
        return text.replaceAll("(?i)rs\\.?|inr|\\u20B9", "").replaceAll("[,\\s]", "");
    }

    /** Removes separators and a +91 / 91 / 0 prefix so only the 10-digit number remains. */
    static String cleanMobile(String text) {
        String digits = text.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+91") && digits.length() == 13) {
            return digits.substring(3);
        }
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return digits.substring(1);
        }
        return digits;
    }

    /** "2025-26" and "2025/26" become "2025-2026". */
    static String normalizeFinancialYear(String text) {
        Matcher m = FINANCIAL_YEAR.matcher(text);
        if (!m.matches()) {
            return text;
        }
        String first = m.group(1);
        String second = m.group(2);
        if (second.length() == 2) {
            second = first.substring(0, 2) + second;
        }
        return first + "-" + second;
    }
}
