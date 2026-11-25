package com.kisansetu.domain;

import com.kisansetu.domain.enums.BatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ClaimBatch extends BaseEntity {

    /** Null until authentication is added in Phase 4. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private AppUser uploadedBy;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int validRows;

    @Column(nullable = false)
    private int errorRows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status = BatchStatus.UPLOADED;
}
