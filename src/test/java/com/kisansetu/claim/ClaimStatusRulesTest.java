package com.kisansetu.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisansetu.domain.enums.ClaimStatus;
import org.junit.jupiter.api.Test;

class ClaimStatusRulesTest {

    @Test
    void rejectionNeedsAReasonWhichIsTrimmed() {
        assertThat(ClaimStatusRules.reasonToStore(ClaimStatus.REJECTED, "  Land record not found ")).isEqualTo("Land record not found");
        assertThatThrownBy(() -> ClaimStatusRules.reasonToStore(ClaimStatus.REJECTED, "  "))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("reason is required");
        assertThatThrownBy(() -> ClaimStatusRules.reasonToStore(ClaimStatus.REJECTED, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> ClaimStatusRules.reasonToStore(ClaimStatus.REJECTED, "x".repeat(501)))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("at most");
    }

    @Test
    void otherStatusesClearTheReason() {
        assertThat(ClaimStatusRules.reasonToStore(ClaimStatus.APPROVED, "old reason")).isEqualTo(null);
        assertThat(ClaimStatusRules.reasonToStore(ClaimStatus.PENDING, "old reason")).isEqualTo(null);
    }

    @Test
    void statusIsRequired() {
        assertThatThrownBy(() -> ClaimStatusRules.reasonToStore(null, null)).isInstanceOf(InvalidRequestException.class);
    }
}
