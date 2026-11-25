package com.kisansetu.repository;

import com.kisansetu.domain.ValidationError;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationErrorRepository extends JpaRepository<ValidationError, Long> {

    List<ValidationError> findByBatch_IdOrderByRowNoAscIdAsc(Long batchId);
}
