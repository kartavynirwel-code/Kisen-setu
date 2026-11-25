-- Bank account numbers are now stored encrypted (AES-256-GCM, see FieldCipher), so the column is renamed and widened.
-- No rows exist yet: Phase 3 never saved farmers.
ALTER TABLE farmer CHANGE COLUMN bank_account_no bank_account_enc VARCHAR(512);

-- A batch whose valid rows were saved as claims is COMMITTED; a dry run stays VALIDATED.
ALTER TABLE claim_batch DROP CHECK ck_claim_batch_status;
ALTER TABLE claim_batch ADD CONSTRAINT ck_claim_batch_status
    CHECK (status IN ('UPLOADED', 'VALIDATED', 'COMMITTED', 'SUBMITTED'));
