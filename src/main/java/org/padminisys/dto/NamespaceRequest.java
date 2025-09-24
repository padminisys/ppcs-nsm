package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a Kubernetes namespace.
 */
@RegisterForReflection
public class NamespaceRequest {

    @NotBlank(message = "Namespace name cannot be blank")
    @Size(max = 63, message = "Namespace name must not exceed 63 characters")
    @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
             message = "Namespace name must be a valid DNS-1123 label")
    @JsonProperty("name")
    private String name;

    public NamespaceRequest() {
    }

    public NamespaceRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "NamespaceRequest{" +
                "name='" + name + '\'' +
                '}';
    }
}