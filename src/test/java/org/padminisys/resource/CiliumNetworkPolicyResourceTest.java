package org.padminisys.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.padminisys.dto.CiliumNetworkPolicyRequest;
import org.padminisys.dto.CiliumNetworkPolicyResponse;
import org.padminisys.service.KubernetesService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class CiliumNetworkPolicyResourceTest {

    @InjectMock
    KubernetesService kubernetesService;

    @BeforeEach
    void setUp() {
        Mockito.reset(kubernetesService);
    }

    @Test
    void testCreateCiliumNetworkPolicy_Success() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "dell-computers-abc123",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "dell-computers-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createValidRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("name", equalTo("dell-computers-abc123"))
                .body("namespace", equalTo("test-namespace"))
                .body("status", equalTo("CREATED"))
                .body("message", equalTo("CiliumNetworkPolicy created successfully"))
                .body("generatedName", equalTo("dell-computers-abc123"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_AlreadyExists() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "dell-computers-abc123",
                "test-namespace",
                "EXISTS",
                Instant.now(),
                "CiliumNetworkPolicy already exists",
                "dell-computers-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createValidRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(200)
                .body("status", equalTo("EXISTS"))
                .body("message", equalTo("CiliumNetworkPolicy already exists"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_NamespaceNotFound() {
        // Given
        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenThrow(new RuntimeException("Namespace 'non-existent' does not exist"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createValidRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_InternalServerError() {
        // Given
        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenThrow(new RuntimeException("Kubernetes API error"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createValidRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(500)
                .body("error", containsString("Failed to create CiliumNetworkPolicy"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_InvalidRequest_MissingNamespace() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "labels": {
                            "tenant": "dell_computers"
                        },
                        "ingressRules": []
                    }
                    """)
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateCiliumNetworkPolicy_InvalidRequest_EmptyLabels() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "namespace": "test-namespace",
                        "labels": {},
                        "ingressRules": []
                    }
                    """)
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateCiliumNetworkPolicy_InvalidRequest_InvalidCIDR() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "namespace": "test-namespace",
                        "labels": {
                            "tenant": "dell_computers"
                        },
                        "ingressRules": [
                            {
                                "ruleType": "INGRESS_ALLOW",
                                "ipAddresses": ["invalid-cidr"]
                            }
                        ]
                    }
                    """)
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(400);
    }

    @Test
    void testCheckHealth_Healthy() {
        // Given
        when(kubernetesService.isKubernetesAvailable()).thenReturn(true);

        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("message", equalTo("CiliumNetworkPolicy service is healthy"));
    }

    @Test
    void testCheckHealth_Unhealthy() {
        // Given
        when(kubernetesService.isKubernetesAvailable()).thenReturn(false);

        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/health")
                .then()
                .statusCode(503)
                .body("status", equalTo("DOWN"))
                .body("message", equalTo("CiliumNetworkPolicy service cannot connect to Kubernetes"));
    }

    private String createValidRequest() {
        return """
            {
                "namespace": "test-namespace",
                "labels": {
                    "tenant": "dell_computers",
                    "environment": "production"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "ipAddresses": ["203.0.113.10/32", "198.51.100.77/32"],
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 443
                            }
                        ]
                    }
                ],
                "ingressDenyRules": [
                    {
                        "ruleType": "INGRESS_DENY",
                        "ipAddresses": ["192.0.2.10/32"]
                    }
                ],
                "egressRules": [
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
}