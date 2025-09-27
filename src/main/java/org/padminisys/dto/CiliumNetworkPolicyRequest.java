package org.padminisys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a CiliumNetworkPolicy.
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CiliumNetworkPolicyRequest {

    @Pattern(regexp = "^$|^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
             message = "Policy name must be a valid DNS-1123 label")
    @Size(max = 63, message = "Policy name must not exceed 63 characters")
    @JsonProperty("name")
    private String name;

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
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", labels=" + labels +
                ", ingressRules=" + ingressRules +
                ", ingressDenyRules=" + ingressDenyRules +
                ", egressRules=" + egressRules +
                ", egressDenyRules=" + egressDenyRules +
                '}';
    }

    /**
     * Custom validation annotation for NetworkRule.
     */
    @Documented
    @Constraint(validatedBy = NetworkRuleValidator.class)
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ValidNetworkRule {
        String message() default "NetworkRule must have either IP addresses or labels, but not both";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    /**
     * Validator for NetworkRule.
     */
    public static class NetworkRuleValidator implements ConstraintValidator<ValidNetworkRule, NetworkRule> {
        @Override
        public boolean isValid(NetworkRule rule, ConstraintValidatorContext context) {
            if (rule == null) {
                return true; // Let @NotNull handle null validation
            }

            boolean hasIpAddresses = rule.getIpAddresses() != null && !rule.getIpAddresses().isEmpty();
            boolean hasFromLabels = rule.getFromLabels() != null && !rule.getFromLabels().isEmpty();
            boolean hasToLabels = rule.getToLabels() != null && !rule.getToLabels().isEmpty();
            boolean hasLabels = hasFromLabels || hasToLabels;

            // Must have either IP addresses or labels, but not both
            boolean isValid = (hasIpAddresses && !hasLabels) || (!hasIpAddresses && hasLabels);

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                if (!hasIpAddresses && !hasLabels) {
                    context.buildConstraintViolationWithTemplate(
                        "NetworkRule must specify either IP addresses or labels")
                        .addConstraintViolation();
                } else if (hasIpAddresses && hasLabels) {
                    context.buildConstraintViolationWithTemplate(
                        "NetworkRule cannot have both IP addresses and labels")
                        .addConstraintViolation();
                }
            }

            return isValid;
        }
    }

    /**
     * Represents a network rule for ingress or egress traffic.
     * Supports both IP-based (CIDR) and label-based (fromEndpoints/toEndpoints) matching.
     */
    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ValidNetworkRule
    public static class NetworkRule {

        @NotNull(message = "Rule type cannot be null")
        @JsonProperty("ruleType")
        private RuleType ruleType;

        @JsonProperty("ipAddresses")
        private List<@Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}/[0-9]{1,2}$",
                              message = "Invalid CIDR format") String> ipAddresses;

        @JsonProperty("fromLabels")
        private Map<String, String> fromLabels;

        @JsonProperty("toLabels")
        private Map<String, String> toLabels;

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

        public Map<String, String> getFromLabels() {
            return fromLabels;
        }

        public void setFromLabels(Map<String, String> fromLabels) {
            this.fromLabels = fromLabels;
        }

        public Map<String, String> getToLabels() {
            return toLabels;
        }

        public void setToLabels(Map<String, String> toLabels) {
            this.toLabels = toLabels;
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
                    ", fromLabels=" + fromLabels +
                    ", toLabels=" + toLabels +
                    ", ports=" + ports +
                    '}';
        }
    }

    /**
     * Represents a port rule with protocol, port range, and optional HTTP header matching.
     */
    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PortRule {

        @NotNull(message = "Protocol cannot be null")
        @JsonProperty("protocol")
        private Protocol protocol;

        @NotNull(message = "Port cannot be null")
        @JsonProperty("port")
        private Integer port;

        @JsonProperty("endPort")
        private Integer endPort;

        @JsonProperty("headerMatches")
        private List<@Valid HeaderMatch> headerMatches;

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

        public List<HeaderMatch> getHeaderMatches() {
            return headerMatches;
        }

        public void setHeaderMatches(List<HeaderMatch> headerMatches) {
            this.headerMatches = headerMatches;
        }

        @Override
        public String toString() {
            return "PortRule{" +
                    "protocol=" + protocol +
                    ", port=" + port +
                    ", endPort=" + endPort +
                    ", headerMatches=" + headerMatches +
                    '}';
        }
    }

    /**
     * Represents an HTTP header match for network policies.
     */
    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HeaderMatch {

        @NotBlank(message = "Header name cannot be blank")
        @JsonProperty("name")
        private String name;

        @NotBlank(message = "Header value cannot be blank")
        @JsonProperty("value")
        private String value;

        public HeaderMatch() {
        }

        public HeaderMatch(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "HeaderMatch{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
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