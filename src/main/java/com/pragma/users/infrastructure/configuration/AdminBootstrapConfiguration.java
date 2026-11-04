package com.pragma.users.infrastructure.configuration;

import com.pragma.users.domain.model.UserRole;
import com.pragma.users.infrastructure.out.jpa.entity.UserEntity;
import com.pragma.users.infrastructure.out.jpa.repository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

/**
 * Seeds a single bootstrap ADMINISTRATOR on startup when one is configured and none exists yet.
 * There is no HU to create an admin, yet creating owners requires an authenticated admin.
 * The admin password is NEVER hardcoded: it must be provided via {@code APP_ADMIN_PASSWORD}
 * ({@code app.admin.password}). When it is absent, seeding is skipped so the repository ships
 * no credentials. Idempotent: it never duplicates the admin on subsequent restarts.
 */
@Configuration
public class AdminBootstrapConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapConfiguration.class);

    @Bean
    public CommandLineRunner adminSeeder(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.name:System}") String name,
            @Value("${app.admin.last-name:Administrator}") String lastName,
            @Value("${app.admin.document-number:1000000000}") String documentNumber,
            @Value("${app.admin.phone:+573000000000}") String phone,
            @Value("${app.admin.email:admin@plazoleta.com}") String email,
            @Value("${app.admin.password:}") String password) {
        return args -> {
            if (password == null || password.isBlank()) {
                LOGGER.warn("app.admin.password is not set; skipping bootstrap administrator seeding.");
                return;
            }
            if (userRepository.existsByEmail(email)) {
                return;
            }
            UserEntity admin = new UserEntity();
            admin.setName(name);
            admin.setLastName(lastName);
            admin.setDocumentNumber(documentNumber);
            admin.setPhone(phone);
            admin.setBirthDate(LocalDate.of(1990, 1, 1));
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRoleId(UserRole.ADMINISTRATOR.getId());
            userRepository.save(admin);
            LOGGER.info("Bootstrap administrator seeded: {}", email);
        };
    }
}
