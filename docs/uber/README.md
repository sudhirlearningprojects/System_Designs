# Uber Clone - Global Ride-Hailing System

## Overview
A production-ready, scalable ride-hailing platform supporting 10M concurrent users, 100K location updates/sec, and <1s ride matching latency.

## Key Features
- **Real-time Matching**: Sub-second driver matching using Redis geospatial indexing
- **Geo-sharding**: Efficient location queries with geohash-based sharding
- **WebSocket Streaming**: Real-time location updates with persistent connections
- **Kafka Event Streaming**: 1M events/sec for analytics and audit logs
- **Dynamic Pricing**: Surge pricing based on demand/supply
- **High Availability**: 99.99% uptime with multi-region deployment
- **Scalability**: Horizontal scaling for all services

## Architecture Highlights

### Geo-Location Service
- **Redis GEOADD/GEORADIUS** for O(log N) location queries
- **Geohash sharding** reduces search space from 100K to ~50 drivers
- **WebSocket** for real-time location streaming (4-second intervals)

### Matching Engine
- **Multi-factor scoring**: Distance (60%) + Rating (30%) + Experience (10%)
- **Fallback mechanism**: Expands radius if no drivers found
- **Timeout handling**: 30-second driver response timeout

### Database Strategy
- **PostgreSQL**: Transactional data (rides, payments)
- **Redis**: Hot location data, driver availability
- **Cassandra**: Location history time-series data

## Quick Start

```bash
# Start infrastructure
docker-compose up -d postgres redis

# Run application
./run-systems.sh uber  # Port 8090
```

## Scale Metrics
- **Users**: 100M riders, 5M drivers
- **Throughput**: 75K location updates/sec, 520 ride requests/sec
- **Storage**: 100TB (7-year retention)
- **Latency**: <100ms location updates, <1s matching

## Documentation
- [System Design](System_Design.md) - Complete HLD/LLD
- [API Documentation](API_Documentation.md) - REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance analysis

## Technology Stack
- Spring Boot 3.2, Java 17
- PostgreSQL, Redis, Cassandra
- WebSocket, Kafka
- Docker, Kubernetes
