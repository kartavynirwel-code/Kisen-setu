package com.kisansetu.upload;

import com.kisansetu.domain.ClaimBatch;
import java.time.LocalDateTime;

public record BatchSummary(Long id, String fileName, String status, int totalRows, int validRows, int errorRows,
                           LocalDateTime createdAt) {

    static BatchSummary from(ClaimBatch batch) {
        return new BatchSummary(batch.getId(), batch.getFileName(), batch.getStatus().name(), batch.getTotalRows(),
                batch.getValidRows(), batch.getErrorRows(), batch.getCreatedAt());
    }
}
