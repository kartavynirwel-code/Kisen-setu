package com.kisansetu.repository;

import com.kisansetu.domain.Farmer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    Optional<Farmer> findByKccAccountNo(String kccAccountNo);

    List<Farmer> findByKccAccountNoIn(Collection<String> kccAccountNos);

    List<Farmer> findByAadhaarHashIn(Collection<String> aadhaarHashes);
}
