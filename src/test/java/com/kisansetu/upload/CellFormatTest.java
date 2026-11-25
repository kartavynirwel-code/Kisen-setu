package com.kisansetu.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CellFormatTest {

    @Test
    void longNumbersHaveNoExponent() {
        assertThat(CellFormat.numberToText(234123412346.0)).isEqualTo("234123412346");
        assertThat(CellFormat.numberToText(9876543210.0)).isEqualTo("9876543210");
    }

    @Test
    void wholeNumbersHaveNoDecimalPoint() {
        assertThat(CellFormat.numberToText(100.0)).isEqualTo("100");
        assertThat(CellFormat.numberToText(150000.0)).isEqualTo("150000");
        assertThat(CellFormat.numberToText(0.0)).isEqualTo("0");
    }

    @Test
    void decimalsAreKept() {
        assertThat(CellFormat.numberToText(1.25)).isEqualTo("1.25");
        assertThat(CellFormat.numberToText(0.2)).isEqualTo("0.2");
    }
}
