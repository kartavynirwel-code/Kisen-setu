package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationEngineTest {

    private static final String VALID_AADHAAR = "234123412346";
    private static final String OTHER_VALID_AADHAAR = "987654321096";

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T00:00:00Z"), ZoneOffset.UTC);
    private final ValidationEngine engine = ValidationEngineFactory.create(clock);

    /** A row that passes every rule, in normalized form. */
    private ClaimRow validRow(int rowNo) {
        Map<String, String> v = new HashMap<>();
        v.put(Fields.KCC_ACCOUNT_NO, "MP2025000101");
        v.put(Fields.FULL_NAME, "Ramesh Patel");
        v.put(Fields.FATHER_NAME, "Hari Patel");
        v.put(Fields.DATE_OF_BIRTH, "1985-04-12");
        v.put(Fields.MOBILE, "9876543210");
        v.put(Fields.AADHAAR, VALID_AADHAAR);
        v.put(Fields.KYC_NAME, "Ramesh Patel");
        v.put(Fields.KYC_DOB, "1985-04-12");
        v.put(Fields.BANK_ACCOUNT_NO, "12345678901");
        v.put(Fields.IFSC, "SBIN0001234");
        v.put(Fields.LAND_RECORD_ID, "MP/IND/1234");
        v.put(Fields.LAND_OWNER_NAME, "Ramesh Patel");
        v.put(Fields.AREA_HECTARES, "1.25");
        v.put(Fields.VILLAGE, "Mhow");
        v.put(Fields.DISTRICT, "Indore");
        v.put(Fields.STATE, "Madhya Pradesh");
        v.put(Fields.CLAIM_TYPE, "IS");
        v.put(Fields.FINANCIAL_YEAR, "2025-2026");
        v.put(Fields.LOAN_AMOUNT, "100000");
        v.put(Fields.CLAIM_AMOUNT, "4000");
        return new ClaimRow(rowNo, v);
    }

    private ClaimRow validRow(int rowNo, String field, String value) {
        ClaimRow row = validRow(rowNo);
        row.put(field, value);
        return row;
    }

    private List<String> codes(ClaimRow... rows) {
        List<String> out = new ArrayList<>();
        for (FieldError e : engine.validate(List.of(rows)).errors()) {
            out.add(e.rowNo() + ":" + e.code());
        }
        return out;
    }

    @Test
    void validRowHasNoErrors() {
        ValidationResult result = engine.validate(List.of(validRow(2)));
        assertThat(result.errors()).isEmpty();
        assertThat(result.validRows()).isEqualTo(1);
    }

    @Test
    void flagsMissingRequiredValue() {
        assertThat(codes(validRow(2, Fields.LAND_RECORD_ID, ""))).containsExactly("2:REQUIRED_MISSING");
    }

    @Test
    void flagsAadhaarChecksum() {
        assertThat(codes(validRow(2, Fields.AADHAAR, "234123412347"))).containsExactly("2:AADHAAR_CHECKSUM");
    }

    @Test
    void flagsAadhaarLengthAndLeadingDigit() {
        assertThat(codes(validRow(2, Fields.AADHAAR, "23412341234"))).containsExactly("2:AADHAAR_FORMAT");
        assertThat(codes(validRow(2, Fields.AADHAAR, "134123412346"))).containsExactly("2:AADHAAR_FORMAT");
    }

    @Test
    void aadhaarMessageNeverShowsTheFullNumber() {
        FieldError error = engine.validate(List.of(validRow(2, Fields.AADHAAR, "234123412347"))).errors().get(0);
        assertThat(error.message().contains("234123412347")).isFalse();
        assertThat(error.message().contains("XXXXXXXX2347")).isTrue();
    }

    @Test
    void flagsBadMobile() {
        assertThat(codes(validRow(2, Fields.MOBILE, "987654321"))).containsExactly("2:MOBILE_FORMAT");
        assertThat(codes(validRow(2, Fields.MOBILE, "5876543210"))).containsExactly("2:MOBILE_FORMAT");
    }

    @Test
    void flagsBadIfscAndBankAccount() {
        assertThat(codes(validRow(2, Fields.IFSC, "SBIN1001234"))).containsExactly("2:IFSC_FORMAT");
        assertThat(codes(validRow(2, Fields.BANK_ACCOUNT_NO, "1234"))).containsExactly("2:BANK_ACCOUNT_FORMAT");
    }

    @Test
    void flagsBadClaimTypeAndFinancialYear() {
        assertThat(codes(validRow(2, Fields.CLAIM_TYPE, "XYZ"))).containsExactly("2:CLAIM_TYPE_INVALID");
        assertThat(codes(validRow(2, Fields.FINANCIAL_YEAR, "2025-2027"))).containsExactly("2:FINANCIAL_YEAR_INVALID");
    }

    @Test
    void flagsDateOfBirthProblems() {
        assertThat(codes(validRow(2, Fields.DATE_OF_BIRTH, "1985-02-31"))).contains("2:DOB_FORMAT");
        assertThat(codes(validRow(2, Fields.DATE_OF_BIRTH, "2035-05-14"), validRow(3, Fields.DATE_OF_BIRTH, "2015-03-02")))
                .contains("2:DOB_AGE_RANGE", "3:DOB_AGE_RANGE");
    }

    @Test
    void flagsKycDateOfBirthMismatch() {
        assertThat(codes(validRow(2, Fields.KYC_DOB, "1986-04-12"))).containsExactly("2:DOB_MISMATCH_KYC");
    }

    @Test
    void kycDateOfBirthColumnIsOptional() {
        assertThat(codes(validRow(2, Fields.KYC_DOB, ""))).isEmpty();
    }

    @Test
    void flagsAmountProblems() {
        assertThat(codes(validRow(2, Fields.CLAIM_AMOUNT, "120000"))).containsExactly("2:CLAIM_AMOUNT_EXCEEDS_LOAN");
        assertThat(codes(validRow(2, Fields.LOAN_AMOUNT, "abc"))).containsExactly("2:LOAN_AMOUNT_INVALID");
        assertThat(codes(validRow(2, Fields.AREA_HECTARES, "0"))).containsExactly("2:AREA_INVALID");
    }

    @Test
    void flagsKycNameMismatchButAcceptsReorderedName() {
        assertThat(codes(validRow(2, Fields.KYC_NAME, "Mahesh Chandra Joshi"))).containsExactly("2:NAME_MISMATCH_KYC");
        assertThat(codes(validRow(2, Fields.KYC_NAME, "Patel Ramesh"))).isEmpty();
    }

    @Test
    void landOwnerMayBeFarmerOrFather() {
        assertThat(codes(validRow(2, Fields.LAND_OWNER_NAME, "Hari Patel"))).isEmpty();
        assertThat(codes(validRow(2, Fields.LAND_OWNER_NAME, "Suresh Verma"))).containsExactly("2:LAND_OWNER_MISMATCH");
    }

    @Test
    void flagsDuplicateClaimOnLaterRowOnly() {
        List<String> codes = codes(validRow(2), validRow(3));
        assertThat(codes).containsExactly("3:DUPLICATE_CLAIM");
    }

    @Test
    void sameFarmerWithDifferentClaimTypeIsNotADuplicate() {
        assertThat(codes(validRow(2), validRow(3, Fields.CLAIM_TYPE, "PRI"))).isEmpty();
    }

    @Test
    void flagsAadhaarUsedWithTwoKccAccounts() {
        ClaimRow second = validRow(3, Fields.KCC_ACCOUNT_NO, "MP2025000202");
        assertThat(codes(validRow(2), second)).containsExactly("3:AADHAAR_CONFLICT");
    }

    @Test
    void flagsKccAccountUsedWithTwoAadhaars() {
        ClaimRow second = validRow(3, Fields.AADHAAR, OTHER_VALID_AADHAAR);
        second.put(Fields.CLAIM_TYPE, "PRI");
        assertThat(codes(validRow(2), second)).containsExactly("3:KCC_CONFLICT");
    }

    @Test
    void malformedAadhaarDoesNotCauseConflictsOnOtherRows() {
        ClaimRow typo = validRow(2, Fields.AADHAAR, "234123412347");
        ClaimRow good = validRow(3, Fields.CLAIM_TYPE, "PRI");
        assertThat(codes(typo, good)).containsExactly("2:AADHAAR_CHECKSUM");
    }

    @Test
    void countsErrorRowsNotErrors() {
        ClaimRow twoProblems = validRow(2, Fields.MOBILE, "123");
        twoProblems.put(Fields.IFSC, "bad");
        ClaimRow otherFarmer = validRow(3, Fields.KCC_ACCOUNT_NO, "MP2025000999");
        otherFarmer.put(Fields.AADHAAR, OTHER_VALID_AADHAAR);
        ValidationResult result = engine.validate(List.of(twoProblems, otherFarmer));
        assertThat(result.errors().size()).isEqualTo(2);
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.validRows()).isEqualTo(1);
    }

    @Test
    void messyInputBecomesValidAfterNormalization() {
        Map<String, String> v = new HashMap<>();
        v.put(Fields.KCC_ACCOUNT_NO, "mp2025 000101");
        v.put(Fields.FULL_NAME, "RAMESH  PATEL");
        v.put(Fields.FATHER_NAME, "hari patel");
        v.put(Fields.DATE_OF_BIRTH, "12-04-1985");
        v.put(Fields.MOBILE, "+91 98765 43210");
        v.put(Fields.AADHAAR, "2341 2341 2346");
        v.put(Fields.KYC_NAME, "ramesh patel");
        v.put(Fields.BANK_ACCOUNT_NO, "1234 5678 901");
        v.put(Fields.IFSC, "sbin0001234");
        v.put(Fields.LAND_RECORD_ID, "mp/ind/1234");
        v.put(Fields.LAND_OWNER_NAME, "RAMESH PATEL");
        v.put(Fields.AREA_HECTARES, "1.25");
        v.put(Fields.CLAIM_TYPE, "is");
        v.put(Fields.FINANCIAL_YEAR, "2025-26");
        v.put(Fields.LOAN_AMOUNT, "Rs. 1,00,000");
        v.put(Fields.CLAIM_AMOUNT, "\u20B94,000");
        ClaimRow row = new ClaimRow(2, v);

        new RowNormalizer().normalize(row);

        assertThat(engine.validate(List.of(row)).errors()).isEmpty();
    }
}
