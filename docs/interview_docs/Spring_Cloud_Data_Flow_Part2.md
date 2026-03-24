# Spring Cloud Data Flow - Part 2

## Table of Contents
- [Deployment Options](#deployment-options)
- [Monitoring and Operations](#monitoring-and-operations)
- [Comparison with Alternatives](#comparison-with-alternatives)
- [Production Best Practices](#production-best-practices)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Interview Questions](#interview-questions)

---

## Deployment Options

### 1. Local Deployment

**Use Case:** Development, testing, POC

**Setup:**
```bash
# Start Data Flow Server
java -jar spring-cloud-dataflow-server-2.11.0.jar

# Start Skipper Server
java -jar spring-cloud-skipper-server-2.11.0.jar

# Access UI
http://localhost:9393/dashboard
```

**Pros:**
- ✅ Quick setup
- ✅ No infrastructure required
- ✅ Easy debugging

**Cons:**
- ❌ No high availability
- ❌ Limited scalability
- ❌ Not production-ready

### 2. Kubernetes Deployment

**Use Case:** Production, cloud-native environments

#### Installation with Helm

**Add Helm Repository:**
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

**Install:**
```bash
helm install my-dataflow bitnami/spring-cloud-dataflow \
  --set server.configuration.accountName=default \
  --set server.configuration.trustK8sCerts=true \
  --set rabbitmq.enabled=false \
  --set kafka.enabled=true \
  --set kafka.replicaCount=3
```

#### Manual Kubernetes Deployment

**namespace.yaml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: scdf
```

**mysql-deployment.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: scdf
spec:
  ports:
  - port: 3306
  selector:
    app: mysql
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: scdf
spec:
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          value: rootpass
        - name: MYSQL_DATABASE
          value: dataflow
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: mysql-storage
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-storage
        persistentVolumeClaim:
          claimName: mysql-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: scdf
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

**kafka-deployment.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: scdf
spec:
  ports:
  - port: 9092
  selector:
    app: kafka
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: scdf
spec:
  serviceName: kafka
  replicas: 3
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_BROKER_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: zookeeper:2181
        - name: KAFKA_ADVERTISED_LISTENERS
          value: PLAINTEXT://kafka:9092
```

**skipper-deployment.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: skipper
  namespace: scdf
spec:
  ports:
  - port: 7577
  selector:
    app: skipper
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: skipper
  namespace: scdf
spec:
  replicas: 1
  selector:
    matchLabels:
      app: skipper
  template:
    metadata:
      labels:
        app: skipper
    spec:
      containers:
      - name: skipper
        image: springcloud/spring-cloud-skipper-server:2.11.0
        ports:
        - containerPort: 7577
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mysql://mysql:3306/dataflow
        - name: SPRING_DATASOURCE_USERNAME
          value: root
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        - name: SPRING_CLOUD_SKIPPER_SERVER_PLATFORM_KUBERNETES_ACCOUNTS_DEFAULT_NAMESPACE
          value: scdf
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
```

**dataflow-deployment.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: dataflow
  namespace: scdf
spec:
  type: LoadBalancer
  ports:
  - port: 9393
    targetPort: 9393
  selector:
    app: dataflow
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dataflow
  namespace: scdf
spec:
  replicas: 1
  selector:
    matchLabels:
      app: dataflow
  template:
    metadata:
      labels:
        app: dataflow
    spec:
      serviceAccountName: scdf-sa
      containers:
      - name: dataflow
        image: springcloud/spring-cloud-dataflow-server:2.11.0
        ports:
        - containerPort: 9393
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:mysql://mysql:3306/dataflow
        - name: SPRING_DATASOURCE_USERNAME
          value: root
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        - name: SPRING_CLOUD_DATAFLOW_APPLICATIONPROPERTIES_STREAM_SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS
          value: kafka:9092
        - name: SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI
          value: http://skipper:7577/api
        - name: SPRING_CLOUD_DATAFLOW_FEATURES_STREAMS_ENABLED
          value: "true"
        - name: SPRING_CLOUD_DATAFLOW_FEATURES_TASKS_ENABLED
          value: "true"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /management/health
            port: 9393
          initialDelaySeconds: 120
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /management/health
            port: 9393
          initialDelaySeconds: 60
          periodSeconds: 10
```

**rbac.yaml:**
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: scdf-sa
  namespace: scdf
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: scdf-role
  namespace: scdf
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps", "secrets"]
  verbs: ["get", "list", "watch", "create", "update", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments", "statefulsets"]
  verbs: ["get", "list", "watch", "create", "update", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: scdf-role-binding
  namespace: scdf
subjects:
- kind: ServiceAccount
  name: scdf-sa
  namespace: scdf
roleRef:
  kind: Role
  name: scdf-role
  apiGroup: rbac.authorization.k8s.io
```

**Deploy:**
```bash
kubectl apply -f namespace.yaml
kubectl apply -f rbac.yaml
kubectl apply -f mysql-deployment.yaml
kubectl apply -f kafka-deployment.yaml
kubectl apply -f skipper-deployment.yaml
kubectl apply -f dataflow-deployment.yaml

# Get external IP
kubectl get svc dataflow -n scdf
```

**Stream Deployment Properties (Kubernetes):**
```bash
stream deploy --name mystream \
  --properties "deployer.*.kubernetes.namespace=scdf,deployer.*.kubernetes.limits.memory=1024Mi,deployer.*.kubernetes.limits.cpu=1000m,deployer.*.kubernetes.requests.memory=512Mi,deployer.*.kubernetes.requests.cpu=500m"
```

### 3. Cloud Foundry Deployment

**Use Case:** Enterprise cloud platforms

**manifest.yml:**
```yaml
applications:
- name: dataflow-server
  memory: 2G
  disk_quota: 2G
  instances: 1
  path: spring-cloud-dataflow-server-2.11.0.jar
  env:
    SPRING_APPLICATION_NAME: dataflow-server
    SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI: https://skipper-server.apps.example.com/api
    SPRING_APPLICATION_JSON: |-
      {
        "spring.cloud.dataflow.features.streams-enabled": true,
        "spring.cloud.dataflow.features.tasks-enabled": true
      }
  services:
  - mysql
  - redis
```

**Deploy:**
```bash
cf push
```

---

## Monitoring and Operations

### 1. Built-in Monitoring

**Metrics Endpoints:**
```
http://localhost:9393/management/metrics
http://localhost:9393/management/health
http://localhost:9393/management/info
```

**Stream Metrics:**
```bash
# Via Shell
stream runtime --name mystream

# Via REST API
curl http://localhost:9393/runtime/streams/mystream
```

**Response:**
```json
{
  "streamName": "mystream",
  "applications": [
    {
      "name": "http",
      "instances": [
        {
          "guid": "http-0",
          "state": "deployed",
          "attributes": {
            "port": "9000",
            "host": "192.168.1.10"
          }
        }
      ]
    }
  ]
}
```

### 2. Prometheus Integration

**Enable Prometheus:**
```yaml
# application.yml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
```

**Prometheus Configuration (prometheus.yml):**
```yaml
scrape_configs:
  - job_name: 'scdf-server'
    metrics_path: '/management/prometheus'
    static_configs:
    - targets: ['dataflow:9393']
  
  - job_name: 'scdf-streams'
    kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
        - scdf
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_spring_deployment_id]
      action: keep
      regex: .+
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
      action: replace
      target_label: __metrics_path__
      regex: (.+)
```

**Key Metrics:**
```promql
# Message throughput
rate(spring_integration_send_seconds_count[5m])

# Message processing time
spring_integration_send_seconds_sum / spring_integration_send_seconds_count

# Error rate
rate(spring_integration_send_failures_total[5m])

# Active streams
scdf_stream_count{status="deployed"}

# Task executions
scdf_task_execution_count
```

### 3. Grafana Dashboards

**Import Dashboard:**
- Dashboard ID: 9933 (Spring Cloud Data Flow)
- Dashboard ID: 11378 (Spring Cloud Stream)

**Custom Dashboard Panels:**

**Panel 1: Message Throughput**
```promql
sum(rate(spring_integration_send_seconds_count{stream_name="mystream"}[5m])) by (application_name)
```

**Panel 2: Processing Latency**
```promql
histogram_quantile(0.99, 
  sum(rate(spring_integration_send_seconds_bucket{stream_name="mystream"}[5m])) by (le, application_name)
)
```

**Panel 3: Error Rate**
```promql
sum(rate(spring_integration_send_failures_total{stream_name="mystream"}[5m])) by (application_name)
```

### 4. Logging

**Centralized Logging with ELK:**

**Filebeat Configuration:**
```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
  - add_kubernetes_metadata:
      host: ${NODE_NAME}
      matchers:
      - logs_path:
          logs_path: "/var/lib/docker/containers/"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "scdf-logs-%{+yyyy.MM.dd}"
```

**Logstash Pipeline:**
```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  if [kubernetes][labels][spring-deployment-id] {
    mutate {
      add_field => {
        "stream_name" => "%{[kubernetes][labels][spring-group-id]}"
        "app_name" => "%{[kubernetes][labels][spring-app-id]}"
      }
    }
  }
  
  grok {
    match => {
      "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "scdf-logs-%{+YYYY.MM.dd}"
  }
}
```

### 5. Alerting

**Prometheus Alert Rules:**
```yaml
groups:
- name: scdf_alerts
  interval: 30s
  rules:
  - alert: StreamDown
    expr: scdf_stream_count{status="deployed"} == 0
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "Stream {{ $labels.stream_name }} is down"
      
  - alert: HighErrorRate
    expr: rate(spring_integration_send_failures_total[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High error rate in {{ $labels.application_name }}"
      
  - alert: HighLatency
    expr: histogram_quantile(0.99, rate(spring_integration_send_seconds_bucket[5m])) > 5
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "High latency in {{ $labels.application_name }}"
      
  - alert: TaskExecutionFailed
    expr: scdf_task_execution_count{status="ERROR"} > 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Task {{ $labels.task_name }} execution failed"
```

---

## Comparison with Alternatives

### Spring Cloud Data Flow vs Apache NiFi

| Feature | SCDF | Apache NiFi |
|---------|------|-------------|
| **Architecture** | Microservices | Monolithic |
| **Language** | Java/Spring | Java |
| **UI** | Web-based | Web-based (better) |
| **Learning Curve** | Moderate | Steep |
| **Scalability** | Excellent | Good |
| **Cloud Native** | Yes | Limited |
| **Message Brokers** | Kafka, RabbitMQ | Built-in |
| **Deployment** | K8s, CF, Local | Standalone, Cluster |
| **Use Case** | Cloud-native streams | Enterprise data flows |

### Spring Cloud Data Flow vs Apache Airflow

| Feature | SCDF | Apache Airflow |
|---------|------|----------------|
| **Primary Use** | Stream + Batch | Batch (DAGs) |
| **Real-time** | Excellent | Limited |
| **Scheduling** | Basic | Advanced |
| **Dependencies** | Limited | Complex DAGs |
| **Language** | Java/Spring | Python |
| **UI** | Good | Excellent |
| **Monitoring** | Built-in | Extensive |
| **Use Case** | Real-time streams | Batch workflows |

### Spring Cloud Data Flow vs Kafka Streams

| Feature | SCDF | Kafka Streams |
|---------|------|---------------|
| **Abstraction** | High-level | Low-level |
| **Code Required** | Minimal | Extensive |
| **Flexibility** | Limited | High |
| **Deployment** | Managed | Self-managed |
| **Learning Curve** | Easy | Moderate |
| **Performance** | Good | Excellent |
| **Use Case** | Quick pipelines | Custom processing |

### When to Use SCDF

✅ **Use SCDF When:**
- Need quick pipeline development
- Want pre-built connectors
- Require visual pipeline designer
- Cloud-native deployment (K8s)
- Spring ecosystem integration
- Minimal custom code

❌ **Don't Use SCDF When:**
- Need complex custom logic
- Require maximum performance
- Want fine-grained control
- Non-Java ecosystem
- Simple use cases (overkill)

---

## Production Best Practices

### 1. High Availability Setup

**Multi-Instance Deployment:**
```yaml
# Kubernetes
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dataflow
spec:
  replicas: 3  # Multiple instances
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

**Database Replication:**
```yaml
# MySQL with read replicas
spring:
  datasource:
    url: jdbc:mysql://mysql-primary:3306/dataflow
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

### 2. Resource Management

**Set Resource Limits:**
```bash
stream deploy --name mystream \
  --properties "deployer.http.kubernetes.limits.memory=1Gi,deployer.http.kubernetes.limits.cpu=1000m,deployer.http.kubernetes.requests.memory=512Mi,deployer.http.kubernetes.requests.cpu=500m"
```

**Auto-scaling:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: http-source-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: http-source
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 3. Security

**Enable Authentication:**
```yaml
spring:
  security:
    user:
      name: admin
      password: ${ADMIN_PASSWORD}
  cloud:
    dataflow:
      security:
        authorization:
          enabled: true
          rules:
            - GET    /management/**         => hasRole('ROLE_MANAGE')
            - POST   /streams/definitions   => hasRole('ROLE_CREATE')
            - DELETE /streams/definitions/* => hasRole('ROLE_DESTROY')
```

**OAuth2 Integration:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          uaa:
            client-id: dataflow
            client-secret: ${CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          uaa:
            authorization-uri: https://uaa.example.com/oauth/authorize
            token-uri: https://uaa.example.com/oauth/token
            user-info-uri: https://uaa.example.com/userinfo
```

### 4. Backup and Recovery

**Database Backup:**
```bash
# MySQL backup
mysqldump -u root -p dataflow > dataflow_backup_$(date +%Y%m%d).sql

# Restore
mysql -u root -p dataflow < dataflow_backup_20240115.sql
```

**Stream Definitions Export:**
```bash
# Export all streams
stream all --export > streams_backup.txt

# Import streams
stream import --uri file:///path/to/streams_backup.txt
```

### 5. Performance Tuning

**JVM Options:**
```bash
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

**Database Connection Pool:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Message Broker Tuning:**
```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka1:9092,kafka2:9092,kafka3:9092
          configuration:
            compression.type: snappy
            batch.size: 32768
            linger.ms: 10
            buffer.memory: 67108864
```

---

## Troubleshooting Guide

### Common Issues

#### 1. Stream Fails to Deploy

**Symptoms:**
```
Stream deployment failed: Application not found
```

**Solutions:**
- Verify application is registered
- Check application URI is accessible
- Validate deployment properties
- Review server logs

**Debug:**
```bash
# List registered apps
app list

# Check app info
app info --name http --type source

# View deployment logs
kubectl logs -n scdf deployment/http-source
```

#### 2. High Memory Usage

**Symptoms:**
```
OutOfMemoryError in stream applications
```

**Solutions:**
- Increase memory limits
- Tune JVM options
- Check for memory leaks
- Reduce batch sizes

**Fix:**
```bash
stream deploy --name mystream \
  --properties "deployer.*.kubernetes.limits.memory=2Gi,deployer.*.javaOpts=-Xmx1536m"
```

#### 3. Message Processing Lag

**Symptoms:**
```
Consumer lag increasing
```

**Solutions:**
- Increase consumer instances
- Tune batch sizes
- Check downstream bottlenecks
- Optimize processing logic

**Fix:**
```bash
stream deploy --name mystream \
  --properties "deployer.processor.count=5,app.processor.spring.cloud.stream.kafka.bindings.input.consumer.concurrency=3"
```

#### 4. Database Connection Issues

**Symptoms:**
```
Unable to acquire JDBC Connection
```

**Solutions:**
- Verify database is running
- Check connection pool settings
- Validate credentials
- Review network connectivity

**Fix:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      connection-timeout: 60000
```

---

## Interview Questions

### Q1: What is Spring Cloud Data Flow?

**Answer:**
SCDF is a cloud-native orchestration service for composable microservice applications. It provides:
- Stream processing (real-time)
- Batch processing (tasks)
- Pre-built applications
- Visual pipeline designer
- Multi-platform deployment (K8s, CF, Local)

### Q2: Difference between Stream and Task?

**Answer:**
- **Stream**: Long-running, event-driven applications
  - Example: `http | transform | log`
  - Use case: Real-time data processing
- **Task**: Short-lived applications that exit after completion
  - Example: `timestamp`
  - Use case: Batch jobs, ETL

### Q3: What is Skipper Server?

**Answer:**
Skipper manages stream lifecycle:
- Deployment and updates
- Rolling updates
- Blue-green deployments
- Rollback capabilities
- Version history

### Q4: How does SCDF handle scaling?

**Answer:**
1. **Horizontal Scaling**: Increase instance count
   ```bash
   deployer.http.count=5
   ```
2. **Vertical Scaling**: Increase resources
   ```bash
   deployer.http.memory=2Gi
   ```
3. **Auto-scaling**: HPA in Kubernetes
4. **Partitioning**: Kafka partition-based scaling

### Q5: What are the deployment platforms?

**Answer:**
- **Local**: Development/testing
- **Kubernetes**: Production, cloud-native
- **Cloud Foundry**: Enterprise platforms
- **Custom**: Implement Deployer SPI

### Q6: How to monitor SCDF applications?

**Answer:**
1. **Built-in Metrics**: Actuator endpoints
2. **Prometheus**: Metrics export
3. **Grafana**: Visualization
4. **Logging**: ELK stack integration
5. **Tracing**: Zipkin/Sleuth

### Q7: SCDF vs Kafka Streams?

**Answer:**
**SCDF:**
- High-level abstraction
- Minimal code
- Visual designer
- Pre-built apps

**Kafka Streams:**
- Low-level API
- Custom code
- Maximum flexibility
- Better performance

**Use SCDF for**: Quick pipelines, pre-built connectors
**Use Kafka Streams for**: Complex logic, custom processing

### Q8: How to handle errors in streams?

**Answer:**
1. **DLQ (Dead Letter Queue)**: Failed messages to separate topic
2. **Retry**: Automatic retry with backoff
3. **Error Handlers**: Custom error handling logic
4. **Monitoring**: Alerts on error metrics

```yaml
spring:
  cloud:
    stream:
      bindings:
        input:
          consumer:
            max-attempts: 3
      kafka:
        bindings:
          input:
            consumer:
              enable-dlq: true
```

### Q9: Best practices for production?

**Answer:**
1. **HA Setup**: Multiple instances
2. **Resource Limits**: Set CPU/memory
3. **Monitoring**: Comprehensive metrics
4. **Security**: Authentication/authorization
5. **Backup**: Database and definitions
6. **Auto-scaling**: HPA configuration
7. **Logging**: Centralized logging
8. **Testing**: Validate before deployment

### Q10: When NOT to use SCDF?

**Answer:**
❌ **Don't use when:**
- Need complex custom logic
- Require maximum performance
- Simple use cases (overkill)
- Non-Java ecosystem
- Want fine-grained control
- Batch-only workflows (use Airflow)

---

## Summary

### Key Takeaways

1. **SCDF** = Orchestration service for data pipelines
2. **Streams** = Real-time processing
3. **Tasks** = Batch processing
4. **Pre-built Apps** = 70+ ready-to-use applications
5. **Multi-Platform** = K8s, CF, Local
6. **Visual Designer** = Drag-and-drop UI

### Architecture

```
Data Flow Server + Skipper Server
         ↓
Stream/Task Applications
         ↓
Message Broker (Kafka/RabbitMQ)
```

### Production Checklist

- [ ] Kubernetes deployment
- [ ] High availability (3+ instances)
- [ ] Resource limits configured
- [ ] Monitoring enabled (Prometheus/Grafana)
- [ ] Security configured (OAuth2)
- [ ] Backup strategy in place
- [ ] Auto-scaling configured
- [ ] Logging centralized (ELK)
- [ ] Alerting configured
- [ ] Documentation complete

---

**Related Documents:**
- [Kafka_Architecture_And_Parallelism.md](Kafka_Architecture_And_Parallelism.md)
- [Spring_Cloud.md](spring/09_Spring_Cloud.md)
- [Spring_Batch_Part1.md](spring/12_Spring_Batch_Part1.md)
