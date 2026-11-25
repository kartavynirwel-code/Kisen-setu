-- KisanSetu initial schema.
-- Notes:
--  * Aadhaar is stored as ciphertext (aadhaar_enc, written by Phase 4 encryption converter)
--    plus an HMAC-SHA256 hash (aadhaar_hash) used only for duplicate lookup.
--  * Plain VARCHAR is used instead of CHAR so Hibernate's ddl-auto=validate passes.
--  * "row_no" is used because ROW_NUMBER is a reserved word in MySQL 8.

CREATE TABLE app_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_role CHECK (role IN ('BANK_ADMIN', 'CSC_OPERATOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE farmer (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    kcc_account_no  VARCHAR(20)  NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    father_name     VARCHAR(150),
    date_of_birth   DATE,
    mobile          VARCHAR(10)  NOT NULL,
    aadhaar_enc     VARCHAR(512) NOT NULL,
    aadhaar_hash    VARCHAR(64)  NOT NULL,
    bank_account_no VARCHAR(20),
    ifsc            VARCHAR(11),
    village         VARCHAR(100),
    district        VARCHAR(100),
    state           VARCHAR(100),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_farmer_kcc_account_no UNIQUE (kcc_account_no),
    CONSTRAINT uk_farmer_aadhaar_hash UNIQUE (aadhaar_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_farmer_mobile ON farmer (mobile);

CREATE TABLE land_record (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    farmer_id      BIGINT         NOT NULL,
    land_record_id VARCHAR(30)    NOT NULL,
    owner_name     VARCHAR(150)   NOT NULL,
    area_hectares  DECIMAL(10, 4) NOT NULL,
    village        VARCHAR(100),
    district       VARCHAR(100),
    state          VARCHAR(100),
    created_at     DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_land_record_farmer FOREIGN KEY (farmer_id) REFERENCES farmer (id) ON DELETE CASCADE,
    CONSTRAINT uk_land_record_farmer_land UNIQUE (farmer_id, land_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kyc_record (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    farmer_id  BIGINT       NOT NULL,
    kyc_name   VARCHAR(150) NOT NULL,
    kyc_dob    DATE,
    kyc_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_kyc_record_farmer FOREIGN KEY (farmer_id) REFERENCES farmer (id) ON DELETE CASCADE,
    CONSTRAINT uk_kyc_record_farmer UNIQUE (farmer_id),
    CONSTRAINT ck_kyc_record_status CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'MISMATCH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE claim_batch (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uploaded_by BIGINT       NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    total_rows  INT          NOT NULL DEFAULT 0,
    valid_rows  INT          NOT NULL DEFAULT 0,
    error_rows  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_claim_batch_user FOREIGN KEY (uploaded_by) REFERENCES app_user (id),
    CONSTRAINT ck_claim_batch_status CHECK (status IN ('UPLOADED', 'VALIDATED', 'SUBMITTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE claim (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    batch_id         BIGINT        NOT NULL,
    farmer_id        BIGINT        NOT NULL,
    claim_type       VARCHAR(10)   NOT NULL,
    financial_year   VARCHAR(9)    NOT NULL,
    loan_amount      DECIMAL(14, 2) NOT NULL,
    claim_amount     DECIMAL(14, 2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_claim_batch  FOREIGN KEY (batch_id)  REFERENCES claim_batch (id),
    CONSTRAINT fk_claim_farmer FOREIGN KEY (farmer_id) REFERENCES farmer (id),
    -- A farmer can have only one claim of a given type per financial year (blocks duplicates).
    CONSTRAINT uk_claim_farmer_type_fy UNIQUE (farmer_id, claim_type, financial_year),
    CONSTRAINT ck_claim_type   CHECK (claim_type IN ('IS', 'PRI')),
    CONSTRAINT ck_claim_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_claim_status ON claim (status);

CREATE TABLE validation_error (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    batch_id   BIGINT       NOT NULL,
    row_no     INT          NOT NULL,
    field_name VARCHAR(50)  NOT NULL,
    error_code VARCHAR(50)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    suggestion VARCHAR(500),
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_validation_error_batch FOREIGN KEY (batch_id) REFERENCES claim_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_validation_error_batch_row ON validation_error (batch_id, row_no);

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     VARCHAR(1000),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
