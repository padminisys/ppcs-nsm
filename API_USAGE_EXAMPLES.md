# CiliumNetworkPolicy API Usage Examples

This document provides comprehensive examples of how to use the CiliumNetworkPolicy API endpoints.

## API Endpoint

**Base URL:** `/api/v1/cilium-network-policies`

## Create CiliumNetworkPolicy

### Endpoint
```
POST /api/v1/cilium-network-policies
```

### Request Structure

The API accepts a JSON payload with the following structure:

```json
{
  "namespace": "string (required)",
  "labels": {
    "key": "value"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["CIDR notation"],
      "fromLabels": {
        "key": "value"
      },
      "ports": [
        {
          "protocol": "TCP|UDP",
          "port": number,
          "endPort": number (optional),
          "headerMatches": [
            {
              "name": "string",
              "value": "string"
            }
          ]
        }
      ]
    }
  ],
  "ingressDenyRules": [...],
  "egressRules": [
    {
      "ruleType": "EGRESS_ALLOW",
      "ipAddresses": ["CIDR notation"],
      "toLabels": {
        "key": "value"
      },
      "ports": [...]
    }
  ],
  "egressDenyRules": [...]
}
```

**Important Notes:**
- Each rule must specify **either** `ipAddresses` **or** labels (`fromLabels`/`toLabels`), but not both
- For ingress rules, use `fromLabels` to match source pods by labels
- For egress rules, use `toLabels` to match destination pods by labels
- The API automatically adds `k8s:io.kubernetes.pod.namespace: <namespace>` to all label selectors for security

## Examples

### 1. Basic Ingress Allow Rule

Allow traffic from specific IP to port 443:

```json
{
  "namespace": "production",
  "labels": {
    "tenant": "dell_computers",
    "environment": "prod"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["203.0.113.10/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 443
        }
      ]
    }
  ]
}
```

**Generated Policy Name:** `dell-computers-prod-abc123` (where `abc123` is a random 6-character suffix)

### 2. Multiple IP Addresses with Port Range

Allow traffic from multiple IPs to a port range:

```json
{
  "namespace": "staging",
  "labels": {
    "tenant": "acme_corp",
    "service": "web_server"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["198.51.100.77/32", "198.51.100.88/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 10000,
          "endPort": 10100
        }
      ]
    }
  ]
}
```

### 3. Allow from Any IP (0.0.0.0/0)

Allow traffic from any IP to specific ports:

```json
{
  "namespace": "public",
  "labels": {
    "tenant": "public_service"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["0.0.0.0/0"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 80
        },
        {
          "protocol": "TCP",
          "port": 443
        }
      ]
    }
  ]
}
```

### 4. Ingress Deny Rules

Deny traffic from specific IPs:

```json
{
  "namespace": "secure",
  "labels": {
    "tenant": "banking_app",
    "security": "high"
  },
  "ingressDenyRules": [
    {
      "ruleType": "INGRESS_DENY",
      "ipAddresses": ["192.0.2.10/32", "192.0.2.11/32"],
      "ports": [
        {
          "protocol": "UDP",
          "port": 123
        }
      ]
    }
  ]
}
```

### 5. Egress Allow Rules

Allow outbound traffic to specific destinations:

```json
{
  "namespace": "backend",
  "labels": {
    "tenant": "microservice",
    "component": "api_gateway"
  },
  "egressRules": [
    {
      "ruleType": "EGRESS_ALLOW",
      "ipAddresses": ["198.51.100.40/32", "198.51.100.41/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 5432
        }
      ]
    }
  ]
}
```

### 6. Egress Deny Rules

Block outbound traffic to specific destinations:

```json
{
  "namespace": "restricted",
  "labels": {
    "tenant": "isolated_service"
  },
  "egressDenyRules": [
    {
      "ruleType": "EGRESS_DENY",
      "ipAddresses": ["203.0.113.30/32"],
      "ports": [
        {
          "protocol": "UDP",
          "port": 514
        }
      ]
    }
  ]
}
```

### 7. Comprehensive Policy (All Rule Types)

A complete example with all types of rules:

```json
{
  "namespace": "tenant-ns-demo",
  "labels": {
    "tenant": "dell_computers",
    "environment": "production",
    "team": "platform"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["203.0.113.10/32"]
    },
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["198.51.100.77/32", "198.51.100.88/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 443
        }
      ]
    },
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["0.0.0.0/0"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 10000,
          "endPort": 10100
        }
      ]
    }
  ],
  "ingressDenyRules": [
    {
      "ruleType": "INGRESS_DENY",
      "ipAddresses": ["203.0.113.200/32"]
    },
    {
      "ruleType": "INGRESS_DENY",
      "ipAddresses": ["192.0.2.10/32", "192.0.2.11/32"],
      "ports": [
        {
          "protocol": "UDP",
          "port": 123
        }
      ]
    }
  ],
  "egressRules": [
    {
      "ruleType": "EGRESS_ALLOW",
      "ipAddresses": ["203.0.113.20/32"]
    },
    {
      "ruleType": "EGRESS_ALLOW",
      "ipAddresses": ["198.51.100.40/32", "198.51.100.41/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 5432
        }
      ]
    }
  ],
  "egressDenyRules": [
    {
      "ruleType": "EGRESS_DENY",
      "ipAddresses": ["203.0.113.30/32"]
    },
    {
      "ruleType": "EGRESS_DENY",
      "ipAddresses": ["198.51.100.60/32", "198.51.100.61/32"],
      "ports": [
        {
          "protocol": "UDP",
          "port": 514
        }
      ]
    }
  ]
}
```

