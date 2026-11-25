package com.kisansetu.claim;

import com.kisansetu.domain.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull ClaimStatus status, String rejectionReason) {
}
