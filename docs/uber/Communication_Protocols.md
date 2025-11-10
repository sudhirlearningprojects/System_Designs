# Uber Clone - Communication Protocols (Production-Grade)

## Overview

Uber uses **three different communication protocols** optimized for specific use cases:

| Protocol | Use Case | Latency | Throughput | When to Use |
|----------|----------|---------|------------|-------------|
| **gRPC** | Internal service-to-service | 1-5ms | 100K RPS | Low-latency, type-safe internal APIs |
| **WebSocket** | Client real-time updates | 50-100ms | 100K connections/server | Bidirectional streaming to clients |
| **Kafka** | Event streaming | 5-10ms | 1M events/sec | Async processing, analytics, audit logs |

---

## 1. gRPC (Internal Microservices)

### Why gRPC?

**Advantages**:
- **Low Latency**: 1-5ms (vs 10-50ms for REST)
- **Binary Protocol**: Protocol Buffers are 3-10x smaller than JSON
- **Type Safety**: Strongly-typed contracts prevent runtime errors
- **Bidirectional Streaming**: Real-time data flow between services
- **HTTP/2**: Multiplexing, header compression, server push

**Use Cases**:
- Matching Engine ↔ Geo-Location Service
- Ride Service ↔ Payment Service
- Pricing Service ↔ Surge Calculator

### Implementation

**Proto Definition** (`location.proto`):
```protobuf
syntax = "proto3";

service LocationService {
  rpc UpdateLocation(LocationUpdateRequest) returns (LocationUpdateResponse);
  rpc FindNearbyDrivers(NearbyDriversRequest) returns (NearbyDriversResponse);
  rpc StreamLocationUpdates(stream LocationUpdateRequest) returns (stream LocationUpdateResponse);
}

message LocationUpdateRequest {
  string driver_id = 1;
  double latitude = 2;
  double longitude = 3;
  int64 timestamp = 4;
}
```

**Performance**:
- Request size: 50 bytes (vs 200 bytes JSON)
- Latency: 2ms p99 (vs 15ms REST)
- Throughput: 100K RPS per instance

---

## 2. WebSocket (Client Communication)

### Why WebSocket?

**Advantages**:
- **Persistent Connection**: No HTTP handshake overhead
- **Bidirectional**: Server can push updates to client
- **Low Latency**: 50-100ms for real-time updates
- **Efficient**: Single TCP connection for all messages

**Use Cases**:
- Driver app → Location updates (every 4 seconds)
- Rider app ← Driver location streaming
- Ride status notifications

### Implementation

**Connection**:
```
ws://api.uber.com/ws/location?driverId={uuid}
```

**Message Format** (JSON):
```json
{
  "type": "LOCATION_UPDATE",
  "driverId": "uuid",
  "latitude": 37.7749,
  "longitude": -122.4194,
  "timestamp": 1699999999
}
```

**Performance**:
- Connections per server: 100K
- Update frequency: 4 seconds (drivers), 2 seconds (riders)
- Bandwidth: 200 bytes/update × 25K updates/sec = 5MB/sec

---

## 3. Apache Kafka (Event Streaming)

### Why Kafka?

**Advantages**:
- **High Throughput**: 1M events/sec per broker
- **Durability**: Persistent storage with replication
- **Scalability**: Horizontal scaling with partitions
- **Decoupling**: Producers and consumers are independent

**Use Cases**:
- Location history archival
- Ride lifecycle events
- Analytics and ML pipelines
- Audit logs

### Topics

```
uber.location.updates        # 100 partitions, 7-day retention
uber.ride.events             # 50 partitions, 90-day retention
uber.payment.transactions    # 20 partitions, 7-year retention
uber.surge.pricing           # 10 partitions, 1-day retention
```

**Event Example**:
```json
{
  "eventType": "LOCATION_UPDATE",
  "driverId": "uuid",
  "latitude": 37.7749,
  "longitude": -122.4194,
  "timestamp": 1699999999,
  "speed": 45.5,
  "heading": 180
}
```

**Performance**:
- Throughput: 75K events/sec
- Latency: 5-10ms p99
- Storage: 1.3TB/day (compressed)

---

## Communication Flow

### Ride Request Flow

