# Elastic Stack Components

Comprehensive guide to all components of the Elastic Stack (formerly ELK Stack): Elasticsearch, Logstash, Kibana, Beats, APM, and Security.

## Table of Contents
- [Elasticsearch](#elasticsearch)
- [Logstash](#logstash)
- [Kibana](#kibana)
- [Beats](#beats)
- [Elastic APM](#elastic-apm)
- [Elastic Security](#elastic-security)
- [Complete Stack Architecture](#complete-stack-architecture)

## Elasticsearch

The core search and analytics engine (covered in previous chapters).

**Key capabilities:**
- Distributed search and analytics
- RESTful API
- Near real-time indexing and search
- Horizontal scalability
- Full-text search with relevance scoring

## Logstash

**Logstash** is a server-side data processing pipeline that ingests, transforms, and sends data to Elasticsearch.

### Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Inputs    │ -> │   Filters   │ -> │   Outputs   │
│  (Sources)  │    │ (Transform) │    │(Destinations)│
└─────────────┘    └─────────────┘    └─────────────┘
```

### Core Concepts

**Pipeline structure:**
```
input {
  # Where data comes from
}

filter {
  # How to process data
}

output {
  # Where to send data
}
```

### Input Plugins

**1. File input (read log files):**
```ruby
input {
  file {
    path => "/var/log/nginx/access.log"
    start_position => "beginning"
    sincedb_path => "/dev/null"
  }
}
```

**2. Beats input (receive from Beats):**
```ruby
input {
  beats {
    port => 5044
  }
}
```

**3. HTTP input (webhook):**
```ruby
input {
  http {
    port => 8080
    codec => json
  }
}
```

**4. Kafka input:**
```ruby
input {
  kafka {
    bootstrap_servers => "localhost:9092"
    topics => ["logs"]
    group_id => "logstash"
  }
}
```

**5. JDBC input (database):**
```ruby
input {
  jdbc {
    jdbc_connection_string => "jdbc:postgresql://localhost:5432/mydb"
    jdbc_user => "user"
    jdbc_password => "password"
    statement => "SELECT * FROM products WHERE updated_at > :sql_last_value"
    schedule => "*/5 * * * *"
  }
}
```

### Filter Plugins

**1. Grok (parse unstructured logs):**
```ruby
filter {
  grok {
    match => {
      "message" => "%{COMBINEDAPACHELOG}"
    }
  }
}
```

**Custom grok pattern:**
```ruby
filter {
  grok {
    match => {
      "message" => "%{IP:client_ip} - - \[%{HTTPDATE:timestamp}\] \"%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}\" %{NUMBER:status_code} %{NUMBER:bytes}"
    }
  }
}
```

**2. Date (parse timestamps):**
```ruby
filter {
  date {
    match => ["timestamp", "dd/MMM/yyyy:HH:mm:ss Z"]
    target => "@timestamp"
  }
}
```

**3. Mutate (modify fields):**
```ruby
filter {
  mutate {
    # Add field
    add_field => { "environment" => "production" }
    
    # Remove field
    remove_field => ["temp_field"]
    
    # Rename field
    rename => { "old_name" => "new_name" }
    
    # Convert type
    convert => { "status_code" => "integer" }
    
    # Lowercase
    lowercase => ["user_agent"]
  }
}
```

**4. GeoIP (add location data):**
```ruby
filter {
  geoip {
    source => "client_ip"
    target => "geo"
  }
}
```

**5. JSON (parse JSON):**
```ruby
filter {
  json {
    source => "message"
    target => "parsed"
  }
}
```

**6. User Agent (parse user agent):**
```ruby
filter {
  useragent {
    source => "user_agent"
    target => "ua"
  }
}
```

**7. Drop (filter out events):**
```ruby
filter {
  if [status_code] == 200 {
    drop { }
  }
}
```

### Output Plugins

**1. Elasticsearch output:**
```ruby
output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "logs-%{+YYYY.MM.dd}"
    document_id => "%{[@metadata][fingerprint]}"
  }
}
```

**2. File output:**
```ruby
output {
  file {
    path => "/var/log/logstash/output.log"
    codec => line { format => "%{message}" }
  }
}
```

**3. Kafka output:**
```ruby
output {
  kafka {
    bootstrap_servers => "localhost:9092"
    topic_id => "processed-logs"
  }
}
```

**4. S3 output:**
```ruby
output {
  s3 {
    bucket => "my-logs-bucket"
    region => "us-east-1"
    time_file => 5
  }
}
```

### Complete Example: Nginx Log Processing

```ruby
input {
  file {
    path => "/var/log/nginx/access.log"
    start_position => "beginning"
  }
}

filter {
  # Parse nginx log
  grok {
    match => {
      "message" => "%{IPORHOST:client_ip} - - \[%{HTTPDATE:timestamp}\] \"%{WORD:method} %{URIPATHPARAM:request} HTTP/%{NUMBER:http_version}\" %{NUMBER:status_code} %{NUMBER:bytes} \"%{DATA:referrer}\" \"%{DATA:user_agent}\""
    }
  }
  
  # Parse timestamp
  date {
    match => ["timestamp", "dd/MMM/yyyy:HH:mm:ss Z"]
    target => "@timestamp"
  }
  
  # Add geo location
  geoip {
    source => "client_ip"
    target => "geo"
  }
  
  # Parse user agent
  useragent {
    source => "user_agent"
    target => "ua"
  }
  
  # Convert types
  mutate {
    convert => {
      "status_code" => "integer"
      "bytes" => "integer"
    }
  }
  
  # Remove original message
  mutate {
    remove_field => ["message"]
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "nginx-logs-%{+YYYY.MM.dd}"
  }
  
  # Debug output
  stdout {
    codec => rubydebug
  }
}
```

### Performance Tuning

**Pipeline workers:**
```yaml
# logstash.yml
pipeline.workers: 4
pipeline.batch.size: 125
pipeline.batch.delay: 50
```

**JVM heap:**
```bash
# jvm.options
-Xms2g
-Xmx2g
```

## Kibana

**Kibana** is the visualization and management interface for the Elastic Stack.

### Key Features

1. **Discover**: Explore and search data
2. **Visualize**: Create charts and graphs
3. **Dashboard**: Combine visualizations
4. **Canvas**: Pixel-perfect presentations
5. **Maps**: Geo-spatial visualizations
6. **Machine Learning**: Anomaly detection
7. **Alerting**: Set up alerts
8. **Dev Tools**: Query console
9. **Stack Management**: Manage Elastic Stack

### Installation

```bash
# Docker
docker run -d \
  --name kibana \
  --link elasticsearch:elasticsearch \
  -p 5601:5601 \
  docker.elastic.co/kibana/kibana:8.11.0

# Access: http://localhost:5601
```

### Creating Visualizations

**1. Line chart (time series):**
```
Visualization Type: Line
Metrics: Count
Buckets: Date Histogram on @timestamp
```

**2. Pie chart (distribution):**
```
Visualization Type: Pie
Metrics: Count
Buckets: Terms on status_code.keyword
```

**3. Data table:**
```
Visualization Type: Data Table
Metrics: Count, Average of response_time
Buckets: Terms on endpoint.keyword
```

**4. Metric (single number):**
```
Visualization Type: Metric
Metrics: Sum of revenue
```

**5. Heat map:**
```
Visualization Type: Heat Map
Metrics: Count
Buckets: 
  - Date Histogram on @timestamp (X-axis)
  - Terms on status_code (Y-axis)
```

### Dashboard Example

**Web Analytics Dashboard:**
```
┌─────────────────────────────────────────────────┐
│  Total Requests: 1.2M    Avg Response: 150ms   │
├─────────────────────────────────────────────────┤
│  Requests Over Time (Line Chart)                │
│  ▁▂▃▅▇█▇▅▃▂▁                                   │
├──────────────────────┬──────────────────────────┤
│  Status Codes (Pie)  │  Top Endpoints (Table)   │
│  ● 200: 85%          │  /api/users    50K       │
│  ● 404: 10%          │  /api/products 30K       │
│  ● 500: 5%           │  /api/orders   20K       │
├──────────────────────┴──────────────────────────┤
│  Geographic Distribution (Map)                  │
│  🗺️ [World map with request heatmap]           │
└─────────────────────────────────────────────────┘
```

### Kibana Query Language (KQL)

**Simple queries:**
```
status_code: 500
method: "GET" and status_code: 200
response_time > 1000
user_agent: *Chrome*
@timestamp >= "2024-01-01" and @timestamp < "2024-02-01"
```

**Boolean operators:**
```
(status_code: 500 or status_code: 502) and method: "POST"
status_code: 200 and not user_agent: *bot*
```

### Alerting

**Create alert:**
```json
{
  "name": "High Error Rate",
  "schedule": {
    "interval": "5m"
  },
  "conditions": [
    {
      "query": {
        "bool": {
          "must": [
            { "term": { "status_code": 500 } },
            { "range": { "@timestamp": { "gte": "now-5m" } } }
          ]
        }
      },
      "threshold": {
        "count": 100
      }
    }
  ],
  "actions": [
    {
      "type": "email",
      "to": ["ops@company.com"],
      "subject": "High error rate detected"
    },
    {
      "type": "slack",
      "webhook": "https://hooks.slack.com/..."
    }
  ]
}
```

## Beats

**Beats** are lightweight data shippers that send data to Elasticsearch or Logstash.

### Beat Types

| Beat | Purpose | Data Source |
|------|---------|-------------|
| **Filebeat** | Log files | Application logs, system logs |
| **Metricbeat** | Metrics | System metrics, service metrics |
| **Packetbeat** | Network data | Network packets, protocols |
| **Winlogbeat** | Windows events | Windows event logs |
| **Auditbeat** | Audit data | File integrity, system calls |
| **Heartbeat** | Uptime monitoring | HTTP, TCP, ICMP checks |
| **Functionbeat** | Serverless | AWS Lambda, Azure Functions |

### Filebeat

**Configuration (filebeat.yml):**
```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/nginx/*.log
    fields:
      service: nginx
      environment: production
    multiline:
      pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
      negate: true
      match: after

filebeat.modules:
  - module: nginx
    access:
      enabled: true
      var.paths: ["/var/log/nginx/access.log"]
    error:
      enabled: true
      var.paths: ["/var/log/nginx/error.log"]

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "filebeat-%{+yyyy.MM.dd}"

processors:
  - add_host_metadata: ~
  - add_cloud_metadata: ~
  - add_docker_metadata: ~
```

**Run Filebeat:**
```bash
filebeat -e -c filebeat.yml
```

### Metricbeat

**Configuration (metricbeat.yml):**
```yaml
metricbeat.modules:
  - module: system
    metricsets:
      - cpu
      - memory
      - network
      - filesystem
    enabled: true
    period: 10s
    
  - module: docker
    metricsets:
      - container
      - cpu
      - memory
      - network
    enabled: true
    period: 10s
    hosts: ["unix:///var/run/docker.sock"]
    
  - module: nginx
    metricsets:
      - stubstatus
    enabled: true
    period: 10s
    hosts: ["http://localhost:80"]

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "metricbeat-%{+yyyy.MM.dd}"
```

### Packetbeat

**Configuration (packetbeat.yml):**
```yaml
packetbeat.interfaces.device: any

packetbeat.protocols:
  - type: http
    ports: [80, 8080, 8000, 5000, 8002]
    
  - type: mysql
    ports: [3306]
    
  - type: redis
    ports: [6379]
    
  - type: pgsql
    ports: [5432]

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "packetbeat-%{+yyyy.MM.dd}"
```

### Heartbeat

**Configuration (heartbeat.yml):**
```yaml
heartbeat.monitors:
  - type: http
    id: my-website
    name: "My Website"
    urls: ["https://example.com"]
    schedule: '@every 60s'
    check.response.status: 200
    
  - type: tcp
    id: my-database
    name: "PostgreSQL"
    hosts: ["localhost:5432"]
    schedule: '@every 30s'
    
  - type: icmp
    id: my-server
    name: "Server Ping"
    hosts: ["192.168.1.100"]
    schedule: '@every 10s'

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "heartbeat-%{+yyyy.MM.dd}"
```

## Elastic APM

**Elastic APM** (Application Performance Monitoring) provides distributed tracing and performance monitoring.

### Architecture

```
┌─────────────────────────────────────────────────┐
│   Application (with APM Agent)                  │
│   - Java, Python, Node.js, Go, .NET, Ruby      │
└────────┬────────────────────────────────────────┘
         │ Traces, Metrics, Errors
         ▼
┌─────────────────────────────────────────────────┐
│   APM Server                                    │
│   - Receives data from agents                   │
│   - Validates and processes                     │
└────────┬────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│   Elasticsearch                                 │
│   - Stores traces and metrics                   │
└────────┬────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│   Kibana APM UI                                 │
│   - Visualize traces                            │
│   - Service maps                                │
│   - Error tracking                              │
└─────────────────────────────────────────────────┘
```

### APM Agent Setup (Java)

**1. Add dependency:**
```xml
<dependency>
  <groupId>co.elastic.apm</groupId>
  <artifactId>apm-agent-attach</artifactId>
  <version>1.40.0</version>
</dependency>
```

**2. Configure:**
```java
ElasticApmAttacher.attach(Map.of(
  "service_name", "my-service",
  "server_url", "http://localhost:8200",
  "application_packages", "com.example"
));
```

**3. Or use Java agent:**
```bash
java -javaagent:/path/to/elastic-apm-agent.jar \
  -Delastic.apm.service_name=my-service \
  -Delastic.apm.server_url=http://localhost:8200 \
  -Delastic.apm.application_packages=com.example \
  -jar myapp.jar
```

### Distributed Tracing Example

```
Request: GET /api/orders/123

┌─────────────────────────────────────────────────┐
│ API Gateway (50ms)                              │
│ ├─ Authentication (10ms)                        │
│ └─ Route to service                             │
└────────┬────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│ Order Service (200ms)                           │
│ ├─ Get order from DB (80ms)                     │
│ ├─ Call User Service (60ms)                     │
│ └─ Call Payment Service (40ms)                  │
└────────┬────────────────────────────────────────┘
         │
         ├──────────────────┬──────────────────────┐
         ▼                  ▼                      ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ User Service │  │ Payment Svc  │  │ PostgreSQL   │
│ (60ms)       │  │ (40ms)       │  │ (80ms)       │
└──────────────┘  └──────────────┘  └──────────────┘

Total: 250ms
```

### APM Metrics

**Transaction metrics:**
- Duration (p50, p95, p99)
- Throughput (requests/sec)
- Error rate

**Span metrics:**
- Database query time
- External HTTP calls
- Cache operations

**JVM metrics:**
- Heap usage
- GC time
- Thread count

## Elastic Security

**Elastic Security** provides SIEM (Security Information and Event Management) capabilities.

### Features

1. **SIEM**: Security analytics and threat detection
2. **Endpoint Security**: Malware prevention and detection
3. **Cloud Security**: Cloud workload protection
4. **Detection Rules**: Pre-built and custom rules
5. **Case Management**: Incident response workflow
6. **Timeline**: Investigation workspace

### Detection Rules Example

**Brute force detection:**
```json
{
  "name": "Multiple Failed Login Attempts",
  "description": "Detects multiple failed login attempts from same IP",
  "severity": "high",
  "risk_score": 75,
  "query": {
    "bool": {
      "must": [
        { "term": { "event.action": "login_failed" } },
        { "range": { "@timestamp": { "gte": "now-5m" } } }
      ]
    }
  },
  "threshold": {
    "field": "source.ip",
    "value": 10
  },
  "actions": [
    {
      "type": "alert",
      "severity": "high"
    }
  ]
}
```

## Complete Stack Architecture

### Production Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Data Sources                         │
│  - Applications (APM agents)                            │
│  - Servers (Beats)                                      │
│  - Network devices (Syslog)                             │
│  - Databases (JDBC)                                     │
│  - Cloud services (AWS, Azure, GCP)                     │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│                  Ingestion Layer                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Filebeat │  │Metricbeat│  │Packetbeat│             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       └─────────────┼─────────────┘                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Processing Layer (Optional)                │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Logstash Cluster (3+ nodes)                     │  │
│  │  - Parse and transform                           │  │
│  │  - Enrich data                                   │  │
│  │  - Filter and route                              │  │
│  └──────────────────────────────────────────────────┘  │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│              Storage & Search Layer                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Elasticsearch Cluster                           │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐            │  │
│  │  │Master 1│  │Master 2│  │Master 3│            │  │
│  │  └────────┘  └────────┘  └────────┘            │  │
│  │  ┌────────┐  ┌────────┐  ┌────────┐            │  │
│  │  │ Data 1 │  │ Data 2 │  │ Data 3 │  ...       │  │
│  │  └────────┘  └────────┘  └────────┘            │  │
│  │  ┌────────┐  ┌────────┐                         │  │
│  │  │Coord 1 │  │Coord 2 │                         │  │
│  │  └────────┘  └────────┘                         │  │
│  └──────────────────────────────────────────────────┘  │
└────────┬────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│              Visualization Layer                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Kibana Cluster (2+ nodes)                       │  │
│  │  - Dashboards                                    │  │
│  │  - Alerting                                      │  │
│  │  - APM UI                                        │  │
│  │  - Security UI                                   │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Docker Compose Example

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    ports:
      - "5044:5044"
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    depends_on:
      - elasticsearch

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    volumes:
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml
      - /var/log:/var/log:ro
    depends_on:
      - elasticsearch

  metricbeat:
    image: docker.elastic.co/beats/metricbeat:8.11.0
    volumes:
      - ./metricbeat.yml:/usr/share/metricbeat/metricbeat.yml
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - elasticsearch

  apm-server:
    image: docker.elastic.co/apm/apm-server:8.11.0
    ports:
      - "8200:8200"
    depends_on:
      - elasticsearch

volumes:
  es-data:
```

---

**Previous**: [← Performance](08_Performance.md) | **Next**: [Use Cases →](10_Use_Cases.md)
