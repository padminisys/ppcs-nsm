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
      "ports": [
        {
          "protocol": "TCP|UDP",
          "port": number,
          "endPort": number (optional)
        }
      ]
    }
  ],
  "ingressDenyRules": [...],
  "egressRules": [...],
  "egressDenyRules": [...]
}
```

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