package com.kisansetu.status;

import com.kisansetu.domain.Claim;
import com.kisansetu.domain.Farmer;
import com.kisansetu.repository.ClaimRepository;
import com.kisansetu.repository.FarmerRepository;
import com.kisansetu.security.SlidingWindowLimiter;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Answers "STATUS <KCC account>" messages. The sender's number must match the mobile number registered for that
 * KCC account. A wrong account and a wrong number get the identical reply, so nobody can probe which accounts exist.
 */
@Service
public class StatusLookupService {

    private final FarmerRepository farmers;
    private final ClaimRepository claims;
    private final SlidingWindowLimiter limiter;

    public StatusLookupService(FarmerRepository farmers, ClaimRepository claims,
                               @Qualifier("webhookLimiter") SlidingWindowLimiter limiter) {
        this.farmers = farmers;
        this.claims = claims;
        this.limiter = limiter;
    }

    @Transactional(readOnly = true)
    public String reply(String from, String body) {
        String senderKey = PhoneNumbers.last10(from);
        if (!limiter.tryAcquire(senderKey.isEmpty() ? "unknown" : senderKey)) {
            return StatusMessages.tooManyRequests();
        }
        Optional<String> kcc = StatusCommandParser.kccAccount(body);
        if (kcc.isEmpty()) {
            return StatusMessages.help();
        }
        Optional<Farmer> farmer = farmers.findByKccAccountNo(kcc.get());
        if (farmer.isEmpty() || !PhoneNumbers.sameNumber(from, farmer.get().getMobile())) {
            return StatusMessages.notFound();
        }
        List<ClaimStatusLine> lines = claims.findByFarmer_IdOrderByFinancialYearDescIdDesc(farmer.get().getId()).stream()
                .map(StatusLookupService::line).toList();
        return StatusMessages.status(kcc.get(), lines);
    }

    private static ClaimStatusLine line(Claim claim) {
        return new ClaimStatusLine(claim.getClaimType().name(), claim.getFinancialYear(), claim.getStatus().name(),
                claim.getRejectionReason());
    }
}
