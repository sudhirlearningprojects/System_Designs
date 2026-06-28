# Apache Camel - Complete In-Depth Course

> **Version**: Apache Camel 4.x (latest as of 2024)  
> **Runtime**: Spring Boot 3.x, Quarkus 3.x, Camel Main  
> **Java**: 17+

## 📋 What is Apache Camel?

Apache Camel is an open-source **integration framework** that implements Enterprise Integration Patterns (EIPs). It provides a routing and mediation engine that allows you to define routing rules in various domain-specific languages (DSL) — Java, XML, YAML, Kotlin.

**In simple terms**: Camel connects systems that speak different protocols (REST, Kafka, JMS, FTP, databases, etc.) using a unified programming model.

```
System A (REST) ──→ ┌──────────────┐ ──→ System B (Kafka)
                    │ Apache Camel │
System C (FTP)  ──→ │  (Routes +   │ ──→ System D (Database)
                    │   EIPs)      │
System E (SOAP) ──→ └──────────────┘ ──→ System F (Email)
```

---

## 🎯 Who Should Learn This?

- Backend engineers integrating multiple systems
- Microservice developers needing reliable messaging
- Teams migrating from ESBs (MuleSoft, IBM Integration Bus)
- Anyone dealing with ETL, event-driven architectures, or data pipelines

---

## 📚 Course Structure

| # | Module | Topics |
|---|--------|--------|
| 1 | [Fundamentals](01_Fundamentals.md) | Core concepts, architecture, CamelContext, Routes, Endpoints, Components |
| 2 | [Enterprise Integration Patterns](02_EIP.md) | All 65+ EIPs: routing, transformation, messaging, error handling |
| 3 | [Components Deep Dive](03_Components.md) | HTTP, Kafka, JMS, File, Database, AWS, Timer, Direct, SEDA |
| 4 | [Data Transformation](04_Data_Transformation.md) | Type converters, data formats (JSON, XML, CSV, Avro, Protobuf), marshal/unmarshal |
| 5 | [Error Handling & Resilience](05_Error_Handling.md) | Exception handling, dead letter channel, retry, circuit breaker, idempotency |
| 6 | [Testing](06_Testing.md) | CamelTestSupport, MockEndpoint, AdviceWith, Testcontainers |
| 7 | [Spring Boot Integration](07_Spring_Boot.md) | Auto-configuration, starters, properties, actuator, health checks |
| 8 | [Camel with Kafka](08_Kafka_Integration.md) | Kafka component, idempotent consumer, manual commits, error handling |
| 9 | [REST DSL & OpenAPI](09_REST_DSL.md) | REST endpoints, OpenAPI generation, request validation, CORS |
| 10 | [Camel Quarkus & Cloud Native](10_Cloud_Native.md) | Quarkus runtime, native compilation, Kubernetes, Knative, Kamelets |
| 11 | [Security](11_Security.md) | SSL/TLS, OAuth2, API keys, vault integration, data masking |
| 12 | [Observability](12_Observability.md) | Metrics, tracing (OpenTelemetry), logging (MDC), health checks |
| 13 | [Performance & Production](13_Performance.md) | Thread pools, async processing, backpressure, connection pooling, tuning |
| 14 | [Real-World Projects](14_Projects.md) | File ETL pipeline, event-driven microservices, API gateway, data sync |

---

## 🆕 What's New in Camel 4.x (2024)

| Feature | Description |
|---------|-------------|
| Java 17+ required | Baseline moved to Java 17 |
| Jakarta EE 10 | javax.* → jakarta.* migration complete |
| YAML DSL | First-class YAML route definitions |
| Kamelets | Pre-built connectors (300+ available) |
| Camel JBang | CLI tool for rapid prototyping |
| Resume API | Resumable routes after restart |
| Camel AI | LangChain4j integration for GenAI |
| Vault support | AWS Secrets Manager, HashiCorp Vault, Azure Key Vault native |
| Improved Kafka | Idempotent consumer, manual offset, batch processing |
| Micrometer native | Built-in metrics without extra config |
| Health checks | Readiness/liveness out of the box |

---

## ⚡ Quick Start (30 seconds)

```bash
# Using Camel JBang (no project setup needed)
jbang -Dcamel.jbang.version=4.4.0 camel@apache/camel run hello.yaml
```

```yaml
# hello.yaml
- route:
    from:
      uri: timer:hello
      parameters:
        period: 1000
    steps:
      - setBody:
          constant: "Hello from Apache Camel!"
      - log: "${body}"
```

---

## 🏗️ Spring Boot Quick Start

```xml
<!-- pom.xml -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.0</version>
</parent>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.camel.springboot</groupId>
            <artifactId>camel-spring-boot-bom</artifactId>
            <version>4.4.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.apache.camel.springboot</groupId>
        <artifactId>camel-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

```java
@Component
public class MyRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("timer:hello?period=1000")
            .setBody(constant("Hello Camel!"))
            .log("${body}");
    }
}
```

---

## 🔗 Prerequisites

- Java 17+
- Maven 3.9+ or Gradle 8+
- Docker (for Testcontainers, Kafka, etc.)
- Basic understanding of messaging concepts
- Familiarity with Spring Boot (for Spring Boot modules)
