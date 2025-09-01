package org.padminisys.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.padminisys.dto.ServiceAccountRequest;
import org.padminisys.dto.ServiceAccountResponse;
import org.padminisys.service.KubernetesService;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class ServiceAccountResourceTest {

    @InjectMock
    KubernetesService kubernetesService;

    @Test
    void testCreateServiceAccountSuccess() {
        // Given
        ServiceAccountResponse mockResponse = new ServiceAccountResponse(
                "test-sa",
                "test-namespace",
                "CREATED",
                Instant.now(),
                "Service account created successfully"
        );

        Mockito.when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(201)
                .body("name", is("test-sa"))
                .body("namespace", is("test-namespace"))
                .body("status", is("CREATED"))
                .body("message", is("Service account created successfully"));
    }

    @Test
    void testCreateServiceAccountAlreadyExists() {
        // Given
        ServiceAccountResponse mockResponse = new ServiceAccountResponse(
                "existing-sa",
                "test-namespace",
                "EXISTS",
                Instant.now(),
                "Service account already exists"
        );

        Mockito.when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"existing-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(200)
                .body("name", is("existing-sa"))
                .body("namespace", is("test-namespace"))
                .body("status", is("EXISTS"))
                .body("message", is("Service account already exists"));
    }

    @Test
    void testCreateServiceAccountNamespaceNotFound() {
        // Given
        Mockito.when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenThrow(new RuntimeException("Namespace 'non-existent' does not exist"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"non-existent\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateServiceAccountInvalidNamespaceName() {
        // When & Then - Test invalid namespace name (uppercase not allowed)
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"INVALID-NAMESPACE\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateServiceAccountInvalidServiceAccountName() {
        // When & Then - Test invalid service account name (uppercase not allowed)
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"INVALID-SA\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateServiceAccountEmptyNamespace() {
        // When & Then - Test empty namespace
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateServiceAccountEmptyName() {
        // When & Then - Test empty service account name
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateServiceAccountServiceException() {
        // Given
        Mockito.when(kubernetesService.createServiceAccount(any(ServiceAccountRequest.class)))
                .thenThrow(new RuntimeException("Kubernetes API error"));

        // When & Then
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(500);
    }
}