package com.luciodowglas.userapi.integration.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;

import org.springframework.core.ParameterizedTypeReference;

/**
 * {@link ExternalProjectClient} backed by a real HTTP call to projects-api.
 *
 * <p>A 404 from the upstream is surfaced as an empty {@link Optional} so the
 * gateway can decide the proper domain error; any other failure becomes an
 * {@link ExternalIntegrationException} so resilience policies can react.</p>
 */
@Component
public class WebClientExternalProjectClient implements ExternalProjectClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientExternalProjectClient.class);

    private final WebClient projectsApiWebClient;

    public WebClientExternalProjectClient(WebClient projectsApiWebClient) {
        this.projectsApiWebClient = projectsApiWebClient;
    }

    @Override
    public Optional<ExternalProjectDto> findById(UUID projectId) {
        log.debug("external_project_lookup_started projectId={}", projectId);
        try {
            ExternalProjectDto dto = projectsApiWebClient.get()
                    .uri("/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(ExternalProjectDto.class)
                    .block();
            log.debug("external_project_lookup_succeeded projectId={}", projectId);
            return Optional.ofNullable(dto);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("external_project_not_found projectId={}", projectId);
                return Optional.empty();
            }
            throw new ExternalIntegrationException(
                    "projects-api returned status " + ex.getStatusCode() + " for project " + projectId, ex);
        } catch (RuntimeException ex) {
            throw new ExternalIntegrationException(
                    "Failed to reach projects-api for project " + projectId, ex);
        }
    }

    @Override
    public List<ExternalProjectDto> findAll() {
        try {
            List<ExternalProjectDto> projects = projectsApiWebClient.get()
                    .uri("/projects")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ExternalProjectDto>>() {})
                    .block();
            return projects == null ? List.of() : projects;
        } catch (RuntimeException ex) {
            throw new ExternalIntegrationException("Failed to list projects from projects-api", ex);
        }
    }
}
