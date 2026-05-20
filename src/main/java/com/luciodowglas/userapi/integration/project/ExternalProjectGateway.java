package com.luciodowglas.userapi.integration.project;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;
import com.luciodowglas.userapi.integration.project.dto.ExternalProjectDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Anti-corruption layer in front of projects-api.
 *
 * <p>Adds resilience ({@link Retry} + {@link CircuitBreaker}), records metrics,
 * and translates infrastructure failures into domain exceptions so callers
 * never depend on {@code WebClient} types.</p>
 */
@Component
public class ExternalProjectGateway {

    private static final Logger log = LoggerFactory.getLogger(ExternalProjectGateway.class);
    private static final String INSTANCE = "projectsApi";

    private final ExternalProjectClient client;
    private final Timer lookupTimer;
    private final Counter lookupFailureCounter;

    public ExternalProjectGateway(ExternalProjectClient client, MeterRegistry meterRegistry) {
        this.client = client;
        this.lookupTimer = Timer.builder("external.project.lookup.duration")
                .description("Time spent resolving an external project")
                .register(meterRegistry);
        this.lookupFailureCounter = Counter.builder("external.project.lookup.failure")
                .description("Number of failed external project lookups")
                .register(meterRegistry);
    }

    /**
     * Resolves an external project, applying retry and circuit-breaker policies.
     *
     * @throws ProjectNotFoundException     when the project does not exist upstream
     * @throws ExternalIntegrationException when projects-api is unreachable or failing
     */
    @Retry(name = INSTANCE)
    @CircuitBreaker(name = INSTANCE, fallbackMethod = "findByIdFallback")
    public ExternalProjectDto findById(UUID projectId) {
        long start = System.nanoTime();
        try {
            return client.findById(projectId)
                    .orElseThrow(() -> new ProjectNotFoundException(projectId));
        } finally {
            lookupTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @SuppressWarnings("unused")
    private ExternalProjectDto findByIdFallback(UUID projectId, ProjectNotFoundException ex) {
        // A missing project is a legitimate business outcome — do not swallow it.
        throw ex;
    }

    @SuppressWarnings("unused")
    private ExternalProjectDto findByIdFallback(UUID projectId, Throwable ex) {
        lookupFailureCounter.increment();
        log.error("external_project_lookup_failed projectId={} reason={}", projectId, ex.toString());
        throw new ExternalIntegrationException(
                "projects-api is currently unavailable for project " + projectId, ex);
    }
}
