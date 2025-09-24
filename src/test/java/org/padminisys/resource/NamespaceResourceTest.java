package org.padminisys.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.padminisys.dto.NamespaceRequest;
import org.padminisys.dto.NamespaceResponse;
import org.padminisys.service.KubernetesService;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@QuarkusTest
class NamespaceResourceTest {

    @InjectMock
    KubernetesService kubernetesService;

    @BeforeEach
    void setUp() {
        Mockito.reset(kubernetesService);
    }

    @Test
    void testCreateNamespaceSuccess() {
        // Given
        NamespaceResponse mockResponse = new NamespaceResponse(
                "test-namespace",
                "CREATED",
                Instant.now(),
                "Namespace created successfully"
        );

        Mockito.when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"test-namespace\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(201)
                .body("name", is("test-namespace"))
                .body("status", is("CREATED"))
                .body("message", is("Namespace created successfully"))
                .body("creationTimestamp", notNullValue());

        // Verify service interaction
        verify(kubernetesService).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testCreateNamespaceAlreadyExists() {
        // Given
        NamespaceResponse mockResponse = new NamespaceResponse(
                "existing-namespace",
                "EXISTS",
                Instant.now(),
                "Namespace already exists"
        );

        Mockito.when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"existing-namespace\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(200)
                .body("name", is("existing-namespace"))
                .body("status", is("EXISTS"))
                .body("message", is("Namespace already exists"))
                .body("creationTimestamp", notNullValue());

        // Verify service interaction
        verify(kubernetesService).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testCreateNamespaceInvalidName() {
        // When & Then - Test invalid namespace name (uppercase not allowed)
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"INVALID-NAMESPACE\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespaceEmptyName() {
        // When & Then - Test empty namespace name
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespaceServiceException() {
        // Given
        Mockito.when(kubernetesService.createNamespace(any(NamespaceRequest.class)))
                .thenThrow(new RuntimeException("Kubernetes API error"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"test-namespace\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(500)
                .body("message", containsString("Failed to create namespace"));

        // Verify service interaction
        verify(kubernetesService).createNamespace(any(NamespaceRequest.class));
    }

    @Test
    void testKubernetesHealthCheckUp() {
        // Given
        Mockito.when(kubernetesService.isKubernetesAvailable()).thenReturn(true);

        // When & Then
        given()
                .when()
                .get("/api/v1/namespaces/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("message", is("Kubernetes is available"));

        // Verify service interaction
        verify(kubernetesService).isKubernetesAvailable();
    }

    @Test
    void testKubernetesHealthCheckDown() {
        // Given
        Mockito.when(kubernetesService.isKubernetesAvailable()).thenReturn(false);

        // When & Then
        given()
                .when()
                .get("/api/v1/namespaces/health")
                .then()
                .statusCode(503)
                .body("status", is("DOWN"))
                .body("message", is("Kubernetes is not available"));

        // Verify service interaction
        verify(kubernetesService).isKubernetesAvailable();
    }

    @Test
    void testCreateNamespace_InvalidJSON() {
        // When & Then - Test malformed JSON
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"test-namespace\", invalid}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to JSON parsing failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespace_MissingName() {
        // When & Then - Test missing name field
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespace_NullName() {
        // When & Then - Test null name field
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": null}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespace_NameTooLong() {
        // When & Then - Test name that's too long (DNS-1123 labels must be <= 63 characters)
        String longName = "a".repeat(64);
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"" + longName + "\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateNamespace_NameWithInvalidCharacters() {
        // When & Then - Test name with invalid characters
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"test_namespace@123\"}")
                .when()
                .post("/api/v1/namespaces")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }
}