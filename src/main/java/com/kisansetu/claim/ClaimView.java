package com.kisansetu.claim;

import com.kisansetu.domain.Claim;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** What the API shows about a claim. Deliberately has no Aadhaar, mobile or bank details. */
public record ClaimView(Long id, Long batchId, String kccAccountNo, String farmerName, String claimType,
                        String financialYear, BigDecimal loanAmount, BigDecimal claimAmount, String status,
                        String rejectionReason, LocalDateTime updatedAt) {

    static ClaimView from(Claim claim) {
        return new ClaimView(claim.getId(), claim.getBatch().getId(), claim.getFarmer().getKccAccountNo(),
                claim.getFarmer().getFullName(), claim.getClaimType().name(), claim.getFinancialYear(),
                claim.getLoanAmount(), claim.getClaimAmount(), claim.getStatus().name(), claim.getRejectionReason(),
                claim.getUpdatedAt());
    }
}
