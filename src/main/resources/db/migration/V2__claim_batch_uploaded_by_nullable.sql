-- Uploads are not tied to a logged-in user until authentication is added (Phase 4).
ALTER TABLE claim_batch MODIFY uploaded_by BIGINT NULL;
