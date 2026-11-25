package com.kisansetu.claim;

import com.kisansetu.auth.AuditService;
import com.kisansetu.domain.Claim;
import com.kisansetu.domain.enums.ClaimStatus;
import com.kisansetu.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClaimService {

    static final int MAX_PAGE_SIZE = 100;

    private final ClaimRepository claims;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PagedClaims list(ClaimStatus status, int page, int size) {
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Claim> result = status == null ? claims.findAll(request) : claims.findByStatus(status, request);
        return new PagedClaims(result.getContent().stream().map(ClaimView::from).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements());
    }

    /** Any status can be set from any other (a bank may need to correct a mistake); every change is audited. */
    @Transactional
    public ClaimView updateStatus(Long id, ClaimStatus status, String reason, Long userId) {
        String reasonToStore = ClaimStatusRules.reasonToStore(status, reason);
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        ClaimStatus previous = claim.getStatus();
        claim.setStatus(status);
        claim.setRejectionReason(reasonToStore);
        Claim saved = claims.saveAndFlush(claim);
        audit.record(userId, "CLAIM_STATUS_UPDATED", "claim", id, previous + " -> " + status);
        return ClaimView.from(saved);
    }
}
