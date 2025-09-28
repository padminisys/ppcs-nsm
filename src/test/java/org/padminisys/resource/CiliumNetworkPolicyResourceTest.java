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
import org.padminisys.service.CiliumNetworkPolicyService;

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

    @InjectMock
    CiliumNetworkPolicyService ciliumNetworkPolicyService;

    @BeforeEach
    void setUp() {
        Mockito.reset(kubernetesService);
        Mockito.reset(ciliumNetworkPolicyService);
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

    @Test
    void testCreateCiliumNetworkPolicy_WithFromLabels_Success() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "gb7yp-abc123",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "gb7yp-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createLabelBasedRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("name", equalTo("gb7yp-abc123"))
                .body("namespace", equalTo("test-namespace"))
                .body("status", equalTo("CREATED"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_MixedIPAndLabels_Success() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "mixed-policy-abc123",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "mixed-policy-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createMixedRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_InvalidRequest_BothIPAndLabels() {
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
                                "ipAddresses": ["203.0.113.10/32"],
                                "fromLabels": {
                                    "serial": "GB7YH"
                                }
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
    void testCreateCiliumNetworkPolicy_InvalidRequest_NoIPOrLabels() {
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
                                "ports": [
                                    {
                                        "protocol": "TCP",
                                        "port": 80
                                    }
                                ]
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
    void testCreateCiliumNetworkPolicy_WithHeaderMatches_Success() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "header-policy-abc123",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "header-policy-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createHeaderMatchRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("name", equalTo("header-policy-abc123"))
                .body("namespace", equalTo("test-namespace"))
                .body("status", equalTo("CREATED"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_WithMultipleHeaderMatches_Success() {
        // Given
        CiliumNetworkPolicyResponse mockResponse = new CiliumNetworkPolicyResponse(
                "multi-header-abc123",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "CiliumNetworkPolicy created successfully",
                "multi-header-abc123"
        );

        when(kubernetesService.createCiliumNetworkPolicy(any(CiliumNetworkPolicyRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body(createMultipleHeaderMatchRequest())
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"));
    }

    @Test
    void testCreateCiliumNetworkPolicy_InvalidRequest_EmptyHeaderName() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "namespace": "test-namespace",
                        "labels": {
                            "tenant": "test"
                        },
                        "ingressRules": [
                            {
                                "ruleType": "INGRESS_ALLOW",
                                "fromLabels": {
                                    "serial": "GB7YH"
                                },
                                "ports": [
                                    {
                                        "protocol": "TCP",
                                        "port": 80,
                                        "headerMatches": [
                                            {
                                                "name": "",
                                                "value": "45.248.67.9"
                                            }
                                        ]
                                    }
                                ]
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
    void testCreateCiliumNetworkPolicy_InvalidRequest_EmptyHeaderValue() {
        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "namespace": "test-namespace",
                        "labels": {
                            "tenant": "test"
                        },
                        "ingressRules": [
                            {
                                "ruleType": "INGRESS_ALLOW",
                                "fromLabels": {
                                    "serial": "GB7YH"
                                },
                                "ports": [
                                    {
                                        "protocol": "TCP",
                                        "port": 80,
                                        "headerMatches": [
                                            {
                                                "name": "X-Real-IP",
                                                "value": ""
                                            }
                                        ]
                                    }
                                ]
                            }
                        ]
                    }
                    """)
                .when()
                .post("/api/v1/cilium-network-policies")
                .then()
                .statusCode(400);
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

    private String createLabelBasedRequest() {
        return """
            {
                "namespace": "test-namespace",
                "labels": {
                    "serial": "GB7YP"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "fromLabels": {
                            "serial": "GB7YH"
                        },
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

    private String createMixedRequest() {
        return """
            {
                "namespace": "test-namespace",
                "labels": {
                    "tenant": "mixed_policy"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "ipAddresses": ["203.0.113.10/32"],
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 443
                            }
                        ]
                    },
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "fromLabels": {
                            "serial": "GB7YH"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 80
                            }
                        ]
                    }
                ],
                "egressRules": [
                    {
                        "ruleType": "EGRESS_ALLOW",
                        "toLabels": {
                            "service": "database"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 5432
                            }
                        ]
                    }
                ]
            }
            """;
    }

    private String createHeaderMatchRequest() {
        return """
            {
                "namespace": "test-namespace",
                "labels": {
                    "tenant": "header_policy"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "fromLabels": {
                            "padmini.systems/tenant-resource-type": "ingress"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 80,
                                "headerMatches": [
                                    {
                                        "name": "X-Real-IP",
                                        "value": "45.248.67.9"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """;
    }

    private String createMultipleHeaderMatchRequest() {
        return """
            {
                "namespace": "test-namespace",
                "labels": {
                    "tenant": "multi_header"
                },
                "ingressRules": [
                    {
                        "ruleType": "INGRESS_ALLOW",
                        "fromLabels": {
                            "padmini.systems/tenant-resource-type": "ingress"
                        },
                        "ports": [
                            {
                                "protocol": "TCP",
                                "port": 80,
                                "headerMatches": [
                                    {
                                        "name": "X-Real-IP",
                                        "value": "45.248.67.9"
                                    },
                                    {
                                        "name": "X-Forwarded-For",
                                        "value": "203.0.113.7"
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """;
    }

    @Test
    void testGetCiliumNetworkPolicyByName_Success() {
        // Given
        CiliumNetworkPolicyRequest mockRequest = createMockPolicyRequest();
        when(ciliumNetworkPolicyService.getCiliumNetworkPolicyByName("gb7yp-md0dy8", "test-namespace"))
                .thenReturn(mockRequest);

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .get("/api/v1/cilium-network-policies/gb7yp-md0dy8")
                .then()
                .statusCode(200)
                .body("name", equalTo("gb7yp-md0dy8"))
                .body("namespace", equalTo("test-namespace"))
                .body("labels.serial", equalTo("GB7YP"))
                .body("ingressRules", hasSize(1))
                .body("ingressRules[0].ruleType", equalTo("INGRESS_ALLOW"))
                .body("ingressRules[0].fromLabels.'padmini.systems/tenant-resource-type'", equalTo("ingress"))
                .body("ingressRules[0].ports", hasSize(1))
                .body("ingressRules[0].ports[0].protocol", equalTo("TCP"))
                .body("ingressRules[0].ports[0].port", equalTo(80))
                .body("ingressRules[0].ports[0].headerMatches", hasSize(1))
                .body("ingressRules[0].ports[0].headerMatches[0].name", equalTo("x-real-ip"))
                .body("ingressRules[0].ports[0].headerMatches[0].value", equalTo("45.248.67.9"))
                // Verify null values are not present
                .body("ingressRules[0].ipAddresses", nullValue())
                .body("ingressRules[0].toLabels", nullValue())
                .body("ingressRules[0].ports[0].endPort", nullValue())
                .body("ingressDenyRules", nullValue())
                .body("egressRules", nullValue())
                .body("egressDenyRules", nullValue());
    }

    @Test
    void testGetCiliumNetworkPolicyByName_NotFound() {
        // Given
        when(ciliumNetworkPolicyService.getCiliumNetworkPolicyByName("non-existent", "test-namespace"))
                .thenThrow(new RuntimeException("CiliumNetworkPolicy 'non-existent' not found in namespace 'test-namespace'"));

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .get("/api/v1/cilium-network-policies/non-existent")
                .then()
                .statusCode(404)
                .body("error", containsString("CiliumNetworkPolicy not found"));
    }

    @Test
    void testGetCiliumNetworkPolicyByName_MissingNamespaceParam() {
        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/gb7yp-md0dy8")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetCiliumNetworkPoliciesByNamespace_Success() {
        // Given
        List<CiliumNetworkPolicyRequest> mockPolicies = List.of(
                createMockPolicyRequest(),
                createMockPolicyRequest2()
        );
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByNamespace("test-namespace"))
                .thenReturn(mockPolicies);

        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/namespace/test-namespace")
                .then()
                .statusCode(200)
                .body("", hasSize(2))
                .body("[0].name", notNullValue())
                .body("[0].namespace", equalTo("test-namespace"))
                .body("[0].labels.serial", equalTo("GB7YP"))
                .body("[1].name", notNullValue())
                .body("[1].namespace", equalTo("test-namespace"))
                .body("[1].labels.serial", equalTo("GB7YH"));
    }

    @Test
    void testGetCiliumNetworkPoliciesByNamespace_EmptyResult() {
        // Given
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByNamespace("empty-namespace"))
                .thenReturn(List.of());

        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/namespace/empty-namespace")
                .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    void testGetCiliumNetworkPoliciesByNamespace_NamespaceNotFound() {
        // Given
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByNamespace("non-existent"))
                .thenThrow(new RuntimeException("Namespace 'non-existent' does not exist"));

        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/namespace/non-existent")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"));
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_Success() {
        // Given
        List<CiliumNetworkPolicyRequest> mockPolicies = List.of(createMockPolicyRequest());
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByEndpointSelector("test-namespace", Map.of("serial", "GB7YP")))
                .thenReturn(mockPolicies);

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .queryParam("labels", "serial=GB7YP")
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(200)
                .body("", hasSize(1))
                .body("[0].name", notNullValue())
                .body("[0].namespace", equalTo("test-namespace"))
                .body("[0].labels.serial", equalTo("GB7YP"));
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_MultipleLabels() {
        // Given
        List<CiliumNetworkPolicyRequest> mockPolicies = List.of(createMockPolicyRequest());
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByEndpointSelector("test-namespace",
                Map.of("serial", "GB7YP", "environment", "production")))
                .thenReturn(mockPolicies);

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .queryParam("labels", "serial=GB7YP,environment=production")
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(200)
                .body("", hasSize(1));
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_AllNamespaces() {
        // Given
        List<CiliumNetworkPolicyRequest> mockPolicies = List.of(
                createMockPolicyRequest(),
                createMockPolicyRequest2()
        );
        when(ciliumNetworkPolicyService.getCiliumNetworkPoliciesByEndpointSelector(null, Map.of("serial", "GB7YP")))
                .thenReturn(mockPolicies);

        // When & Then
        given()
                .queryParam("labels", "serial=GB7YP")
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(200)
                .body("", hasSize(2));
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_InvalidLabelsFormat() {
        // When & Then
        given()
                .queryParam("labels", "invalid-format")
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid labels parameter"));
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_MissingLabelsParam() {
        // When & Then
        given()
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_EmptyLabelsParam() {
        // When & Then - Empty string triggers @NotBlank validation
        given()
                .queryParam("labels", "")
                .when()
                .get("/api/v1/cilium-network-policies/endpoint-selector")
                .then()
                .statusCode(400)
                .body("title", equalTo("Constraint Violation"));
    }

    private CiliumNetworkPolicyRequest createMockPolicyRequest() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setName("gb7yp-md0dy8");
        request.setNamespace("test-namespace");
        request.setLabels(Map.of("serial", "GB7YP"));
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        ingressRule.setFromLabels(Map.of("padmini.systems/tenant-resource-type", "ingress"));
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(80);
        
        CiliumNetworkPolicyRequest.HeaderMatch headerMatch = new CiliumNetworkPolicyRequest.HeaderMatch();
        headerMatch.setName("x-real-ip");
        headerMatch.setValue("45.248.67.9");
        portRule.setHeaderMatches(List.of(headerMatch));
        
        ingressRule.setPorts(List.of(portRule));
        request.setIngressRules(List.of(ingressRule));
        
        return request;
    }

    private CiliumNetworkPolicyRequest createMockPolicyRequest2() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setName("gb7yh-abc123");
        request.setNamespace("test-namespace");
        request.setLabels(Map.of("serial", "GB7YH"));
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = new CiliumNetworkPolicyRequest.NetworkRule();
        ingressRule.setRuleType(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW);
        ingressRule.setFromLabels(Map.of("padmini.systems/tenant-resource-type", "ingress"));
        
        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
        portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.TCP);
        portRule.setPort(443);
        
        ingressRule.setPorts(List.of(portRule));
        request.setIngressRules(List.of(ingressRule));
        
        return request;
    }

    @Test
    void testDeleteCiliumNetworkPolicy_Success() {
        // Given
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("test-policy", "test-namespace"))
                .thenReturn(true);

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(200)
                .body("message", equalTo("CiliumNetworkPolicy deleted successfully"))
                .body("policyName", equalTo("test-policy"))
                .body("namespace", equalTo("test-namespace"))
                .body("deletedCount", equalTo(1))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteCiliumNetworkPolicy_PolicyNotFound() {
        // Given
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("non-existent", "test-namespace"))
                .thenReturn(false);

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .delete("/api/v1/cilium-network-policies/non-existent")
                .then()
                .statusCode(404)
                .body("error", containsString("CiliumNetworkPolicy 'non-existent' not found in namespace 'test-namespace'"))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteCiliumNetworkPolicy_NamespaceNotFound() {
        // Given
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("test-policy", "non-existent"))
                .thenThrow(new RuntimeException("Namespace 'non-existent' does not exist"));

        // When & Then
        given()
                .queryParam("namespace", "non-existent")
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteCiliumNetworkPolicy_InternalServerError() {
        // Given
        when(ciliumNetworkPolicyService.deleteCiliumNetworkPolicy("test-policy", "test-namespace"))
                .thenThrow(new RuntimeException("Kubernetes API error"));

        // When & Then
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(500)
                .body("error", containsString("Failed to delete CiliumNetworkPolicy"))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteCiliumNetworkPolicy_MissingNamespaceParam() {
        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(400);
    }

    @Test
    void testDeleteAllCiliumNetworkPoliciesInNamespace_Success() {
        // Given
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("test-namespace"))
                .thenReturn(3);

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/test-namespace")
                .then()
                .statusCode(200)
                .body("message", equalTo("Successfully deleted 3 CiliumNetworkPolicies"))
                .body("policyName", nullValue())
                .body("namespace", equalTo("test-namespace"))
                .body("deletedCount", equalTo(3))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteAllCiliumNetworkPoliciesInNamespace_NoPolicies() {
        // Given
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
    }

    @Test
    void testDeleteAllCiliumNetworkPoliciesInNamespace_NamespaceNotFound() {
        // Given
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("non-existent"))
                .thenThrow(new RuntimeException("Namespace 'non-existent' does not exist"));

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/non-existent")
                .then()
                .statusCode(404)
                .body("error", containsString("Namespace not found"))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteAllCiliumNetworkPoliciesInNamespace_InternalServerError() {
        // Given
        when(ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace("test-namespace"))
                .thenThrow(new RuntimeException("Kubernetes API error"));

        // When & Then
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/test-namespace")
                .then()
                .statusCode(500)
                .body("error", containsString("Failed to delete CiliumNetworkPolicies"))
                .body("timestamp", notNullValue());
    }

    @Test
    void testDeleteCiliumNetworkPolicy_ValidatesPathParameters() {
        // Test with empty policy name in path - should return 400 for missing path param
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .delete("/api/v1/cilium-network-policies/")
                .then()
                .statusCode(400); // Bad request for malformed path

        // Test with empty namespace in path for delete all - should return 400 for malformed path
        given()
                .when()
                .delete("/api/v1/cilium-network-policies/namespace/")
                .then()
                .statusCode(400); // Bad request for malformed path
    }

    @Test
    void testDeleteEndpoints_MethodNotAllowed() {
        // Test that other HTTP methods are not allowed on delete endpoints
        // Note: These paths don't exist as POST endpoints, so they return 400 (Bad Request) instead of 405
        given()
                .queryParam("namespace", "test-namespace")
                .when()
                .post("/api/v1/cilium-network-policies/test-policy")
                .then()
                .statusCode(400); // Bad request - path doesn't exist for POST

        given()
                .when()
                .post("/api/v1/cilium-network-policies/namespace/test-namespace")
                .then()
                .statusCode(400); // Bad request - path doesn't exist for POST
    }
}