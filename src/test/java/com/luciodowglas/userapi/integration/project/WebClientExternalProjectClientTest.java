package com.luciodowglas.userapi.integration.project;

import static com.luciodowglas.userapi.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.luciodowglas.userapi.exception.ExternalIntegrationException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class WebClientExternalProjectClientTest {

    private MockWebServer mockWebServer;
    private WebClientExternalProjectClient client;
    private boolean serverShutdown = false;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        client = new WebClientExternalProjectClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (!serverShutdown) {
            mockWebServer.shutdown();
        }
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void externalProject_isRetrievedSuccessfully_whenServerResponds200() {
        // given
        String body = """
                {"id":"%s","name":"Atlas","description":"Customer platform"}
                """.formatted(PROJECT_ID);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));

        // when
        var result = client.findById(PROJECT_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(PROJECT_ID);
        assertThat(result.get().name()).isEqualTo("Atlas");
    }

    @Test
    void externalProject_returnsEmpty_whenServerResponds404() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        // when
        var result = client.findById(PROJECT_ID);

        // then — 404 maps to Optional.empty(), not an exception
        assertThat(result).isEmpty();
    }

    @Test
    void externalProject_throwsIntegrationException_whenServerResponds500() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // when / then
        assertThatThrownBy(() -> client.findById(PROJECT_ID))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    @Test
    void externalProject_throwsIntegrationException_whenServerIsUnreachable() throws IOException {
        // given — shut down the server before the call
        mockWebServer.shutdown();
        serverShutdown = true;

        // when / then
        assertThatThrownBy(() -> client.findById(PROJECT_ID))
                .isInstanceOf(ExternalIntegrationException.class);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void allProjects_areReturned_whenServerRespondsWithArray() {
        // given
        String body = """
                [
                  {"id":"%s","name":"Atlas","description":"Customer platform"},
                  {"id":"22222222-0000-0000-0000-000000000002","name":"Orion","description":"Logistics"}
                ]
                """.formatted(PROJECT_ID);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));

        // when
        var result = client.findAll();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void allProjects_returnsEmptyList_whenServerReturnsEmptyArray() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        // when
        var result = client.findAll();

        // then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void allProjects_throwsIntegrationException_whenServerFails() {
        // given
        mockWebServer.enqueue(new MockResponse().setResponseCode(502));

        // when / then
        assertThatThrownBy(() -> client.findAll())
                .isInstanceOf(ExternalIntegrationException.class);
    }
}
