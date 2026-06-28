# Module 10: Cloud Native Camel (Quarkus, Kamelets, Kubernetes)

## 1. Camel Quarkus

Camel Quarkus = Camel optimized for cloud-native with fast startup and low memory.

### Setup

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.camel.quarkus</groupId>
            <artifactId>camel-quarkus-bom</artifactId>
            <version>3.8.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.apache.camel.quarkus</groupId>
        <artifactId>camel-quarkus-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.quarkus</groupId>
        <artifactId>camel-quarkus-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.quarkus</groupId>
        <artifactId>camel-quarkus-jackson</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.camel.quarkus</groupId>
        <artifactId>camel-quarkus-platform-http</artifactId>
    </dependency>
</dependencies>
```

### Startup Comparison

| Metric | Camel + Spring Boot | Camel Quarkus (JVM) | Camel Quarkus (Native) |
|--------|--------------------|--------------------|----------------------|
| Startup time | 3-5 seconds | 0.8-1.5 seconds | 0.02-0.05 seconds |
| Memory (RSS) | 200-400 MB | 80-150 MB | 30-60 MB |
| First request | ~5 seconds | ~1.5 seconds | ~0.1 seconds |
| Docker image | ~300 MB | ~200 MB | ~50 MB |

### Native Compilation

```bash
# Build native executable
./mvnw package -Pnative

# Build native container
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t my-camel-app .
```

---

## 2. Kamelets (Pre-built Connectors)

Kamelets are pre-packaged, reusable integration patterns. Think of them as "Camel components made simple."

### What are Kamelets?

```
Traditional:  from("kafka:topic?brokers=x&group=y&security=z&serializer=...")  ← Complex
Kamelet:      from("kamelet:kafka-source?topic=orders&bootstrapServers=x")     ← Simple
```

### Available Kamelets (300+)

| Category | Examples |
|----------|---------|
| Sources | kafka-source, aws-s3-source, timer-source, webhook-source |
| Sinks | kafka-sink, aws-s3-sink, slack-sink, elasticsearch-sink |
| Actions | json-deserialize, insert-header, regex-router, log |

### Using Kamelets

```yaml
# YAML DSL with Kamelets
- route:
    from:
      uri: kamelet:kafka-source
      parameters:
        topic: orders
        bootstrapServers: localhost:9092
        groupId: my-app
    steps:
      - kamelet:
          name: json-deserialize-action
      - kamelet:
          name: insert-header-action
          parameters:
            name: processedAt
            value: "${date:now:yyyy-MM-dd'T'HH:mm:ss}"
      - kamelet:
          name: kafka-sink
          parameters:
            topic: processed-orders
            bootstrapServers: localhost:9092
```

```java
// Java DSL with Kamelets
from("kamelet:aws-s3-source?bucketNameOrArn=my-bucket&region=us-east-1")
    .to("kamelet:kafka-sink?topic=s3-events&bootstrapServers=localhost:9092");
```

### Custom Kamelet

```yaml
# my-transform-action.kamelet.yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: my-transform-action
  labels:
    camel.apache.org/kamelet.type: action
spec:
  definition:
    title: My Custom Transform
    description: Transforms order data to canonical format
    properties:
      outputFormat:
        title: Output Format
        type: string
        default: json
  template:
    from:
      uri: kamelet:source
      steps:
        - bean:
            ref: myTransformer
        - marshal:
            json: {}
        - to: kamelet:sink
```

---

## 3. Camel K (Kubernetes-Native)

Camel K is a lightweight integration platform that runs natively on Kubernetes.

### Install

```bash
# Install Camel K operator
kubectl apply -f https://github.com/apache/camel-k/releases/download/v2.2.0/camel-k-install.yaml

# Or with Helm
helm install camel-k camel-k/camel-k --namespace camel-system
```

### Deploy Integration

```bash
# Deploy a route directly (no build step needed!)
kamel run routes.yaml

# Or Java file
kamel run OrderRoute.java --trait knative.enabled=true
```

```yaml
# kafka-to-s3.yaml — deployed directly to Kubernetes
- route:
    from:
      uri: kafka:orders
      parameters:
        brokers: kafka:9092
    steps:
      - marshal:
          json: {}
      - to:
          uri: aws2-s3://order-archive
          parameters:
            region: us-east-1
```

```bash
kamel run kafka-to-s3.yaml \
  --property kafka.brokers=my-kafka:9092 \
  --trait container.request-cpu=200m \
  --trait container.request-memory=256Mi \
  --trait scaling.min=1 \
  --trait scaling.max=5
```

### Camel K Traits (Configuration)

| Trait | Purpose |
|-------|---------|
| `container` | CPU/memory resources |
| `scaling` | HPA auto-scaling |
| `knative` | Knative serving/eventing |
| `prometheus` | Metrics export |
| `health` | Liveness/readiness probes |
| `logging` | Log level configuration |

---

## 4. Knative Integration

```yaml
# Knative Eventing source → Camel route → Knative sink
- route:
    from:
      uri: knative:channel/orders
    steps:
      - unmarshal:
          json: {}
      - bean:
          ref: orderProcessor
      - to: knative:channel/processed-orders
```

### Event-Driven Scaling (Scale to Zero)

```bash
kamel run order-processor.yaml \
  --trait knative.enabled=true \
  --trait scaling.min=0         # Scale to zero when no events
```

The route will:
1. Scale to 0 when no Kafka messages arrive
2. Scale up when messages appear
3. Scale based on message backlog

---

## 5. Camel JBang (Rapid Prototyping)

```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash

# Run a route instantly (no project, no build)
jbang -Dcamel.jbang.version=4.4.0 camel@apache/camel run timer-log.yaml

# Run with dependencies auto-resolved
jbang camel@apache/camel run kafka-consumer.java

# Export to Spring Boot project
jbang camel@apache/camel export --runtime=spring-boot --directory=my-project

# Export to Quarkus
jbang camel@apache/camel export --runtime=quarkus --directory=my-project
```

```yaml
# timer-log.yaml — run instantly with jbang
- route:
    from:
      uri: timer:tick
      parameters:
        period: 2000
    steps:
      - setBody:
          simple: "Current time: ${date:now:HH:mm:ss}"
      - log: "${body}"
```

---

## 6. Kubernetes Deployment (Helm)

```yaml
# values.yaml
replicaCount: 3

image:
  repository: your-registry/camel-order-service
  tag: "1.0.0"

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

env:
  - name: KAFKA_BROKERS
    value: "kafka-cluster:9092"
  - name: CAMEL_COMPONENT_KAFKA_SECURITY_PROTOCOL
    value: "SASL_SSL"

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### ConfigMap for Route Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: camel-routes
data:
  application.yml: |
    camel:
      component:
        kafka:
          brokers: ${KAFKA_BROKERS}
          group-id: order-service
    app:
      retry:
        max-attempts: 5
        delay: 2000
```

---

## 7. Camel + Service Mesh (Istio)

```yaml
# Camel route with retry disabled (let Istio handle retries)
from("direct:call-service")
    .to("http://payment-service:8080/api/charge");
    # No retry in Camel — Istio VirtualService handles retries

---
# Istio VirtualService
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
  name: payment-service
spec:
  hosts: [payment-service]
  http:
    - route:
        - destination:
            host: payment-service
      retries:
        attempts: 3
        perTryTimeout: 5s
        retryOn: 5xx,connect-failure
```

**When to use Camel retry vs Service Mesh retry:**
- **Camel retry**: Business logic retries (e.g., idempotent operations, DLQ routing)
- **Service Mesh retry**: Infrastructure retries (network glitches, pod restarts)
