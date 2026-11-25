package com.kisansetu.upload;

import com.kisansetu.domain.Farmer;
import com.kisansetu.repository.ClaimRepository;
import com.kisansetu.repository.FarmerRepository;
import com.kisansetu.security.AadhaarHasher;
import com.kisansetu.validation.ClaimRow;
import com.kisansetu.validation.FieldError;
import com.kisansetu.validation.Fields;
import com.kisansetu.validation.StoredClaim;
import com.kisansetu.validation.StoredConflictChecker;
import com.kisansetu.validation.StoredFarmer;
import com.kisansetu.validation.rules.AadhaarRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Loads what the database already holds for the rows in this file and runs {@link StoredConflictChecker}. */
@Component
@RequiredArgsConstructor
public class StoredConflictService {

    private final FarmerRepository farmers;
    private final ClaimRepository claims;
    private final AadhaarHasher hasher;

    @Transactional(readOnly = true)
    public List<FieldError> check(List<ClaimRow> rows) {
        Set<String> kccAccounts = rows.stream().map(r -> r.get(Fields.KCC_ACCOUNT_NO))
                .filter(s -> !s.isBlank()).collect(Collectors.toSet());
        Set<String> hashes = rows.stream().map(r -> r.get(Fields.AADHAAR))
                .filter(AadhaarRule::isWellFormed).map(hasher::hash).collect(Collectors.toSet());

        Map<Long, Farmer> found = new HashMap<>();
        if (!kccAccounts.isEmpty()) {
            farmers.findByKccAccountNoIn(kccAccounts).forEach(f -> found.put(f.getId(), f));
        }
        if (!hashes.isEmpty()) {
            farmers.findByAadhaarHashIn(hashes).forEach(f -> found.put(f.getId(), f));
        }
        if (found.isEmpty()) {
            return List.of();
        }

        List<StoredFarmer> storedFarmers = found.values().stream()
                .map(f -> new StoredFarmer(f.getKccAccountNo(), f.getAadhaarHash())).toList();
        List<StoredClaim> storedClaims = claims.findByFarmer_IdIn(found.keySet()).stream()
                .map(c -> new StoredClaim(c.getFarmer().getKccAccountNo(), c.getClaimType().name(),
                        c.getFinancialYear(), c.getBatch().getId()))
                .toList();
        return StoredConflictChecker.check(rows, hasher::hash, storedFarmers, storedClaims);
    }
}
