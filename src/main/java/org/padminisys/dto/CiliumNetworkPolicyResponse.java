package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

/**
 * Response DTO for CiliumNetworkPolicy operations.
 */
@RegisterForReflection
public class CiliumNetworkPolicyResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("status")
    private String status;

    @JsonProperty("createdAt")
    private Instant createdAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("generatedName")
    private String generatedName;

    public CiliumNetworkPolicyResponse() {
    }

    public CiliumNetworkPolicyResponse(String name, String namespace, String status, 
                                     Instant createdAt, String message, String generatedName) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.createdAt = createdAt;
        this.message = message;
        this.generatedName = generatedName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGeneratedName() {
        return generatedName;
    }

    public void setGeneratedName(String generatedName) {
        this.generatedName = generatedName;
    }

    @Override
    public String toString() {
        return "CiliumNetworkPolicyResponse{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", message='" + message + '\'' +
                ", generatedName='" + generatedName + '\'' +
                '}';
    }
}