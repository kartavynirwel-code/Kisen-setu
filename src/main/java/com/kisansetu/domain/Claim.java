package com.kisansetu.domain;

import com.kisansetu.domain.enums.ClaimStatus;
import com.kisansetu.domain.enums.ClaimType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Claim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ClaimBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClaimType claimType;

    /** Format: 2025-2026 */
    @Column(nullable = false, length = 9)
    private String financialYear;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal loanAmount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal claimAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
