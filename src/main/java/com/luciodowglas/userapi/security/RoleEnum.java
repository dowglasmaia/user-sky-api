package com.luciodowglas.userapi.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Single source of truth for authorization roles in the application.
 *
 * <p>Persisted as a string ({@code @Enumerated(EnumType.STRING)}) and used by
 * Spring Security through {@link #authority()}. Adding a new role here is the
 * only place needed — controllers and security configuration consume the enum
 * directly, preventing role-name typos at compile time.</p>
 */
public enum RoleEnum {

    ROLE_USER,
    ROLE_ADMIN;

    public GrantedAuthority authority() {
        return new SimpleGrantedAuthority(name());
    }

    public String shortName() {
        return name().replace("ROLE_", "");
    }
}