## Response Format

### Success Response (201 Created)

```json
{
  "name": "dell-computers-production-platform-abc123",
  "namespace": "tenant-ns-demo",
  "status": "CREATED",
  "createdAt": "2023-12-01T10:30:00Z",
  "message": "CiliumNetworkPolicy created successfully",
  "generatedName": "dell-computers-production-platform-abc123"
}
```

### Already Exists Response (200 OK)

```json
{
  "name": "dell-computers-production-platform-abc123",
  "namespace": "tenant-ns-demo",
  "status": "EXISTS",
  "createdAt": "2023-12-01T10:25:00Z",
  "message": "CiliumNetworkPolicy already exists",
  "generatedName": "dell-computers-production-platform-abc123"
}
```

### Error Response (400 Bad Request)

```json
{
  "error": "Validation failed: Invalid CIDR format",
  "timestamp": 1701425400000
}
```

### Error Response (404 Not Found)

```json
{
  "error": "Namespace not found: Namespace 'non-existent' does not exist",
  "timestamp": 1701425400000
}
```

## Policy Name Generation Rules

The API automatically generates policy names based on the provided labels:

1. **Concatenate all label values** with hyphens
2. **Convert to lowercase**
3. **Remove special characters** (keep only letters, numbers, hyphens)
4. **Replace underscores** with hyphens
5. **Remove multiple consecutive hyphens**
6. **Trim leading/trailing hyphens**
7. **Limit to 15 characters** (truncate if longer)
8. **Add random 6-character suffix** for uniqueness

### Examples:
- `tenant=dell_computers` → `dell-computers-abc123`
- `tenant=Dell_Computers@123!` → `dell-computers123-xyz789`
- `tenant=very_long_tenant_name_that_exceeds_fifteen_characters` → `very-long-tenan-def456`
- Empty labels → `policy-ghi123`

## Health Check

### Endpoint
```
GET /api/v1/cilium-network-policies/health
```

### Response (200 OK)
```json
{
  "message": "CiliumNetworkPolicy service is healthy",
  "status": "UP",
  "timestamp": 1701425400000
}
```

### Response (503 Service Unavailable)
```json
{
  "message": "CiliumNetworkPolicy service cannot connect to Kubernetes",
  "status": "DOWN",
  "timestamp": 1701425400000
}
```

## cURL Examples

### Create a basic policy
```bash
curl -X POST http://localhost:8080/api/v1/cilium-network-policies \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "production",
    "labels": {
      "tenant": "dell_computers"
    },
    "ingressRules": [
      {
        "ruleType": "INGRESS_ALLOW",
        "ipAddresses": ["203.0.113.10/32"],
        "ports": [
          {
            "protocol": "TCP",
            "port": 443
          }
        ]
      }
    ]
  }'
```

### Check health
```bash
curl -X GET http://localhost:8080/api/v1/cilium-network-policies/health
```

## Notes
- **Namespace must exist** before creating a CiliumNetworkPolicy
- **Labels are required** and cannot be empty
- **IP addresses must be in valid CIDR format** (e.g., `192.168.1.1/32`, `0.0.0.0/0`)
- **Ports must be valid numbers** (1-65535)
- **EndPort is optional** and used for port ranges
- **Protocol must be TCP or UDP**
- **Policy names are automatically generated** and guaranteed to be unique
- **HTTP header matching is optional** and only works with HTTP/HTTPS traffic
- **Header names and values must be non-empty strings** and are case-sensitive
- **Multiple header matches create separate HTTP rules** in the generated policy



## HTTP Header Matching Examples

The API supports HTTP header matching for advanced traffic filtering. This feature allows you to create policies that match specific HTTP headers in addition to IP addresses and labels.

### 8. Basic HTTP Header Matching

Allow traffic from pods with specific labels, but only if the HTTP request contains a specific header:

```json
{
  "namespace": "satish-kaushik-t45d6",
  "labels": {
    "serial": "GB7YP"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "fromLabels": {
        "padmini.systems/tenant-resource-type": "ingress"
      },
      "ports": [
        {
          "protocol": "TCP",
          "port": 80,
          "headerMatches": [
            {
              "name": "X-Real-IP",
              "value": "45.248.67.9"
            }
          ]
        }
      ]
    }
  ]
}
```

**Generated CiliumNetworkPolicy:**
```yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: gb7yp-nlq2qa
  namespace: satish-kaushik-t45d6
spec:
  endpointSelector:
    matchLabels:
      serial: GB7YP
  ingress:
  - fromEndpoints:
    - matchLabels:
        k8s:io.kubernetes.pod.namespace: satish-kaushik-t45d6
        padmini.systems/tenant-resource-type: ingress
    toPorts:
    - ports:
      - port: "80"
        protocol: TCP
      rules:
        http:
        - headerMatches:
          - name: "X-Real-IP"
            value: "45.248.67.9"
```

