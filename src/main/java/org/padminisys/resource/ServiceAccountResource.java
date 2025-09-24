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
import org.padminisys.dto.ServiceAccountRequest;
import org.padminisys.dto.ServiceAccountResponse;
import org.padminisys.service.KubernetesService;

/**
 * REST endpoint for Kubernetes service account operations.
 */
@Path("/api/v1/serviceaccounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Service Account Management", description = "Operations for managing Kubernetes service accounts")
public class ServiceAccountResource {

    private static final Logger LOG = Logger.getLogger(ServiceAccountResource.class);

    @Inject
    KubernetesService kubernetesService;

    @POST
    @Operation(
            summary = "Create a new service account",
            description = "Creates a new Kubernetes service account in the specified namespace"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Service account created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ServiceAccountResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "200",
                    description = "Service account already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ServiceAccountResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request data or namespace does not exist"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response createServiceAccount(@Valid ServiceAccountRequest request) {
        LOG.infof("Received request to create service account: %s in namespace: %s", 
                  request.getName(), request.getNamespace());

        try {
            ServiceAccountResponse response = kubernetesService.createServiceAccount(request);
            
            if (response == null) {
                LOG.errorf("Service returned null response for service account: %s in namespace: %s",
                          request.getName(), request.getNamespace());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorResponse("Failed to create service account: Service returned null response"))
                        .build();
            }
            
            if ("CREATED".equals(response.getStatus())) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.ok(response).build();
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "Error creating service account: %s in namespace: %s",
                      request.getName(), request.getNamespace());
            
            // Check if it's a namespace not found error
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse(e.getMessage()))
                        .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create service account: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error creating service account: %s in namespace: %s",
                      request.getName(), request.getNamespace());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create service account: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Simple error response DTO
     */
    public static class ErrorResponse {
        public String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}