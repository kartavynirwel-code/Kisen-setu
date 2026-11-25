package com.kisansetu.validation.rules;

import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.RowRule;
import com.kisansetu.validation.Verhoeff;
import java.util.List;

public class AadhaarRule implements RowRule {

    @Override
    public List<FieldError> check(ClaimRow row) {
        String aadhaar = row.get(Fields.AADHAAR);
        if (aadhaar.isBlank()) {
            return List.of();
        }
        if (!aadhaar.matches("\\d{12}")) {
            return List.of(error(row, "AADHAAR_FORMAT", "Aadhaar must be exactly 12 digits",
                    "Re-enter the 12-digit number from the Aadhaar card"));
        }
        if (aadhaar.charAt(0) == '0' || aadhaar.charAt(0) == '1') {
            return List.of(error(row, "AADHAAR_FORMAT", "Aadhaar cannot start with 0 or 1",
                    "Re-enter the 12-digit number from the Aadhaar card"));
        }
        if (!Verhoeff.isValid(aadhaar)) {
            return List.of(error(row, "AADHAAR_CHECKSUM",
                    "Aadhaar fails the checksum test (" + mask(aadhaar) + ")",
                    "Look for a mistyped or swapped digit"));
        }
        return List.of();
    }

    /** True if the value is 12 digits, does not start with 0/1 and passes the checksum. */
    public static boolean isWellFormed(String aadhaar) {
        return aadhaar.matches("[2-9]\\d{11}") && Verhoeff.isValid(aadhaar);
    }

    /** Only the last four digits are ever shown in messages. */
    public static String mask(String aadhaar) {
        return "XXXXXXXX" + aadhaar.substring(aadhaar.length() - 4);
    }

    private static FieldError error(ClaimRow row, String code, String message, String suggestion) {
        return new FieldError(row.rowNo(), Fields.AADHAAR, code, message, suggestion);
    }
}
