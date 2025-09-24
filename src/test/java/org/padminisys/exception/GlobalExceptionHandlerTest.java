package org.padminisys.exception;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class GlobalExceptionHandlerTest {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandlerTest.class);
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleConstraintViolationException() {
        LOG.info("TEST: GlobalExceptionHandler - ConstraintViolationException with multiple violations");
        LOG.info("Scenario: Bean validation fails with multiple field violations, should return 400 with detailed error");
        
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("name");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("cannot be blank");
        violations.add(violation1);
        
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("namespace");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be valid DNS-1123 label");
        violations.add(violation2);
        
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
        LOG.info("Input: ConstraintViolationException with 2 violations: name + namespace");

        // When
        LOG.info("Executing: exceptionHandler.toResponse() for validation exception");
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("Validation failed"));
        assertTrue(errorResponse.message.contains("name: cannot be blank") ||
                  errorResponse.message.contains("namespace: must be valid DNS-1123 label"));
        assertEquals(400, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
        LOG.info("✓ Exception handling PASSED: Status=400, ErrorCode=VALIDATION_ERROR");
        LOG.info("  - Error message: " + errorResponse.message);
    }

    @Test
    void testHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("BAD_REQUEST", errorResponse.errorCode);
        assertEquals("Invalid argument provided", errorResponse.message);
        assertEquals(400, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
    }

    @Test
    void testHandleRuntimeExceptionWithDoesNotExist() {
        LOG.info("TEST: GlobalExceptionHandler - RuntimeException with 'does not exist' pattern");
        LOG.info("Scenario: Business logic error (resource not found), should return 400 with specific message");
        
        // Given
        RuntimeException exception = new RuntimeException("Namespace 'test-namespace' does not exist");
        LOG.info("Input: RuntimeException with message containing 'does not exist'");

        // When
        LOG.info("Executing: exceptionHandler.toResponse() for business logic exception");
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("BAD_REQUEST", errorResponse.errorCode);
        assertEquals("Namespace 'test-namespace' does not exist", errorResponse.message);
        assertEquals(400, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
        LOG.info("✓ Exception handling PASSED: Status=400, ErrorCode=BAD_REQUEST");
        LOG.info("  - Specific business error message preserved: " + errorResponse.message);
    }

    @Test
    void testHandleGenericRuntimeException() {
        // Given
        RuntimeException exception = new RuntimeException("Some generic runtime error");

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("An unexpected error occurred"));
        assertTrue(errorResponse.message.contains("Some generic runtime error"));
        assertEquals(500, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception exception = new Exception("Generic exception occurred");

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("An unexpected error occurred"));
        assertTrue(errorResponse.message.contains("Generic exception occurred"));
        assertEquals(500, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
    }

    @Test
    void testHandleNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("Null pointer encountered");

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("An unexpected error occurred"));
        assertTrue(errorResponse.message.contains("Null pointer encountered"));
        assertEquals(500, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
    }

    @Test
    void testHandleExceptionWithNullMessage() {
        LOG.info("TEST: GlobalExceptionHandler - Exception with null message (null safety)");
        LOG.info("Scenario: Exception has null message, should handle gracefully without NPE");
        
        // Given
        RuntimeException exception = new RuntimeException((String) null);
        LOG.info("Input: RuntimeException with null message (testing null safety)");

        // When
        LOG.info("Executing: exceptionHandler.toResponse() for null message exception");
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("An unexpected error occurred"));
        assertEquals(500, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp > 0);
        LOG.info("✓ Null safety PASSED: Status=500, ErrorCode=INTERNAL_SERVER_ERROR");
        LOG.info("  - Graceful handling of null message: " + errorResponse.message);
    }

    @Test
    void testErrorResponseDefaultConstructor() {
        // Given & When
        GlobalExceptionHandler.ErrorResponse errorResponse = new GlobalExceptionHandler.ErrorResponse();

        // Then
        assertNotNull(errorResponse);
        assertTrue(errorResponse.timestamp > 0);
        assertNull(errorResponse.errorCode);
        assertNull(errorResponse.message);
        assertEquals(0, errorResponse.statusCode);
    }

    @Test
    void testErrorResponseParameterizedConstructor() {
        // Given
        long beforeTimestamp = System.currentTimeMillis();
        
        // When
        GlobalExceptionHandler.ErrorResponse errorResponse = new GlobalExceptionHandler.ErrorResponse(
            "TEST_ERROR", "Test error message", 400);
        
        long afterTimestamp = System.currentTimeMillis();

        // Then
        assertNotNull(errorResponse);
        assertEquals("TEST_ERROR", errorResponse.errorCode);
        assertEquals("Test error message", errorResponse.message);
        assertEquals(400, errorResponse.statusCode);
        assertTrue(errorResponse.timestamp >= beforeTimestamp);
        assertTrue(errorResponse.timestamp <= afterTimestamp);
    }

    @Test
    void testConstraintViolationExceptionWithEmptyViolations() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("Validation failed"));
        assertEquals(400, errorResponse.statusCode);
    }

    @Test
    void testConstraintViolationExceptionWithSingleViolation() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("cannot be blank");
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // When
        Response response = exceptionHandler.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        
        GlobalExceptionHandler.ErrorResponse errorResponse = (GlobalExceptionHandler.ErrorResponse) response.getEntity();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorCode);
        assertTrue(errorResponse.message.contains("Validation failed"));
        assertTrue(errorResponse.message.contains("name: cannot be blank"));
        assertEquals(400, errorResponse.statusCode);
    }
}