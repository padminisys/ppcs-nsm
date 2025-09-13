package org.padminisys.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
}