package com.luciodowglas.userapi.integration.project;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for {@code integration.projects-api.*} in application.yaml.
 * Registered via {@link com.luciodowglas.userapi.config.SecurityConfig} or
 * the main application class so Spring Boot generates metadata for IDE support.
 */
@ConfigurationProperties(prefix = "integration.projects-api")
public record ProjectsApiProperties(
        String baseUrl,
        int connectTimeoutMs,
        int responseTimeoutMs
) {
    public ProjectsApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("integration.projects-api.base-url must be configured");
        }
        if (connectTimeoutMs <= 0) connectTimeoutMs = 2000;
        if (responseTimeoutMs <= 0) responseTimeoutMs = 3000;
    }
}
