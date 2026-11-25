package com.kisansetu.upload;

import com.kisansetu.auth.AuditService;
import com.kisansetu.domain.ClaimBatch;
import com.kisansetu.domain.ValidationError;
import com.kisansetu.domain.enums.BatchStatus;
import com.kisansetu.repository.AppUserRepository;
import com.kisansetu.repository.ClaimBatchRepository;
import com.kisansetu.repository.ValidationErrorRepository;
import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.RowNormalizer;
import com.kisansetu.validation.ValidationEngine;
import com.kisansetu.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Upload pipeline: read file, auto-format rows, validate (file rules, then rules against saved data), store the
 * batch summary and errors, and unless this is a dry run save the rows that passed as claims.
 * Rows with any error are never saved; they are fixed in the sheet and uploaded again.
 */
@Service
@RequiredArgsConstructor
public class BatchService {

    private final ClaimFileReader reader;
    private final RowNormalizer normalizer;
    private final ValidationEngine engine;
    private final StoredConflictService storedConflicts;
    private final ClaimPersister persister;
    private final ClaimBatchRepository batches;
    private final ValidationErrorRepository validationErrors;
    private final AppUserRepository users;
    private final AuditService audit;

    @Transactional
    public ClaimBatch processUpload(String originalFileName, InputStream in, boolean dryRun, Long userId)
            throws IOException {
        List<ClaimRow> rows = reader.read(originalFileName, in);
        if (rows.isEmpty()) {
            throw new InvalidUploadException("The file has no data rows");
        }
        rows.forEach(normalizer::normalize);
        ValidationResult result = engine.validate(rows).with(storedConflicts.check(rows));

        ClaimBatch batch = new ClaimBatch();
        batch.setFileName(displayName(originalFileName));
        batch.setTotalRows(result.totalRows());
        batch.setValidRows(result.validRows());
        batch.setErrorRows(result.errorRows());
        batch.setStatus(dryRun ? BatchStatus.VALIDATED : BatchStatus.COMMITTED);
        if (userId != null) {
            batch.setUploadedBy(users.getReferenceById(userId));
        }
        ClaimBatch saved = batches.save(batch);

        validationErrors.saveAll(result.errors().stream().map(e -> toEntity(saved, e)).toList());

        if (!dryRun) {
            Set<Integer> errorRows = new HashSet<>();
            result.errors().forEach(e -> errorRows.add(e.rowNo()));
            persister.persist(saved, rows.stream().filter(r -> !errorRows.contains(r.rowNo())).toList());
        }

        audit.record(userId, dryRun ? "BATCH_VALIDATED" : "BATCH_COMMITTED", "claim_batch", saved.getId(),
                result.validRows() + " of " + result.totalRows() + " rows valid");
        return saved;
    }

    @Transactional(readOnly = true)
    public ClaimBatch get(Long id) {
        return batches.findById(id).orElseThrow(() -> new BatchNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ValidationError> errorsOf(Long batchId) {
        get(batchId);
        return validationErrors.findByBatch_IdOrderByRowNoAscIdAsc(batchId);
    }

    private static ValidationError toEntity(ClaimBatch batch, FieldError error) {
        ValidationError entity = new ValidationError();
        entity.setBatch(batch);
        entity.setRowNo(error.rowNo());
        entity.setFieldName(truncate(error.field(), 50));
        entity.setErrorCode(truncate(error.code(), 50));
        entity.setMessage(truncate(error.message(), 500));
        entity.setSuggestion(error.suggestion() == null ? null : truncate(error.suggestion(), 500));
        return entity;
    }

    private static String displayName(String originalFileName) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(originalFileName == null ? "" : originalFileName));
        return truncate(name == null || name.isBlank() ? "upload" : name, 255);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
