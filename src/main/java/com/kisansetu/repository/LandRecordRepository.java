package com.kisansetu.repository;

import com.kisansetu.domain.LandRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandRecordRepository extends JpaRepository<LandRecord, Long> {

    List<LandRecord> findByFarmer_IdIn(Collection<Long> farmerIds);
}
