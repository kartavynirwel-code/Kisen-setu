package com.kisansetu.repository;

import com.kisansetu.domain.KycRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    List<KycRecord> findByFarmer_IdIn(Collection<Long> farmerIds);
}