### 9. Multiple HTTP Header Matches

Allow traffic only when multiple HTTP headers match specific values:

```json
{
  "namespace": "production",
  "labels": {
    "tenant": "multi_header_policy"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "fromLabels": {
        "padmini.systems/tenant-resource-type": "ingress"
      },
      "ports": [
        {
          "protocol": "TCP",
          "port": 80,
          "headerMatches": [
            {
              "name": "X-Real-IP",
              "value": "45.248.67.9"
            },
            {
              "name": "X-Forwarded-For",
              "value": "203.0.113.7"
            }
          ]
        }
      ]
    }
  ]
}
```

**Generated CiliumNetworkPolicy:**
```yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: multi-header-policy-abc123
  namespace: production
spec:
  endpointSelector:
    matchLabels:
      tenant: multi_header_policy
  ingress:
  - fromEndpoints:
    - matchLabels:
        k8s:io.kubernetes.pod.namespace: production
        padmini.systems/tenant-resource-type: ingress
    toPorts:
    - ports:
      - port: "80"
        protocol: TCP
      rules:
        http:
        - headerMatches:
          - name: "X-Real-IP"
            value: "45.248.67.9"
        - headerMatches:
          - name: "X-Forwarded-For"
            value: "203.0.113.7"
```

### 10. HTTP Header Matching with Multiple Ports

Apply header matching to multiple ports:

```json
{
  "namespace": "api-gateway",
  "labels": {
    "service": "gateway",
    "version": "v2"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "fromLabels": {
        "role": "load-balancer"
      },
      "ports": [
        {
          "protocol": "TCP",
          "port": 80,
          "headerMatches": [
            {
              "name": "X-Forwarded-Proto",
              "value": "http"
            }
          ]
        },
        {
          "protocol": "TCP",
          "port": 443,
          "headerMatches": [
            {
              "name": "X-Forwarded-Proto",
              "value": "https"
            }
          ]
        }
      ]
    }
  ]
}
```

### 11. Mixed Rules: IP-based and Header-based

Combine IP-based rules with header-based rules in the same policy:

```json
{
  "namespace": "hybrid-service",
  "labels": {
    "tenant": "mixed_policy"
  },
  "ingressRules": [
    {
      "ruleType": "INGRESS_ALLOW",
      "ipAddresses": ["203.0.113.10/32"],
      "ports": [
        {
          "protocol": "TCP",
          "port": 443
        }
      ]
    },
    {
      "ruleType": "INGRESS_ALLOW",
      "fromLabels": {
        "padmini.systems/tenant-resource-type": "ingress"
      },
      "ports": [
        {
          "protocol": "TCP",
          "port": 80,
          "headerMatches": [
            {
              "name": "X-API-Key",
              "value": "secret-api-key-123"
            }
          ]
        }
      ]
    }
  ]
}
```

### 12. Common HTTP Header Matching Use Cases

#### Authentication Headers
```json
{
  "name": "Authorization",
  "value": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### API Versioning
```json
{
  "name": "Accept",
  "value": "application/vnd.api+json;version=2"
}
```

#### Content Type Filtering
```json
{
  "name": "Content-Type",
  "value": "application/json"
}
```

#### Custom Headers
```json
{
  "name": "X-Tenant-ID",
  "value": "tenant-12345"
}
```

#### User Agent Filtering
```json
{
  "name": "User-Agent",
  "value": "MyApp/1.0"
}
```

## HTTP Header Matching Features

### Key Features:
- **Multiple Headers**: Support for multiple header matches in a single port rule
- **Exact Match**: Headers must match exactly (case-sensitive)
- **Label Integration**: Works seamlessly with label-based pod selection
- **Security**: Automatic namespace constraints still apply
- **Flexibility**: Can be combined with IP-based rules in the same policy

### Important Notes:
- **HTTP Only**: Header matching only works with HTTP/HTTPS traffic
- **Exact Match**: Header values must match exactly (no wildcards or regex)
- **Case Sensitive**: Header names and values are case-sensitive
- **Multiple Rules**: Each header match creates a separate HTTP rule in the policy
- **Validation**: Both header name and value must be non-empty strings

### Security Benefits:
- **Fine-grained Control**: Filter traffic based on application-level headers
- **API Security**: Validate API keys, tokens, or custom authentication headers
- **Content Filtering**: Allow only specific content types or API versions
- **Multi-layer Security**: Combine network-level (IP/labels) with application-level (headers) filtering

- **Namespace must exist** before creating a CiliumNetworkPolicy
- **Labels are required** and cannot be empty
- **IP addresses must be in valid CIDR format** (e.g., `192.168.1.1/32`, `0.0.0.0/0`)
- **Ports must be valid numbers** (1-65535)
- **EndPort is optional** and used for port ranges
- **Protocol must be TCP or UDP**
- **Policy names are automatically generated** and guaranteed to be unique