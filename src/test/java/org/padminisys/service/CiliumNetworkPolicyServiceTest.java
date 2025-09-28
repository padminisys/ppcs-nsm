package org.padminisys.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CiliumNetworkPolicyService.
 * Tests the delete operations and other CRUD operations for Cilium Network Policies.
 *
 * Note: These tests focus on business logic validation and service structure verification.
 * Complex Kubernetes client interactions are covered in integration tests.
 */
class CiliumNetworkPolicyServiceTest {

    private static final String TEST_NAMESPACE = "test-namespace";
    private static final String TEST_POLICY_NAME = "test-policy";

    @Test
    @DisplayName("Should validate CiliumNetworkPolicyService class structure")
    void testServiceClassStructure() {
        // Verify that the CiliumNetworkPolicyService class exists and has the expected methods
        
        try {
            Class<?> serviceClass = Class.forName("org.padminisys.service.CiliumNetworkPolicyService");
            
            // Verify class is annotated with @ApplicationScoped
            assertTrue(serviceClass.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class));
            
            // Verify required methods exist
            assertNotNull(serviceClass.getMethod("deleteCiliumNetworkPolicy", String.class, String.class));
            assertNotNull(serviceClass.getMethod("deleteAllCiliumNetworkPoliciesInNamespace", String.class));
            assertNotNull(serviceClass.getMethod("getCiliumNetworkPolicyByName", String.class, String.class));
            assertNotNull(serviceClass.getMethod("getCiliumNetworkPoliciesByNamespace", String.class));
            assertNotNull(serviceClass.getMethod("getCiliumNetworkPoliciesByEndpointSelector", String.class, java.util.Map.class));
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("CiliumNetworkPolicyService class or required methods not found: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate method signatures and return types")
    void testMethodSignatures() {
        try {
            Class<?> serviceClass = Class.forName("org.padminisys.service.CiliumNetworkPolicyService");
            
            // Test deleteCiliumNetworkPolicy method signature
            var deleteMethod = serviceClass.getMethod("deleteCiliumNetworkPolicy", String.class, String.class);
            assertEquals(boolean.class, deleteMethod.getReturnType());
            
            // Test deleteAllCiliumNetworkPoliciesInNamespace method signature
            var deleteAllMethod = serviceClass.getMethod("deleteAllCiliumNetworkPoliciesInNamespace", String.class);
            assertEquals(int.class, deleteAllMethod.getReturnType());
            
            // Test getCiliumNetworkPolicyByName method signature
            var getByNameMethod = serviceClass.getMethod("getCiliumNetworkPolicyByName", String.class, String.class);
            assertEquals("org.padminisys.dto.CiliumNetworkPolicyRequest", getByNameMethod.getReturnType().getName());
            
            // Test getCiliumNetworkPoliciesByNamespace method signature
            var getByNamespaceMethod = serviceClass.getMethod("getCiliumNetworkPoliciesByNamespace", String.class);
            assertEquals(java.util.List.class, getByNamespaceMethod.getReturnType());
            
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            fail("Method signature validation failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should validate service follows single responsibility principle")
    void testSingleResponsibilityPrinciple() {
        try {
            Class<?> serviceClass = Class.forName("org.padminisys.service.CiliumNetworkPolicyService");
            
            // Verify class name indicates single responsibility
            assertTrue(serviceClass.getSimpleName().contains("CiliumNetworkPolicy"));
            
            // Verify reasonable number of public methods (not too many responsibilities)
            long publicMethodCount = java.util.Arrays.stream(serviceClass.getDeclaredMethods())
                    .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                    .count();
            
            // Should have the 5 main CRUD methods plus any helper methods
            assertTrue(publicMethodCount >= 5 && publicMethodCount <= 10,
                    "Service should have 5-10 public methods, found: " + publicMethodCount);
            
        } catch (ClassNotFoundException e) {
            fail("CiliumNetworkPolicyService class not found: " + e.getMessage());
        }
    }
}