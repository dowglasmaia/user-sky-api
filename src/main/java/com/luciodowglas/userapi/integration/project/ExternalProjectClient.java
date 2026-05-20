package com.luciodowglas.userapi.integration.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;

/**
 * Low-level HTTP contract for the projects-api service.
 *
 * <p>Implementations only perform the network call and map the payload; cross-cutting
 * concerns (resilience, exception translation, metrics) belong to
 * {@link ExternalProjectGateway}.</p>
 */
public interface ExternalProjectClient {

    Optional<ExternalProjectDto> findById(UUID projectId);

    List<ExternalProjectDto> findAll();
}
