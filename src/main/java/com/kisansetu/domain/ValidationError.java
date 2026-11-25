package com.kisansetu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class ValidationError extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ClaimBatch batch;

    /** Row number in the uploaded file. */
    @Column(nullable = false)
    private int rowNo;

    @Column(nullable = false, length = 50)
    private String fieldName;

    @Column(nullable = false, length = 50)
    private String errorCode;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 500)
    private String suggestion;
}
