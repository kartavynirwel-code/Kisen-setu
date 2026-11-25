package com.kisansetu.claim;

import com.kisansetu.auth.CurrentUser;
import com.kisansetu.domain.enums.ClaimStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService service;

    @GetMapping
    public PagedClaims list(@RequestParam(required = false) ClaimStatus status,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "50") int size) {
        return service.list(status, page, size);
    }

    /** BANK_ADMIN only (enforced in SecurityConfig). */
    @PatchMapping("/{id}/status")
    public ClaimView updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.updateStatus(id, request.status(), request.rejectionReason(), CurrentUser.id(jwt));
    }
}
