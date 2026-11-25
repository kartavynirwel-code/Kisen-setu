package com.kisansetu.config;

import com.kisansetu.security.AadhaarHasher;
import com.kisansetu.security.FieldCipher;
import com.kisansetu.security.Keys;
import com.kisansetu.security.SlidingWindowLimiter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Secrets come from the environment only. The application refuses to start if one is missing or too short. */
@Configuration
public class CryptoConfig {

    @Bean
    FieldCipher fieldCipher(@Value("${app.security.encryption-key}") String key) {
        return new FieldCipher(Keys.decode(key, 32, "APP_ENCRYPTION_KEY"));
    }

    @Bean
    AadhaarHasher aadhaarHasher(@Value("${app.security.hmac-key}") String key) {
        return new AadhaarHasher(Keys.decode(key, 32, "APP_HMAC_KEY"));
    }

    /** 5 failed logins per username+address in 15 minutes. */
    @Bean
    SlidingWindowLimiter loginLimiter(Clock clock) {
        return new SlidingWindowLimiter(5, Duration.ofMinutes(15), clock);
    }

    /** 10 status requests per phone number per hour. */
    @Bean
    SlidingWindowLimiter webhookLimiter(Clock clock) {
        return new SlidingWindowLimiter(10, Duration.ofHours(1), clock);
    }
}
