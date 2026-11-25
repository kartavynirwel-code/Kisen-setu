package com.kisansetu.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredConflictCheckerTest {

    private static final String AADHAAR = "234123412346";
    private static final String OTHER_AADHAAR = "987654321096";

    /** Stand-in for the real keyed hash; only equality matters here. */
    private static String hash(String aadhaar) {
        return "h(" + aadhaar + ")";
    }

    private ClaimRow row(int rowNo, String kcc, String aadhaar, String type, String year) {
        return new ClaimRow(rowNo, Map.of(Fields.KCC_ACCOUNT_NO, kcc, Fields.AADHAAR, aadhaar,
                Fields.CLAIM_TYPE, type, Fields.FINANCIAL_YEAR, year));
    }

    private List<String> codes(List<ClaimRow> rows, List<StoredFarmer> farmers, List<StoredClaim> claims) {
        List<String> out = new ArrayList<>();
        for (FieldError e : StoredConflictChecker.check(rows, StoredConflictCheckerTest::hash, farmers, claims)) {
            out.add(e.rowNo() + ":" + e.code());
        }
        return out;
    }

    private final List<StoredFarmer> stored = List.of(new StoredFarmer("MP2025000101", hash(AADHAAR)));

    @Test
    void noConflictForNewFarmerOrSameFarmerNewClaim() {
        assertThat(codes(List.of(row(2, "MP2025000999", OTHER_AADHAAR, "IS", "2025-2026")), stored, List.of())).isEmpty();
        assertThat(codes(List.of(row(2, "MP2025000101", AADHAAR, "PRI", "2025-2026")), stored, List.of())).isEmpty();
    }

    @Test
    void flagsAlreadySavedClaimAndNamesTheBatch() {
        List<StoredClaim> claims = List.of(new StoredClaim("MP2025000101", "IS", "2025-2026", 7L));
        List<FieldError> errors = StoredConflictChecker.check(
                List.of(row(2, "MP2025000101", AADHAAR, "IS", "2025-2026")),
                StoredConflictCheckerTest::hash, stored, claims);

        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.get(0).code()).isEqualTo("DUPLICATE_CLAIM_STORED");
        assertThat(errors.get(0).message().contains("batch 7")).isTrue();
    }

    @Test
    void flagsAadhaarRegisteredUnderAnotherKcc() {
        assertThat(codes(List.of(row(2, "MP2025000999", AADHAAR, "IS", "2025-2026")), stored, List.of()))
                .containsExactly("2:AADHAAR_CONFLICT_STORED");
    }

    @Test
    void flagsKccRegisteredWithAnotherAadhaar() {
        assertThat(codes(List.of(row(2, "MP2025000101", OTHER_AADHAAR, "IS", "2025-2026")), stored, List.of()))
                .containsExactly("2:KCC_CONFLICT_STORED");
    }

    @Test
    void malformedAadhaarSkipsTheAadhaarChecks() {
        assertThat(codes(List.of(row(2, "MP2025000101", "123", "IS", "2025-2026")), stored, List.of())).isEmpty();
    }

    @Test
    void messageNeverShowsTheFullAadhaar() {
        List<FieldError> errors = StoredConflictChecker.check(
                List.of(row(2, "MP2025000999", AADHAAR, "IS", "2025-2026")),
                StoredConflictCheckerTest::hash, stored, List.of());
        assertThat(errors.get(0).message().contains(AADHAAR)).isFalse();
    }
}
