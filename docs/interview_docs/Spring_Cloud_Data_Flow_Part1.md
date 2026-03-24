# Spring Cloud Data Flow Deep Dive

## Table of Contents
- [Introduction](#introduction)
- [Architecture Overview](#architecture-overview)
- [Core Concepts](#core-concepts)
- [Components Deep Dive](#components-deep-dive)
- [Stream Processing](#stream-processing)
- [Batch Processing](#batch-processing)
- [Real-World Example](#real-world-example)
- [Deployment Options](#deployment-options)
- [Monitoring and Operations](#monitoring-and-operations)
- [Comparison with Alternatives](#comparison-with-alternatives)
- [Production Best Practices](#production-best-practices)
- [Interview Questions](#interview-questions)

---

## Introduction

### What is Spring Cloud Data Flow?

Spring Cloud Data Flow (SCDF) is a **cloud-native orchestration service** for composable microservice applications on modern runtimes. It provides tools to create complex data processing pipelines using:

- **Stream Processing**: Real-time event-driven applications
- **Batch Processing**: Short-lived task executions
- **Pre-built Applications**: Out-of-the-box connectors and processors

### Key Features

- ✅ **Visual Pipeline Designer**: Drag-and-drop UI for building pipelines
- ✅ **Pre-built Applications**: 70+ ready-to-use stream/task apps
- ✅ **Multiple Platforms**: Kubernetes, Cloud Foundry, Local
- ✅ **Multiple Messaging**: Kafka, RabbitMQ
- ✅ **Monitoring**: Built-in metrics and dashboards
- ✅ **Scaling**: Auto-scaling and resource management
- ✅ **CI/CD Integration**: GitOps and deployment automation

### Why Use Spring Cloud Data Flow?

**Without SCDF:**
```java
// Custom microservices for each step
@SpringBootApplication
public class DataIngestionService { }

@SpringBootApplication
public class DataTransformationService { }

@SpringBootApplication
public class DataSinkService { }

// Manual deployment, scaling, monitoring
```

**With SCDF:**
```
http --server.port=9000 | transform --expression=payload.toUpperCase() | log
```

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Cloud Data Flow                        │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Data Flow Server                         │  │
│  │  - REST API                                               │  │
│  │  - Stream/Task Definitions                                │  │
│  │  - Deployment Management                                  │  │
│  │  - Monitoring & Metrics                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Skipper Server                           │  │
│  │  - Stream Lifecycle Management                            │  │
│  │  - Rolling Updates                                        │  │
│  │  - Rollback Support                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Deployment Platform                           │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ Source   │→ │Processor │→ │Processor │→ │  Sink    │       │
│  │  App     │  │  App 1   │  │  App 2   │  │  App     │       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                                   │
│  Kubernetes / Cloud Foundry / Local                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Message Broker                                │
│                  (Kafka / RabbitMQ)                              │
└─────────────────────────────────────────────────────────────────┘
```

### Components

#### 1. **Data Flow Server**
- Central orchestration component
- Manages stream and task definitions
- Handles deployment requests
- Provides REST API and UI
- Stores metadata in database (MySQL, PostgreSQL)

#### 2. **Skipper Server**
- Manages stream lifecycle
- Handles rolling updates
- Supports blue-green deployments
- Enables rollback capabilities
- Stores deployment history

#### 3. **Applications**
- **Source**: Ingests data (HTTP, File, JDBC, Kafka)
- **Processor**: Transforms data (Filter, Transform, Splitter)
- **Sink**: Outputs data (Log, File, JDBC, MongoDB)

#### 4. **Message Broker**
- Kafka or RabbitMQ
- Connects applications in streams
- Provides buffering and reliability

---

## Core Concepts

### 1. Streams

**Definition:** Long-running, event-driven microservice applications.

**DSL Syntax:**
```
<source> | <processor> | <processor> | <sink>
```

**Example:**
```
http --server.port=9000 | transform --expression=payload.toUpperCase() | log
```

**Components:**
- **Source**: Data ingestion point
- **Processor**: Data transformation (optional, multiple allowed)
- **Sink**: Data destination

### 2. Tasks

**Definition:** Short-lived applications that perform a specific job and exit.

**Example:**
```
timestamp
```

**Use Cases:**
- Database migrations
- Data imports/exports
- Scheduled jobs
- ETL operations

### 3. Applications

**Types:**

| Type | Purpose | Lifecycle | Examples |
|------|---------|-----------|----------|
| **Source** | Ingest data | Long-running | http, file, jdbc, kafka |
| **Processor** | Transform data | Long-running | filter, transform, splitter |
| **Sink** | Output data | Long-running | log, file, jdbc, mongodb |
| **Task** | Execute job | Short-lived | timestamp, spark, composed-task |

### 4. Composed Tasks

**Definition:** Orchestrate multiple tasks with conditional execution.

**DSL Syntax:**
```
task1 && task2 || task3
```

**Example:**
```
jdbchdfs-local && spark-client && hdfs-delete
```

---

## Components Deep Dive

### Data Flow Server

**Configuration (application.yml):**
```yaml
spring:
  cloud:
    dataflow:
      version-info:
        dependency-fetch:
          enabled: true
      task:
        platform:
          kubernetes:
            accounts:
              default:
                namespace: default
      applicationProperties:
        stream:
          management:
            metrics:
              export:
                prometheus:
                  enabled: true
  datasource:
    url: jdbc:mysql://mysql:3306/dataflow
    username: root
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

**Start Data Flow Server:**
```bash
java -jar spring-cloud-dataflow-server-2.11.0.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/dataflow \
  --spring.datasource.username=root \
  --spring.datasource.password=password
```

### Skipper Server

**Configuration (application.yml):**
```yaml
spring:
  cloud:
    skipper:
      server:
        platform:
          kubernetes:
            accounts:
              default:
                namespace: default
                environmentVariables: 'SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=kafka:9092'
  datasource:
    url: jdbc:mysql://mysql:3306/skipper
    username: root
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Start Skipper Server:**
```bash
java -jar spring-cloud-skipper-server-2.11.0.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/skipper \
  --spring.datasource.username=root \
  --spring.datasource.password=password
```

### Shell (CLI)

**Start Shell:**
```bash
java -jar spring-cloud-dataflow-shell-2.11.0.jar
```

**Common Commands:**
```bash
# Register applications
app register --name http --type source --uri maven://org.springframework.cloud.stream.app:http-source-kafka:3.2.1

# Create stream
stream create --name httptest --definition "http | log"

# Deploy stream
stream deploy --name httptest

# List streams
stream list

# Undeploy stream
stream undeploy --name httptest

# Destroy stream
stream destroy --name httptest
```

---

## Stream Processing

### Stream Lifecycle

```
Define → Deploy → Monitor → Update → Undeploy → Destroy
```

### Creating Streams

#### 1. Simple HTTP to Log Stream

**Definition:**
```
http --server.port=9000 | log
```

**Create:**
```bash
stream create --name http-to-log \
  --definition "http --server.port=9000 | log"
```

**Deploy:**
```bash
stream deploy --name http-to-log
```

**Test:**
```bash
curl -X POST http://localhost:9000 -H "Content-Type: text/plain" -d "Hello World"
```

**Output:**
```
2024-01-15 10:30:45.123  INFO [log-sink] : Hello World
```

#### 2. HTTP with Transformation

**Definition:**
```
http --server.port=9000 | 
transform --expression=payload.toUpperCase() | 
log
```

**Create and Deploy:**
```bash
stream create --name http-transform-log \
  --definition "http --server.port=9000 | transform --expression=payload.toUpperCase() | log"

stream deploy --name http-transform-log
```

**Test:**
```bash
curl -X POST http://localhost:9000 -H "Content-Type: text/plain" -d "hello world"
```

**Output:**
```
2024-01-15 10:30:45.123  INFO [log-sink] : HELLO WORLD
```

#### 3. File to JDBC Stream

**Definition:**
```
file --directory=/tmp/input --filename-pattern=*.csv | 
transform --expression=#jsonPath(payload,'$.name') | 
jdbc --table-name=users --columns=name
```

**Create:**
```bash
stream create --name file-to-jdbc \
  --definition "file --directory=/tmp/input --filename-pattern=*.csv | transform --expression=#jsonPath(payload,'$.name') | jdbc --table-name=users --columns=name"
```

**Deploy with Properties:**
```bash
stream deploy --name file-to-jdbc \
  --properties "app.jdbc.spring.datasource.url=jdbc:mysql://localhost:3306/mydb,app.jdbc.spring.datasource.username=root,app.jdbc.spring.datasource.password=password"
```

#### 4. Kafka to MongoDB Stream

**Definition:**
```
:orders > 
filter --expression=payload.amount>100 | 
transform --expression=payload.customerId | 
mongodb --collection=high-value-customers
```

**Create:**
```bash
stream create --name kafka-to-mongo \
  --definition ":orders > filter --expression=payload.amount>100 | transform --expression=payload.customerId | mongodb --collection=high-value-customers"
```

**Deploy:**
```bash
stream deploy --name kafka-to-mongo \
  --properties "app.mongodb.spring.data.mongodb.uri=mongodb://localhost:27017/mydb"
```

### Stream DSL Syntax

#### Named Destinations

**Producer:**
```
http --server.port=9000 > :orders
```

**Consumer:**
```
:orders > log
```

#### Labels

```
http --server.port=9000 | myTransform: transform --expression=payload.toUpperCase() | log
```

#### Tap into Existing Stream

```
:httptest.http > log
```

### Deployment Properties

**Deployer Properties:**
```bash
stream deploy --name mystream \
  --properties "deployer.http.count=2,deployer.http.memory=512m,deployer.log.count=1"
```

**Application Properties:**
```bash
stream deploy --name mystream \
  --properties "app.http.server.port=9000,app.log.level=DEBUG"
```

**Platform Properties (Kubernetes):**
```bash
stream deploy --name mystream \
  --properties "deployer.http.kubernetes.limits.cpu=500m,deployer.http.kubernetes.limits.memory=1024Mi"
```

---

## Batch Processing

### Task Lifecycle

```
Define → Launch → Monitor → Destroy
```

### Creating Tasks

#### 1. Simple Timestamp Task

**Definition:**
```
timestamp
```

**Create:**
```bash
task create --name mytask --definition "timestamp"
```

**Launch:**
```bash
task launch --name mytask
```

**Check Execution:**
```bash
task execution list
```

#### 2. JDBC to HDFS Task

**Definition:**
```
jdbchdfs-local
```

**Create:**
```bash
task create --name jdbc-to-hdfs \
  --definition "jdbchdfs-local"
```

**Launch with Arguments:**
```bash
task launch --name jdbc-to-hdfs \
  --arguments "jdbchdfs-local.sql=SELECT * FROM users,jdbchdfs-local.hdfsDirectory=/data/users"
```

#### 3. Composed Task

**Definition:**
```
task1 && task2 || task3
```

**Operators:**
- `&&`: Sequential execution (AND)
- `||`: Execute if previous failed (OR)
- `*`: Parallel execution

**Example:**
```
extract-data && transform-data && load-data || send-alert
```

**Create:**
```bash
task create --name etl-pipeline \
  --definition "extract-data && transform-data && load-data || send-alert"
```

**Launch:**
```bash
task launch --name etl-pipeline
```

### Scheduling Tasks

**Using Cron:**
```bash
task schedule create --name mytask-schedule \
  --definitionName mytask \
  --expression "0 0 * * * *"
```

**List Schedules:**
```bash
task schedule list
```

**Delete Schedule:**
```bash
task schedule destroy --name mytask-schedule
```

---

## Real-World Example

### Use Case: Real-Time Order Processing Pipeline

**Scenario:** E-commerce platform processing orders in real-time.

```
Orders (Kafka) → Validate → Enrich → Filter → Store (MongoDB) → Notify (Email)
                                    ↓
                              Analytics (ClickHouse)
```

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Order Processing Stream                       │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Kafka   │→ │ Validate │→ │  Enrich  │→ │  Filter  │       │
│  │ Source   │  │ Processor│  │ Processor│  │ Processor│       │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘       │
│                                                   │               │
│                                    ┌──────────────┴──────────┐  │
│                                    ▼                         ▼  │
│                              ┌──────────┐            ┌──────────┐│
│                              │ MongoDB  │            │ClickHouse││
│                              │   Sink   │            │   Sink   ││
│                              └──────────┘            └──────────┘│
│                                    │                              │
│                                    ▼                              │
│                              ┌──────────┐                        │
│                              │  Email   │                        │
│                              │   Sink   │                        │
│                              └──────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

### Step 1: Setup Infrastructure

**Docker Compose (infrastructure.yml):**
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: dataflow
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  skipper:
    image: springcloud/spring-cloud-skipper-server:2.11.0
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/dataflow
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: rootpass
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: com.mysql.cj.jdbc.Driver
      SPRING_CLOUD_SKIPPER_SERVER_PLATFORM_LOCAL_ACCOUNTS_DEFAULT_PORTRANGE_LOW: 20000
      SPRING_CLOUD_SKIPPER_SERVER_PLATFORM_LOCAL_ACCOUNTS_DEFAULT_PORTRANGE_HIGH: 20100
    ports:
      - "7577:7577"
    depends_on:
      - mysql

  dataflow:
    image: springcloud/spring-cloud-dataflow-server:2.11.0
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/dataflow
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: rootpass
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: com.mysql.cj.jdbc.Driver
      SPRING_CLOUD_DATAFLOW_APPLICATIONPROPERTIES_STREAM_SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: kafka:9092
      SPRING_CLOUD_DATAFLOW_APPLICATIONPROPERTIES_STREAM_SPRING_CLOUD_STREAM_KAFKA_BINDER_ZKNODES: zookeeper:2181
      SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI: http://skipper:7577/api
    ports:
      - "9393:9393"
    depends_on:
      - mysql
      - kafka
      - skipper

volumes:
  mysql-data:
  mongo-data:
```

**Start Infrastructure:**
```bash
docker-compose -f infrastructure.yml up -d
```

### Step 2: Register Applications

```bash
# Start Data Flow Shell
java -jar spring-cloud-dataflow-shell-2.11.0.jar

# Register Kafka source
app register --name kafka --type source \
  --uri maven://org.springframework.cloud.stream.app:kafka-source-kafka:3.2.1

# Register processors
app register --name transform --type processor \
  --uri maven://org.springframework.cloud.stream.app:transform-processor-kafka:3.2.1

app register --name filter --type processor \
  --uri maven://org.springframework.cloud.stream.app:filter-processor-kafka:3.2.1

# Register MongoDB sink
app register --name mongodb --type sink \
  --uri maven://org.springframework.cloud.stream.app:mongodb-sink-kafka:3.2.1

# Register log sink
app register --name log --type sink \
  --uri maven://org.springframework.cloud.stream.app:log-sink-kafka:3.2.1
```

### Step 3: Create Custom Processor (Validation)

**OrderValidationProcessor.java:**
```java
@SpringBootApplication
public class OrderValidationProcessor {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderValidationProcessor.class, args);
    }
    
    @Bean
    public Function<Order, Order> validate() {
        return order -> {
            // Validate order
            if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid order amount");
            }
            
            if (order.getCustomerId() == null || order.getCustomerId().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            
            // Add validation timestamp
            order.setValidatedAt(Instant.now());
            
            return order;
        };
    }
}

@Data
class Order {
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;
    private Instant validatedAt;
    private Instant enrichedAt;
}
```

**application.yml:**
```yaml
spring:
  cloud:
    function:
      definition: validate
    stream:
      bindings:
        validate-in-0:
          destination: orders
        validate-out-0:
          destination: validated-orders
```

**Build and Register:**
```bash
mvn clean package
app register --name order-validator --type processor \
  --uri file:///path/to/order-validation-processor-1.0.0.jar
```

### Step 4: Create Custom Processor (Enrichment)

**OrderEnrichmentProcessor.java:**
```java
@SpringBootApplication
public class OrderEnrichmentProcessor {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Bean
    public Function<Order, Order> enrich() {
        return order -> {
            // Fetch customer details
            Customer customer = restTemplate.getForObject(
                "http://customer-service/api/customers/" + order.getCustomerId(),
                Customer.class
            );
            
            // Enrich order with customer data
            order.setCustomerName(customer.getName());
            order.setCustomerEmail(customer.getEmail());
            order.setCustomerTier(customer.getTier());
            
            // Add enrichment timestamp
            order.setEnrichedAt(Instant.now());
            
            return order;
        };
    }
}
```

### Step 5: Create Stream Definition

**Complete Stream:**
```
kafka --topics=orders --group-id=order-processor | 
order-validator | 
order-enricher | 
filter --expression=payload.amount>100 | 
mongodb --collection=orders
```

**Create Stream:**
```bash
stream create --name order-processing \
  --definition "kafka --topics=orders --group-id=order-processor | order-validator | order-enricher | filter --expression=payload.amount>100 | mongodb --collection=orders"
```

### Step 6: Deploy Stream

```bash
stream deploy --name order-processing \
  --properties "deployer.kafka.count=2,deployer.kafka.memory=512m,deployer.order-validator.count=3,deployer.order-enricher.count=3,deployer.filter.count=2,deployer.mongodb.count=2,app.mongodb.spring.data.mongodb.uri=mongodb://mongodb:27017/orders"
```

### Step 7: Create Analytics Branch

**Stream Definition:**
```
:order-processing.filter > 
transform --expression=payload.customerId + ',' + payload.amount | 
log
```

**Create:**
```bash
stream create --name order-analytics \
  --definition ":order-processing.filter > transform --expression=payload.customerId + ',' + payload.amount | log"

stream deploy --name order-analytics
```

### Step 8: Test Pipeline

**Produce Test Orders:**
```bash
kafka-console-producer --broker-list localhost:9092 --topic orders

# Enter orders (JSON)
{"orderId":"ORD-001","customerId":"CUST-123","amount":150.00,"status":"PENDING","createdAt":"2024-01-15T10:30:00Z"}
{"orderId":"ORD-002","customerId":"CUST-456","amount":50.00,"status":"PENDING","createdAt":"2024-01-15T10:31:00Z"}
{"orderId":"ORD-003","customerId":"CUST-789","amount":200.00,"status":"PENDING","createdAt":"2024-01-15T10:32:00Z"}
```

**Verify in MongoDB:**
```bash
mongo
use orders
db.orders.find().pretty()
```

**Expected Output:**
```json
{
  "_id": ObjectId("..."),
  "orderId": "ORD-001",
  "customerId": "CUST-123",
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "customerTier": "GOLD",
  "amount": 150.00,
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z",
  "validatedAt": "2024-01-15T10:30:01Z",
  "enrichedAt": "2024-01-15T10:30:02Z"
}
```

---

## Continue to Part 2

Part 2 will cover:
- Deployment Options (Kubernetes, Cloud Foundry, Local)
- Monitoring and Operations
- Comparison with Alternatives
- Production Best Practices
- Troubleshooting
- Interview Questions
