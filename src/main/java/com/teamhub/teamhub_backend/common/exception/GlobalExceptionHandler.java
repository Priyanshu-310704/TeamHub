package com.teamhub.teamhub_backend.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // CLIENT DISCONNECTION HANDLERS (graceful, no stack trace pollution)
    // =========================================================================

    /**
     * Handles AsyncRequestNotUsableException - thrown when the client disconnects
     * during async/streaming response processing. This is an expected condition
     * when AI responses take longer than the client's patience or timeout.
     *
     * Spring MVC throws this when attempting to write to a response whose
     * underlying connection has been aborted (ClientAbortException).
     *
     * We return null because there is no client to send a response to.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ProblemDetail handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        log.info("[AI-STREAM] Client disconnected during async response processing: {}",
                ex.getMessage() != null ? ex.getMessage() : "connection aborted");
        // No response can be sent - the client is gone
        return null;
    }

    // =========================================================================
    // BUSINESS EXCEPTION HANDLERS (unchanged)
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Resource Conflict");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ProblemDetail handleForbidden(ForbiddenAccessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        problem.setTitle("Authentication Failed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request body");
        problem.setTitle("Validation Error");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // =========================================================================
    // AI-SPECIFIC ERROR HANDLERS
    // =========================================================================

    @ExceptionHandler(org.springframework.ai.retry.NonTransientAiException.class)
    public ProblemDetail handleAiNonTransientError(org.springframework.ai.retry.NonTransientAiException ex) {
        log.error("[AI-ERROR] Non-transient AI error (auth/config): {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI service is temporarily unavailable. Please try again later."
        );
        problem.setTitle("AI Service Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // =========================================================================
    // GENERIC FALLBACK
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        // Check if the root cause is a client abort (can be nested)
        if (isClientAbort(ex)) {
            log.info("[AI-STREAM] Client disconnected (nested abort): {}", ex.getMessage());
            return null;
        }

        log.error("Unhandled exception occurred: ", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again."
        );
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Walks the exception cause chain looking for a ClientAbortException.
     */
    private boolean isClientAbort(Throwable ex) {
        Throwable current = ex;
        int depth = 0;
        while (current != null && depth < 10) {
            String name = current.getClass().getName();
            if (name.contains("ClientAbortException") || name.contains("BrokenPipeException")
                    || name.contains("ConnectionResetException")) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
