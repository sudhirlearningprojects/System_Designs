# Dropbox Clone - Cloud Storage System

A comprehensive cloud storage system implementation similar to Dropbox with high availability, scalability, and real-time synchronization capabilities.

## 📋 Overview

This package implements a distributed cloud storage system with:
- Multi-device file synchronization
- Real-time collaboration
- Conflict resolution
- Data deduplication
- Scalable architecture supporting millions of users

## 🏗️ Package Structure

```
org.sudhir512kj.dropbox/
├── model/           # Domain entities (User, FileEntity, FileChunk, FileShare)
├── service/         # Business logic (FileService, StorageService, SyncService)
├── repository/      # Data access layer
├── controller/      # REST API endpoints
├── dto/            # Data transfer objects
└── config/         # Configuration classes
```

## 📚 Documentation

- [System Design](System_Design.md) - Complete HLD and LLD
- [Architecture Diagrams](Architecture_Diagrams.md) - Visual system architecture
- [API Documentation](API_Documentation.md) - REST API reference
- [Scale Calculations](Scale_Calculations.md) - Performance and cost analysis

## 🚀 Quick Start

1. Configure database and storage settings in `application.yml`
2. Run the Spring Boot application
3. Access APIs at `http://localhost:8080/api/v1/dropbox/`

## 🎯 Key Features

- File upload/download with chunking
- Real-time sync across devices
- File sharing with permissions
- Version control and history
- Conflict resolution
- Data deduplication (30% storage savings)
- WebSocket notifications

## 📊 Scale Targets

- **Users**: 500M total, 100M DAU
- **Storage**: 210PB with deduplication
- **Throughput**: 27.77 GB/s peak
- **QPS**: 138,888 metadata ops/sec
- **Availability**: 99.99% uptime