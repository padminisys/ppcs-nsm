package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;

/**
 * Response DTO for namespace operations.
 */
@RegisterForReflection
public class NamespaceResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("creationTimestamp")
    private Instant creationTimestamp;

    @JsonProperty("message")
    private String message;

    public NamespaceResponse() {
    }

    public NamespaceResponse(String name, String status, Instant creationTimestamp, String message) {
        this.name = name;
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
        return "NamespaceResponse{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", message='" + message + '\'' +
                '}';
    }
}