package com.kisansetu.auth;

import com.kisansetu.domain.AppUser;
import com.kisansetu.domain.enums.Role;
import com.kisansetu.repository.AppUserRepository;
import com.kisansetu.security.PasswordPolicy;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first BANK_ADMIN from ADMIN_USERNAME / ADMIN_PASSWORD, but only while no user exists at all. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminSeeder(AppUserRepository users, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.username:}") String username,
                       @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            log.warn("No users exist and ADMIN_USERNAME / ADMIN_PASSWORD are not set: nobody will be able to log in");
            return;
        }
        PasswordPolicy.check(password); // refuse to start with a weak first password
        AppUser admin = new AppUser();
        admin.setUsername(username.trim().toLowerCase(Locale.ROOT));
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.BANK_ADMIN);
        users.save(admin);
        log.info("Created first admin user '{}'", admin.getUsername());
    }
}
