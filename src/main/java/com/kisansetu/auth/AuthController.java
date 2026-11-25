package com.kisansetu.auth;

import com.kisansetu.domain.AppUser;
import com.kisansetu.repository.AppUserRepository;
import com.kisansetu.security.SlidingWindowLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SlidingWindowLimiter limiter;
    private final AuditService audit;
    private final Clock clock;
    private final Duration tokenLifetime;
    /** Compared against when the username does not exist, so response time does not reveal valid usernames. */
    private final String dummyHash;

    public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                          @Qualifier("loginLimiter") SlidingWindowLimiter limiter, AuditService audit, Clock clock,
                          @Value("${app.security.jwt-ttl-minutes:60}") long ttlMinutes) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.limiter = limiter;
        this.audit = audit;
        this.clock = clock;
        this.tokenLifetime = Duration.ofMinutes(ttlMinutes);
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String limitKey = username + "|" + http.getRemoteAddr();
        if (limiter.isBlocked(limitKey)) {
            throw new TooManyRequestsException();
        }

        AppUser user = users.findByUsername(username).orElse(null);
        boolean passwordOk = passwordEncoder.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !passwordOk) {
            limiter.record(limitKey);
            throw new AuthenticationFailedException();
        }
        limiter.reset(limitKey);

        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(tokenLifetime))
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        audit.record(user.getId(), "LOGIN", "app_user", user.getId(), null);
        return new LoginResponse(token, "Bearer", tokenLifetime.toSeconds(), user.getUsername(), user.getRole().name());
    }
}
