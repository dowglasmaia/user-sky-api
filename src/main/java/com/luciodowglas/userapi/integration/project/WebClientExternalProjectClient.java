package com.luciodowglas.userapi.integration.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;

/**
 * HTTP client implementation for projects-api integration.
 */
@Component
public class WebClientExternalProjectClient implements ExternalProjectClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientExternalProjectClient.class);

    private static final String LOG_PREFIX = "[PROJECT][INTEGRATION]";

    private final WebClient projectsApiWebClient;

    public WebClientExternalProjectClient(WebClient projectsApiWebClient) {
        this.projectsApiWebClient = projectsApiWebClient;
    }

    @Override
    public Optional<ExternalProjectDto> findById(UUID projectId) {

        log.info(
                "{}[FIND_BY_ID][STARTED] projectId={}",
                LOG_PREFIX,
                projectId
        );

        try {

            ExternalProjectDto response = projectsApiWebClient.get()
                    .uri("/projects/{id}", projectId)
                    .retrieve()
                    .bodyToMono(ExternalProjectDto.class)
                    .block();

            log.info(
                    "{}[FIND_BY_ID][SUCCESS] projectId={}",
                    LOG_PREFIX,
                    projectId
            );

            return Optional.ofNullable(response);

        } catch (WebClientResponseException exception) {

            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {

                log.warn(
                        "{}[FIND_BY_ID][NOT_FOUND] projectId={} status={}",
                        LOG_PREFIX,
                        projectId,
                        exception.getRawStatusCode()
                );

                return Optional.empty();
            }

            log.error(
                    "{}[FIND_BY_ID][FAILED] projectId={} status={} responseBody={}",
                    LOG_PREFIX,
                    projectId,
                    exception.getRawStatusCode(),
                    exception.getResponseBodyAsString(),
                    exception
            );

            throw new ExternalIntegrationException(
                    String.format(
                            "projects-api returned status=%s for projectId=%s",
                            exception.getStatusCode(),
                            projectId
                    ),
                    exception
            );

        } catch (Exception exception) {

            log.error(
                    "{}[FIND_BY_ID][ERROR] projectId={} message={}",
                    LOG_PREFIX,
                    projectId,
                    exception.getMessage(),
                    exception
            );

            throw new ExternalIntegrationException(
                    String.format(
                            "Unexpected error while requesting projects-api for projectId=%s",
                            projectId
                    ),
                    exception
            );
        }
    }

    @Override
    public List<ExternalProjectDto> findAll() {

        log.info(
                "{}[FIND_ALL][STARTED]",
                LOG_PREFIX
        );

        try {

            List<ExternalProjectDto> response = projectsApiWebClient.get()
                    .uri("/projects")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ExternalProjectDto>>() {})
                    .block();

            List<ExternalProjectDto> projects = response == null
                    ? List.of()
                    : response;

            log.info(
                    "{}[FIND_ALL][SUCCESS] totalProjects={}",
                    LOG_PREFIX,
                    projects.size()
            );

            return projects;

        } catch (WebClientResponseException exception) {

            log.error(
                    "{}[FIND_ALL][FAILED] status={} responseBody={}",
                    LOG_PREFIX,
                    exception.getRawStatusCode(),
                    exception.getResponseBodyAsString(),
                    exception
            );

            throw new ExternalIntegrationException(
                    String.format(
                            "projects-api returned status=%s while listing projects",
                            exception.getStatusCode()
                    ),
                    exception
            );

        } catch (Exception exception) {

            log.error(
                    "{}[FIND_ALL][ERROR] message={}",
                    LOG_PREFIX,
                    exception.getMessage(),
                    exception
            );

            throw new ExternalIntegrationException(
                    "Unexpected error while listing projects from projects-api",
                    exception
            );
        }
    }
}
