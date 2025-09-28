package org.padminisys.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.padminisys.dto.CiliumNetworkPolicyRequest;
import org.padminisys.dto.CiliumNetworkPolicyResponse;
import org.padminisys.service.KubernetesService;
import org.padminisys.service.CiliumNetworkPolicyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoint for CiliumNetworkPolicy operations.
 */
@Path("/api/v1/cilium-network-policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "CiliumNetworkPolicy Management", description = "Operations for managing Cilium Network Policies")
public class CiliumNetworkPolicyResource {

    private static final Logger LOG = Logger.getLogger(CiliumNetworkPolicyResource.class);

    @Inject
    KubernetesService kubernetesService;

    @Inject
    CiliumNetworkPolicyService ciliumNetworkPolicyService;

    @POST
    @Operation(
            summary = "Create a new CiliumNetworkPolicy",
            description = "Creates a new CiliumNetworkPolicy with the specified configuration including ingress/egress rules"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "CiliumNetworkPolicy created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CiliumNetworkPolicyResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicy already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CiliumNetworkPolicyResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Namespace not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response createCiliumNetworkPolicy(@Valid CiliumNetworkPolicyRequest request) {
        LOG.infof("Received request to create CiliumNetworkPolicy in namespace: %s with labels: %s", 
                  request.getNamespace(), request.getLabels());

        try {
            CiliumNetworkPolicyResponse response = kubernetesService.createCiliumNetworkPolicy(request);
            
            if ("CREATED".equals(response.getStatus())) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.ok(response).build();
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error creating CiliumNetworkPolicy in namespace: %s", request.getNamespace());
            
            if (e.getMessage().contains("does not exist")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Namespace not found: " + e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create CiliumNetworkPolicy: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error creating CiliumNetworkPolicy in namespace: %s", request.getNamespace());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{name}")
    @Operation(
            summary = "Get a CiliumNetworkPolicy by name",
            description = "Retrieves a specific CiliumNetworkPolicy by its name and converts it back to the original request format"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicy found and returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = CiliumNetworkPolicyRequest.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "CiliumNetworkPolicy not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response getCiliumNetworkPolicyByName(
            @PathParam("name") String name,
            @QueryParam("namespace") @NotBlank String namespace) {
        LOG.infof("Received request to get CiliumNetworkPolicy: %s in namespace: %s", name, namespace);

        try {
            CiliumNetworkPolicyRequest policy = ciliumNetworkPolicyService.getCiliumNetworkPolicyByName(name, namespace);
            return Response.ok(policy).build();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error getting CiliumNetworkPolicy: %s in namespace: %s", name, namespace);
            
            if (e.getMessage().contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("CiliumNetworkPolicy not found: " + e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get CiliumNetworkPolicy: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error getting CiliumNetworkPolicy: %s in namespace: %s", name, namespace);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/namespace/{namespace}")
    @Operation(
            summary = "Get all CiliumNetworkPolicies in a namespace",
            description = "Retrieves all CiliumNetworkPolicies in the specified namespace and converts them back to the original request format"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicies found and returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Namespace not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response getCiliumNetworkPoliciesByNamespace(@PathParam("namespace") String namespace) {
        LOG.infof("Received request to get all CiliumNetworkPolicies in namespace: %s", namespace);

        try {
            List<CiliumNetworkPolicyRequest> policies = ciliumNetworkPolicyService.getCiliumNetworkPoliciesByNamespace(namespace);
            return Response.ok(policies).build();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error getting CiliumNetworkPolicies in namespace: %s", namespace);
            
            if (e.getMessage().contains("does not exist")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Namespace not found: " + e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get CiliumNetworkPolicies: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error getting CiliumNetworkPolicies in namespace: %s", namespace);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/endpoint-selector")
    @Operation(
            summary = "Get CiliumNetworkPolicies by endpoint selector labels",
            description = "Retrieves all CiliumNetworkPolicies that match the specified endpoint selector labels"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicies found and returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON)
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response getCiliumNetworkPoliciesByEndpointSelector(
            @QueryParam("namespace") String namespace,
            @QueryParam("labels") @NotBlank String labelsParam) {
        LOG.infof("Received request to get CiliumNetworkPolicies by endpoint selector labels: %s in namespace: %s", labelsParam, namespace);

        try {
            // Parse labels from query parameter (format: key1=value1,key2=value2)
            Map<String, String> labels = parseLabelsFromQueryParam(labelsParam);
            
            List<CiliumNetworkPolicyRequest> policies = ciliumNetworkPolicyService.getCiliumNetworkPoliciesByEndpointSelector(namespace, labels);
            return Response.ok(policies).build();
        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "Invalid labels parameter: %s", labelsParam);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid labels parameter: " + e.getMessage()))
                    .build();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error getting CiliumNetworkPolicies by endpoint selector: %s", labelsParam);
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get CiliumNetworkPolicies: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error getting CiliumNetworkPolicies by endpoint selector: %s", labelsParam);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{name}")
    @Operation(
            summary = "Delete a specific CiliumNetworkPolicy",
            description = "Deletes a specific CiliumNetworkPolicy by name in the given namespace. Equivalent to: kubectl delete cnp <policy-name> -n <namespace>"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicy deleted successfully"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "CiliumNetworkPolicy or namespace not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response deleteCiliumNetworkPolicy(
            @PathParam("name") String name,
            @QueryParam("namespace") @NotBlank String namespace) {
        LOG.infof("Received request to delete CiliumNetworkPolicy: %s in namespace: %s", name, namespace);

        try {
            boolean deleted = ciliumNetworkPolicyService.deleteCiliumNetworkPolicy(name, namespace);
            
            if (deleted) {
                return Response.ok(new DeleteResponse("CiliumNetworkPolicy deleted successfully", name, namespace, 1)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("CiliumNetworkPolicy '" + name + "' not found in namespace '" + namespace + "'"))
                        .build();
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error deleting CiliumNetworkPolicy: %s in namespace: %s", name, namespace);
            
            if (e.getMessage().contains("does not exist")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Namespace not found: " + e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete CiliumNetworkPolicy: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error deleting CiliumNetworkPolicy: %s in namespace: %s", name, namespace);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/namespace/{namespace}")
    @Operation(
            summary = "Delete all CiliumNetworkPolicies in a namespace",
            description = "Deletes all CiliumNetworkPolicies in the specified namespace. Equivalent to: kubectl delete cnp -n <namespace> --all"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "CiliumNetworkPolicies deleted successfully"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Namespace not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response deleteAllCiliumNetworkPoliciesInNamespace(@PathParam("namespace") String namespace) {
        LOG.infof("Received request to delete all CiliumNetworkPolicies in namespace: %s", namespace);

        try {
            int deletedCount = ciliumNetworkPolicyService.deleteAllCiliumNetworkPoliciesInNamespace(namespace);
            
            String message = deletedCount > 0
                    ? "Successfully deleted " + deletedCount + " CiliumNetworkPolicies"
                    : "No CiliumNetworkPolicies found to delete";
            
            return Response.ok(new DeleteResponse(message, null, namespace, deletedCount)).build();
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error deleting all CiliumNetworkPolicies in namespace: %s", namespace);
            
            if (e.getMessage().contains("does not exist")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Namespace not found: " + e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete CiliumNetworkPolicies: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error deleting all CiliumNetworkPolicies in namespace: %s", namespace);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Unexpected error: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Parses labels from query parameter string.
     * Expected format: key1=value1,key2=value2
     */
    private Map<String, String> parseLabelsFromQueryParam(String labelsParam) {
        Map<String, String> labels = new HashMap<>();
        
        if (labelsParam == null || labelsParam.trim().isEmpty()) {
            throw new IllegalArgumentException("Labels parameter cannot be empty");
        }
        
        String[] labelPairs = labelsParam.split(",");
        for (String pair : labelPairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Invalid label format. Expected: key=value,key2=value2");
            }
            labels.put(keyValue[0].trim(), keyValue[1].trim());
        }
        
        return labels;
    }

    @GET
    @Path("/health")
    @Operation(
            summary = "Check CiliumNetworkPolicy service health",
            description = "Checks if the service can connect to the Kubernetes cluster for CiliumNetworkPolicy operations"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Service is healthy and can connect to Kubernetes"
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Service cannot connect to Kubernetes"
            )
    })
    public Response checkHealth() {
        boolean isAvailable = kubernetesService.isKubernetesAvailable();
        
        if (isAvailable) {
            return Response.ok(new HealthResponse("CiliumNetworkPolicy service is healthy", "UP")).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthResponse("CiliumNetworkPolicy service cannot connect to Kubernetes", "DOWN"))
                    .build();
        }
    }

    /**
     * Simple error response DTO
     */
    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Simple health response DTO
     */
    public static class HealthResponse {
        public String message;
        public String status;
        public long timestamp;

        public HealthResponse(String message, String status) {
            this.message = message;
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Simple delete response DTO
     */
    public static class DeleteResponse {
        public String message;
        public String policyName;
        public String namespace;
        public int deletedCount;
        public long timestamp;

        public DeleteResponse(String message, String policyName, String namespace, int deletedCount) {
            this.message = message;
            this.policyName = policyName;
            this.namespace = namespace;
            this.deletedCount = deletedCount;
            this.timestamp = System.currentTimeMillis();
        }
    }
}