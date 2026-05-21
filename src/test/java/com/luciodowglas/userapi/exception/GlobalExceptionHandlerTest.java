package com.luciodowglas.userapi.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luciodowglas.userapi.entity.User;
import com.luciodowglas.userapi.security.JwtAuthFilter;

import br.com.luciodowglas.openapi.model.CreateUserRequest;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import jakarta.validation.Valid;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.StubController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.StubController.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtAuthFilter jwtAuthFilter;

    // ─────────────────────────────────────────────────────────────────────────
    // Stub controller — each endpoint triggers a specific exception path
    // ─────────────────────────────────────────────────────────────────────────

    @RestController
    static class StubController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new UserNotFoundException(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new UserAlreadyExistsException("alice@test.com");
        }

        @GetMapping("/test/project-conflict")
        void projectConflict() {
            UUID p = UUID.fromString("11111111-0000-0000-0000-000000000001");
            UUID u = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
            throw new ProjectAlreadyExistsException(p, u);
        }

        @GetMapping("/test/optimistic-lock")
        void optimisticLock() {
            throw new ObjectOptimisticLockingFailureException(User.class, "some-id");
        }

        @GetMapping("/test/external-integration")
        void externalIntegration() {
            throw new ExternalIntegrationException("upstream down", new RuntimeException("timeout"));
        }

        @GetMapping("/test/rate-limit")
        void rateLimit() {
            throw RequestNotPermitted.createRequestNotPermitted(
                    io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("test"));
        }

        @GetMapping("/test/generic")
        void generic() {
            throw new RuntimeException("internal detail should not leak");
        }

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("forbidden");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody CreateUserRequest req) {}

        @GetMapping("/test/missing-param")
        void missingParam(@RequestParam String required) {}

        @GetMapping("/test/method-only-get")
        void methodOnlyGet() {}
    }

    // ── 404 ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void userNotFound_isReported_asResourceNotFoundWith404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void duplicateUser_isReported_asConflictWith409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Resource conflict"))
                .andExpect(jsonPath("$.detail").value("User already exists with email: alice@test.com"));
    }

    @Test
    @WithMockUser
    void duplicateProjectLink_isReported_asConflictWith409() throws Exception {
        mockMvc.perform(get("/test/project-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    void concurrentModification_isReported_asConflictWith409AndRetryHint() throws Exception {
        mockMvc.perform(get("/test/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent modification"))
                .andExpect(jsonPath("$.detail").value("The resource was modified by another request. Please retry."));
    }

    // ── 400 Validation ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void invalidRequestFields_areReported_withFieldLevelErrorsIn400() throws Exception {
        String body = """
                {"name":"Alice","email":"not-an-email","password":"short"}
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    // ── 502 ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void externalServiceDown_isReported_asGatewayErrorWith502() throws Exception {
        mockMvc.perform(get("/test/external-integration"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("External service unavailable"));
    }

    // ── 429 ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void rateLimitExceeded_isReported_with429() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too many requests"));
    }

    // ── 500 — no detail leak ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void internalError_neverLeaksImplementationDetails() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    // ── 403 ───────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void accessDenied_isReported_with403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── 404 — rota inexistente ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void unknownRoute_isReported_asNotFoundWith404() throws Exception {
        mockMvc.perform(get("/test/this-endpoint-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Endpoint not found"));
    }

    // ── 405 — método HTTP errado ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void wrongHttpMethod_isReported_asMethodNotAllowedWith405() throws Exception {
        mockMvc.perform(delete("/test/method-only-get"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.title").value("Method not allowed"));
    }

    // ── 400 — JSON malformado ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void malformedJson_isReported_asBadRequestWith400() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Malformed request body"));
    }

    // ── 400 — query param obrigatório ausente ─────────────────────────────────

    @Test
    @WithMockUser
    void missingRequiredQueryParam_isReported_asBadRequestWith400() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Missing request parameter"))
                .andExpect(jsonPath("$.detail").value("Required parameter 'required' is missing."));
    }
}
