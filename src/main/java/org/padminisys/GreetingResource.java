package org.padminisys;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Path("/")
@Tag(name = "Application Info", description = "Application information and available endpoints")
public class GreetingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get application information",
            description = "Returns information about the PPCS-NSM service and available API endpoints"
    )
    @APIResponse(responseCode = "200", description = "Application information")
    public Response getApplicationInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "PPCS-NSM (Private Platform Cloud Services - Network Security Manager)");
        info.put("version", "1.0.0-SNAPSHOT");
        info.put("description", "Cloud-native microservice for managing Kubernetes resources and Cilium Network Policies");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("API Documentation", "/q/swagger-ui");
        endpoints.put("Health Check", "/q/health");
        endpoints.put("OpenAPI Spec", "/q/openapi");
        endpoints.put("Namespaces", "/api/v1/namespaces");
        endpoints.put("Service Accounts", "/api/v1/service-accounts");
        endpoints.put("Cilium Network Policies", "/api/v1/cilium-network-policies");
        
        info.put("endpoints", endpoints);
        
        Map<String, String> quickStart = new HashMap<>();
        quickStart.put("Swagger UI", "http://localhost:8080/q/swagger-ui");
        quickStart.put("Health Check", "http://localhost:8080/q/health");
        quickStart.put("API Examples", "See API_USAGE_EXAMPLES.md in the project root");
        
        info.put("quickStart", quickStart);
        
        return Response.ok(info).build();
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Simple greeting",
            description = "Returns a simple greeting message"
    )
    @APIResponse(responseCode = "200", description = "Greeting message")
    public String hello() {
        return "Hello from PPCS-NSM - Your Network Security Manager is running!";
    }
}
