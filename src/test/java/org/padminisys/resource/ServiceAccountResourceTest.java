package org.padminisys.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.padminisys.dto.ServiceAccountRequest;
import org.padminisys.dto.ServiceAccountResponse;
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
class ServiceAccountResourceTest {

    @InjectMock
    KubernetesService kubernetesService;

    @BeforeEach
    void setUp() {
        Mockito.reset(kubernetesService);
    }

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
                .body("message", is("Service account created successfully"))
                .body("creationTimestamp", notNullValue());

        // Verify service interaction
        verify(kubernetesService).createServiceAccount(any(ServiceAccountRequest.class));
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
                .body("message", is("Service account already exists"))
                .body("creationTimestamp", notNullValue());

        // Verify service interaction
        verify(kubernetesService).createServiceAccount(any(ServiceAccountRequest.class));
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
                .statusCode(400)
                .body("message", containsString("does not exist"));

        // Verify service interaction
        verify(kubernetesService).createServiceAccount(any(ServiceAccountRequest.class));
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

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
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

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
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

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
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

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
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
                .statusCode(500)
                .body("message", containsString("Failed to create service account"));

        // Verify service interaction
        verify(kubernetesService).createServiceAccount(any(ServiceAccountRequest.class));
    }

    @Test
    void testCreateServiceAccount_InvalidJSON() {
        // When & Then - Test malformed JSON
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"test-sa\", invalid}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to JSON parsing failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_MissingFields() {
        // When & Then - Test missing required fields
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_NullFields() {
        // When & Then - Test null fields
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": null, \"name\": null}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_NamespaceTooLong() {
        // When & Then - Test namespace name that's too long
        String longName = "a".repeat(64);
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"" + longName + "\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_ServiceAccountNameTooLong() {
        // When & Then - Test service account name that's too long
        String longName = "a".repeat(64);
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"" + longName + "\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_NamespaceWithInvalidCharacters() {
        // When & Then - Test namespace with invalid characters
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test_namespace@123\", \"name\": \"test-sa\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }

    @Test
    void testCreateServiceAccount_ServiceAccountNameWithInvalidCharacters() {
        // When & Then - Test service account name with invalid characters
        given()
                .contentType(ContentType.JSON)
                .body("{\"namespace\": \"test-namespace\", \"name\": \"test_sa@123\"}")
                .when()
                .post("/api/v1/serviceaccounts")
                .then()
                .statusCode(400);

        // Verify service was not called due to validation failure
        verifyNoInteractions(kubernetesService);
    }
}