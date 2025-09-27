package org.padminisys.dto;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CiliumNetworkPolicyRequestValidationTest {

    @Inject
    Validator validator;

    @Test
    void testValidRequest_IPBased() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid IP-based request should have no violations");
    }

    @Test
    void testValidRequest_LabelBased() {
        // Given
        CiliumNetworkPolicyRequest request = createValidLabelBasedRequest();

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid label-based request should have no violations");
    }

    @Test
    void testInvalidRequest_BlankNamespace() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setNamespace("");

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace cannot be blank")));
    }

    @Test
    void testInvalidRequest_InvalidNamespacePattern() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setNamespace("INVALID-NAMESPACE");

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Namespace must be a valid DNS-1123 label")));
    }

    @Test
    void testInvalidRequest_EmptyLabels() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setLabels(new HashMap<>());

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Labels cannot be empty")));
    }

    @Test
    void testInvalidRequest_NullLabels() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setLabels(null);

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Labels cannot be empty")));
    }

    @Test
    void testInvalidNetworkRule_BothIPAndLabels() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule invalidRule = new CiliumNetworkPolicyRequest.NetworkRule();
        invalidRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        invalidRule.setIpAddresses(Arrays.asList("192.168.1.0/24"));
        
        Map<String, String> fromLabels = new HashMap<>();
        fromLabels.put("app", "web");
        invalidRule.setFromLabels(fromLabels);
        
        request.setIngressRules(Arrays.asList(invalidRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("NetworkRule cannot have both IP addresses and labels")));
    }

    @Test
    void testInvalidNetworkRule_NoIPOrLabels() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule invalidRule = new CiliumNetworkPolicyRequest.NetworkRule();
        invalidRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        
        List<CiliumNetworkPolicyRequest.PortRule> ports = new ArrayList<>();
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(80);
        ports.add(portRule);
        invalidRule.setPorts(ports);
        
        request.setIngressRules(Arrays.asList(invalidRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("NetworkRule must specify either IP addresses or labels")));
    }

    @Test
    void testInvalidNetworkRule_InvalidCIDR() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule invalidRule = new CiliumNetworkPolicyRequest.NetworkRule();
        invalidRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        invalidRule.setIpAddresses(Arrays.asList("invalid-cidr", "192.168.1.300/24"));

        request.setIngressRules(Arrays.asList(invalidRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Invalid CIDR format")));
    }

    @Test
    void testInvalidNetworkRule_NullRuleType() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule invalidRule = new CiliumNetworkPolicyRequest.NetworkRule();
        invalidRule.setRuleType(null);
        invalidRule.setIpAddresses(Arrays.asList("192.168.1.0/24"));

        request.setIngressRules(Arrays.asList(invalidRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Rule type cannot be null")));
    }

    @Test
    void testInvalidPortRule_NullProtocol() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule rule = request.getIngressRules().get(0);
        
        CiliumNetworkPolicyRequest.PortRule invalidPortRule = new CiliumNetworkPolicyRequest.PortRule();
        invalidPortRule.setProtocol(null);
        invalidPortRule.setPort(80);
        
        rule.setPorts(Arrays.asList(invalidPortRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Protocol cannot be null")));
    }

    @Test
    void testInvalidPortRule_NullPort() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule rule = request.getIngressRules().get(0);
        
        CiliumNetworkPolicyRequest.PortRule invalidPortRule = new CiliumNetworkPolicyRequest.PortRule();
        invalidPortRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        invalidPortRule.setPort(null);
        
        rule.setPorts(Arrays.asList(invalidPortRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Port cannot be null")));
    }

    @Test
    void testValidRequest_WithPortRange() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        CiliumNetworkPolicyRequest.NetworkRule rule = request.getIngressRules().get(0);
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(8000);
        portRule.setEndPort(8999);
        
        rule.setPorts(Arrays.asList(portRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request with port range should have no violations");
    }

    @Test
    void testValidRequest_MixedRuleTypes() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        
        // Add egress rules
        CiliumNetworkPolicyRequest.NetworkRule egressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        egressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.EGRESS_ALLOW);
        
        Map<String, String> toLabels = new HashMap<>();
        toLabels.put("service", "database");
        egressRule.setToLabels(toLabels);
        
        request.setEgressRules(Arrays.asList(egressRule));
        
        // Add deny rules
        CiliumNetworkPolicyRequest.NetworkRule ingressDenyRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressDenyRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_DENY);
        ingressDenyRule.setIpAddresses(Arrays.asList("192.0.2.0/24"));
        
        request.setIngressDenyRules(Arrays.asList(ingressDenyRule));

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request with mixed rule types should have no violations");
    }

    @Test
    void testValidRequest_WithValidUserProvidedName() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setName("valid-policy-name");

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request with valid user-provided name should have no violations");
    }

    @Test
    void testValidRequest_WithoutName() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setName(null); // No name provided, should use auto-generation

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request without name should have no violations (auto-generation)");
    }

    @Test
    void testValidRequest_WithBlankName() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        request.setName(""); // Blank name, should fall back to auto-generation

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Valid request with blank name should have no violations (auto-generation fallback)");
    }

    @Test
    void testValidRequest_WithValidDNS1123Names() {
        // Test various valid DNS-1123 names
        String[] validNames = {
            "a",                    // Single character
            "test",                 // Simple name
            "test-policy",          // With dash
            "policy123",            // With numbers
            "123policy",            // Starting with number
            "a-b-c-d-e-f-g-h-i-j-k-l-m-n-o-p-q-r-s-t-u-v-w-x-y-z-1-2-3-4-5", // Long but valid (exactly 63 chars)
            "my-app-v2"             // Common pattern
        };

        for (String validName : validNames) {
            // Given
            CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
            request.setName(validName);

            // When
            Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.isEmpty(),
                "Valid DNS-1123 name '" + validName + "' (length: " + validName.length() + ") should have no violations");
        }
    }

    @Test
    void testInvalidRequest_WithInvalidDNS1123Names() {
        // Test various invalid DNS-1123 names
        String[] invalidNames = {
            "UPPERCASE",            // Uppercase not allowed
            "test_underscore",      // Underscore not allowed
            "test.dot",             // Dot not allowed
            "-start-dash",          // Cannot start with dash
            "end-dash-",            // Cannot end with dash
            "test space",           // Space not allowed
            "test@symbol",          // Special characters not allowed
            "test/slash",           // Slash not allowed
            "test\\backslash",      // Backslash not allowed
            "test:colon",           // Colon not allowed
            "a".repeat(64)          // Too long (64 characters, limit is 63)
        };

        for (String invalidName : invalidNames) {
            // Given
            CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
            request.setName(invalidName);

            // When
            Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

            // Then
            assertFalse(violations.isEmpty(),
                "Invalid DNS-1123 name '" + invalidName + "' should have violations");
            assertTrue(violations.stream().anyMatch(v ->
                v.getMessage().contains("Policy name must be a valid DNS-1123 label") ||
                v.getMessage().contains("Policy name must not exceed 63 characters")),
                "Should have validation error for name: " + invalidName + " - violations: " +
                violations.stream().map(ConstraintViolation::getMessage).toList());
        }
    }

    @Test
    void testInvalidRequest_WithMaxLengthExceeded() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        String tooLongName = "a".repeat(64); // 64 characters, exceeds limit of 63
        request.setName(tooLongName);

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v ->
            v.getMessage().contains("Policy name must not exceed 63 characters")),
            "Should have size validation error for too long name - violations: " +
            violations.stream().map(ConstraintViolation::getMessage).toList());
    }

    @Test
    void testValidRequest_WithMaxLengthName() {
        // Given
        CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
        String maxLengthName = "a".repeat(63); // Exactly 63 characters, should be valid
        request.setName(maxLengthName);

        // When
        Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(),
            "Valid DNS-1123 name with max length (63 chars) should have no violations");
    }

    @Test
    void testValidRequest_WithCommonPolicyNamePatterns() {
        // Test common real-world policy name patterns
        String[] commonPatterns = {
            "allow-ingress",
            "deny-egress",
            "web-to-db",
            "frontend-policy",
            "backend-access",
            "api-gateway-rules",
            "microservice-mesh",
            "tenant-isolation",
            "network-segmentation",
            "security-policy-v1"
        };

        for (String pattern : commonPatterns) {
            // Given
            CiliumNetworkPolicyRequest request = createValidIPBasedRequest();
            request.setName(pattern);

            // When
            Set<ConstraintViolation<CiliumNetworkPolicyRequest>> violations = validator.validate(request);

            // Then
            assertTrue(violations.isEmpty(),
                "Common policy name pattern '" + pattern + "' should be valid");
        }
    }

    // Helper methods
    private CiliumNetworkPolicyRequest createValidIPBasedRequest() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setNamespace("test-namespace");
        
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "dell_computers");
        request.setLabels(labels);
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        ingressRule.setIpAddresses(Arrays.asList("192.168.1.0/24", "10.0.0.0/8"));
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(443);
        ingressRule.setPorts(Arrays.asList(portRule));
        
        request.setIngressRules(Arrays.asList(ingressRule));
        
        return request;
    }

    private CiliumNetworkPolicyRequest createValidLabelBasedRequest() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setNamespace("test-namespace");
        
        Map<String, String> labels = new HashMap<>();
        labels.put("serial", "GB7YP");
        request.setLabels(labels);
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        
        Map<String, String> fromLabels = new HashMap<>();
        fromLabels.put("serial", "GB7YH");
        ingressRule.setFromLabels(fromLabels);
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.UDP);
        portRule.setPort(53);
        ingressRule.setPorts(Arrays.asList(portRule));
        
        request.setIngressRules(Arrays.asList(ingressRule));
        
        return request;
    }
}