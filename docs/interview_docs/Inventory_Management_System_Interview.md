# Inventory Management System - System Design Interview Guide

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Requirements Gathering](#requirements-gathering)
3. [Capacity Planning](#capacity-planning)
4. [High-Level Design](#high-level-design)
5. [Low-Level Design](#low-level-design)
6. [Database Schema](#database-schema)
7. [API Design](#api-design)
8. [Working Code Implementation](#working-code-implementation)
9. [Concurrency Handling](#concurrency-handling)
10. [Scalability & Performance](#scalability--performance)
11. [Interview Discussion Points](#interview-discussion-points)

---

## Problem Statement

Design a scalable Inventory Management System for an e-commerce platform that handles:
- Product inventory tracking across multiple warehouses
- Real-time stock updates
- Order fulfillment and reservation
- Low stock alerts
- Inventory auditing and reporting
- High concurrency (thousands of orders per second)
- Zero overselling guarantee

**Interview Duration**: 45-60 minutes

---

## Requirements Gathering

### Functional Requirements

1. **Inventory Management**
   - Add/update/delete products
   - Track stock levels across multiple warehouses
   - Support multiple SKUs (Stock Keeping Units)
   - Product variants (size, color, etc.)

2. **Stock Operations**
   - Reserve stock for orders (hold inventory)
   - Release reserved stock (order cancellation)
   - Commit reserved stock (order completion)
   - Adjust stock (manual corrections, damages, returns)

3. **Order Fulfillment**
   - Check product availability
   - Allocate inventory from optimal warehouse
   - Support partial fulfillment
   - Handle backorders

4. **Alerts & Notifications**
   - Low stock alerts
   - Out of stock notifications
   - Reorder point triggers

5. **Reporting**
   - Current inventory levels
   - Inventory movement history
   - Stock valuation
   - Warehouse performance

### Non-Functional Requirements

1. **Consistency**: Strong consistency for inventory counts (no overselling)
2. **Availability**: 99.99% uptime
3. **Performance**: 
   - Stock check: <50ms
   - Stock reservation: <100ms
   - 10,000 concurrent requests
4. **Scalability**: Support 1M+ SKUs, 100+ warehouses
5. **Concurrency**: Handle race conditions (multiple orders for same item)
6. **Audit Trail**: Complete history of all inventory changes

---

## Capacity Planning

### Scale Estimates

**Assumptions:**
- 1 million products (SKUs)
- 100 warehouses
- 10,000 orders per second (peak)
- 50,000 stock checks per second
- Average order: 3 items
- Data retention: 5 years

### Storage Calculations

```
Products: 1M products × 1KB = 1GB
Inventory Records: 1M products × 100 warehouses × 500 bytes = 50GB
Orders: 10K orders/sec × 86,400 sec/day × 365 days × 5 years = 1.5 trillion orders
Order Items: 1.5T orders × 3 items × 200 bytes = 900TB
Inventory Transactions: Similar to order items = ~1PB (with compression)

Total Storage: ~1.5PB (with replication and indexes)
```

### Traffic Estimates

```
Read Operations (Stock Checks):
- 50,000 requests/sec
- Peak: 100,000 requests/sec

Write Operations (Stock Updates):
- 10,000 orders/sec × 3 items = 30,000 updates/sec
- Peak: 60,000 updates/sec

Database QPS:
- Reads: 100K QPS
- Writes: 60K QPS
- Total: 160K QPS (peak)
```

### Bandwidth

```
Average Request Size: 1KB
Average Response Size: 2KB

Incoming: 160K requests/sec × 1KB = 160 MB/sec = 1.28 Gbps
Outgoing: 160K responses/sec × 2KB = 320 MB/sec = 2.56 Gbps

Total Bandwidth: ~4 Gbps (peak)
```

---

## High-Level Design

### Architecture Diagram

```
┌─────────────┐
│   Clients   │
│ (Web/Mobile)│
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│         API Gateway / Load Balancer      │
│         (Rate Limiting, Auth)            │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
┌─────────────┐  ┌─────────────┐
│  Inventory  │  │   Order     │
│  Service    │  │  Service    │
└──────┬──────┘  └──────┬──────┘
       │                │
       └────────┬───────┘
                ▼
    ┌──────────────────────┐
    │   Message Queue      │
    │   (Kafka/RabbitMQ)   │
    └──────────┬───────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
┌─────────────┐  ┌─────────────┐
│ Notification│  │  Analytics  │
│  Service    │  │  Service    │
└─────────────┘  └─────────────┘

┌──────────────────────────────────────────┐
│           Data Layer                      │
├──────────────┬───────────────────────────┤
│  PostgreSQL  │  Redis Cache  │  S3       │
│  (Primary)   │  (Hot Data)   │ (Reports) │
└──────────────┴───────────────────────────┘
```

### Component Breakdown

1. **API Gateway**
   - Authentication & Authorization
   - Rate limiting
   - Request routing
   - API versioning

2. **Inventory Service**
   - Core inventory operations
   - Stock reservation/release
   - Warehouse management
   - Inventory adjustments

3. **Order Service**
   - Order placement
   - Order fulfillment
   - Integration with inventory

4. **Notification Service**
   - Low stock alerts
   - Email/SMS notifications
   - Webhook callbacks

5. **Analytics Service**
   - Real-time dashboards
   - Inventory reports
   - Predictive analytics

6. **Data Stores**
   - PostgreSQL: Primary data store
   - Redis: Caching + distributed locks
   - Kafka: Event streaming
   - Elasticsearch: Search & analytics

---

## Low-Level Design

### Core Classes and Interfaces

```java
// Domain Models
public class Product {
    private String productId;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public class Warehouse {
    private String warehouseId;
    private String name;
    private String location;
    private String address;
    private WarehouseStatus status;
    private int priority; // For allocation strategy
}

public class Inventory {
    private String inventoryId;
    private String productId;
    private String warehouseId;
    private int availableQuantity;
    private int reservedQuantity;
    private int totalQuantity; // available + reserved
    private int reorderPoint;
    private int reorderQuantity;
    private LocalDateTime lastUpdated;
}

public class InventoryTransaction {
    private String transactionId;
    private String inventoryId;
    private TransactionType type; // RESERVE, RELEASE, COMMIT, ADJUST
    private int quantity;
    private String orderId;
    private String reason;
    private LocalDateTime timestamp;
}

public class StockReservation {
    private String reservationId;
    private String orderId;
    private String productId;
    private String warehouseId;
    private int quantity;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}

// Enums
public enum TransactionType {
    RESERVE, RELEASE, COMMIT, ADJUST_IN, ADJUST_OUT, RETURN, DAMAGE
}

public enum ReservationStatus {
    ACTIVE, COMMITTED, RELEASED, EXPIRED
}

public enum ProductStatus {
    ACTIVE, INACTIVE, DISCONTINUED
}

public enum WarehouseStatus {
    ACTIVE, INACTIVE, MAINTENANCE
}
```

### Service Interfaces

```java
public interface InventoryService {
    // Stock Operations
    boolean checkAvailability(String productId, int quantity);
    Map<String, Integer> checkAvailabilityMultiple(Map<String, Integer> items);
    
    // Reservation Operations
    StockReservation reserveStock(String productId, int quantity, String orderId);
    void releaseReservation(String reservationId);
    void commitReservation(String reservationId);
    
    // Inventory Management
    void addStock(String productId, String warehouseId, int quantity);
    void removeStock(String productId, String warehouseId, int quantity);
    void adjustStock(String productId, String warehouseId, int quantity, String reason);
    
    // Queries
    Inventory getInventory(String productId, String warehouseId);
    List<Inventory> getInventoryByProduct(String productId);
    List<Product> getLowStockProducts();
}

public interface WarehouseService {
    Warehouse createWarehouse(WarehouseRequest request);
    Warehouse getWarehouse(String warehouseId);
    List<Warehouse> getAllWarehouses();
    void updateWarehouse(String warehouseId, WarehouseRequest request);
}

public interface AllocationStrategy {
    String selectWarehouse(String productId, int quantity, List<Warehouse> warehouses);
}
```

---

## Database Schema

### PostgreSQL Schema

```sql
-- Products Table
CREATE TABLE products (
    product_id VARCHAR(50) PRIMARY KEY,
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sku (sku),
    INDEX idx_category (category),
    INDEX idx_status (status)
);

-- Warehouses Table
CREATE TABLE warehouses (
    warehouse_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(100) NOT NULL,
    address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_priority (priority)
);

-- Inventory Table (Core table with high concurrency)
CREATE TABLE inventory (
    inventory_id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    total_quantity INT GENERATED ALWAYS AS (available_quantity + reserved_quantity) STORED,
    reorder_point INT NOT NULL DEFAULT 10,
    reorder_quantity INT NOT NULL DEFAULT 100,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0, -- Optimistic locking
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    UNIQUE KEY uk_product_warehouse (product_id, warehouse_id),
    INDEX idx_product (product_id),
    INDEX idx_warehouse (warehouse_id),
    INDEX idx_available_qty (available_quantity),
    CHECK (available_quantity >= 0),
    CHECK (reserved_quantity >= 0)
);

-- Stock Reservations Table
CREATE TABLE stock_reservations (
    reservation_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    INDEX idx_order (order_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    INDEX idx_product_warehouse (product_id, warehouse_id)
);

-- Inventory Transactions Table (Audit log)
CREATE TABLE inventory_transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    inventory_id VARCHAR(50) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    order_id VARCHAR(50),
    reservation_id VARCHAR(50),
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id),
    INDEX idx_inventory (inventory_id),
    INDEX idx_order (order_id),
    INDEX idx_type (transaction_type),
    INDEX idx_created_at (created_at)
);

-- Low Stock Alerts Table
CREATE TABLE low_stock_alerts (
    alert_id VARCHAR(50) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    current_quantity INT NOT NULL,
    reorder_point INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(warehouse_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);
```

### Redis Data Structures

```
# Hot inventory cache (frequently accessed products)
Key: inventory:{productId}:{warehouseId}
Value: JSON {availableQuantity, reservedQuantity, lastUpdated}
TTL: 300 seconds (5 minutes)

# Distributed locks for concurrency control
Key: lock:inventory:{productId}:{warehouseId}
Value: {lockId}
TTL: 10 seconds

# Low stock products set
Key: low_stock_products
Type: Sorted Set
Score: available_quantity
Member: productId:warehouseId

# Reservation expiry tracking
Key: reservation:expiry:{timestamp}
Type: Set
Members: reservationIds
```

---

## API Design

### REST API Endpoints

```java
// Product APIs
POST   /api/v1/products                    // Create product
GET    /api/v1/products/{productId}        // Get product
PUT    /api/v1/products/{productId}        // Update product
DELETE /api/v1/products/{productId}        // Delete product
GET    /api/v1/products                    // List products (paginated)

// Warehouse APIs
POST   /api/v1/warehouses                  // Create warehouse
GET    /api/v1/warehouses/{warehouseId}    // Get warehouse
PUT    /api/v1/warehouses/{warehouseId}    // Update warehouse
GET    /api/v1/warehouses                  // List warehouses

// Inventory APIs
GET    /api/v1/inventory/check             // Check availability
POST   /api/v1/inventory/reserve           // Reserve stock
POST   /api/v1/inventory/release           // Release reservation
POST   /api/v1/inventory/commit            // Commit reservation
POST   /api/v1/inventory/adjust            // Adjust stock
GET    /api/v1/inventory/product/{productId}  // Get inventory by product
GET    /api/v1/inventory/warehouse/{warehouseId}  // Get inventory by warehouse
GET    /api/v1/inventory/low-stock         // Get low stock products

// Reporting APIs
GET    /api/v1/reports/inventory-summary   // Inventory summary
GET    /api/v1/reports/transactions        // Transaction history
GET    /api/v1/reports/warehouse-performance  // Warehouse metrics
```

### Request/Response Examples

```json
// Check Availability Request
POST /api/v1/inventory/check
{
  "items": [
    {"productId": "PROD-001", "quantity": 5},
    {"productId": "PROD-002", "quantity": 10}
  ]
}

// Check Availability Response
{
  "available": true,
  "items": [
    {
      "productId": "PROD-001",
      "requestedQuantity": 5,
      "availableQuantity": 100,
      "available": true,
      "warehouses": [
        {"warehouseId": "WH-001", "quantity": 60},
        {"warehouseId": "WH-002", "quantity": 40}
      ]
    },
    {
      "productId": "PROD-002",
      "requestedQuantity": 10,
      "availableQuantity": 50,
      "available": true,
      "warehouses": [
        {"warehouseId": "WH-001", "quantity": 50}
      ]
    }
  ]
}

// Reserve Stock Request
POST /api/v1/inventory/reserve
{
  "orderId": "ORD-12345",
  "items": [
    {"productId": "PROD-001", "quantity": 5},
    {"productId": "PROD-002", "quantity": 10}
  ],
  "expiryMinutes": 15
}

// Reserve Stock Response
{
  "success": true,
  "reservations": [
    {
      "reservationId": "RES-001",
      "productId": "PROD-001",
      "warehouseId": "WH-001",
      "quantity": 5,
      "expiresAt": "2024-01-15T10:30:00Z"
    },
    {
      "reservationId": "RES-002",
      "productId": "PROD-002",
      "warehouseId": "WH-001",
      "quantity": 10,
      "expiresAt": "2024-01-15T10:30:00Z"
    }
  ]
}
```

---

This completes Part 1. Part 2 will contain the complete working code implementation.

## Working Code Implementation

### Complete Live Interview Code

This is production-ready code that can be written in a live interview coding editor.

---

### 1. Domain Models

```java
package com.inventory.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String productId;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    private String warehouseId;
    private String name;
    private String location;
    private String address;
    private WarehouseStatus status;
    private int priority;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private String inventoryId;
    private String productId;
    private String warehouseId;
    private int availableQuantity;
    private int reservedQuantity;
    private int totalQuantity;
    private int reorderPoint;
    private int reorderQuantity;
    private LocalDateTime lastUpdated;
    private int version; // For optimistic locking
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
    private String reservationId;
    private String orderId;
    private String productId;
    private String warehouseId;
    private int quantity;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    private String transactionId;
    private String inventoryId;
    private String productId;
    private String warehouseId;
    private TransactionType type;
    private int quantity;
    private String orderId;
    private String reservationId;
    private String reason;
    private LocalDateTime timestamp;
}

// Enums
public enum ProductStatus {
    ACTIVE, INACTIVE, DISCONTINUED
}

public enum WarehouseStatus {
    ACTIVE, INACTIVE, MAINTENANCE
}

public enum ReservationStatus {
    ACTIVE, COMMITTED, RELEASED, EXPIRED
}

public enum TransactionType {
    RESERVE, RELEASE, COMMIT, ADJUST_IN, ADJUST_OUT, RETURN, DAMAGE
}
```

---

### 2. DTOs (Data Transfer Objects)

```java
package com.inventory.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCheckRequest {
    private List<ItemRequest> items;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {
    private String productId;
    private int quantity;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCheckResponse {
    private boolean available;
    private List<ItemAvailability> items;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemAvailability {
    private String productId;
    private int requestedQuantity;
    private int availableQuantity;
    private boolean available;
    private List<WarehouseStock> warehouses;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStock {
    private String warehouseId;
    private String warehouseName;
    private int quantity;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {
    private String orderId;
    private List<ItemRequest> items;
    private int expiryMinutes; // Default: 15 minutes
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockResponse {
    private boolean success;
    private String message;
    private List<StockReservation> reservations;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockRequest {
    private String productId;
    private String warehouseId;
    private int quantity; // Positive for add, negative for remove
    private String reason;
}
```

---

### 3. Repository Layer (JPA)

```java
package com.inventory.repository;

import com.inventory.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findBySku(String sku);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByCategory(String category);
}

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByStatus(WarehouseStatus status);
    List<Warehouse> findByStatusOrderByPriorityDesc(WarehouseStatus status);
}

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.warehouseId = :warehouseId")
    Optional<Inventory> findByProductAndWarehouseWithLock(
        @Param("productId") String productId,
        @Param("warehouseId") String warehouseId
    );
    
    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);
    
    List<Inventory> findByProductId(String productId);
    
    List<Inventory> findByWarehouseId(String warehouseId);
    
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= i.reorderPoint")
    List<Inventory> findLowStockInventory();
    
    @Query("SELECT SUM(i.availableQuantity) FROM Inventory i WHERE i.productId = :productId")
    Integer getTotalAvailableQuantity(@Param("productId") String productId);
}

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, String> {
    
    List<StockReservation> findByOrderId(String orderId);
    
    List<StockReservation> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM StockReservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);
    
    List<StockReservation> findByProductIdAndWarehouseIdAndStatus(
        String productId, String warehouseId, ReservationStatus status
    );
}

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
    
    List<InventoryTransaction> findByInventoryId(String inventoryId);
    
    List<InventoryTransaction> findByOrderId(String orderId);
    
    List<InventoryTransaction> findByProductId(String productId);
    
    @Query("SELECT t FROM InventoryTransaction t WHERE t.timestamp BETWEEN :start AND :end")
    List<InventoryTransaction> findByTimestampBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
```

---

### 4. Core Service Implementation

```java
package com.inventory.service;

import com.inventory.dto.*;
import com.inventory.exception.*;
import com.inventory.model.*;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final int DEFAULT_EXPIRY_MINUTES = 15;
    private static final String CACHE_PREFIX = "inventory:";
    private static final String LOCK_PREFIX = "lock:inventory:";
    
    /**
     * Check availability of multiple products
     */
    public AvailabilityCheckResponse checkAvailability(AvailabilityCheckRequest request) {
        log.info("Checking availability for {} items", request.getItems().size());
        
        List<ItemAvailability> itemAvailabilities = new ArrayList<>();
        boolean allAvailable = true;
        
        for (ItemRequest item : request.getItems()) {
            ItemAvailability availability = checkSingleProductAvailability(
                item.getProductId(), 
                item.getQuantity()
            );
            itemAvailabilities.add(availability);
            
            if (!availability.isAvailable()) {
                allAvailable = false;
            }
        }
        
        return AvailabilityCheckResponse.builder()
            .available(allAvailable)
            .items(itemAvailabilities)
            .build();
    }
    
    /**
     * Check availability for a single product
     */
    private ItemAvailability checkSingleProductAvailability(String productId, int quantity) {
        // Try cache first
        String cacheKey = CACHE_PREFIX + productId;
        Integer cachedQuantity = (Integer) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedQuantity != null && cachedQuantity >= quantity) {
            log.debug("Cache hit for product: {}", productId);
            return ItemAvailability.builder()
                .productId(productId)
                .requestedQuantity(quantity)
                .availableQuantity(cachedQuantity)
                .available(true)
                .build();
        }
        
        // Cache miss - query database
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        
        int totalAvailable = inventories.stream()
            .mapToInt(Inventory::getAvailableQuantity)
            .sum();
        
        // Update cache
        redisTemplate.opsForValue().set(cacheKey, totalAvailable, 5, TimeUnit.MINUTES);
        
        List<WarehouseStock> warehouseStocks = inventories.stream()
            .filter(inv -> inv.getAvailableQuantity() > 0)
            .map(inv -> WarehouseStock.builder()
                .warehouseId(inv.getWarehouseId())
                .quantity(inv.getAvailableQuantity())
                .build())
            .collect(Collectors.toList());
        
        return ItemAvailability.builder()
            .productId(productId)
            .requestedQuantity(quantity)
            .availableQuantity(totalAvailable)
            .available(totalAvailable >= quantity)
            .warehouses(warehouseStocks)
            .build();
    }
    
    /**
     * Reserve stock for an order (with distributed locking)
     */
    @Transactional
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        log.info("Reserving stock for order: {}", request.getOrderId());
        
        List<StockReservation> reservations = new ArrayList<>();
        List<String> acquiredLocks = new ArrayList<>();
        
        try {
            // Check availability first
            AvailabilityCheckResponse availability = checkAvailability(
                AvailabilityCheckRequest.builder()
                    .items(request.getItems())
                    .build()
            );
            
            if (!availability.isAvailable()) {
                return ReserveStockResponse.builder()
                    .success(false)
                    .message("Insufficient stock for one or more items")
                    .build();
            }
            
            // Reserve each item
            for (ItemRequest item : request.getItems()) {
                StockReservation reservation = reserveSingleProduct(
                    item.getProductId(),
                    item.getQuantity(),
                    request.getOrderId(),
                    request.getExpiryMinutes() > 0 ? request.getExpiryMinutes() : DEFAULT_EXPIRY_MINUTES,
                    acquiredLocks
                );
                reservations.add(reservation);
            }
            
            return ReserveStockResponse.builder()
                .success(true)
                .message("Stock reserved successfully")
                .reservations(reservations)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", request.getOrderId(), e);
            // Rollback reservations
            rollbackReservations(reservations);
            throw new StockReservationException("Failed to reserve stock: " + e.getMessage());
        } finally {
            // Release all locks
            releaseLocks(acquiredLocks);
        }
    }
    
    /**
     * Reserve stock for a single product
     */
    private StockReservation reserveSingleProduct(
            String productId, 
            int quantity, 
            String orderId,
            int expiryMinutes,
            List<String> acquiredLocks) {
        
        // Get active warehouses sorted by priority
        List<Warehouse> warehouses = warehouseRepository
            .findByStatusOrderByPriorityDesc(WarehouseStatus.ACTIVE);
        
        // Try to allocate from warehouses
        for (Warehouse warehouse : warehouses) {
            String lockKey = LOCK_PREFIX + productId + ":" + warehouse.getWarehouseId();
            
            // Acquire distributed lock
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                acquiredLocks.add(lockKey);
                
                try {
                    // Get inventory with pessimistic lock
                    Optional<Inventory> inventoryOpt = inventoryRepository
                        .findByProductAndWarehouseWithLock(productId, warehouse.getWarehouseId());
                    
                    if (inventoryOpt.isPresent()) {
                        Inventory inventory = inventoryOpt.get();
                        
                        if (inventory.getAvailableQuantity() >= quantity) {
                            // Update inventory
                            inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
                            inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                            inventory.setLastUpdated(LocalDateTime.now());
                            inventoryRepository.save(inventory);
                            
                            // Create reservation
                            StockReservation reservation = StockReservation.builder()
                                .reservationId(UUID.randomUUID().toString())
                                .orderId(orderId)
                                .productId(productId)
                                .warehouseId(warehouse.getWarehouseId())
                                .quantity(quantity)
                                .status(ReservationStatus.ACTIVE)
                                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                            
                            reservationRepository.save(reservation);
                            
                            // Log transaction
                            logTransaction(inventory, TransactionType.RESERVE, quantity, orderId, reservation.getReservationId());
                            
                            // Invalidate cache
                            invalidateCache(productId);
                            
                            log.info("Reserved {} units of product {} from warehouse {}", 
                                quantity, productId, warehouse.getWarehouseId());
                            
                            return reservation;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reserving from warehouse: {}", warehouse.getWarehouseId(), e);
                }
            }
        }
        
        throw new InsufficientStockException(
            "Unable to reserve " + quantity + " units of product " + productId
        );
    }
    
    /**
     * Release reservation (order cancelled)
     */
    @Transactional
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: {}", reservationId);
        
        StockReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + reservationId));
        
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Reservation is not active: " + reservationId);
        }
        
        String lockKey = LOCK_PREFIX + reservation.getProductId() + ":" + reservation.getWarehouseId();
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new ConcurrencyException("Unable to acquire lock for releasing reservation");
        }
        
        try {
            // Get inventory with lock
            Inventory inventory = inventoryRepository
                .findByProductAndWarehouseWithLock(reservation.getProductId(), reservation.getWarehouseId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
            
            // Update inventory
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + reservation.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);
            
            // Update reservation
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setUpdatedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
            
            // Log transaction
            logTransaction(inventory, TransactionType.RELEASE, reservation.getQuantity(), 
                reservation.getOrderId(), reservationId);
            
            // Invalidate cache
            invalidateCache(reservation.getProductId());
            
            log.info("Released reservation: {}", reservationId);
            
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * Commit reservation (order completed)
     */
    @Transactional
    public void commitReservation(String reservationId) {
        log.info("Committing reservation: {}", reservationId);
        
        StockReservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException("Reservation not found: " + reservationId));
        
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException("Reservation is not active: " + reservationId);
        }
        
        String lockKey = LOCK_PREFIX + reservation.getProductId() + ":" + reservation.getWarehouseId();
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new ConcurrencyException("Unable to acquire lock for committing reservation");
        }
        
        try {
            // Get inventory with lock
            Inventory inventory = inventoryRepository
                .findByProductAndWarehouseWithLock(reservation.getProductId(), reservation.getWarehouseId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
            
            // Update inventory (only reduce reserved quantity)
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reservation.getQuantity());
            inventory.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);
            
            // Update reservation
            reservation.setStatus(ReservationStatus.COMMITTED);
            reservation.setUpdatedAt(LocalDateTime.now());
            reservationRepository.save(reservation);
            
            // Log transaction
            logTransaction(inventory, TransactionType.COMMIT, reservation.getQuantity(), 
                reservation.getOrderId(), reservationId);
            
            // Invalidate cache
            invalidateCache(reservation.getProductId());
            
            log.info("Committed reservation: {}", reservationId);
            
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * Adjust stock (manual correction, returns, damages)
     */
    @Transactional
    public void adjustStock(AdjustStockRequest request) {
        log.info("Adjusting stock for product {} in warehouse {}: {} units", 
            request.getProductId(), request.getWarehouseId(), request.getQuantity());
        
        String lockKey = LOCK_PREFIX + request.getProductId() + ":" + request.getWarehouseId();
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new ConcurrencyException("Unable to acquire lock for adjusting stock");
        }
        
        try {
            Inventory inventory = inventoryRepository
                .findByProductAndWarehouseWithLock(request.getProductId(), request.getWarehouseId())
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found"));
            
            int newQuantity = inventory.getAvailableQuantity() + request.getQuantity();
            
            if (newQuantity < 0) {
                throw new InvalidStockAdjustmentException(
                    "Adjustment would result in negative stock: " + newQuantity
                );
            }
            
            inventory.setAvailableQuantity(newQuantity);
            inventory.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(inventory);
            
            // Log transaction
            TransactionType type = request.getQuantity() > 0 ? 
                TransactionType.ADJUST_IN : TransactionType.ADJUST_OUT;
            logTransaction(inventory, type, Math.abs(request.getQuantity()), null, null, request.getReason());
            
            // Invalidate cache
            invalidateCache(request.getProductId());
            
            log.info("Stock adjusted successfully");
            
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * Get inventory for a product across all warehouses
     */
    public List<Inventory> getInventoryByProduct(String productId) {
        return inventoryRepository.findByProductId(productId);
    }
    
    /**
     * Get low stock products
     */
    public List<Inventory> getLowStockProducts() {
        return inventoryRepository.findLowStockInventory();
    }
    
    /**
     * Process expired reservations (scheduled job)
     */
    @Transactional
    public void processExpiredReservations() {
        log.info("Processing expired reservations");
        
        List<StockReservation> expiredReservations = reservationRepository
            .findExpiredReservations(LocalDateTime.now());
        
        for (StockReservation reservation : expiredReservations) {
            try {
                releaseReservation(reservation.getReservationId());
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);
            } catch (Exception e) {
                log.error("Failed to process expired reservation: {}", reservation.getReservationId(), e);
            }
        }
        
        log.info("Processed {} expired reservations", expiredReservations.size());
    }
    
    // Helper methods
    
    private void logTransaction(Inventory inventory, TransactionType type, int quantity, 
                               String orderId, String reservationId) {
        logTransaction(inventory, type, quantity, orderId, reservationId, null);
    }
    
    private void logTransaction(Inventory inventory, TransactionType type, int quantity, 
                               String orderId, String reservationId, String reason) {
        InventoryTransaction transaction = InventoryTransaction.builder()
            .transactionId(UUID.randomUUID().toString())
            .inventoryId(inventory.getInventoryId())
            .productId(inventory.getProductId())
            .warehouseId(inventory.getWarehouseId())
            .type(type)
            .quantity(quantity)
            .orderId(orderId)
            .reservationId(reservationId)
            .reason(reason)
            .timestamp(LocalDateTime.now())
            .build();
        
        transactionRepository.save(transaction);
    }
    
    private void invalidateCache(String productId) {
        String cacheKey = CACHE_PREFIX + productId;
        redisTemplate.delete(cacheKey);
    }
    
    private void releaseLocks(List<String> locks) {
        for (String lock : locks) {
            redisTemplate.delete(lock);
        }
    }
    
    private void rollbackReservations(List<StockReservation> reservations) {
        for (StockReservation reservation : reservations) {
            try {
                releaseReservation(reservation.getReservationId());
            } catch (Exception e) {
                log.error("Failed to rollback reservation: {}", reservation.getReservationId(), e);
            }
        }
    }
}
```

---

This completes Part 2. Part 3 will continue with REST Controller, Exception Handling, and Interview Discussion Points.

### 5. REST Controller

```java
package com.inventory.controller;

import com.inventory.dto.*;
import com.inventory.model.*;
import com.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    /**
     * Check stock availability
     */
    @PostMapping("/check")
    public ResponseEntity<AvailabilityCheckResponse> checkAvailability(
            @Valid @RequestBody AvailabilityCheckRequest request) {
        log.info("Checking availability for {} items", request.getItems().size());
        
        AvailabilityCheckResponse response = inventoryService.checkAvailability(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reserve stock for an order
     */
    @PostMapping("/reserve")
    public ResponseEntity<ReserveStockResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        log.info("Reserving stock for order: {}", request.getOrderId());
        
        ReserveStockResponse response = inventoryService.reserveStock(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    
    /**
     * Release reservation (order cancelled)
     */
    @PostMapping("/release/{reservationId}")
    public ResponseEntity<Void> releaseReservation(@PathVariable String reservationId) {
        log.info("Releasing reservation: {}", reservationId);
        
        inventoryService.releaseReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Commit reservation (order completed)
     */
    @PostMapping("/commit/{reservationId}")
    public ResponseEntity<Void> commitReservation(@PathVariable String reservationId) {
        log.info("Committing reservation: {}", reservationId);
        
        inventoryService.commitReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Adjust stock manually
     */
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@Valid @RequestBody AdjustStockRequest request) {
        log.info("Adjusting stock for product {} in warehouse {}", 
            request.getProductId(), request.getWarehouseId());
        
        inventoryService.adjustStock(request);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get inventory for a product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Inventory>> getInventoryByProduct(@PathVariable String productId) {
        log.info("Getting inventory for product: {}", productId);
        
        List<Inventory> inventories = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(inventories);
    }
    
    /**
     * Get low stock products
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Inventory>> getLowStockProducts() {
        log.info("Getting low stock products");
        
        List<Inventory> lowStockProducts = inventoryService.getLowStockProducts();
        return ResponseEntity.ok(lowStockProducts);
    }
}
```

---

### 6. Exception Handling

```java
package com.inventory.exception;

public class StockReservationException extends RuntimeException {
    public StockReservationException(String message) {
        super(message);
    }
}

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}

public class InvalidReservationStateException extends RuntimeException {
    public InvalidReservationStateException(String message) {
        super(message);
    }
}

public class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}

public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String message) {
        super(message);
    }
}

public class InvalidStockAdjustmentException extends RuntimeException {
    public InvalidStockAdjustmentException(String message) {
        super(message);
    }
}

// Global Exception Handler
package com.inventory.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        log.error("Insufficient stock: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Insufficient Stock")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException ex) {
        log.error("Reservation not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Reservation Not Found")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(ConcurrencyException ex) {
        log.error("Concurrency error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Concurrency Error")
            .message(ex.getMessage())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

@Data
@Builder
class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
}
```

---

### 7. Configuration

```java
package com.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}

// Scheduled Tasks Configuration
package com.inventory.config;

import com.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTasksConfig {
    
    private final InventoryService inventoryService;
    
    /**
     * Process expired reservations every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void processExpiredReservations() {
        log.debug("Running scheduled task: processExpiredReservations");
        inventoryService.processExpiredReservations();
    }
}
```

---

### 8. Application Properties

```yaml
# application.yml
spring:
  application:
    name: inventory-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/inventory_db
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: inventory-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer

server:
  port: 8085

logging:
  level:
    com.inventory: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG

# Custom properties
inventory:
  reservation:
    default-expiry-minutes: 15
    cleanup-interval-seconds: 60
  cache:
    ttl-seconds: 300
  warehouse:
    allocation-strategy: PRIORITY # PRIORITY, NEAREST, ROUND_ROBIN
```

---

## Concurrency Handling

### Race Condition Scenarios

**Scenario 1: Multiple orders for the same product**
```
Time    Order-1                     Order-2
T1      Check stock (10 available)
T2                                  Check stock (10 available)
T3      Reserve 8 units
T4                                  Reserve 5 units (SHOULD FAIL!)
```

**Solution: Pessimistic Locking + Distributed Locks**

```java
// Database-level pessimistic lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventory> findByProductAndWarehouseWithLock(...);

// Redis distributed lock
Boolean lockAcquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
```

---

### Optimistic Locking Alternative

```java
@Entity
@Table(name = "inventory")
public class Inventory {
    // ... other fields
    
    @Version
    private int version; // Hibernate manages this
}

// In service
try {
    inventory.setAvailableQuantity(newQuantity);
    inventoryRepository.save(inventory);
} catch (OptimisticLockException e) {
    // Retry logic
    throw new ConcurrencyException("Stock was modified by another transaction");
}
```

---

## Scalability & Performance

### 1. Database Optimization

```sql
-- Partitioning by warehouse
CREATE TABLE inventory_wh_001 PARTITION OF inventory
    FOR VALUES IN ('WH-001');

CREATE TABLE inventory_wh_002 PARTITION OF inventory
    FOR VALUES IN ('WH-002');

-- Indexes for fast lookups
CREATE INDEX CONCURRENTLY idx_inventory_product_warehouse 
    ON inventory(product_id, warehouse_id);

CREATE INDEX CONCURRENTLY idx_inventory_available_qty 
    ON inventory(available_quantity) WHERE available_quantity <= reorder_point;

-- Materialized view for reporting
CREATE MATERIALIZED VIEW inventory_summary AS
SELECT 
    product_id,
    SUM(available_quantity) as total_available,
    SUM(reserved_quantity) as total_reserved,
    COUNT(DISTINCT warehouse_id) as warehouse_count
FROM inventory
GROUP BY product_id;

-- Refresh periodically
REFRESH MATERIALIZED VIEW CONCURRENTLY inventory_summary;
```

---

### 2. Caching Strategy

```java
// Multi-layer caching
public class CachingInventoryService {
    
    // L1: Application cache (Caffeine)
    private final Cache<String, Integer> localCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();
    
    // L2: Redis cache
    private final RedisTemplate<String, Object> redisTemplate;
    
    // L3: Database
    private final InventoryRepository inventoryRepository;
    
    public int getAvailableQuantity(String productId) {
        // Try L1 cache
        Integer quantity = localCache.getIfPresent(productId);
        if (quantity != null) {
            return quantity;
        }
        
        // Try L2 cache
        String cacheKey = "inventory:" + productId;
        quantity = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (quantity != null) {
            localCache.put(productId, quantity);
            return quantity;
        }
        
        // Query database
        quantity = inventoryRepository.getTotalAvailableQuantity(productId);
        
        // Update caches
        redisTemplate.opsForValue().set(cacheKey, quantity, 5, TimeUnit.MINUTES);
        localCache.put(productId, quantity);
        
        return quantity;
    }
}
```

---

### 3. Read Replicas

```yaml
spring:
  datasource:
    primary:
      url: jdbc:postgresql://primary-db:5432/inventory_db
      username: postgres
      password: password
    
    replica:
      url: jdbc:postgresql://replica-db:5432/inventory_db
      username: postgres
      password: password
```

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        // Primary for writes
    }
    
    @Bean
    public DataSource replicaDataSource() {
        // Replica for reads
    }
    
    @Bean
    public DataSource routingDataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(
            "primary", primaryDataSource(),
            "replica", replicaDataSource()
        ));
        routing.setDefaultTargetDataSource(primaryDataSource());
        return routing;
    }
}

// Use @Transactional(readOnly = true) for read operations
@Transactional(readOnly = true)
public List<Inventory> getInventoryByProduct(String productId) {
    // Routes to replica
    return inventoryRepository.findByProductId(productId);
}
```

---

### 4. Sharding Strategy

```java
// Shard by product ID hash
public class ProductShardingStrategy {
    
    private static final int NUM_SHARDS = 4;
    
    public String getShardKey(String productId) {
        int hash = Math.abs(productId.hashCode());
        int shardId = hash % NUM_SHARDS;
        return "shard_" + shardId;
    }
}

// Route to appropriate shard
@Service
public class ShardedInventoryService {
    
    private final Map<String, InventoryRepository> shardRepositories;
    
    public Inventory getInventory(String productId, String warehouseId) {
        String shardKey = shardingStrategy.getShardKey(productId);
        InventoryRepository repository = shardRepositories.get(shardKey);
        return repository.findByProductIdAndWarehouseId(productId, warehouseId)
            .orElseThrow();
    }
}
```

---

### 5. Event-Driven Architecture

```java
// Publish inventory events to Kafka
@Service
public class InventoryEventPublisher {
    
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
    
    public void publishStockReserved(StockReservation reservation) {
        InventoryEvent event = InventoryEvent.builder()
            .eventType("STOCK_RESERVED")
            .productId(reservation.getProductId())
            .warehouseId(reservation.getWarehouseId())
            .quantity(reservation.getQuantity())
            .orderId(reservation.getOrderId())
            .timestamp(LocalDateTime.now())
            .build();
        
        kafkaTemplate.send("inventory-events", event.getProductId(), event);
    }
    
    public void publishLowStockAlert(Inventory inventory) {
        InventoryEvent event = InventoryEvent.builder()
            .eventType("LOW_STOCK_ALERT")
            .productId(inventory.getProductId())
            .warehouseId(inventory.getWarehouseId())
            .quantity(inventory.getAvailableQuantity())
            .timestamp(LocalDateTime.now())
            .build();
        
        kafkaTemplate.send("inventory-alerts", event.getProductId(), event);
    }
}
```

---

## Interview Discussion Points

### 1. How do you prevent overselling?

**Answer:**
- **Pessimistic Locking**: Use database row-level locks (`SELECT ... FOR UPDATE`)
- **Distributed Locks**: Redis SETNX for cross-instance coordination
- **Atomic Operations**: Use database constraints (`CHECK available_quantity >= 0`)
- **Reservation System**: Reserve stock before order confirmation
- **Idempotency**: Use idempotency keys to prevent duplicate reservations

---

### 2. How do you handle high concurrency?

**Answer:**
- **Connection Pooling**: HikariCP with optimized pool size
- **Caching**: Multi-layer caching (L1: Caffeine, L2: Redis, L3: DB)
- **Read Replicas**: Route read queries to replicas
- **Async Processing**: Use message queues for non-critical operations
- **Rate Limiting**: Prevent system overload
- **Horizontal Scaling**: Stateless services behind load balancer

---

### 3. How do you ensure data consistency?

**Answer:**
- **ACID Transactions**: Use `@Transactional` for atomic operations
- **Two-Phase Commit**: For distributed transactions
- **Saga Pattern**: For long-running transactions
- **Event Sourcing**: Maintain complete audit trail
- **Optimistic/Pessimistic Locking**: Prevent concurrent modifications

---

### 4. How do you handle reservation expiry?

**Answer:**
- **Scheduled Job**: Run every minute to check expired reservations
- **Redis TTL**: Set expiry on reservation keys
- **Database Trigger**: Automatic cleanup on expiry
- **Event-Driven**: Publish expiry events to Kafka
- **Grace Period**: Allow 1-2 minute buffer before releasing stock

---

### 5. How do you scale to millions of products?

**Answer:**
- **Database Sharding**: Partition by product ID or warehouse
- **Caching**: Cache hot products (80/20 rule)
- **Read Replicas**: Scale read operations horizontally
- **Microservices**: Separate inventory service from order service
- **CDN**: Cache product availability at edge locations
- **Elasticsearch**: For fast product search

---

### 6. How do you handle warehouse allocation?

**Answer:**
```java
public interface AllocationStrategy {
    String selectWarehouse(String productId, int quantity);
}

// Priority-based (default)
public class PriorityAllocationStrategy implements AllocationStrategy {
    public String selectWarehouse(String productId, int quantity) {
        // Select warehouse with highest priority and sufficient stock
    }
}

// Nearest warehouse (geo-based)
public class NearestWarehouseStrategy implements AllocationStrategy {
    public String selectWarehouse(String productId, int quantity, Location customerLocation) {
        // Calculate distance and select nearest warehouse
    }
}

// Load balancing
public class RoundRobinStrategy implements AllocationStrategy {
    public String selectWarehouse(String productId, int quantity) {
        // Distribute load evenly across warehouses
    }
}
```

---

### 7. How do you monitor inventory health?

**Answer:**
- **Metrics**: Prometheus + Grafana
  - Stock levels by product/warehouse
  - Reservation success rate
  - Average reservation time
  - Cache hit ratio
  - Database query latency
- **Alerts**: PagerDuty/Slack
  - Low stock alerts
  - High reservation failure rate
  - Database connection pool exhaustion
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: Jaeger for distributed tracing

---

### 8. Trade-offs and Design Decisions

| Decision | Trade-off |
|----------|-----------|
| Pessimistic Locking | Higher consistency, lower throughput |
| Optimistic Locking | Higher throughput, retry overhead |
| Strong Consistency | Slower, but no overselling |
| Eventual Consistency | Faster, but risk of overselling |
| Synchronous Reservation | Immediate feedback, slower |
| Asynchronous Reservation | Faster, delayed confirmation |
| Single Database | Simpler, limited scale |
| Sharded Database | Complex, unlimited scale |

---

## Performance Benchmarks

```
Expected Performance:
- Stock Check: <50ms (p99)
- Stock Reservation: <100ms (p99)
- Throughput: 10,000 reservations/sec
- Cache Hit Ratio: >80%
- Database Connections: 20-50 per instance
- CPU Usage: <70% under peak load
- Memory Usage: <4GB per instance
```

---

## Summary

### Key Components
1. **Inventory Service**: Core business logic
2. **Reservation System**: Temporary stock holds
3. **Distributed Locking**: Concurrency control
4. **Caching Layer**: Performance optimization
5. **Event System**: Async notifications
6. **Audit Trail**: Complete transaction history

### Critical Features
✅ Zero overselling guarantee  
✅ High concurrency support  
✅ Distributed locking  
✅ Reservation expiry  
✅ Multi-warehouse support  
✅ Real-time stock updates  
✅ Complete audit trail  
✅ Low stock alerts  

### Technologies Used
- **Backend**: Spring Boot, Java 17
- **Database**: PostgreSQL (primary), Read Replicas
- **Cache**: Redis (distributed locks + caching)
- **Message Queue**: Kafka (event streaming)
- **Monitoring**: Prometheus, Grafana
- **Search**: Elasticsearch (optional)

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: System Designs Collection
