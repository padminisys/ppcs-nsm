package org.padminisys.dto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.jboss.logging.Logger;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BasicDTOValidationTest {

    private static final Logger LOG = Logger.getLogger(BasicDTOValidationTest.class);

    @Inject
    Validator validator;

    // NamespaceRequest Tests
    @Test
    void testNamespaceRequest_Valid() {
        LOG.info("Testing NamespaceRequest validation: Valid DNS-1123 compliant namespace name");
        
        // Given
        String namespaceName = "test-namespace";
        NamespaceRequest request = new NamespaceRequest(namespaceName);
        LOG.info("Testing namespace name: '" + namespaceName + "' - Expected: VALID (DNS-1123 compliant)");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid namespace request should have no violations");
        LOG.info("✓ Validation PASSED: No constraint violations found for valid namespace name");
    }

    @Test
    void testNamespaceRequest_BlankName() {
        LOG.info("Testing NamespaceRequest validation: Blank namespace name (should fail @NotBlank)");
        
        // Given
        String namespaceName = "";
        NamespaceRequest request = new NamespaceRequest(namespaceName);
        LOG.info("Testing namespace name: '" + namespaceName + "' - Expected: INVALID (@NotBlank violation)");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name cannot be blank")));
        LOG.info("✓ Validation PASSED: Found expected @NotBlank constraint violation for blank namespace name");
        violations.forEach(v -> LOG.info("  - Violation: " + v.getMessage()));
    }

    @Test
    void testNamespaceRequest_NullName() {
        LOG.info("Testing NamespaceRequest validation: Null namespace name (should fail @NotBlank)");
        
        // Given
        NamespaceRequest request = new NamespaceRequest(null);
        LOG.info("Testing namespace name: null - Expected: INVALID (@NotBlank violation)");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name cannot be blank")));
        LOG.info("✓ Validation PASSED: Found expected @NotBlank constraint violation for null namespace name");
        violations.forEach(v -> LOG.info("  - Violation: " + v.getMessage()));
    }

    @Test
    void testNamespaceRequest_InvalidPattern_Uppercase() {
        LOG.info("Testing NamespaceRequest validation: Uppercase characters (should fail DNS-1123 pattern)");
        
        // Given
        String namespaceName = "INVALID-NAMESPACE";
        NamespaceRequest request = new NamespaceRequest(namespaceName);
        LOG.info("Testing namespace name: '" + namespaceName + "' - Expected: INVALID (uppercase not allowed in DNS-1123)");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
        LOG.info("✓ Validation PASSED: Found expected DNS-1123 pattern violation for uppercase namespace name");
        violations.forEach(v -> LOG.info("  - Violation: " + v.getMessage()));
    }

    @Test
    void testNamespaceRequest_InvalidPattern_SpecialChars() {
        LOG.info("Testing NamespaceRequest validation: Special characters (should fail DNS-1123 pattern)");
        
        // Given
        String namespaceName = "test_namespace@123";
        NamespaceRequest request = new NamespaceRequest(namespaceName);
        LOG.info("Testing namespace name: '" + namespaceName + "' - Expected: INVALID (special chars not allowed in DNS-1123)");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
        LOG.info("✓ Validation PASSED: Found expected DNS-1123 pattern violation for special characters");
        violations.forEach(v -> LOG.info("  - Violation: " + v.getMessage()));
    }

    @Test
    void testNamespaceRequest_InvalidPattern_StartWithHyphen() {
        // Given
        NamespaceRequest request = new NamespaceRequest("-test-namespace");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
    }

    @Test
    void testNamespaceRequest_InvalidPattern_EndWithHyphen() {
        // Given
        NamespaceRequest request = new NamespaceRequest("test-namespace-");

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
    }

    @Test
    void testNamespaceRequest_ValidEdgeCases() {
        LOG.info("Testing NamespaceRequest validation: Various valid DNS-1123 edge cases");
        
        // Test various valid namespace names
        String[] validNames = {
            "a",
            "test",
            "test-namespace",
            "test123",
            "123test",
            "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z-1-2-3-4-5"
        };

        for (String name : validNames) {
            LOG.info("Testing valid edge case: '" + name + "' (length: " + name.length() + ")");
            NamespaceRequest request = new NamespaceRequest(name);
            Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Valid namespace name '" + name + "' should have no violations");
            LOG.info("  ✓ PASSED: No violations for valid name '" + name + "'");
        }
        LOG.info("✓ All valid edge cases passed DNS-1123 validation");
    }

    // ServiceAccountRequest Tests
    @Test
    void testServiceAccountRequest_Valid() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", "test-sa");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid service account request should have no violations");
    }

    @Test
    void testServiceAccountRequest_BlankNamespace() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("", "test-sa");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name cannot be blank")));
    }

    @Test
    void testServiceAccountRequest_BlankName() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", "");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Service account name cannot be blank")));
    }

    @Test
    void testServiceAccountRequest_NullNamespace() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest(null, "test-sa");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name cannot be blank")));
    }

    @Test
    void testServiceAccountRequest_NullName() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", null);

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Service account name cannot be blank")));
    }

    @Test
    void testServiceAccountRequest_InvalidNamespacePattern() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("INVALID-NAMESPACE", "test-sa");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
    }

    @Test
    void testServiceAccountRequest_InvalidServiceAccountPattern() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", "INVALID-SA");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Service account name must be a valid DNS-1123 label")));
    }

    @Test
    void testServiceAccountRequest_BothInvalid() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("INVALID-NAMESPACE", "INVALID-SA");

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size(), "Should have violations for both namespace and service account name");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name must be a valid DNS-1123 label")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Service account name must be a valid DNS-1123 label")));
    }

    @Test
    void testServiceAccountRequest_ValidEdgeCases() {
        // Test various valid combinations
        String[][] validCombinations = {
            {"a", "b"},
            {"test", "sa"},
            {"test-namespace", "test-sa"},
            {"ns123", "sa123"},
            {"123ns", "123sa"},
            {"very-long-namespace-name-with-many-hyphens", "very-long-service-account-name"}
        };

        for (String[] combination : validCombinations) {
            ServiceAccountRequest request = new ServiceAccountRequest(combination[0], combination[1]);
            Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), 
                "Valid combination '" + combination[0] + "'/'" + combination[1] + "' should have no violations");
        }
    }

    @Test
    void testServiceAccountRequest_DefaultConstructor() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest();

        // When
        Set<ConstraintViolation<ServiceAccountRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size(), "Should have violations for both null namespace and service account name");
    }

    @Test
    void testNamespaceRequest_DefaultConstructor() {
        // Given
        NamespaceRequest request = new NamespaceRequest();

        // When
        Set<ConstraintViolation<NamespaceRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace name cannot be blank")));
    }
}