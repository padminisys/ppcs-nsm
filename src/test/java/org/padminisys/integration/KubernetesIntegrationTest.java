package org.padminisys.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.padminisys.dto.*;
import org.padminisys.service.KubernetesService;
import org.padminisys.service.CiliumNetworkPolicyService;

import java.time.Instant;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Integration tests that verify the complete flow from REST API to Kubernetes service.
 * These tests ensure that the API contracts are maintained and that all components
 * work together correctly.
 */
@QuarkusTest
class KubernetesIntegrationTest {

    @InjectMock
    KubernetesService kubernetesService;

    @InjectMock
    CiliumNetworkPolicyService ciliumNetworkPolicyService;

    @BeforeEach
    void setUp() {
        Mockito.reset(kubernetesService);
        Mockito.reset(ciliumNetworkPolicyService);
    }

    @Test
    void testCompleteNamespaceCreationFlow() {
        // Given
        NamespaceResponse mockResponse = new NamespaceResponse(
                "integration-test-ns",
                "CREATED",
                Instant.now(),
                "Namespace created successfully"
        );

        when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"integration-test-ns\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(201)
                .body("name", equalTo("integration-test-ns"))
                .body("status", equalTo("CREATED"))
                .body("message", equalTo("Namespace created successfully"))
                .body("creationTimestamp", notNullValue());

        // Verify service was called with correct parameters
        verify(kubernetesService, times(1)).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testCompleteServiceAccountCreationFlow() {
        // Given
        ServiceAccountResponse mockResponse = new ServiceAccountResponse(
                "integration-test-sa",
                "integration-test-ns",
                "CREATED",
                Instant.now(),
                "Service account created successfully"
        );

        when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"integration-test-ns\", \"name\": \"integration-test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(201)
                .body("name", equalTo("integration-test-sa"))
                .body("namespace", equalTo("integration-test-ns"))
                .body("status", equalTo("CREATED"))
                .body("message", equalTo("Service account created successfully"))
                .body("creationTimestamp", notNullValue());

        // Verify service was called with correct parameters
        verify(kubernetesService, times(1)).createServiceAccount(any(ServiceAccountRequest.class));
    }

    @Test
    void testCompleteCiliumNetworkPolicyCreationFlow() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "integration-test-policy-abc123",
                "integration-test-ns",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "integration-test-policy-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createComplexCiliumNetworkPolicyRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("name", equalTo("integration-test-policy-abc123"))
                .body("namespace", equalTo("integration-test-ns"))
                .body("status", equalTo("CREATED"))
                .body("message", equalTo("CiliumNetworkPolicy created successfully"))
                .body("generatedName", equalTo("integration-test-policy-abc123"))
                .body("createdAt", notNullValue());

        // Verify service was called with correct parameters
        verify(kubernetesService, times(1)).createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class));
    }

    @Test
    void testHealthCheckIntegration() {
        // Given
        when(kubernetesService.isKubernetesAvailable()).thenReturn(true);

        // When & Then - Test namespace health
        given()
                .when()
                .get("/api/v1/namespaces/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("message", equalTo("Kubernetes is available"));

        // When & Then - Test Cilium health
        given()
                .when()
                .get("/api/v1/cilium-network-policies/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("message", equalTo("CiliumNetworkPolicy service is healthy"));

        // Verify service was called twice
        verify(kubernetesService, times(2)).isKubernetesAvailable();
    }

    @Test
    void testErrorHandlingIntegration() {
        // Given
        when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenThrow(new RuntimeException("Kubernetes API server is unavailable"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"test-namespace\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(500)
                .body("message", containsString("Failed to create namespace"))
                .body("message", containsString("Kubernetes API server is unavailable"));

        // Verify service was called
        verify(kubernetesService, times(1)).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testValidationIntegration() {
        // Test that validation errors are properly handled without calling the service

        // Invalid namespace name
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"INVALID-NAMESPACE\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Invalid service account request
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"\", \"name\": \"\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Invalid Cilium policy request
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test\", \"labels\": {}}")
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(400);

        // Verify service was never called due to validation failures
        verify(kubernetesService, times(0)).createNamespace(any(NamespaceRequest.class));
        verify(kubernetesService, times(0)).createServiceAccount(any(ServiceAccountRequest.class));
        verify(kubernetesService, times(0)).createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class));
    }

    @Test
    void testConcurrentRequestsHandling() {
        // Given
        NamespaceResponse mockResponse1 = new NamespaceResponse(
                "concurrent-ns-1", "CREATED", Instant.now(), "Namespace created successfully");
        NamespaceResponse mockResponse2 = new NamespaceResponse(
                "concurrent-ns-2", "CREATED", Instant.now(), "Namespace created successfully");

        when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(mockResponse1)
                .thenReturn(mockResponse2);

        // When & Then - Simulate concurrent requests
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"concurrent-ns-1\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(201)
                .body("name", equalTo("concurrent-ns-1"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"concurrent-ns-2\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(201)
                .body("name", equalTo("concurrent-ns-2"));

        // Verify service was called twice
        verify(kubernetesService, times(2)).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testResourceAlreadyExistsFlow() {
        // Given
        NamespaceResponse existingNamespaceResponse = new NamespaceResponse(
                "existing-namespace",
                "EXISTS",
                Instant.now().minusSeconds(3600), // Created 1 hour ago
                "Namespace already exists"
        );

        ServiceAccountResponse existingServiceAccountResponse = new ServiceAccountResponse(
                "existing-sa",
                "existing-namespace",
                "EXISTS",
                Instant.now().minusSeconds(1800), // Created 30 minutes ago
                "Service account already exists"
        );

        when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(existingNamespaceResponse);
        when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenReturn(existingServiceAccountResponse);

        // When & Then - Test namespace already exists
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"existing-namespace\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(200)
                .body("status", equalTo("EXISTS"))
                .body("message", equalTo("Namespace already exists"));

        // When & Then - Test service account already exists
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"existing-namespace\", \"name\": \"existing-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(200)
                .body("status", equalTo("EXISTS"))
                .body("message", equalTo("Service account already exists"));

        // Verify services were called
        verify(kubernetesService, times(1)).createNamespace(any(NamespaceRequest.class));
        verify(kubernetesService, times(1)).createServiceAccount(any(ServiceAccountRequest.class));
    }

    @Test
    void testContentTypeHandling() {
        // Given
        NamespaceResponse mockResponse = new NamespaceResponse(
                "content-type-test", "CREATED", Instant.now(), "Namespace created successfully");
        when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then - Test with correct content type
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"content-type-test\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(201);

        // When & Then - Test with missing content type (should fail)
        given()
                .body("{\"name\": \"content-type-test\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400); // Bad Request - JSON parsing will fail without proper content type

        // Verify service was called only once (for the successful request)
        verify(kubernetesService, times(1)).createNamespace(any(NamespaceRequest.class));
    }

    private String createComplexCiliumNetworkPolicyRequest() {
        return """
            {
                "namespace": "integration-test-ns",
                "labels": {
                    "tenant": "integration_test",
                    "environment": "testing"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "ipAddresses": ["203.0.113.10/32", "198.51.100.77/32"],
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 443
                            },
                            {
                                "protocol": "TCP",
                                "port": 8080
                            }
                        ]
                    },
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "fromLabels": {
                            "app": "frontend",
                            "version": "v1"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 80
                            }
                        ]
                    }
                ],
                "ingressDenyRules": [
                    {
                        "ruleType": "INGRESS_DENY",
                        "ipAddresses": ["192.0.2.0/24"]
                    }
                ],
                "egressRules": [
                    {
                        "ruleType": "EGRESS_ALLOW",
                        "toLabels": {
                            "service": "database",
                            "tier": "backend"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 5432
                            }
                        ]
                    },
                    {
                        "ruleType": "EGRESS_ALLOW",
                        "ipAddresses": ["0.0.0.0/0"],
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 2000,
                                "endPort": 2999
                            }
                        ]
                    }
                ],
                "egressDenyRules": [
                    {
                        "ruleType": "EGRESS_DENY",
                        "ipAddresses": ["203.0.113.30/32"]
                    }
                ]
            }
            """;
    }

    @Test
    void testCompleteCiliumNetworkPolicyDeletionFlow() {
        // Given
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("test-policy", "integration-test-ns"))
                .thenReturn(true);

        // When & Then
        given()
                .queryParam("namespace", "integration-test-ns")
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(200)
                .body("message", equalTo("CiliumNetworkPolicy deleted successfully"))
                .body("policyName", equalTo("test-policy"))
                .body("namespace", equalTo("integration-test-ns"))
                .body("deletedCount", equalTo(1))
                .body("timestamp", notNullValue());

        // Verify service was called with correct parameters
        verify(ciliumNetworkPolicyService, times(1)).deleteCiliumNetworkPolicy("test-policy", "integration-test-ns");
    }

    @Test
    void testCompleteCiliumNetworkPolicyDeleteAllFlow() {
        // Given
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("integration-test-ns"))
                .thenReturn(5);

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/integration-test-ns")
                .then()
                .statusCode(200)
                .body("message", equalTo("Successfully deleted 5 CiliumNetworkPolicies"))
                .body("policyName", nullValue())
                .body("namespace", equalTo("integration-test-ns"))
                .body("deletedCount", equalTo(5))
                .body("timestamp", notNullValue());

        // Verify service was called with correct parameters
        verify(ciliumNetworkPolicyService, times(1)).deleteAllCiliumNetworkPoliciesInNamespace("integration-test-ns");
    }

    @Test
    void testCiliumNetworkPolicyDeletionErrorHandling() {
        // Given - Test policy not found
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("non-existent", "integration-test-ns"))
                .thenReturn(false);

        // When & Then
        given()
                .queryParam("namespace", "integration-test-ns")
                .when()
                .delete("/api/v1/cilium-network-policies/non-existent")
                .then()
                .statusCode(404)
                .body("error", containsString("CiliumNetworkPolicy 'non-existent' not found"))
                .body("timestamp", notNullValue());

        // Given - Test namespace not found
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("test-policy", "non-existent-ns"))
                .thenThrow(new RuntimeException("Namespace 'non-existent-ns' does not exist"));

        // When & Then
        given()
                .queryParam("namespace", "non-existent-ns")
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"))
                .body("timestamp", notNullValue());

        // Verify services were called
        verify(ciliumNetworkPolicyService, times(1)).deleteCiliumNetworkPolicy("non-existent", "integration-test-ns");
        verify(ciliumNetworkPolicyService, times(1)).deleteCiliumNetworkPolicy("test-policy", "non-existent-ns");
    }

    @Test
    void testCiliumNetworkPolicyDeleteAllErrorHandling() {
        // Given - Test namespace not found
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("non-existent-ns"))
                .thenThrow(new RuntimeException("Namespace 'non-existent-ns' does not exist"));

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/non-existent-ns")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"))
                .body("timestamp", notNullValue());

        // Given - Test Kubernetes API error
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("integration-test-ns"))
                .thenThrow(new RuntimeException("Kubernetes API server is unavailable"));

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/integration-test-ns")
                .then()
                .statusCode(500)
                .body("error", containsString("Failed to delete CiliumNetworkPolicies"))
                .body("error", containsString("Kubernetes API server is unavailable"))
                .body("timestamp", notNullValue());

        // Verify services were called
        verify(ciliumNetworkPolicyService, times(1)).deleteAllCiliumNetworkPoliciesInNamespace("non-existent-ns");
        verify(ciliumNetworkPolicyService, times(1)).deleteAllCiliumNetworkPoliciesInNamespace("integration-test-ns");
    }

    @Test
    void testCiliumNetworkPolicyDeleteAllNoPoliciesFlow() {
        // Given - No policies to delete
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("empty-namespace"))
                .thenReturn(0);

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/empty-namespace")
                .then()
                .statusCode(200)
                .body("message", equalTo("No CiliumNetworkPolicies found to delete"))
                .body("policyName", nullValue())
                .body("namespace", equalTo("empty-namespace"))
                .body("deletedCount", equalTo(0))
                .body("timestamp", notNullValue());

        // Verify service was called
        verify(ciliumNetworkPolicyService, times(1)).deleteAllCiliumNetworkPoliciesInNamespace("empty-namespace");
    }

    @Test
    void testCiliumNetworkPolicyDeleteValidationIntegration() {
        // Test that validation errors are properly handled without calling the service

        // Missing namespace parameter for specific policy deletion
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(400);

        // Verify service was never called due to validation failure
        verify(ciliumNetworkPolicyService, times(0)).deleteCiliumNetworkPolicy(any(), any());
        verify(ciliumNetworkPolicyService, times(0)).deleteAllCiliumNetworkPoliciesInNamespace(any());
    }

    @Test
    void testCRUDIntegrationFlow() {
        // This test demonstrates a complete CRUD flow for CiliumNetworkPolicy
        
        // 1. Create a policy
        CiliumNetworkPolicyResponse createResponse = new CiliumNetworkPolicyResponse(
                "crud-test-policy-abc123",
                "crud-test-ns",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "crud-test-policy-abc123"
        );
        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(createResponse);

        given()
                .contentType(ContentType.JSON)
                .body(createSimpleCiliumNetworkPolicyRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"));

        // 2. Read the policy
        CiliumNetworkPolicyRequest readResponse = createMockPolicyRequest();
        when(ciliumNetworkPolicyService.getCiliumNetworkPolicyByName("crud-test-policy-abc123", "crud-test-ns"))
                .thenReturn(readResponse);

        given()
                .queryParam("namespace", "crud-test-ns")
                .when()
                .get("/api/v1/cilium-network-policies/crud-test-policy-abc123")
                .then()
                .statusCode(200)
                .body("name", equalTo("crud-test-policy-abc123"));

        // 3. Delete the policy
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("crud-test-policy-abc123", "crud-test-ns"))
                .thenReturn(true);

        given()
                .queryParam("namespace", "crud-test-ns")
                .when()
                .delete("/api/v1/cilium-network-policies/crud-test-policy-abc123")
                .then()
                .statusCode(200)
                .body("message", equalTo("CiliumNetworkPolicy deleted successfully"));

        // Verify all services were called
        verify(kubernetesService, times(1)).createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class));
        verify(ciliumNetworkPolicyService, times(1)).getCiliumNetworkPolicyByName("crud-test-policy-abc123", "crud-test-ns");
        verify(ciliumNetworkPolicyService, times(1)).deleteCiliumNetworkPolicy("crud-test-policy-abc123", "crud-test-ns");
    }

    private String createSimpleCiliumNetworkPolicyRequest() {
        return """
            {
                "namespace": "crud-test-ns",
                "labels": {
                    "app": "crud-test"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "ipAddresses": ["203.0.113.10/32"],
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 80
                            }
                        ]
                    }
                ]
            }
            """;
    }

    private CiliumNetworkPolicyRequest createMockPolicyRequest() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setName("crud-test-policy-abc123");
        request.setNamespace("crud-test-ns");
        request.setLabels(Map.of("app", "crud-test"));
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        ingressRule.setIpAddresses(List.of("203.0.113.10/32"));
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(80);
        
        ingressRule.setPorts(List.of(portRule));
        request.setIngressRules(List.of(ingressRule));
        
        return request;
    }
}