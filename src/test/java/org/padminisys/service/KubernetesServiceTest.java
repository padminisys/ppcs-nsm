package org.padminisys.service;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class KubernetesServiceTest {

    @Test
    void testPolicyNameGeneration_WithValidLabels() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "dell_computers");
        labels.put("environment", "production");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        assertTrue(generatedName.contains("dell-computers") || generatedName.contains("production"));
        assertTrue(generatedName.matches(".*-[a-z0-9]{6}$")); // Ends with 6-char random suffix
        assertFalse(generatedName.contains("_")); // Underscores should be replaced with hyphens
    }

    @Test
    void testPolicyNameGeneration_WithSpecialCharacters() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "Dell_Computers@123!");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        // Should contain only lowercase letters, numbers, and hyphens
        assertTrue(generatedName.matches("^[a-z0-9-]+$"));
        assertFalse(generatedName.contains("@"));
        assertFalse(generatedName.contains("!"));
        assertFalse(generatedName.contains("_"));
        assertTrue(generatedName.contains("dell") || generatedName.contains("computers"));
    }

    @Test
    void testPolicyNameGeneration_LongName() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "very_long_tenant_name_that_exceeds_fifteen_characters");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        // Should be truncated to 15 chars + hyphen + 6 char suffix = max 22 chars
        assertTrue(generatedName.length() <= 22);
        assertTrue(generatedName.startsWith("very-long-tenan"));
        assertTrue(generatedName.matches(".*-[a-z0-9]{6}$"));
    }

    @Test
    void testPolicyNameGeneration_EmptyLabels() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        assertTrue(generatedName.startsWith("policy-"));
        assertTrue(generatedName.matches("policy-[a-z0-9]{6}"));
        assertEquals(13, generatedName.length()); // "policy-" (7) + 6 random chars
    }

    @Test
    void testPolicyNameGeneration_NullLabels() {
        // Given
        KubernetesService service = new KubernetesService();
        
        // When
        String generatedName = invokeGeneratePolicyName(service, null);
        
        // Then
        assertNotNull(generatedName);
        assertTrue(generatedName.startsWith("policy-"));
        assertTrue(generatedName.matches("policy-[a-z0-9]{6}"));
    }

    @Test
    void testPolicyNameGeneration_MultipleHyphens() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "dell---computers");
        labels.put("env", "test--env");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        // Multiple consecutive hyphens should be replaced with single hyphen
        assertFalse(generatedName.contains("--"));
        assertFalse(generatedName.contains("---"));
        assertTrue(generatedName.contains("dell") || generatedName.contains("computers") || generatedName.contains("test") || generatedName.contains("env"));
    }

    @Test
    void testPolicyNameGeneration_LeadingTrailingHyphens() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "-dell_computers-");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        // Should not start or end with hyphen (before the random suffix)
        String nameWithoutSuffix = generatedName.substring(0, generatedName.lastIndexOf('-'));
        assertFalse(nameWithoutSuffix.startsWith("-"));
        assertFalse(nameWithoutSuffix.endsWith("-"));
        assertTrue(generatedName.contains("dell-computers"));
    }

    @Test
    void testPolicyNameGeneration_OnlySpecialCharacters() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "@#$%^&*()");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        // Should fallback to "policy-" when all characters are removed
        assertTrue(generatedName.startsWith("policy-"));
        assertTrue(generatedName.matches("policy-[a-z0-9]{6}"));
    }

    @RepeatedTest(10)
    void testPolicyNameGeneration_RandomSuffixUniqueness() {
        // Given
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "dell_computers");
        
        // When
        String name1 = invokeGeneratePolicyName(service, labels);
        String name2 = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(name1);
        assertNotNull(name2);
        assertNotEquals(name1, name2); // Should be different due to random suffix
        
        // Both should have same prefix but different suffix
        String prefix1 = name1.substring(0, name1.lastIndexOf('-'));
        String prefix2 = name2.substring(0, name2.lastIndexOf('-'));
        assertEquals(prefix1, prefix2);
        
        String suffix1 = name1.substring(name1.lastIndexOf('-') + 1);
        String suffix2 = name2.substring(name2.lastIndexOf('-') + 1);
        assertNotEquals(suffix1, suffix2);
        
        // Both suffixes should be 6 characters
        assertEquals(6, suffix1.length());
        assertEquals(6, suffix2.length());
        
        // Both suffixes should be alphanumeric
        assertTrue(suffix1.matches("[a-z0-9]{6}"));
        assertTrue(suffix2.matches("[a-z0-9]{6}"));
    }

    @Test
    void testPolicyNameGeneration_ExampleFromRequirement() {
        // Given - Example from the requirement: tenant=dell_computers should become dell-computers-sw5d8d
        KubernetesService service = new KubernetesService();
        Map<String, String> labels = new HashMap<>();
        labels.put("tenant", "dell_computers");
        
        // When
        String generatedName = invokeGeneratePolicyName(service, labels);
        
        // Then
        assertNotNull(generatedName);
        assertTrue(generatedName.contains("dell-computers"));
        assertTrue(generatedName.matches(".*-[a-z0-9]{6}$"));
        assertEquals(21, generatedName.length()); // Actual observed length
    }

    /**
     * Helper method to invoke the private generatePolicyName method using reflection
     */
    private String invokeGeneratePolicyName(KubernetesService service, Map<String, String> labels) {
        try {
            var method = KubernetesService.class.getDeclaredMethod("generatePolicyName", Map.class);
            method.setAccessible(true);
            return (String) method.invoke(service, labels);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke generatePolicyName method", e);
        }
    }
}