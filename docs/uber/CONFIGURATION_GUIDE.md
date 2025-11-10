# Uber Clone - Configuration Guide

## ✅ All Required Configurations

### 1. Application Configuration
**File**: `src/main/resources/application-uber.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/uber_db
    username: postgres
    password: password
  data:
    redis:
      host: localhost
      port: 6379
    cassandra:
      keyspace-name: uber_keyspace
      contact-points: localhost
      port: 9042
      local-datacenter: datacenter1
  kafka:
    bootstrap-servers: localhost:9092

elasticsearch:
  host: localhost
  port: 9200
  index:
    rides: uber_rides

server:
  port: 8090
```

---

### 2. Java Configuration Classes

#### RedisConfig ✅
- **Location**: `src/main/java/org/sudhir512kj/uber/config/RedisConfig.java`
- **Purpose**: Redis connection and geospatial operations
- **Beans**: `RedisTemplate<String, String>`

#### KafkaConfig ✅
- **Location**: `src/main/java/org/sudhir512kj/uber/config/KafkaConfig.java`
- **Purpose**: Kafka producer for event streaming
- **Beans**: `KafkaTemplate<String, String>`, `ProducerFactory`

#### CassandraConfig ✅
- **Location**: `src/main/java/org/sudhir512kj/uber/config/CassandraConfig.java`
- **Purpose**: Cassandra connection for location history
- **Beans**: `CqlSession`

#### ElasticsearchConfig ✅
- **Location**: `src/main/java/org/sudhir512kj/uber/config/ElasticsearchConfig.java`
- **Purpose**: Elasticsearch client for search/analytics
- **Beans**: `ElasticsearchClient`

#### WebSocketConfig ✅
- **Location**: `src/main/java/org/sudhir512kj/uber/websocket/WebSocketConfig.java`
- **Purpose**: WebSocket endpoint for real-time location
- **Endpoint**: `/ws/location`

---

### 3. Database Schemas

#### PostgreSQL Schema ✅
- **File**: `src/main/resources/db/uber-schema.sql`
- **Tables**: users, drivers, vehicles, rides
- **Indexes**: Optimized for queries

#### Cassandra Schema ✅
- **File**: `src/main/resources/db/cassandra-schema.cql`
- **Keyspace**: uber_keyspace
- **Table**: location_history (time-series)

---

### 4. Infrastructure Setup

#### Start Services
```bash
# PostgreSQL
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=uber_db \
  -e POSTGRES_PASSWORD=password \
  postgres:14

# Redis
docker run -d --name redis -p 6379:6379 redis:6

# Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  confluentinc/cp-kafka:latest

# Cassandra
docker run -d --name cassandra -p 9042:9042 cassandra:4

# Elasticsearch
docker run -d --name elasticsearch -p 9200:9200 \
  -e "discovery.type=single-node" \
  elasticsearch:8.11.0
```

#### Initialize Databases
```bash
# PostgreSQL
psql -h localhost -U postgres -d uber_db -f src/main/resources/db/uber-schema.sql

# Cassandra
cqlsh -f src/main/resources/db/cassandra-schema.cql
```

---

### 5. Configuration Summary

| Component | Config File | Java Config | Status |
|-----------|-------------|-------------|--------|
| PostgreSQL | application-uber.yml | Spring Boot Auto | ✅ |
| Redis | application-uber.yml | RedisConfig.java | ✅ |
| Kafka | application-uber.yml | KafkaConfig.java | ✅ |
| Cassandra | application-uber.yml | CassandraConfig.java | ✅ |
| Elasticsearch | application-uber.yml | ElasticsearchConfig.java | ✅ |
| WebSocket | - | WebSocketConfig.java | ✅ |

**Total: 6/6 configurations complete (100%)**

---

### 6. Environment Variables (Optional)

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=uber_db
export DB_USER=postgres
export DB_PASSWORD=password

export REDIS_HOST=localhost
export REDIS_PORT=6379

export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

export CASSANDRA_HOST=localhost
export CASSANDRA_PORT=9042
export CASSANDRA_KEYSPACE=uber_keyspace

export ELASTICSEARCH_HOST=localhost
export ELASTICSEARCH_PORT=9200
```

---

### 7. Run Application

```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.profiles=uber

# Using script
./run-systems.sh uber

# Direct JAR
java -jar target/system-designs.jar --spring.profiles.active=uber
```

---

## ✅ All Configurations Available

- ✅ Application properties (YAML)
- ✅ Redis configuration
- ✅ Kafka configuration
- ✅ Cassandra configuration
- ✅ Elasticsearch configuration
- ✅ WebSocket configuration
- ✅ Database schemas (PostgreSQL + Cassandra)
- ✅ No Lombok dependencies

**The system is fully configured and ready to run!** 🚀
