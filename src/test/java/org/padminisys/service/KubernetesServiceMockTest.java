package org.padminisys.service;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.dsl.*;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.padminisys.dto.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for KubernetesService with proper mocking of Kubernetes client.
 * These tests ensure that all Kubernetes API interactions are properly mocked and tested.
 */
@ExtendWith(MockitoExtension.class)
class KubernetesServiceMockTest {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(KubernetesServiceMockTest.class);

    @Mock
    KubernetesClient kubernetesClient;

    @InjectMocks
    KubernetesService kubernetesService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // Reset all mocks before each test
        reset(kubernetesClient);
    }

    @Test
    void testCreateNamespace_Success() {
        LOG.info("TEST: Creating new namespace - Success scenario");
        LOG.info("Scenario: Namespace doesn't exist, should create successfully with proper labels");
        
        // Given
        NamespaceRequest request = new NamespaceRequest("test-namespace");
        LOG.info("Input: NamespaceRequest with name='test-namespace'");
        
        // Mock namespace operations
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("test-namespace")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null); // Namespace doesn't exist
        LOG.info("Mock Setup: Kubernetes client configured to return null (namespace doesn't exist)");
        
        // Mock successful creation
        Namespace createdNamespace = createMockNamespace("test-namespace", "2023-01-01T10:00:00Z");
        when(namespacesOp.create(any(Namespace.class))).thenReturn(createdNamespace);
        LOG.info("Mock Setup: Namespace creation will return successfully created namespace");

        // When
        LOG.info("Executing: kubernetesService.createNamespace()");
        NamespaceResponse response = kubernetesService.createNamespace(request);

        // Then
        assertNotNull(response);
        assertEquals("test-namespace", response.getName());
        assertEquals("CREATED", response.getStatus());
        assertEquals("Namespace created successfully", response.getMessage());
        assertNotNull(response.getCreationTimestamp());
        LOG.info("✓ Response validation PASSED: Status=CREATED, Name=test-namespace");

        // Verify interactions
        verify(namespaceResource).get();
        verify(namespacesOp).create(any(Namespace.class));
        LOG.info("✓ Mock interactions verified: Checked existence and created namespace");
        
        // Verify the created namespace has correct properties
        ArgumentCaptor<Namespace> namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
        verify(namespacesOp).create(namespaceCaptor.capture());
        Namespace capturedNamespace = namespaceCaptor.getValue();
        assertEquals("test-namespace", capturedNamespace.getMetadata().getName());
        assertEquals("ppcs-nsm", capturedNamespace.getMetadata().getLabels().get("created-by"));
        assertEquals("quarkus-app", capturedNamespace.getMetadata().getLabels().get("managed-by"));
        LOG.info("✓ Namespace properties verified: Correct name and management labels applied");
    }

    @Test
    void testCreateNamespace_AlreadyExists() {
        LOG.info("TEST: Creating namespace that already exists - Idempotent behavior");
        LOG.info("Scenario: Namespace exists, should return EXISTS status without attempting creation");
        
        // Given
        NamespaceRequest request = new NamespaceRequest("existing-namespace");
        LOG.info("Input: NamespaceRequest with name='existing-namespace'");
        
        // Mock namespace operations
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("existing-namespace")).thenReturn(namespaceResource);
        
        // Mock existing namespace
        Namespace existingNamespace = createMockNamespace("existing-namespace", "2023-01-01T09:00:00Z");
        when(namespaceResource.get()).thenReturn(existingNamespace);
        LOG.info("Mock Setup: Kubernetes client configured to return existing namespace");

        // When
        LOG.info("Executing: kubernetesService.createNamespace() for existing namespace");
        NamespaceResponse response = kubernetesService.createNamespace(request);

        // Then
        assertNotNull(response);
        assertEquals("existing-namespace", response.getName());
        assertEquals("EXISTS", response.getStatus());
        assertEquals("Namespace already exists", response.getMessage());
        assertNotNull(response.getCreationTimestamp());
        LOG.info("✓ Response validation PASSED: Status=EXISTS, idempotent behavior confirmed");

        // Verify interactions
        verify(namespaceResource).get();
        verify(namespacesOp, never()).create(any(Namespace.class));
        LOG.info("✓ Mock interactions verified: Checked existence but did NOT attempt creation");
    }

    @Test
    void testCreateNamespace_KubernetesException() {
        // Given
        NamespaceRequest request = new NamespaceRequest("test-namespace");
        
        // Mock namespace operations
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("test-namespace")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null);
        when(namespacesOp.create(any(Namespace.class)))
                .thenThrow(new KubernetesClientException("API server unavailable"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            kubernetesService.createNamespace(request);
        });

        assertTrue(exception.getMessage().contains("Failed to create namespace"));
        assertTrue(exception.getMessage().contains("API server unavailable"));

        // Verify interactions
        verify(namespaceResource).get();
        verify(namespacesOp).create(any(Namespace.class));
    }

    @Test
    void testCreateServiceAccount_Success() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", "test-sa");
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace("test-namespace", "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("test-namespace")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock service account operations
        MixedOperation<ServiceAccount, ServiceAccountList, ServiceAccountResource> serviceAccountsOp = mock(MixedOperation.class);
        ServiceAccountResource serviceAccountResource = mock(ServiceAccountResource.class);
        
        when(kubernetesClient.serviceAccounts()).thenReturn(serviceAccountsOp);
        when(serviceAccountsOp.inNamespace("test-namespace")).thenReturn(serviceAccountsOp);
        when(serviceAccountsOp.withName("test-sa")).thenReturn(serviceAccountResource);
        when(serviceAccountResource.get()).thenReturn(null); // Service account doesn't exist
        
        // Mock successful creation
        ServiceAccount createdServiceAccount = createMockServiceAccount("test-sa", "test-namespace", "2023-01-01T10:00:00Z");
        when(serviceAccountsOp.create(any(ServiceAccount.class))).thenReturn(createdServiceAccount);

        // When
        ServiceAccountResponse response = kubernetesService.createServiceAccount(request);

        // Then
        assertNotNull(response);
        assertEquals("test-sa", response.getName());
        assertEquals("test-namespace", response.getNamespace());
        assertEquals("CREATED", response.getStatus());
        assertEquals("Service account created successfully", response.getMessage());
        assertNotNull(response.getCreationTimestamp());

        // Verify interactions
        verify(namespaceResource).get();
        verify(serviceAccountResource).get();
        verify(serviceAccountsOp).create(any(ServiceAccount.class));
        
        // Verify the created service account has correct properties
        ArgumentCaptor<ServiceAccount> serviceAccountCaptor = ArgumentCaptor.forClass(ServiceAccount.class);
        verify(serviceAccountsOp).create(serviceAccountCaptor.capture());
        ServiceAccount capturedServiceAccount = serviceAccountCaptor.getValue();
        assertEquals("test-sa", capturedServiceAccount.getMetadata().getName());
        assertEquals("test-namespace", capturedServiceAccount.getMetadata().getNamespace());
        assertEquals("ppcs-nsm", capturedServiceAccount.getMetadata().getLabels().get("created-by"));
        assertEquals("quarkus-app", capturedServiceAccount.getMetadata().getLabels().get("managed-by"));
    }

    @Test
    void testCreateServiceAccount_NamespaceNotFound() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("non-existent", "test-sa");
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("non-existent")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null); // Namespace doesn't exist

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            kubernetesService.createServiceAccount(request);
        });

        assertEquals("Namespace 'non-existent' does not exist", exception.getMessage());

        // Verify interactions
        verify(namespaceResource).get();
        verify(kubernetesClient, never()).serviceAccounts();
    }

    @Test
    void testCreateServiceAccount_AlreadyExists() {
        // Given
        ServiceAccountRequest request = new ServiceAccountRequest("test-namespace", "existing-sa");
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace("test-namespace", "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("test-namespace")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock service account operations
        MixedOperation<ServiceAccount, ServiceAccountList, ServiceAccountResource> serviceAccountsOp = mock(MixedOperation.class);
        ServiceAccountResource serviceAccountResource = mock(ServiceAccountResource.class);
        
        when(kubernetesClient.serviceAccounts()).thenReturn(serviceAccountsOp);
        when(serviceAccountsOp.inNamespace("test-namespace")).thenReturn(serviceAccountsOp);
        when(serviceAccountsOp.withName("existing-sa")).thenReturn(serviceAccountResource);
        
        // Mock existing service account
        ServiceAccount existingServiceAccount = createMockServiceAccount("existing-sa", "test-namespace", "2023-01-01T09:30:00Z");
        when(serviceAccountResource.get()).thenReturn(existingServiceAccount);

        // When
        ServiceAccountResponse response = kubernetesService.createServiceAccount(request);

        // Then
        assertNotNull(response);
        assertEquals("existing-sa", response.getName());
        assertEquals("test-namespace", response.getNamespace());
        assertEquals("EXISTS", response.getStatus());
        assertEquals("Service account already exists", response.getMessage());
        assertNotNull(response.getCreationTimestamp());

        // Verify interactions
        verify(namespaceResource).get();
        verify(serviceAccountResource).get();
        verify(serviceAccountsOp, never()).create(any(ServiceAccount.class));
    }

    @Test
    void testCreateCiliumNetworkPolicy_Success() {
        LOG.info("TEST: Creating CiliumNetworkPolicy - Success scenario with name generation");
        LOG.info("Scenario: Policy doesn't exist, should create with auto-generated name from labels");
        
        // Given
        CiliumNetworkPolicyRequest request = createValidCiliumNetworkPolicyRequest();
        LOG.info("Input: CiliumNetworkPolicyRequest with labels: " + request.getLabels());
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace("test-namespace", "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName("test-namespace")).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        LOG.info("Mock Setup: Namespace 'test-namespace' exists");
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        Resource<GenericKubernetesResource> customResource = mock(Resource.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace("test-namespace")).thenReturn(customResourceOp);
        when(customResourceOp.withName(anyString())).thenReturn(customResource);
        when(customResource.get()).thenReturn(null); // Policy doesn't exist
        LOG.info("Mock Setup: CiliumNetworkPolicy doesn't exist, will be created");
        
        // Mock successful creation
        GenericKubernetesResource createdPolicy = createMockCiliumNetworkPolicy("test-policy-abc123", "test-namespace");
        when(customResourceOp.create(any(GenericKubernetesResource.class))).thenReturn(createdPolicy);

        // When
        LOG.info("Executing: kubernetesService.createCiliumNetworkPolicy()");
        CiliumNetworkPolicyResponse response = kubernetesService.createCiliumNetworkPolicy(request);

        // Then
        assertNotNull(response);
        assertEquals("test-policy-abc123", response.getName());
        assertEquals("test-namespace", response.getNamespace());
        assertEquals("CREATED", response.getStatus());
        assertEquals("CiliumNetworkPolicy created successfully", response.getMessage());
        assertNotNull(response.getCreatedAt());
        LOG.info("✓ Response validation PASSED: Status=CREATED, Name=test-policy-abc123");
        
        // Verify the generated name follows the expected pattern: processed labels + random suffix
        // Pattern: alphanumeric with dashes, ending with dash + 6 random alphanumeric chars
        String generatedName = response.getGeneratedName();
        LOG.info("Testing generated name pattern: '" + generatedName + "'");
        assertTrue(generatedName.matches("^[a-z0-9-]+-[a-z0-9]{6}$"),
                   "Generated name should match pattern: alphanumeric-with-dashes-6randomchars, but was: " + generatedName);
        
        // Verify it has reasonable structure
        assertTrue(generatedName.length() > 7, "Generated name should have reasonable length");
        assertTrue(generatedName.contains("-"), "Generated name should contain dash separator");
        
        // Verify the suffix is exactly 6 alphanumeric characters
        String[] parts = generatedName.split("-");
        String suffix = parts[parts.length - 1];
        assertEquals(6, suffix.length(), "Random suffix should be exactly 6 characters");
        assertTrue(suffix.matches("[a-z0-9]{6}"), "Random suffix should be alphanumeric");
        LOG.info("✓ Generated name validation PASSED: Pattern='" + generatedName + "', Suffix='" + suffix + "'");

        // Verify interactions
        verify(namespaceResource).get();
        verify(customResource).get();
        verify(customResourceOp).create(any(GenericKubernetesResource.class));
        LOG.info("✓ Mock interactions verified: Checked namespace, checked policy existence, created policy");
    }

    @Test
    void testIsKubernetesAvailable_Success() {
        // Given
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.list()).thenReturn(mock(NamespaceList.class));

        // When
        boolean result = kubernetesService.isKubernetesAvailable();

        // Then
        assertTrue(result);
        verify(kubernetesClient).namespaces();
        verify(namespacesOp).list();
    }

    @Test
    void testIsKubernetesAvailable_Exception() {
        // Given
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.list()).thenThrow(new KubernetesClientException("Connection refused"));

        // When
        boolean result = kubernetesService.isKubernetesAvailable();

        // Then
        assertFalse(result);
        verify(kubernetesClient).namespaces();
        verify(namespacesOp).list();
    }

    // Helper methods for creating mock objects
    private Namespace createMockNamespace(String name, String creationTimestamp) {
        Namespace namespace = new Namespace();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setCreationTimestamp(creationTimestamp);
        metadata.setLabels(Map.of(
                "created-by", "ppcs-nsm",
                "managed-by", "quarkus-app"
        ));
        namespace.setMetadata(metadata);
        return namespace;
    }

    private ServiceAccount createMockServiceAccount(String name, String namespace, String creationTimestamp) {
        ServiceAccount serviceAccount = new ServiceAccount();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        metadata.setCreationTimestamp(creationTimestamp);
        metadata.setLabels(Map.of(
                "created-by", "ppcs-nsm",
                "managed-by", "quarkus-app"
        ));
        serviceAccount.setMetadata(metadata);
        return serviceAccount;
    }

    private GenericKubernetesResource createMockCiliumNetworkPolicy(String name, String namespace) {
        GenericKubernetesResource policy = new GenericKubernetesResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(name);
        metadata.setNamespace(namespace);
        metadata.setCreationTimestamp("2023-01-01T10:00:00Z");
        metadata.setLabels(Map.of(
                "created-by", "ppcs-nsm",
                "managed-by", "quarkus-app"
        ));
        policy.setMetadata(metadata);
        return policy;
    }

    private CiliumNetworkPolicyRequest createValidCiliumNetworkPolicyRequest() {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        request.setNamespace("test-namespace");
        request.setLabels(Map.of("tenant", "test_tenant", "environment", "testing"));
        
        // Add simple ingress rules - we'll mock the creation anyway
        request.setIngressRules(new ArrayList<>());
        
        return request;
    }

    @Test
    void testGetCiliumNetworkPolicyByName_Success() {
        LOG.info("TEST: Getting CiliumNetworkPolicy by name - Success scenario");
        
        // Given
        String policyName = "gb7yp-md0dy8";
        String namespace = "test-namespace";
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace(namespace, "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        Resource<GenericKubernetesResource> customResource = mock(Resource.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace(namespace)).thenReturn(customResourceOp);
        when(customResourceOp.withName(policyName)).thenReturn(customResource);
        
        // Mock existing policy with proper structure
        GenericKubernetesResource existingPolicy = createMockCiliumNetworkPolicyWithSpec(policyName, namespace);
        when(customResource.get()).thenReturn(existingPolicy);

        // When
        CiliumNetworkPolicyRequest result = kubernetesService.getCiliumNetworkPolicyByName(policyName, namespace);

        // Then
        assertNotNull(result);
        assertEquals(policyName, result.getName());
        assertEquals(namespace, result.getNamespace());
        assertEquals("GB7YP", result.getLabels().get("serial"));
        assertNotNull(result.getIngressRules());
        assertEquals(1, result.getIngressRules().size());
        
        CiliumNetworkPolicyRequest.NetworkRule ingressRule = result.getIngressRules().get(0);
        assertEquals(CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW, ingressRule.getRuleType());
        assertEquals("ingress", ingressRule.getFromLabels().get("padmini.systems/tenant-resource-type"));
        
        // Verify interactions
        verify(namespaceResource).get();
        verify(customResource).get();
        LOG.info("✓ Successfully converted Kubernetes CNP back to request format");
    }

    @Test
    void testGetCiliumNetworkPolicyByName_NotFound() {
        // Given
        String policyName = "non-existent";
        String namespace = "test-namespace";
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace(namespace, "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        Resource<GenericKubernetesResource> customResource = mock(Resource.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace(namespace)).thenReturn(customResourceOp);
        when(customResourceOp.withName(policyName)).thenReturn(customResource);
        when(customResource.get()).thenReturn(null); // Policy doesn't exist

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            kubernetesService.getCiliumNetworkPolicyByName(policyName, namespace);
        });

        assertTrue(exception.getMessage().contains("not found"));
        verify(namespaceResource).get();
        verify(customResource).get();
    }

    @Test
    void testGetCiliumNetworkPolicyByName_NamespaceNotFound() {
        // Given
        String policyName = "test-policy";
        String namespace = "non-existent";
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(null); // Namespace doesn't exist

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            kubernetesService.getCiliumNetworkPolicyByName(policyName, namespace);
        });

        assertEquals("Namespace 'non-existent' does not exist", exception.getMessage());
        verify(namespaceResource).get();
    }

    @Test
    void testGetCiliumNetworkPoliciesByNamespace_Success() {
        LOG.info("TEST: Getting all CiliumNetworkPolicies by namespace - Success scenario");
        
        // Given
        String namespace = "test-namespace";
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace(namespace, "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        GenericKubernetesResourceList policyList = mock(GenericKubernetesResourceList.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace(namespace)).thenReturn(customResourceOp);
        when(customResourceOp.list()).thenReturn(policyList);
        
        // Mock list of policies
        List<GenericKubernetesResource> policies = List.of(
                createMockCiliumNetworkPolicyWithSpec("policy1", namespace),
                createMockCiliumNetworkPolicyWithSpec("policy2", namespace)
        );
        when(policyList.getItems()).thenReturn(policies);

        // When
        List<CiliumNetworkPolicyRequest> result = kubernetesService.getCiliumNetworkPoliciesByNamespace(namespace);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        for (CiliumNetworkPolicyRequest policy : result) {
            assertNotNull(policy.getName());
            assertEquals(namespace, policy.getNamespace());
            assertNotNull(policy.getLabels());
            assertEquals("GB7YP", policy.getLabels().get("serial"));
        }
        
        // Verify interactions
        verify(namespaceResource).get();
        verify(customResourceOp).list();
        LOG.info("✓ Successfully retrieved and converted multiple policies");
    }

    @Test
    void testGetCiliumNetworkPoliciesByNamespace_EmptyResult() {
        // Given
        String namespace = "empty-namespace";
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace(namespace, "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        GenericKubernetesResourceList policyList = mock(GenericKubernetesResourceList.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace(namespace)).thenReturn(customResourceOp);
        when(customResourceOp.list()).thenReturn(policyList);
        when(policyList.getItems()).thenReturn(List.of()); // Empty list

        // When
        List<CiliumNetworkPolicyRequest> result = kubernetesService.getCiliumNetworkPoliciesByNamespace(namespace);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        
        // Verify interactions
        verify(namespaceResource).get();
        verify(customResourceOp).list();
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_Success() {
        LOG.info("TEST: Getting CiliumNetworkPolicies by endpoint selector - Success scenario");
        
        // Given
        String namespace = "test-namespace";
        Map<String, String> endpointLabels = Map.of("serial", "GB7YP");
        
        // Mock namespace check
        NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(NonNamespaceOperation.class);
        Resource<Namespace> namespaceResource = mock(Resource.class);
        Namespace existingNamespace = createMockNamespace(namespace, "2023-01-01T09:00:00Z");
        
        when(kubernetesClient.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withName(namespace)).thenReturn(namespaceResource);
        when(namespaceResource.get()).thenReturn(existingNamespace);
        
        // Mock custom resource operations
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        GenericKubernetesResourceList policyList = mock(GenericKubernetesResourceList.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inNamespace(namespace)).thenReturn(customResourceOp);
        when(customResourceOp.list()).thenReturn(policyList);
        
        // Mock list of policies - one matching, one not matching
        List<GenericKubernetesResource> policies = List.of(
                createMockCiliumNetworkPolicyWithSpec("matching-policy", namespace), // This matches serial=GB7YP
                createMockCiliumNetworkPolicyWithDifferentSpec("non-matching-policy", namespace) // This has different serial
        );
        when(policyList.getItems()).thenReturn(policies);

        // When
        List<CiliumNetworkPolicyRequest> result = kubernetesService.getCiliumNetworkPoliciesByEndpointSelector(namespace, endpointLabels);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Only one should match
        
        CiliumNetworkPolicyRequest matchingPolicy = result.get(0);
        assertNotNull(matchingPolicy.getName());
        assertEquals(namespace, matchingPolicy.getNamespace());
        assertEquals("GB7YP", matchingPolicy.getLabels().get("serial"));
        
        // Verify interactions
        verify(namespaceResource).get();
        verify(customResourceOp).list();
        LOG.info("✓ Successfully filtered policies by endpoint selector");
    }

    @Test
    void testGetCiliumNetworkPoliciesByEndpointSelector_AllNamespaces() {
        // Given
        Map<String, String> endpointLabels = Map.of("serial", "GB7YP");
        
        // Mock custom resource operations for all namespaces
        MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> customResourceOp = mock(MixedOperation.class);
        GenericKubernetesResourceList policyList = mock(GenericKubernetesResourceList.class);
        
        when(kubernetesClient.genericKubernetesResources(any(CustomResourceDefinitionContext.class)))
                .thenReturn(customResourceOp);
        when(customResourceOp.inAnyNamespace()).thenReturn(customResourceOp);
        when(customResourceOp.list()).thenReturn(policyList);
        
        // Mock list of policies from different namespaces
        List<GenericKubernetesResource> policies = List.of(
                createMockCiliumNetworkPolicyWithSpec("policy1", "namespace1"),
                createMockCiliumNetworkPolicyWithSpec("policy2", "namespace2")
        );
        when(policyList.getItems()).thenReturn(policies);

        // When
        List<CiliumNetworkPolicyRequest> result = kubernetesService.getCiliumNetworkPoliciesByEndpointSelector(null, endpointLabels);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify interactions
        verify(customResourceOp).inAnyNamespace();
        verify(customResourceOp).list();
        verify(kubernetesClient, never()).namespaces(); // Should not check namespace when searching all
    }

    // Helper method to create a mock CNP with proper spec structure
    private GenericKubernetesResource createMockCiliumNetworkPolicyWithSpec(String name, String namespace) {
        GenericKubernetesResource policy = createMockCiliumNetworkPolicy(name, namespace);
        
        // Create spec with endpointSelector and ingress rules
        Map<String, Object> spec = new HashMap<>();
        
        // EndpointSelector
        Map<String, Object> endpointSelector = new HashMap<>();
        Map<String, Object> matchLabels = new HashMap<>();
        matchLabels.put("serial", "GB7YP");
        endpointSelector.put("matchLabels", matchLabels);
        spec.put("endpointSelector", endpointSelector);
        
        // Ingress rules
        List<Map<String, Object>> ingressRules = new ArrayList<>();
        Map<String, Object> ingressRule = new HashMap<>();
        
        // fromEndpoints
        List<Map<String, Object>> fromEndpoints = new ArrayList<>();
        Map<String, Object> fromEndpoint = new HashMap<>();
        Map<String, Object> fromMatchLabels = new HashMap<>();
        fromMatchLabels.put("k8s:io.kubernetes.pod.namespace", namespace);
        fromMatchLabels.put("padmini.systems/tenant-resource-type", "ingress");
        fromEndpoint.put("matchLabels", fromMatchLabels);
        fromEndpoints.add(fromEndpoint);
        ingressRule.put("fromEndpoints", fromEndpoints);
        
        // toPorts
        List<Map<String, Object>> toPorts = new ArrayList<>();
        Map<String, Object> toPortsEntry = new HashMap<>();
        
        List<Map<String, Object>> ports = new ArrayList<>();
        Map<String, Object> port = new HashMap<>();
        port.put("port", "80");
        port.put("protocol", "TCP");
        ports.add(port);
        toPortsEntry.put("ports", ports);
        
        // HTTP rules with header matches
        Map<String, Object> rules = new HashMap<>();
        List<Map<String, Object>> httpRules = new ArrayList<>();
        Map<String, Object> httpRule = new HashMap<>();
        List<Map<String, Object>> headerMatches = new ArrayList<>();
        Map<String, Object> headerMatch = new HashMap<>();
        headerMatch.put("name", "x-real-ip");
        headerMatch.put("value", "45.248.67.9");
        headerMatches.add(headerMatch);
        httpRule.put("headerMatches", headerMatches);
        httpRules.add(httpRule);
        rules.put("http", httpRules);
        toPortsEntry.put("rules", rules);
        
        toPorts.add(toPortsEntry);
        ingressRule.put("toPorts", toPorts);
        
        ingressRules.add(ingressRule);
        spec.put("ingress", ingressRules);
        
        policy.setAdditionalProperty("spec", spec);
        return policy;
    }

    // Helper method to create a mock CNP with different spec (for filtering tests)
    private GenericKubernetesResource createMockCiliumNetworkPolicyWithDifferentSpec(String name, String namespace) {
        GenericKubernetesResource policy = createMockCiliumNetworkPolicy(name, namespace);
        
        // Create spec with different endpointSelector
        Map<String, Object> spec = new HashMap<>();
        
        // EndpointSelector with different serial
        Map<String, Object> endpointSelector = new HashMap<>();
        Map<String, Object> matchLabels = new HashMap<>();
        matchLabels.put("serial", "GB7YH"); // Different serial
        endpointSelector.put("matchLabels", matchLabels);
        spec.put("endpointSelector", endpointSelector);
        
        policy.setAdditionalProperty("spec", spec);
        return policy;
    }
}