package com.luciodowglas.userapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.repository.UserRepository;
import com.luciodowglas.userapi.security.RoleEnum;

import lombok.RequiredArgsConstructor;

/**
 * <b>FOR DEVELOPMENT AND TESTING ONLY — DO NOT USE IN PRODUCTION.</b>
 *
 * <p>Ensures the existence of a default administrator user in the database
 * immediately after application startup, allowing manual and automated tests
 * to exercise {@code ROLE_ADMIN}-restricted endpoints without requiring
 * external SQL scripts or manual database setup.</p>
 *
 * <p>The operation is <b>idempotent</b>: if the admin user already exists the
 * seeder logs a message and returns without making any changes, so restarting
 * the application never causes a conflict error.</p>
 *
 * <h3>Default credentials (dev only)</h3>
 * <pre>
 *   email    : admin@system.com
 *   password : admin123
 * </pre>
 *
 * <h3>How to disable</h3>
 * <ul>
 *   <li>Run the application with the {@code prod} Spring profile
 *       ({@code --spring.profiles.active=prod}) — this bean will not be
 *       instantiated at all.</li>
 *   <li>For CI environments that do not activate the {@code prod} profile,
 *       add a {@code @ConditionalOnProperty(name = "app.seed-admin.enabled",
 *       havingValue = "true")} guard if finer-grained control is needed.</li>
 * </ul>
 *
 * <h3>Production guidance</h3>
 * <p>In production the administrator account must be provisioned through a
 * dedicated Flyway migration ({@code V*__seed_admin_user.sql}) using an
 * externally generated, strong password stored in a secrets manager — never
 * through this class.</p>
 *
 * @see RoleEnum
 * @see UserRepository
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class AdminUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    static final String ADMIN_EMAIL    = "admin@system.com";
    static final String ADMIN_PASSWORD = "admin123";
    static final String ADMIN_NAME     = "System Administrator";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @SuppressWarnings("null")
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("[SEEDER][ADMIN_USER] already exists — skipping creation email={}", ADMIN_EMAIL);
            return;
        }

        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .name(ADMIN_NAME)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(RoleEnum.ROLE_ADMIN)
                .build();

        userRepository.save(admin);
        log.info("[SEEDER][ADMIN_USER] created email={} role=ROLE_ADMIN", ADMIN_EMAIL);
    }
}
