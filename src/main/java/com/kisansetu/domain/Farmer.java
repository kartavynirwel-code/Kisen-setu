package com.kisansetu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Farmer extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String kccAccountNo;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String fatherName;

    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 10)
    private String mobile;

    /** Aadhaar number, AES-256-GCM encrypted (context "farmer.aadhaar"). */
    @Column(nullable = false, length = 512)
    private String aadhaarEnc;

    /** HMAC-SHA256 of Aadhaar, used only for duplicate lookup. */
    @Column(nullable = false, unique = true, length = 64)
    private String aadhaarHash;

    /** Bank account number, AES-256-GCM encrypted (context "farmer.bank_account"). */
    @Column(length = 512)
    private String bankAccountEnc;

    @Column(length = 11)
    private String ifsc;

    @Column(length = 100)
    private String village;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
