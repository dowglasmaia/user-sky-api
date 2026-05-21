package com.luciodowglas.userapi.integration.project;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;
import com.luciodowglas.userapi.exception.ProjectNotFoundException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for ExternalProjectGateway.
 *
 * Note: @Retry and @CircuitBreaker are AOP-driven and NOT active without Spring context.
 * These tests cover the gateway's own delegation and exception-translation logic only.
 */
@ExtendWith(MockitoExtension.class)
class ExternalProjectGatewayTest {

    @Mock private ExternalProjectClient client;

    private ExternalProjectGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new ExternalProjectGateway(client, new SimpleMeterRegistry());
    }

    @Test
    void gateway_returnsProject_whenClientFindsIt() {
        // given
        var dto = anExternalProjectDto();
        when(client.findById(PROJECT_ID)).thenReturn(Optional.of(dto));

        // when
        var result = gateway.findById(PROJECT_ID);

        // then
        assertThat(result.id()).isEqualTo(PROJECT_ID);
        assertThat(result.name()).isEqualTo("Atlas");
    }

    @Test
    void gateway_throwsProjectNotFoundException_whenClientReturnsEmpty() {
        // given
        when(client.findById(PROJECT_ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> gateway.findById(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining(PROJECT_ID.toString());
    }

    @Test
    void gateway_projectNotFoundFallback_doesNotWrapInIntegrationException() {
        // given — client returns empty, orElseThrow produces ProjectNotFoundException
        when(client.findById(PROJECT_ID)).thenReturn(Optional.empty());

        // when / then — must be ProjectNotFoundException, NOT ExternalIntegrationException
        assertThatThrownBy(() -> gateway.findById(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class)
                .isNotInstanceOf(ExternalIntegrationException.class);
    }
}
