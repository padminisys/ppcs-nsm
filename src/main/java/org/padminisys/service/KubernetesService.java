package org.padminisys.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.padminisys.dto.NamespaceRequest;
import org.padminisys.dto.NamespaceResponse;
import org.padminisys.dto.ServiceAccountRequest;
import org.padminisys.dto.ServiceAccountResponse;

import java.time.Instant;

/**
 * Service class for Kubernetes operations.
 * Handles namespace and service account creation using the Kubernetes client.
 */
@ApplicationScoped
public class KubernetesService {

    private static final Logger LOG = Logger.getLogger(KubernetesService.class);

    @Inject
    KubernetesClient kubernetesClient;

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
}