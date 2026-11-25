package com.kisansetu.upload;

import java.math.BigDecimal;

/** Text conversion for numeric spreadsheet cells. */
final class CellFormat {

    private CellFormat() {
    }

    /** 234123412346.0 becomes "234123412346" (not "2.34123412346E11"); 1.25 stays "1.25"; 100.0 becomes "100". */
    static String numberToText(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
