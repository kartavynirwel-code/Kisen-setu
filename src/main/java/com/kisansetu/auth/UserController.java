package com.kisansetu.auth;

import com.kisansetu.claim.InvalidRequestException;
import com.kisansetu.domain.AppUser;
import com.kisansetu.repository.AppUserRepository;
import com.kisansetu.security.PasswordPolicy;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Creating users is a BANK_ADMIN-only action (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @PostMapping
    @Transactional
    public ResponseEntity<UserView> create(@Valid @RequestBody UserRequest request, @AuthenticationPrincipal Jwt jwt) {
        try {
            PasswordPolicy.check(request.password());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        String username = request.username().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw new ConflictException("That username is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        AppUser saved = users.save(user);

        audit.record(CurrentUser.id(jwt), "USER_CREATED", "app_user", saved.getId(), "role " + saved.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserView(saved.getId(), saved.getUsername(), saved.getRole().name()));
    }
}
