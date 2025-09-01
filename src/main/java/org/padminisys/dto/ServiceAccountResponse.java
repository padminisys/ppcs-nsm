package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

/**
 * Response DTO for service account operations.
 */
@RegisterForReflection
public class ServiceAccountResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("status")
    private String status;

    @JsonProperty("creationTimestamp")
    private Instant creationTimestamp;

    @JsonProperty("message")
    private String message;

    public ServiceAccountResponse() {
    }

    public ServiceAccountResponse(String name, String namespace, String status, Instant creationTimestamp, String message) {
        this.name = name;
        this.namespace = namespace;
        this.status = status;
        this.creationTimestamp = creationTimestamp;
        this.message = message;
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

    public Instant getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Instant creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ServiceAccountResponse{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", status='" + status + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", message='" + message + '\'' +
                '}';
    }
}