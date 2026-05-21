package com.luciodowglas.userapi.exception;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String BASE_TYPE = "https://user-api/errors/";

    @ExceptionHandler({UserNotFoundException.class, ProjectNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex, HttpServletRequest request) {
        log.info("resource_not_found path={} message={}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage(), request);
    }

    @ExceptionHandler({UserAlreadyExistsException.class, ProjectAlreadyExistsException.class})
    public ProblemDetail handleConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("resource_conflict path={} message={}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Resource conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        log.warn("optimistic_lock_conflict path={} entity={}", request.getRequestURI(),
                ex.getPersistentClassName());
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was modified by another request. Please retry.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid.", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Constraint violation");
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, request);
    }

    @ExceptionHandler(ExternalIntegrationException.class)
    public ProblemDetail handleExternalIntegration(ExternalIntegrationException ex, HttpServletRequest request) {
        log.error("external_integration_error path={} message={}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "External service unavailable",
                "An upstream service failed to respond. Please try again later.", request);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail handleWebClientResponse(WebClientResponseException ex, HttpServletRequest request) {
        log.error("webclient_error path={} upstream_status={}", request.getRequestURI(), ex.getStatusCode());
        return problem(HttpStatus.BAD_GATEWAY, "External service error",
                "Upstream service returned an unexpected response.", request);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ProblemDetail handleRateLimit(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("rate_limit_exceeded path={}", request.getRequestURI());
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many requests",
                "Rate limit exceeded. Please slow down.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("[HTTP][REQUEST][FAILED] reason=unsupported_content_type path={} contentType={}",
                request.getRequestURI(), request.getContentType());
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "Set Content-Type: application/json (or omit body entirely).", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("bad_credentials path={}", request.getRequestURI());
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("access_denied path={}", request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action.", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("unhandled_exception path={}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred.", request);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + status.value()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("traceId", MDC.get("traceId"));
        problem.setProperty("correlationId", MDC.get("correlationId"));
        return problem;
    }

    record FieldViolation(String field, String message) {}
}
