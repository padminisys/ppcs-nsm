package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a Kubernetes service account.
 */
@RegisterForReflection
public class ServiceAccountRequest {

    @NotBlank(message = "Namespace name cannot be blank")
    @Size(max = 63, message = "Namespace name must not exceed 63 characters")
    @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
             message = "Namespace name must be a valid DNS-1123 label")
    @JsonProperty("namespace")
    private String namespace;

    @NotBlank(message = "Service account name cannot be blank")
    @Size(max = 63, message = "Service account name must not exceed 63 characters")
    @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
             message = "Service account name must be a valid DNS-1123 label")
    @JsonProperty("name")
    private String name;

    public ServiceAccountRequest() {
    }

    public ServiceAccountRequest(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ServiceAccountRequest{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}