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