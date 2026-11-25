package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RowNormalizerTest {

    private final RowNormalizer normalizer = new RowNormalizer();

    private ClaimRow normalized(Map<String, String> values) {
        ClaimRow row = new ClaimRow(2, values);
        normalizer.normalize(row);
        return row;
    }

    @Test
    void cleansIdentifiers() {
        ClaimRow row = normalized(Map.of(
                Fields.AADHAAR, "2341 2341-2346",
                Fields.IFSC, " sbin 0001234 ",
                Fields.KCC_ACCOUNT_NO, "mp2025 000101",
                Fields.BANK_ACCOUNT_NO, "1234-5678-901",
                Fields.LAND_RECORD_ID, "mp/ind/ 1234",
                Fields.CLAIM_TYPE, " pri "));

        assertThat(row.get(Fields.AADHAAR)).isEqualTo("234123412346");
        assertThat(row.get(Fields.IFSC)).isEqualTo("SBIN0001234");
        assertThat(row.get(Fields.KCC_ACCOUNT_NO)).isEqualTo("MP2025000101");
        assertThat(row.get(Fields.BANK_ACCOUNT_NO)).isEqualTo("12345678901");
        assertThat(row.get(Fields.LAND_RECORD_ID)).isEqualTo("MP/IND/1234");
        assertThat(row.get(Fields.CLAIM_TYPE)).isEqualTo("PRI");
    }

    @Test
    void stripsCountryCodeFromMobile() {
        assertThat(normalized(Map.of(Fields.MOBILE, "+91 98765 43210")).get(Fields.MOBILE)).isEqualTo("9876543210");
        assertThat(normalized(Map.of(Fields.MOBILE, "919876543210")).get(Fields.MOBILE)).isEqualTo("9876543210");
        assertThat(normalized(Map.of(Fields.MOBILE, "09876543210")).get(Fields.MOBILE)).isEqualTo("9876543210");
        assertThat(normalized(Map.of(Fields.MOBILE, "98765-43210")).get(Fields.MOBILE)).isEqualTo("9876543210");
    }

    @Test
    void standardisesNamesAndPlaces() {
        ClaimRow row = normalized(Map.of(
                Fields.FULL_NAME, "  RAMESH   KUMAR patel ",
                Fields.DISTRICT, "indore"));

        assertThat(row.get(Fields.FULL_NAME)).isEqualTo("Ramesh Kumar Patel");
        assertThat(row.get(Fields.DISTRICT)).isEqualTo("Indore");
    }

    @Test
    void convertsDatesToIso() {
        assertThat(normalized(Map.of(Fields.DATE_OF_BIRTH, "05-03-1990")).get(Fields.DATE_OF_BIRTH)).isEqualTo("1990-03-05");
        assertThat(normalized(Map.of(Fields.DATE_OF_BIRTH, "5/3/1990")).get(Fields.DATE_OF_BIRTH)).isEqualTo("1990-03-05");
        assertThat(normalized(Map.of(Fields.DATE_OF_BIRTH, "05.03.1990")).get(Fields.DATE_OF_BIRTH)).isEqualTo("1990-03-05");
        assertThat(normalized(Map.of(Fields.DATE_OF_BIRTH, "1990-03-05")).get(Fields.DATE_OF_BIRTH)).isEqualTo("1990-03-05");
    }

    @Test
    void leavesUnfixableDatesForTheRuleToReport() {
        assertThat(normalized(Map.of(Fields.DATE_OF_BIRTH, "31/02/1990")).get(Fields.DATE_OF_BIRTH)).isEqualTo("31/02/1990");
    }

    @Test
    void stripsCurrencyFromAmounts() {
        assertThat(normalized(Map.of(Fields.LOAN_AMOUNT, "Rs. 1,50,000")).get(Fields.LOAN_AMOUNT)).isEqualTo("150000");
        assertThat(normalized(Map.of(Fields.LOAN_AMOUNT, "\u20B92,00,000.50")).get(Fields.LOAN_AMOUNT)).isEqualTo("200000.50");
        assertThat(normalized(Map.of(Fields.CLAIM_AMOUNT, "INR 4000")).get(Fields.CLAIM_AMOUNT)).isEqualTo("4000");
    }

    @Test
    void expandsShortFinancialYear() {
        assertThat(normalized(Map.of(Fields.FINANCIAL_YEAR, "2025-26")).get(Fields.FINANCIAL_YEAR)).isEqualTo("2025-2026");
        assertThat(normalized(Map.of(Fields.FINANCIAL_YEAR, "2025/26")).get(Fields.FINANCIAL_YEAR)).isEqualTo("2025-2026");
        assertThat(normalized(Map.of(Fields.FINANCIAL_YEAR, "2025-2026")).get(Fields.FINANCIAL_YEAR)).isEqualTo("2025-2026");
    }
}
