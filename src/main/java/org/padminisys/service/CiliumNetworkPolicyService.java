package org.padminisys.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.padminisys.dto.CiliumNetworkPolicyRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class dedicated to CiliumNetworkPolicy operations.
 * Handles CRUD operations for Cilium Network Policies using the Kubernetes client.
 * Follows single responsibility principle by focusing only on CNP operations.
 */
@ApplicationScoped
public class CiliumNetworkPolicyService {

    private static final Logger LOG = Logger.getLogger(CiliumNetworkPolicyService.class);
    private static final String NAMESPACE_LABEL_KEY = "k8s:io.kubernetes.pod.namespace";

    @Inject
    KubernetesClient kubernetesClient;

    private final CustomResourceDefinitionContext ciliumNetworkPolicyContext =
        new CustomResourceDefinitionContext.Builder()
            .withGroup("cilium.io")
            .withVersion("v2")
            .withScope("Namespaced")
            .withPlural("ciliumnetworkpolicies")
            .build();

    /**
     * Deletes a specific CiliumNetworkPolicy by name in the given namespace.
     * Equivalent to: kubectl delete cnp <policy-name> -n <namespace>
     *
     * @param policyName the name of the policy to delete
     * @param namespace the namespace containing the policy
     * @return true if policy was deleted, false if policy didn't exist
     * @throws RuntimeException if namespace doesn't exist or deletion fails
     */
    public boolean deleteCiliumNetworkPolicy(String policyName, String namespace) {
        LOG.infof("Deleting CiliumNetworkPolicy: %s in namespace: %s", policyName, namespace);

        try {
            // Check if namespace exists
            validateNamespaceExists(namespace);

            // Check if policy exists
            GenericKubernetesResource existingPolicy = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .withName(policyName)
                    .get();

            if (existingPolicy == null) {
                LOG.warnf("CiliumNetworkPolicy %s not found in namespace %s", policyName, namespace);
                return false;
            }

            // Delete the policy
            boolean deleted = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .withName(policyName)
                    .delete()
                    .size() > 0;

            if (deleted) {
                LOG.infof("Successfully deleted CiliumNetworkPolicy: %s in namespace: %s", policyName, namespace);
            } else {
                LOG.warnf("Failed to delete CiliumNetworkPolicy: %s in namespace: %s", policyName, namespace);
            }

            return deleted;

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to delete CiliumNetworkPolicy: %s in namespace: %s", policyName, namespace);
            throw new RuntimeException("Failed to delete CiliumNetworkPolicy: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all CiliumNetworkPolicies in the given namespace.
     * Equivalent to: kubectl delete cnp -n <namespace> --all
     *
     * @param namespace the namespace to delete all policies from
     * @return the number of policies that were deleted
     * @throws RuntimeException if namespace doesn't exist or deletion fails
     */
    public int deleteAllCiliumNetworkPoliciesInNamespace(String namespace) {
        LOG.infof("Deleting all CiliumNetworkPolicies in namespace: %s", namespace);

        try {
            // Check if namespace exists
            validateNamespaceExists(namespace);

            // Get all policies in the namespace first
            List<GenericKubernetesResource> policies = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .list()
                    .getItems();

            if (policies.isEmpty()) {
                LOG.infof("No CiliumNetworkPolicies found in namespace: %s", namespace);
                return 0;
            }

            LOG.infof("Found %d CiliumNetworkPolicies to delete in namespace: %s", policies.size(), namespace);

            // Delete all policies in the namespace
            boolean deleted = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .delete()
                    .size() > 0;

            int deletedCount = deleted ? policies.size() : 0;
            LOG.infof("Successfully deleted %d CiliumNetworkPolicies in namespace: %s", deletedCount, namespace);

            return deletedCount;

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to delete all CiliumNetworkPolicies in namespace: %s", namespace);
            throw new RuntimeException("Failed to delete all CiliumNetworkPolicies: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a CiliumNetworkPolicy by name and converts it back to the original request format.
     *
     * @param name the name of the policy
     * @param namespace the namespace of the policy
     * @return the policy converted to request format
     * @throws RuntimeException if policy is not found or conversion fails
     */
    public CiliumNetworkPolicyRequest getCiliumNetworkPolicyByName(String name, String namespace) {
        LOG.infof("Getting CiliumNetworkPolicy: %s in namespace: %s", name, namespace);

        try {
            // Check if namespace exists
            validateNamespaceExists(namespace);

            // Get the policy
            GenericKubernetesResource policy = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .withName(name)
                    .get();

            if (policy == null) {
                throw new RuntimeException("CiliumNetworkPolicy '" + name + "' not found in namespace '" + namespace + "'");
            }

            return convertKubernetesCNPToRequest(policy);

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to get CiliumNetworkPolicy: %s in namespace: %s", name, namespace);
            throw new RuntimeException("Failed to get CiliumNetworkPolicy: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all CiliumNetworkPolicies in a namespace and converts them back to the original request format.
     *
     * @param namespace the namespace to search in
     * @return list of policies converted to request format
     * @throws RuntimeException if namespace doesn't exist or retrieval fails
     */
    public List<CiliumNetworkPolicyRequest> getCiliumNetworkPoliciesByNamespace(String namespace) {
        LOG.infof("Getting all CiliumNetworkPolicies in namespace: %s", namespace);

        try {
            // Check if namespace exists
            validateNamespaceExists(namespace);

            // Get all policies in the namespace
            List<GenericKubernetesResource> policies = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(namespace)
                    .list()
                    .getItems();

            return policies.stream()
                    .map(this::convertKubernetesCNPToRequest)
                    .collect(Collectors.toList());

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to get CiliumNetworkPolicies in namespace: %s", namespace);
            throw new RuntimeException("Failed to get CiliumNetworkPolicies: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves CiliumNetworkPolicies by endpoint selector labels and converts them back to the original request format.
     *
     * @param namespace the namespace to search in (optional, if null searches all namespaces)
     * @param endpointLabels the endpoint selector labels to match
     * @return list of policies converted to request format
     * @throws RuntimeException if retrieval fails
     */
    public List<CiliumNetworkPolicyRequest> getCiliumNetworkPoliciesByEndpointSelector(String namespace, Map<String, String> endpointLabels) {
        LOG.infof("Getting CiliumNetworkPolicies by endpoint selector labels: %s in namespace: %s", endpointLabels, namespace);

        try {
            List<GenericKubernetesResource> allPolicies;
            
            if (namespace != null && !namespace.trim().isEmpty()) {
                // Check if namespace exists
                validateNamespaceExists(namespace);

                // Get policies from specific namespace
                allPolicies = kubernetesClient
                        .genericKubernetesResources(ciliumNetworkPolicyContext)
                        .inNamespace(namespace)
                        .list()
                        .getItems();
            } else {
                // Get policies from all namespaces
                allPolicies = kubernetesClient
                        .genericKubernetesResources(ciliumNetworkPolicyContext)
                        .inAnyNamespace()
                        .list()
                        .getItems();
            }

            // Filter policies by endpoint selector labels
            return allPolicies.stream()
                    .filter(policy -> matchesEndpointSelector(policy, endpointLabels))
                    .map(this::convertKubernetesCNPToRequest)
                    .collect(Collectors.toList());

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to get CiliumNetworkPolicies by endpoint selector: %s", endpointLabels);
            throw new RuntimeException("Failed to get CiliumNetworkPolicies: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a namespace exists in the cluster.
     *
     * @param namespace the namespace to validate
     * @throws RuntimeException if namespace doesn't exist
     */
    private void validateNamespaceExists(String namespace) {
        Namespace namespaceObj = kubernetesClient.namespaces()
                .withName(namespace)
                .get();

        if (namespaceObj == null) {
            throw new RuntimeException("Namespace '" + namespace + "' does not exist");
        }
    }

    /**
     * Checks if a policy matches the given endpoint selector labels.
     *
     * @param policy the Kubernetes CNP resource
     * @param targetLabels the labels to match against
     * @return true if the policy's endpoint selector matches the target labels
     */
    @SuppressWarnings("unchecked")
    private boolean matchesEndpointSelector(GenericKubernetesResource policy, Map<String, String> targetLabels) {
        try {
            Map<String, Object> spec = (Map<String, Object>) policy.getAdditionalProperties().get("spec");
            if (spec == null) {
                return false;
            }

            Map<String, Object> endpointSelector = (Map<String, Object>) spec.get("endpointSelector");
            if (endpointSelector == null) {
                return false;
            }

            Map<String, Object> matchLabels = (Map<String, Object>) endpointSelector.get("matchLabels");
            if (matchLabels == null || matchLabels.isEmpty()) {
                return targetLabels == null || targetLabels.isEmpty();
            }

            // Check if all target labels match the policy's endpoint selector
            for (Map.Entry<String, String> targetEntry : targetLabels.entrySet()) {
                Object policyValue = matchLabels.get(targetEntry.getKey());
                if (policyValue == null || !policyValue.toString().equals(targetEntry.getValue())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            LOG.warnf("Error checking endpoint selector match for policy %s: %s",
                     policy.getMetadata().getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Converts a Kubernetes CiliumNetworkPolicy resource back to our API request format.
     * This is a simplified version focusing on the essential conversion logic.
     *
     * @param policy the Kubernetes CNP resource
     * @return the converted request object
     */
    @SuppressWarnings("unchecked")
    private CiliumNetworkPolicyRequest convertKubernetesCNPToRequest(GenericKubernetesResource policy) {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        
        // Set name and namespace
        request.setName(policy.getMetadata().getName());
        request.setNamespace(policy.getMetadata().getNamespace());
        
        // Extract spec
        Map<String, Object> spec = (Map<String, Object>) policy.getAdditionalProperties().get("spec");
        if (spec == null) {
            LOG.warnf("Policy %s has no spec", policy.getMetadata().getName());
            return request;
        }
        
        // Extract endpoint selector labels
        Map<String, Object> endpointSelector = (Map<String, Object>) spec.get("endpointSelector");
        if (endpointSelector != null) {
            Map<String, Object> matchLabels = (Map<String, Object>) endpointSelector.get("matchLabels");
            if (matchLabels != null) {
                Map<String, String> labels = matchLabels.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().toString()
                        ));
                request.setLabels(labels);
            }
        }
        
        return request;
    }
}