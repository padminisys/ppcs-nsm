package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a CiliumNetworkPolicy.
 */
@RegisterForReflection
public class CiliumNetworkPolicyRequest {

    @NotBlank(message = "Namespace cannot be blank")
    @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", 
             message = "Namespace must be a valid DNS-1123 label")
    @JsonProperty("namespace")
    private String namespace;

    @NotEmpty(message = "Labels cannot be empty")
    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("ingressRules")
    private List<@Valid NetworkRule> ingressRules;

    @JsonProperty("ingressDenyRules")
    private List<@Valid NetworkRule> ingressDenyRules;

    @JsonProperty("egressRules")
    private List<@Valid NetworkRule> egressRules;

    @JsonProperty("egressDenyRules")
    private List<@Valid NetworkRule> egressDenyRules;

    public CiliumNetworkPolicyRequest() {
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<NetworkRule> getIngressRules() {
        return ingressRules;
    }

    public void setIngressRules(List<NetworkRule> ingressRules) {
        this.ingressRules = ingressRules;
    }

    public List<NetworkRule> getIngressDenyRules() {
        return ingressDenyRules;
    }

    public void setIngressDenyRules(List<NetworkRule> ingressDenyRules) {
        this.ingressDenyRules = ingressDenyRules;
    }

    public List<NetworkRule> getEgressRules() {
        return egressRules;
    }

    public void setEgressRules(List<NetworkRule> egressRules) {
        this.egressRules = egressRules;
    }

    public List<NetworkRule> getEgressDenyRules() {
        return egressDenyRules;
    }

    public void setEgressDenyRules(List<NetworkRule> egressDenyRules) {
        this.egressDenyRules = egressDenyRules;
    }

    @Override
    public String toString() {
        return "CiliumNetworkPolicyRequest{" +
                "namespace='" + namespace + '\'' +
                ", labels=" + labels +
                ", ingressRules=" + ingressRules +
                ", ingressDenyRules=" + ingressDenyRules +
                ", egressRules=" + egressRules +
                ", egressDenyRules=" + egressDenyRules +
                '}';
    }

    /**
     * Represents a network rule for ingress or egress traffic.
     */
    @RegisterForReflection
    public static class NetworkRule {

        @NotNull(message = "Rule type cannot be null")
        @JsonProperty("ruleType")
        private RuleType ruleType;

        @NotEmpty(message = "IP addresses cannot be empty")
        @JsonProperty("ipAddresses")
        private List<@Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}/[0-9]{1,2}$", 
                              message = "Invalid CIDR format") String> ipAddresses;

        @JsonProperty("ports")
        private List<@Valid PortRule> ports;

        public NetworkRule() {
        }

        public RuleType getRuleType() {
            return ruleType;
        }

        public void setRuleType(RuleType ruleType) {
            this.ruleType = ruleType;
        }

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }

        public List<PortRule> getPorts() {
            return ports;
        }

        public void setPorts(List<PortRule> ports) {
            this.ports = ports;
        }

        @Override
        public String toString() {
            return "NetworkRule{" +
                    "ruleType=" + ruleType +
                    ", ipAddresses=" + ipAddresses +
                    ", ports=" + ports +
                    '}';
        }
    }

    /**
     * Represents a port rule with protocol and port range.
     */
    @RegisterForReflection
    public static class PortRule {

        @NotNull(message = "Protocol cannot be null")
        @JsonProperty("protocol")
        private Protocol protocol;

        @NotNull(message = "Port cannot be null")
        @JsonProperty("port")
        private Integer port;

        @JsonProperty("endPort")
        private Integer endPort;

        public PortRule() {
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public void setProtocol(Protocol protocol) {
            this.protocol = protocol;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Integer getEndPort() {
            return endPort;
        }

        public void setEndPort(Integer endPort) {
            this.endPort = endPort;
        }

        @Override
        public String toString() {
            return "PortRule{" +
                    "protocol=" + protocol +
                    ", port=" + port +
                    ", endPort=" + endPort +
                    '}';
        }
    }

    /**
     * Enum for rule types.
     */
    public enum RuleType {
        INGRESS_ALLOW,
        INGRESS_DENY,
        EGRESS_ALLOW,
        EGRESS_DENY
    }

    /**
     * Enum for network protocols.
     */
    public enum Protocol {
        TCP,
        UDP
    }
}