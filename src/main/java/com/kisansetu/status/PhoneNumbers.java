package com.kisansetu.status;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Last 10 digits of "whatsapp:+919876543210", "+91 98765 43210", "9876543210"...; "" if fewer than 10 digits. */
    public static String last10(String number) {
        if (number == null) {
            return "";
        }
        String digits = number.replaceAll("\\D", "");
        return digits.length() < 10 ? "" : digits.substring(digits.length() - 10);
    }

    public static boolean sameNumber(String a, String b) {
        String x = last10(a);
        return !x.isEmpty() && x.equals(last10(b));
    }
}
