package com.kisansetu.repository;

import com.kisansetu.domain.Claim;
import com.kisansetu.domain.enums.ClaimStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @EntityGraph(attributePaths = "farmer")
    List<Claim> findByFarmer_IdIn(Collection<Long> farmerIds);

    List<Claim> findByFarmer_IdOrderByFinancialYearDescIdDesc(Long farmerId);

    @Override
    @EntityGraph(attributePaths = "farmer")
    Page<Claim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "farmer")
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
}
