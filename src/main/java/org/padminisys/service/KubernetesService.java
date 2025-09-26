package org.padminisys.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.padminisys.dto.NamespaceRequest;
import org.padminisys.dto.NamespaceResponse;
import org.padminisys.dto.ServiceAccountRequest;
import org.padminisys.dto.ServiceAccountResponse;
import org.padminisys.dto.CiliumNetworkPolicyRequest;
import org.padminisys.dto.CiliumNetworkPolicyResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for Kubernetes operations.
 * Handles namespace, service account, and CiliumNetworkPolicy creation using the Kubernetes client.
 */
@ApplicationScoped
public class KubernetesService {

    private static final Logger LOG = Logger.getLogger(KubernetesService.class);
    private static final String CILIUM_API_VERSION = "cilium.io/v2";
    private static final String CILIUM_KIND = "CiliumNetworkPolicy";
    private static final String NAMESPACE_LABEL_KEY = "k8s:io.kubernetes.pod.namespace";
    private static final Random RANDOM = new Random();

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
     * Creates a new namespace in the Kubernetes cluster.
     *
     * @param request the namespace creation request
     * @return the namespace creation response
     * @throws KubernetesClientException if namespace creation fails
     */
    public NamespaceResponse createNamespace(NamespaceRequest request) {
        LOG.infof("Creating namespace: %s", request.getName());

        try {
            // Check if namespace already exists
            Namespace existingNamespace = kubernetesClient.namespaces()
                    .withName(request.getName())
                    .get();

            if (existingNamespace != null) {
                LOG.warnf("Namespace %s already exists", request.getName());
                return new NamespaceResponse(
                        request.getName(),
                        "EXISTS",
                        Instant.parse(existingNamespace.getMetadata().getCreationTimestamp()),
                        "Namespace already exists"
                );
            }

            // Create new namespace
            Namespace namespace = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(request.getName())
                    .addToLabels("created-by", "ppcs-nsm")
                    .addToLabels("managed-by", "quarkus-app")
                    .endMetadata()
                    .build();

            Namespace createdNamespace = kubernetesClient.namespaces().create(namespace);

            LOG.infof("Successfully created namespace: %s", request.getName());

            return new NamespaceResponse(
                    createdNamespace.getMetadata().getName(),
                    "CREATED",
                    Instant.parse(createdNamespace.getMetadata().getCreationTimestamp()),
                    "Namespace created successfully"
            );

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to create namespace: %s", request.getName());
            throw new RuntimeException("Failed to create namespace: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new service account in the specified namespace.
     *
     * @param request the service account creation request
     * @return the service account creation response
     * @throws KubernetesClientException if service account creation fails
     */
    public ServiceAccountResponse createServiceAccount(ServiceAccountRequest request) {
        LOG.infof("Creating service account: %s in namespace: %s", request.getName(), request.getNamespace());

        try {
            // Check if namespace exists
            Namespace namespace = kubernetesClient.namespaces()
                    .withName(request.getNamespace())
                    .get();

            if (namespace == null) {
                throw new RuntimeException("Namespace '" + request.getNamespace() + "' does not exist");
            }

            // Check if service account already exists
            ServiceAccount existingServiceAccount = kubernetesClient.serviceAccounts()
                    .inNamespace(request.getNamespace())
                    .withName(request.getName())
                    .get();

            if (existingServiceAccount != null) {
                LOG.warnf("Service account %s already exists in namespace %s", request.getName(), request.getNamespace());
                return new ServiceAccountResponse(
                        request.getName(),
                        request.getNamespace(),
                        "EXISTS",
                        Instant.parse(existingServiceAccount.getMetadata().getCreationTimestamp()),
                        "Service account already exists"
                );
            }

            // Create new service account
            ServiceAccount serviceAccount = new ServiceAccountBuilder()
                    .withNewMetadata()
                    .withName(request.getName())
                    .withNamespace(request.getNamespace())
                    .addToLabels("created-by", "ppcs-nsm")
                    .addToLabels("managed-by", "quarkus-app")
                    .endMetadata()
                    .build();

            ServiceAccount createdServiceAccount = kubernetesClient.serviceAccounts()
                    .inNamespace(request.getNamespace())
                    .create(serviceAccount);

            LOG.infof("Successfully created service account: %s in namespace: %s", request.getName(), request.getNamespace());

            return new ServiceAccountResponse(
                    createdServiceAccount.getMetadata().getName(),
                    createdServiceAccount.getMetadata().getNamespace(),
                    "CREATED",
                    Instant.parse(createdServiceAccount.getMetadata().getCreationTimestamp()),
                    "Service account created successfully"
            );

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to create service account: %s in namespace: %s", request.getName(), request.getNamespace());
            throw new RuntimeException("Failed to create service account: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new CiliumNetworkPolicy in the Kubernetes cluster.
     *
     * @param request the CiliumNetworkPolicy creation request
     * @return the CiliumNetworkPolicy creation response
     * @throws KubernetesClientException if policy creation fails
     */
    public CiliumNetworkPolicyResponse createCiliumNetworkPolicy(CiliumNetworkPolicyRequest request) {
        String generatedName = generatePolicyName(request.getLabels());
        LOG.infof("Creating CiliumNetworkPolicy: %s in namespace: %s", generatedName, request.getNamespace());

        try {
            // Check if namespace exists
            Namespace namespace = kubernetesClient.namespaces()
                    .withName(request.getNamespace())
                    .get();

            if (namespace == null) {
                throw new RuntimeException("Namespace '" + request.getNamespace() + "' does not exist");
            }

            // Check if policy already exists
            GenericKubernetesResource existingPolicy = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(request.getNamespace())
                    .withName(generatedName)
                    .get();

            if (existingPolicy != null) {
                LOG.warnf("CiliumNetworkPolicy %s already exists in namespace %s", generatedName, request.getNamespace());
                return new CiliumNetworkPolicyResponse(
                        generatedName,
                        request.getNamespace(),
                        "EXISTS",
                        Instant.parse(existingPolicy.getMetadata().getCreationTimestamp()),
                        "CiliumNetworkPolicy already exists",
                        generatedName
                );
            }

            // Create the CiliumNetworkPolicy resource
            GenericKubernetesResource ciliumPolicy = createCiliumPolicyResource(request, generatedName);

            GenericKubernetesResource createdPolicy = kubernetesClient
                    .genericKubernetesResources(ciliumNetworkPolicyContext)
                    .inNamespace(request.getNamespace())
                    .create(ciliumPolicy);

            LOG.infof("Successfully created CiliumNetworkPolicy: %s in namespace: %s", generatedName, request.getNamespace());

            return new CiliumNetworkPolicyResponse(
                    createdPolicy.getMetadata().getName(),
                    createdPolicy.getMetadata().getNamespace(),
                    "CREATED",
                    Instant.parse(createdPolicy.getMetadata().getCreationTimestamp()),
                    "CiliumNetworkPolicy created successfully",
                    generatedName
            );

        } catch (KubernetesClientException e) {
            LOG.errorf(e, "Failed to create CiliumNetworkPolicy: %s in namespace: %s", generatedName, request.getNamespace());
            throw new RuntimeException("Failed to create CiliumNetworkPolicy: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a policy name based on labels following the specified naming convention.
     *
     * @param labels the labels map
     * @return generated policy name
     */
    private String generatePolicyName(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "policy-" + generateRandomSuffix();
        }

        // Concatenate all label values
        String labelValues = labels.values().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("-"));

        // Process the name according to requirements
        String processedName = labelValues
                .toLowerCase()                          // Convert to lowercase
                .replaceAll("[^a-z0-9_-]", "")         // Remove special characters except _ and -
                .replaceAll("_", "-")                   // Replace _ with -
                .replaceAll("-+", "-")                  // Replace multiple consecutive - with single -
                .replaceAll("^-|-$", "");               // Remove leading/trailing -

        // Ensure name is not empty
        if (processedName.isEmpty()) {
            processedName = "policy";
        }

        // Limit to 15 characters
        if (processedName.length() > 15) {
            processedName = processedName.substring(0, 15);
        }

        // Remove trailing - if any after truncation
        processedName = processedName.replaceAll("-$", "");

        // Add random suffix for uniqueness
        return processedName + "-" + generateRandomSuffix();
    }

    /**
     * Generates a random alphanumeric suffix of 6 characters.
     *
     * @return random suffix
     */
    private String generateRandomSuffix() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            suffix.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return suffix.toString();
    }

    /**
     * Creates a GenericKubernetesResource for CiliumNetworkPolicy.
     *
     * @param request the policy request
     * @param policyName the generated policy name
     * @return GenericKubernetesResource representing the CiliumNetworkPolicy
     */
    private GenericKubernetesResource createCiliumPolicyResource(CiliumNetworkPolicyRequest request, String policyName) {
        Map<String, Object> spec = new HashMap<>();
        
        // Set endpointSelector based on labels
        Map<String, Object> endpointSelector = new HashMap<>();
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            Map<String, Object> matchLabels = new HashMap<>();
            request.getLabels().forEach(matchLabels::put);
            endpointSelector.put("matchLabels", matchLabels);
        }
        spec.put("endpointSelector", endpointSelector);

        // Process ingress rules
        if (request.getIngressRules() != null && !request.getIngressRules().isEmpty()) {
            List<Map<String, Object>> ingressRules = request.getIngressRules().stream()
                    .map(rule -> convertToIngressRule(rule, request.getNamespace()))
                    .collect(Collectors.toList());
            spec.put("ingress", ingressRules);
        }

        // Process ingress deny rules
        if (request.getIngressDenyRules() != null && !request.getIngressDenyRules().isEmpty()) {
            List<Map<String, Object>> ingressDenyRules = request.getIngressDenyRules().stream()
                    .map(rule -> convertToIngressRule(rule, request.getNamespace()))
                    .collect(Collectors.toList());
            spec.put("ingressDeny", ingressDenyRules);
        }

        // Process egress rules
        if (request.getEgressRules() != null && !request.getEgressRules().isEmpty()) {
            List<Map<String, Object>> egressRules = request.getEgressRules().stream()
                    .map(rule -> convertToEgressRule(rule, request.getNamespace()))
                    .collect(Collectors.toList());
            spec.put("egress", egressRules);
        }

        // Process egress deny rules
        if (request.getEgressDenyRules() != null && !request.getEgressDenyRules().isEmpty()) {
            List<Map<String, Object>> egressDenyRules = request.getEgressDenyRules().stream()
                    .map(rule -> convertToEgressRule(rule, request.getNamespace()))
                    .collect(Collectors.toList());
            spec.put("egressDeny", egressDenyRules);
        }

        // Create the resource
        GenericKubernetesResource resource = new GenericKubernetesResource();
        resource.setApiVersion(CILIUM_API_VERSION);
        resource.setKind(CILIUM_KIND);
        resource.setMetadata(new ObjectMetaBuilder()
                .withName(policyName)
                .withNamespace(request.getNamespace())
                .addToLabels("created-by", "ppcs-nsm")
                .addToLabels("managed-by", "quarkus-app")
                .build());
        resource.setAdditionalProperty("spec", spec);

        return resource;
    }

    /**
     * Converts a NetworkRule to an ingress rule map.
     *
     * @param rule the network rule
     * @param namespace the namespace for automatic constraint
     * @return map representing the ingress rule
     */
    private Map<String, Object> convertToIngressRule(CiliumNetworkPolicyRequest.NetworkRule rule, String namespace) {
        Map<String, Object> ingressRule = new HashMap<>();
        
        // Add fromCIDR for IP-based rules
        if (rule.getIpAddresses() != null && !rule.getIpAddresses().isEmpty()) {
            ingressRule.put("fromCIDR", rule.getIpAddresses());
        }
        
        // Add fromEndpoints for label-based rules
        if (rule.getFromLabels() != null && !rule.getFromLabels().isEmpty()) {
            List<Map<String, Object>> fromEndpoints = new ArrayList<>();
            Map<String, Object> endpointSelector = new HashMap<>();
            Map<String, Object> matchLabels = new HashMap<>();
            
            // Add user-provided labels
            rule.getFromLabels().forEach(matchLabels::put);
            
            // Automatically add namespace constraint
            matchLabels.put(NAMESPACE_LABEL_KEY, namespace);
            
            endpointSelector.put("matchLabels", matchLabels);
            fromEndpoints.add(endpointSelector);
            ingressRule.put("fromEndpoints", fromEndpoints);
        }

        // Add toPorts if specified
        if (rule.getPorts() != null && !rule.getPorts().isEmpty()) {
            List<Map<String, Object>> toPorts = new ArrayList<>();
            Map<String, Object> toPortsEntry = new HashMap<>();
            
            List<Map<String, Object>> ports = rule.getPorts().stream()
                    .map(this::convertPortRule)
                    .collect(Collectors.toList());
            
            toPortsEntry.put("ports", ports);
            
            // Add HTTP rules if any port has header matches
            List<Map<String, Object>> httpRules = createHttpRules(rule.getPorts());
            if (!httpRules.isEmpty()) {
                Map<String, Object> rules = new HashMap<>();
                rules.put("http", httpRules);
                toPortsEntry.put("rules", rules);
            }
            
            toPorts.add(toPortsEntry);
            ingressRule.put("toPorts", toPorts);
        }

        return ingressRule;
    }

    /**
     * Converts a NetworkRule to an egress rule map.
     *
     * @param rule the network rule
     * @param namespace the namespace for automatic constraint
     * @return map representing the egress rule
     */
    private Map<String, Object> convertToEgressRule(CiliumNetworkPolicyRequest.NetworkRule rule, String namespace) {
        Map<String, Object> egressRule = new HashMap<>();
        
        // Add toCIDR for IP-based rules
        if (rule.getIpAddresses() != null && !rule.getIpAddresses().isEmpty()) {
            egressRule.put("toCIDR", rule.getIpAddresses());
        }
        
        // Add toEndpoints for label-based rules
        if (rule.getToLabels() != null && !rule.getToLabels().isEmpty()) {
            List<Map<String, Object>> toEndpoints = new ArrayList<>();
            Map<String, Object> endpointSelector = new HashMap<>();
            Map<String, Object> matchLabels = new HashMap<>();
            
            // Add user-provided labels
            rule.getToLabels().forEach(matchLabels::put);
            
            // Automatically add namespace constraint
            matchLabels.put(NAMESPACE_LABEL_KEY, namespace);
            
            endpointSelector.put("matchLabels", matchLabels);
            toEndpoints.add(endpointSelector);
            egressRule.put("toEndpoints", toEndpoints);
        }

        // Add toPorts if specified
        if (rule.getPorts() != null && !rule.getPorts().isEmpty()) {
            List<Map<String, Object>> toPorts = new ArrayList<>();
            Map<String, Object> toPortsEntry = new HashMap<>();
            
            List<Map<String, Object>> ports = rule.getPorts().stream()
                    .map(this::convertPortRule)
                    .collect(Collectors.toList());
            
            toPortsEntry.put("ports", ports);
            
            // Add HTTP rules if any port has header matches
            List<Map<String, Object>> httpRules = createHttpRules(rule.getPorts());
            if (!httpRules.isEmpty()) {
                Map<String, Object> rules = new HashMap<>();
                rules.put("http", httpRules);
                toPortsEntry.put("rules", rules);
            }
            
            toPorts.add(toPortsEntry);
            egressRule.put("toPorts", toPorts);
        }

        return egressRule;
    }

    /**
     * Converts a PortRule to a port map.
     *
     * @param portRule the port rule
     * @return map representing the port
     */
    private Map<String, Object> convertPortRule(CiliumNetworkPolicyRequest.PortRule portRule) {
        Map<String, Object> port = new HashMap<>();
        port.put("port", portRule.getPort().toString());
        port.put("protocol", portRule.getProtocol().name());
        
        if (portRule.getEndPort() != null) {
            port.put("endPort", portRule.getEndPort());
        }
        
        return port;
    }

    /**
     * Creates HTTP rules from port rules that have header matches.
     *
     * @param portRules the list of port rules
     * @return list of HTTP rules
     */
    private List<Map<String, Object>> createHttpRules(List<CiliumNetworkPolicyRequest.PortRule> portRules) {
        List<Map<String, Object>> httpRules = new ArrayList<>();
        
        for (CiliumNetworkPolicyRequest.PortRule portRule : portRules) {
            if (portRule.getHeaderMatches() != null && !portRule.getHeaderMatches().isEmpty()) {
                for (CiliumNetworkPolicyRequest.HeaderMatch headerMatch : portRule.getHeaderMatches()) {
                    Map<String, Object> httpRule = new HashMap<>();
                    List<Map<String, Object>> headerMatches = new ArrayList<>();
                    
                    Map<String, Object> headerMatchMap = new HashMap<>();
                    headerMatchMap.put("name", headerMatch.getName());
                    headerMatchMap.put("value", headerMatch.getValue());
                    headerMatches.add(headerMatchMap);
                    
                    httpRule.put("headerMatches", headerMatches);
                    httpRules.add(httpRule);
                }
            }
        }
        
        return httpRules;
    }

    /**
     * Checks if the Kubernetes client is properly configured and can connect to the cluster.
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean isKubernetesAvailable() {
        try {
            kubernetesClient.namespaces().list();
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Kubernetes client is not available: %s", e.getMessage());
            return false;
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
            Namespace namespaceObj = kubernetesClient.namespaces()
                    .withName(namespace)
                    .get();

            if (namespaceObj == null) {
                throw new RuntimeException("Namespace '" + namespace + "' does not exist");
            }

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
            Namespace namespaceObj = kubernetesClient.namespaces()
                    .withName(namespace)
                    .get();

            if (namespaceObj == null) {
                throw new RuntimeException("Namespace '" + namespace + "' does not exist");
            }

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
                Namespace namespaceObj = kubernetesClient.namespaces()
                        .withName(namespace)
                        .get();

                if (namespaceObj == null) {
                    throw new RuntimeException("Namespace '" + namespace + "' does not exist");
                }

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
     *
     * @param policy the Kubernetes CNP resource
     * @return the converted request object
     */
    @SuppressWarnings("unchecked")
    private CiliumNetworkPolicyRequest convertKubernetesCNPToRequest(GenericKubernetesResource policy) {
        CiliumNetworkPolicyRequest request = new CiliumNetworkPolicyRequest();
        
        // Set namespace
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
                Map<String, String> labels = new HashMap<>();
                matchLabels.forEach((key, value) -> labels.put(key, value.toString()));
                request.setLabels(labels);
            }
        }
        
        // Convert ingress rules
        List<Map<String, Object>> ingressRules = (List<Map<String, Object>>) spec.get("ingress");
        if (ingressRules != null) {
            request.setIngressRules(convertKubernetesRulesToNetworkRules(ingressRules, CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW));
        }
        
        // Convert ingress deny rules
        List<Map<String, Object>> ingressDenyRules = (List<Map<String, Object>>) spec.get("ingressDeny");
        if (ingressDenyRules != null) {
            request.setIngressDenyRules(convertKubernetesRulesToNetworkRules(ingressDenyRules, CiliumNetworkPolicyRequest.RuleType.INGRESS_DENY));
        }
        
        // Convert egress rules
        List<Map<String, Object>> egressRules = (List<Map<String, Object>>) spec.get("egress");
        if (egressRules != null) {
            request.setEgressRules(convertKubernetesRulesToNetworkRules(egressRules, CiliumNetworkPolicyRequest.RuleType.EGRESS_ALLOW));
        }
        
        // Convert egress deny rules
        List<Map<String, Object>> egressDenyRules = (List<Map<String, Object>>) spec.get("egressDeny");
        if (egressDenyRules != null) {
            request.setEgressDenyRules(convertKubernetesRulesToNetworkRules(egressDenyRules, CiliumNetworkPolicyRequest.RuleType.EGRESS_DENY));
        }
        
        return request;
    }

    /**
     * Converts Kubernetes CNP rules back to our NetworkRule format.
     *
     * @param kubernetesRules the Kubernetes rules
     * @param ruleType the rule type
     * @return list of converted network rules
     */
    @SuppressWarnings("unchecked")
    private List<CiliumNetworkPolicyRequest.NetworkRule> convertKubernetesRulesToNetworkRules(
            List<Map<String, Object>> kubernetesRules, CiliumNetworkPolicyRequest.RuleType ruleType) {
        
        List<CiliumNetworkPolicyRequest.NetworkRule> networkRules = new ArrayList<>();
        
        for (Map<String, Object> kubernetesRule : kubernetesRules) {
            CiliumNetworkPolicyRequest.NetworkRule networkRule = new CiliumNetworkPolicyRequest.NetworkRule();
            networkRule.setRuleType(ruleType);
            
            // Handle IP-based rules (fromCIDR/toCIDR)
            List<String> cidrs = null;
            if (ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW || ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_DENY) {
                cidrs = (List<String>) kubernetesRule.get("fromCIDR");
            } else {
                cidrs = (List<String>) kubernetesRule.get("toCIDR");
            }
            
            if (cidrs != null && !cidrs.isEmpty()) {
                networkRule.setIpAddresses(cidrs);
            }
            
            // Handle label-based rules (fromEndpoints/toEndpoints)
            List<Map<String, Object>> endpoints = null;
            if (ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW || ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_DENY) {
                endpoints = (List<Map<String, Object>>) kubernetesRule.get("fromEndpoints");
            } else {
                endpoints = (List<Map<String, Object>>) kubernetesRule.get("toEndpoints");
            }
            
            if (endpoints != null && !endpoints.isEmpty()) {
                // Take the first endpoint selector (our API supports one set of labels per rule)
                Map<String, Object> firstEndpoint = endpoints.get(0);
                Map<String, Object> matchLabels = (Map<String, Object>) firstEndpoint.get("matchLabels");
                
                if (matchLabels != null) {
                    Map<String, String> labels = new HashMap<>();
                    matchLabels.forEach((key, value) -> {
                        // Skip the automatic namespace constraint we added
                        if (!NAMESPACE_LABEL_KEY.equals(key)) {
                            labels.put(key, value.toString());
                        }
                    });
                    
                    if (!labels.isEmpty()) {
                        if (ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_ALLOW || ruleType == CiliumNetworkPolicyRequest.RuleType.INGRESS_DENY) {
                            networkRule.setFromLabels(labels);
                        } else {
                            networkRule.setToLabels(labels);
                        }
                    }
                }
            }
            
            // Handle port rules
            List<Map<String, Object>> toPorts = (List<Map<String, Object>>) kubernetesRule.get("toPorts");
            if (toPorts != null && !toPorts.isEmpty()) {
                Map<String, Object> toPortsEntry = toPorts.get(0);
                List<Map<String, Object>> ports = (List<Map<String, Object>>) toPortsEntry.get("ports");
                
                if (ports != null && !ports.isEmpty()) {
                    List<CiliumNetworkPolicyRequest.PortRule> portRules = new ArrayList<>();
                    
                    for (Map<String, Object> port : ports) {
                        CiliumNetworkPolicyRequest.PortRule portRule = new CiliumNetworkPolicyRequest.PortRule();
                        
                        // Set protocol
                        String protocol = (String) port.get("protocol");
                        if (protocol != null) {
                            portRule.setProtocol(CiliumNetworkPolicyRequest.Protocol.valueOf(protocol));
                        }
                        
                        // Set port
                        Object portObj = port.get("port");
                        if (portObj != null) {
                            portRule.setPort(Integer.valueOf(portObj.toString()));
                        }
                        
                        // Set end port if present
                        Object endPortObj = port.get("endPort");
                        if (endPortObj != null) {
                            portRule.setEndPort(Integer.valueOf(endPortObj.toString()));
                        }
                        
                        portRules.add(portRule);
                    }
                    
                    // Handle HTTP header matches
                    Map<String, Object> rules = (Map<String, Object>) toPortsEntry.get("rules");
                    if (rules != null) {
                        List<Map<String, Object>> httpRules = (List<Map<String, Object>>) rules.get("http");
                        if (httpRules != null && !httpRules.isEmpty() && !portRules.isEmpty()) {
                            // Add header matches to the first port rule (our API structure)
                            CiliumNetworkPolicyRequest.PortRule firstPortRule = portRules.get(0);
                            List<CiliumNetworkPolicyRequest.HeaderMatch> headerMatches = new ArrayList<>();
                            
                            for (Map<String, Object> httpRule : httpRules) {
                                List<Map<String, Object>> headerMatchesList = (List<Map<String, Object>>) httpRule.get("headerMatches");
                                if (headerMatchesList != null) {
                                    for (Map<String, Object> headerMatch : headerMatchesList) {
                                        String name = (String) headerMatch.get("name");
                                        String value = (String) headerMatch.get("value");
                                        if (name != null && value != null) {
                                            headerMatches.add(new CiliumNetworkPolicyRequest.HeaderMatch(name, value));
                                        }
                                    }
                                }
                            }
                            
                            if (!headerMatches.isEmpty()) {
                                firstPortRule.setHeaderMatches(headerMatches);
                            }
                        }
                    }
                    
                    networkRule.setPorts(portRules);
                }
            }
            
            networkRules.add(networkRule);
        }
        
        return networkRules;
    }
}