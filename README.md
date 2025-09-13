# PPCS-NSM (Private Platform Cloud Services - Network Security Manager)

A cloud-native microservice built with Quarkus for managing Kubernetes resources including Namespaces, Service Accounts, and CiliumNetworkPolicies. This service provides REST APIs for creating and managing network security policies in Kubernetes clusters with Cilium CNI.

## Features

- **Namespace Management**: Create and manage Kubernetes namespaces
- **Service Account Management**: Create and manage Kubernetes service accounts
- **CiliumNetworkPolicy Management**: Create and manage Cilium network policies with comprehensive ingress/egress rules
- **Automatic Policy Naming**: Intelligent policy name generation based on labels
- **Comprehensive Validation**: Input validation with detailed error messages
- **Health Checks**: Built-in health monitoring for Kubernetes connectivity
- **OpenAPI Documentation**: Complete API documentation with Swagger UI

## Technology Stack

- **Quarkus**: Supersonic Subatomic Java Framework
- **Kubernetes Client**: For interacting with Kubernetes API
- **Cilium**: For advanced network policy management
- **Jackson**: JSON serialization/deserialization
- **Hibernate Validator**: Input validation
- **SmallRye Health**: Health check endpoints
- **SmallRye OpenAPI**: API documentation

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## API Endpoints

### CiliumNetworkPolicy Management
- `POST /api/v1/cilium-network-policies` - Create a new CiliumNetworkPolicy
- `GET /api/v1/cilium-network-policies/health` - Check service health

### Namespace Management
- `POST /api/v1/namespaces` - Create a new namespace
- `GET /api/v1/namespaces/health` - Check Kubernetes connectivity

### Service Account Management
- `POST /api/v1/service-accounts` - Create a new service account
- `GET /api/v1/service-accounts/health` - Check service health

### API Documentation
- `GET /q/swagger-ui` - Swagger UI for interactive API documentation
- `GET /q/openapi` - OpenAPI specification

## CiliumNetworkPolicy Features

The CiliumNetworkPolicy API supports comprehensive network security management:

### Supported Rule Types
- **Ingress Allow**: Allow incoming traffic from specified sources
- **Ingress Deny**: Block incoming traffic from specified sources
- **Egress Allow**: Allow outgoing traffic to specified destinations
- **Egress Deny**: Block outgoing traffic to specified destinations

### IP Address Support
- Single IP addresses with CIDR notation (e.g., `192.168.1.1/32`)
- Multiple IP addresses in arrays
- Any IP address using `0.0.0.0/0`

### Port Configuration
- Single ports (e.g., `80`, `443`)
- Multiple ports in arrays
- Port ranges using `endPort` field
- TCP and UDP protocol support

### Automatic Policy Naming
The service automatically generates unique policy names based on provided labels:
- Concatenates label values with hyphens
- Converts to lowercase
- Removes special characters
- Replaces underscores with hyphens
- Limits to 15 characters + 6-character random suffix
- Example: `tenant=dell_computers` → `dell-computers-abc123`

## Quick Start Example

Create a CiliumNetworkPolicy that allows HTTPS traffic from specific IPs:

```bash
curl -X POST http://localhost:8080/api/v1/cilium-network-policies \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "production",
    "labels": {
      "tenant": "dell_computers",
      "environment": "prod"
    },
    "ingressRules": [
      {
        "ruleType": "INGRESS_ALLOW",
        "ipAddresses": ["203.0.113.10/32", "198.51.100.77/32"],
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

For detailed API usage examples, see [`API_USAGE_EXAMPLES.md`](API_USAGE_EXAMPLES.md).

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/ppcs-nsm-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Kubernetes Client ([guide](https://quarkus.io/guides/kubernetes-client)): Interact with Kubernetes and develop Kubernetes Operators
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health

## Configuration

The application requires access to a Kubernetes cluster. Configure using one of these methods:

### Using kubeconfig file
```properties
# application.properties
quarkus.kubernetes-client.kubeconfig-path=/path/to/kubeconfig
```

### Using in-cluster configuration
When running inside a Kubernetes cluster, the service will automatically use the service account token.

### Environment Variables
```bash
export KUBECONFIG=/path/to/kubeconfig
```

## Testing

Run all tests:
```shell script
./mvnw test
```

Run specific test classes:
```shell script
./mvnw test -Dtest=CiliumNetworkPolicyResourceTest
./mvnw test -Dtest=KubernetesServiceTest
```

## Deployment

### Container Build
```shell script
./mvnw package -Dquarkus.container-image.build=true
```

### Kubernetes Deployment
The `k8s/` directory contains Kubernetes manifests for deployment:
- `namespace.yaml` - Application namespace
- `serviceaccount.yaml` - Service account with required permissions
- `clusterrole.yaml` - Cluster role for Kubernetes API access
- `clusterrolebinding.yaml` - Binding service account to cluster role
- `deployment.yaml` - Application deployment
- `service.yaml` - Kubernetes service
- `ingress.yaml` - Ingress configuration

Deploy to Kubernetes:
```shell script
kubectl apply -f k8s/
```

## Security Considerations

- The service requires cluster-wide permissions to manage namespaces and CiliumNetworkPolicies
- All API endpoints should be secured with appropriate authentication/authorization
- Input validation is enforced at multiple levels
- Generated policy names include random suffixes to prevent conflicts
- Sensitive configuration should be stored in Kubernetes secrets

## Monitoring and Observability

- Health check endpoints for monitoring service and Kubernetes connectivity
- Structured logging with correlation IDs
- Metrics endpoints (when enabled)
- OpenAPI documentation for API discovery

## Development

### Code Structure
- `src/main/java/org/padminisys/dto/` - Data Transfer Objects
- `src/main/java/org/padminisys/resource/` - REST endpoints
- `src/main/java/org/padminisys/service/` - Business logic
- `src/main/java/org/padminisys/exception/` - Exception handling
- `src/test/` - Unit and integration tests

### Best Practices Implemented
- Single Responsibility Principle
- Comprehensive input validation
- Proper error handling and logging
- Unit test coverage > 80%
- OpenAPI documentation
- Health check endpoints
- Immutable DTOs where possible

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)
