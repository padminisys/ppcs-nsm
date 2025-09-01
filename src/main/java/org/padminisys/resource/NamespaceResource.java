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
import org.padminisys.dto.NamespaceRequest;
import org.padminisys.dto.NamespaceResponse;
import org.padminisys.service.KubernetesService;

/**
 * REST endpoint for Kubernetes namespace operations.
 */
@Path("/api/v1/namespaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Namespace Management", description = "Operations for managing Kubernetes namespaces")
public class NamespaceResource {

    private static final Logger LOG = Logger.getLogger(NamespaceResource.class);

    @Inject
    KubernetesService kubernetesService;

    @POST
    @Operation(
            summary = "Create a new namespace",
            description = "Creates a new Kubernetes namespace with the specified name"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Namespace created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = NamespaceResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "200",
                    description = "Namespace already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = NamespaceResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response createNamespace(@Valid NamespaceRequest request) {
        LOG.infof("Received request to create namespace: %s", request.getName());

        try {
            NamespaceResponse response = kubernetesService.createNamespace(request);
            
            if ("CREATED".equals(response.getStatus())) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error creating namespace: %s", request.getName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create namespace: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Operation(
            summary = "Check Kubernetes connectivity",
            description = "Checks if the service can connect to the Kubernetes cluster"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Kubernetes is available"
            ),
            @APIResponse(
                    responseCode = "503",
                    description = "Kubernetes is not available"
            )
    })
    public Response checkKubernetesHealth() {
        boolean isAvailable = kubernetesService.isKubernetesAvailable();
        
        if (isAvailable) {
            return Response.ok(new HealthResponse("Kubernetes is available", "UP")).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthResponse("Kubernetes is not available", "DOWN"))
                    .build();
        }
    }

    /**
     * Simple error response DTO
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    /**
     * Simple health response DTO
     */
    public static class HealthResponse {
        public String message;
        public String status;

        public HealthResponse(String message, String status) {
            this.message = message;
            this.status = status;
        }
    }
}