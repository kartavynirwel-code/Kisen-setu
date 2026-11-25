package com.kisansetu.config;

import com.kisansetu.validation.RowNormalizer;
import com.kisansetu.validation.ValidationEngine;
import com.kisansetu.validation.ValidationEngineFactory;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    RowNormalizer rowNormalizer() {
        return new RowNormalizer();
    }

    @Bean
    ValidationEngine validationEngine(Clock clock) {
        return ValidationEngineFactory.create(clock);
    }
}