```
1. Rider App → REST API → Ride Service
   Protocol: HTTPS (HTTP/2)
   Latency: 50ms

2. Ride Service → gRPC → Matching Engine
   Protocol: gRPC
   Latency: 2ms

3. Matching Engine → gRPC → Geo-Location Service
   Protocol: gRPC
   Latency: 3ms

4. Geo-Location Service → Redis GEORADIUS
   Protocol: Redis Protocol
   Latency: 1ms

5. Matching Engine → gRPC → Notification Service
   Protocol: gRPC
   Latency: 2ms

6. Notification Service → WebSocket → Driver App
   Protocol: WebSocket
   Latency: 50ms

Total: ~110ms (p99)
```

### Location Update Flow

```
1. Driver App → WebSocket → Location Service
   Protocol: WebSocket
   Latency: 50ms

2. Location Service → Redis GEOADD
   Protocol: Redis Protocol
   Latency: 1ms

3. Location Service → Kafka → location.updates topic
   Protocol: Kafka Protocol
   Latency: 5ms

4. Kafka Consumer → Cassandra (archival)
   Protocol: CQL
   Latency: 10ms (async)

5. Location Service → WebSocket → Rider App (if on active ride)
   Protocol: WebSocket
   Latency: 50ms

Total: ~55ms (p99)
```

---

## Protocol Comparison

### REST vs gRPC

| Metric | REST (JSON) | gRPC (Protobuf) | Improvement |
|--------|-------------|-----------------|-------------|
| Payload Size | 200 bytes | 50 bytes | 4x smaller |
| Latency (p99) | 15ms | 2ms | 7.5x faster |
| Throughput | 20K RPS | 100K RPS | 5x higher |
| Type Safety | No | Yes | ✓ |
| Streaming | No | Yes | ✓ |

### HTTP Polling vs WebSocket

| Metric | HTTP Polling | WebSocket | Improvement |
|--------|--------------|-----------|-------------|
| Connections | 1 per request | 1 persistent | 100x fewer |
| Overhead | 500 bytes/req | 2 bytes/msg | 250x less |
| Latency | 100-500ms | 50ms | 2-10x faster |
| Server Load | High | Low | 10x reduction |

---

## Production Configuration

### gRPC Server

```yaml
grpc:
  server:
    port: 9090
    max-inbound-message-size: 4MB
    keep-alive-time: 30s
    keep-alive-timeout: 5s
    permit-keep-alive-without-calls: true
```

### WebSocket Server

```yaml
websocket:
  max-connections: 100000
  idle-timeout: 300s
  message-size-limit: 64KB
  heartbeat-interval: 30s
```

### Kafka Producer

```yaml
kafka:
  bootstrap-servers: kafka1:9092,kafka2:9092,kafka3:9092
  producer:
    acks: 1
    compression-type: lz4
    batch-size: 16384
    linger-ms: 10
    buffer-memory: 33554432
```

---

## Interview Talking Points

### Why This Architecture?

1. **gRPC for Internal Services**:
   - "We use gRPC between microservices because it provides 5-10x lower latency than REST due to binary serialization and HTTP/2 multiplexing"
   - "Type safety with Protocol Buffers prevents runtime errors and enables automatic code generation"

2. **WebSocket for Clients**:
   - "WebSocket maintains persistent connections, eliminating HTTP handshake overhead for real-time location updates"
   - "Bidirectional streaming allows server to push driver location to riders without polling"

3. **Kafka for Events**:
   - "Kafka decouples services and provides durable event storage for analytics and audit logs"
   - "Partitioning enables horizontal scaling to handle 75K location updates per second"

### Trade-offs

| Protocol | Pros | Cons |
|----------|------|------|
| **gRPC** | Low latency, type-safe, streaming | Complex setup, not browser-native |
| **WebSocket** | Real-time, bidirectional, efficient | Stateful, harder to scale |
| **Kafka** | High throughput, durable, scalable | Higher latency, eventual consistency |

---

## Monitoring

### Key Metrics

**gRPC**:
- Request latency (p50, p95, p99)
- Error rate by method
- Active connections

**WebSocket**:
- Active connections
- Message throughput
- Connection duration

**Kafka**:
- Producer throughput
- Consumer lag
- Partition distribution

---

**Conclusion**: This multi-protocol architecture optimizes for different use cases - gRPC for low-latency internal calls, WebSocket for real-time client updates, and Kafka for high-throughput event streaming.
