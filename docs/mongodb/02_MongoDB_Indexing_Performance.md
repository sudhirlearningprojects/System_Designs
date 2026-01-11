# MongoDB Complete Guide - Part 2: Indexing & Performance

## 📋 Table of Contents
1. [Index Fundamentals](#index-fundamentals)
2. [Index Types](#index-types)
3. [Query Optimization](#query-optimization)
4. [Explain Plans](#explain-plans)
5. [Performance Best Practices](#performance-best-practices)

---

## Index Fundamentals

Indexes improve query performance by reducing the number of documents MongoDB needs to scan.

### Why Indexes Matter
```javascript
// Without index: Collection scan (slow)
db.users.find({ email: "john@example.com" })
// Scans all 1M documents

// With index: Index scan (fast)
db.users.createIndex({ email: 1 })
db.users.find({ email: "john@example.com" })
// Scans only 1 document
```

### Create Index
```javascript
// Single field index (ascending)
db.users.createIndex({ email: 1 })

// Single field index (descending)
db.users.createIndex({ createdAt: -1 })

// Compound index
db.orders.createIndex({ userId: 1, createdAt: -1 })

// Unique index
db.users.createIndex({ email: 1 }, { unique: true })

// Sparse index (only indexes documents with the field)
db.users.createIndex({ phone: 1 }, { sparse: true })

// TTL index (auto-delete after expiration)
db.sessions.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 3600 }
)

// Partial index (index subset of documents)
db.orders.createIndex(
  { status: 1, createdAt: -1 },
  { partialFilterExpression: { status: "pending" } }
)

// Background index (non-blocking)
db.products.createIndex({ name: 1 }, { background: true })
```

### Manage Indexes
```javascript
// List all indexes
db.users.getIndexes()

// Drop index
db.users.dropIndex("email_1")
db.users.dropIndex({ email: 1 })

// Drop all indexes (except _id)
db.users.dropIndexes()

// Get index stats
db.users.stats().indexSizes
```

---

## Index Types

### 1. Single Field Index
```javascript
db.users.createIndex({ email: 1 })

// Supports queries
db.users.find({ email: "john@example.com" })
db.users.find({ email: { $in: ["john@example.com", "jane@example.com"] } })
```

### 2. Compound Index
```javascript
// Index on multiple fields
db.orders.createIndex({ userId: 1, status: 1, createdAt: -1 })

// Supports queries (left-to-right prefix)
db.orders.find({ userId: "user123" }) // ✅ Uses index
db.orders.find({ userId: "user123", status: "pending" }) // ✅ Uses index
db.orders.find({ userId: "user123", status: "pending", createdAt: { $gte: date } }) // ✅ Uses index
db.orders.find({ status: "pending" }) // ❌ Doesn't use index
db.orders.find({ createdAt: { $gte: date } }) // ❌ Doesn't use index
```

**Index Prefix Rule**: Compound index can support queries on:
- First field only
- First + second fields
- First + second + third fields
- NOT second or third field alone

### 3. Multikey Index (Array Fields)
```javascript
// Automatically created for array fields
db.products.createIndex({ tags: 1 })

// Supports queries
db.products.find({ tags: "electronics" })
db.products.find({ tags: { $in: ["electronics", "computers"] } })
```

### 4. Text Index (Full-Text Search)
```javascript
// Create text index
db.products.createIndex({ name: "text", description: "text" })

// Search
db.products.find({ $text: { $search: "laptop computer" } })

// Search with score
db.products.find(
  { $text: { $search: "laptop" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })

// Exact phrase search
db.products.find({ $text: { $search: "\"MacBook Pro\"" } })

// Exclude words
db.products.find({ $text: { $search: "laptop -gaming" } })
```

### 5. Geospatial Index
```javascript
// 2dsphere index for GeoJSON
db.stores.createIndex({ location: "2dsphere" })

// Insert location data
db.stores.insertOne({
  name: "Store A",
  location: {
    type: "Point",
    coordinates: [-73.97, 40.77] // [longitude, latitude]
  }
})

// Find nearby stores (within 5km)
db.stores.find({
  location: {
    $near: {
      $geometry: {
        type: "Point",
        coordinates: [-73.98, 40.75]
      },
      $maxDistance: 5000 // meters
    }
  }
})

// Find stores within polygon
db.stores.find({
  location: {
    $geoWithin: {
      $geometry: {
        type: "Polygon",
        coordinates: [[
          [-74.0, 40.7],
          [-73.9, 40.7],
          [-73.9, 40.8],
          [-74.0, 40.8],
          [-74.0, 40.7]
        ]]
      }
    }
  }
})
```

### 6. Hashed Index (Sharding)
```javascript
// Used for hash-based sharding
db.users.createIndex({ userId: "hashed" })

// Supports equality queries only
db.users.find({ userId: "user123" }) // ✅
db.users.find({ userId: { $gt: "user100" } }) // ❌ Doesn't use index
```

### 7. Wildcard Index
```javascript
// Index all fields in subdocument
db.products.createIndex({ "specs.$**": 1 })

// Supports queries on any spec field
db.products.find({ "specs.cpu": "M3 Pro" })
db.products.find({ "specs.ram": "32GB" })
```

---

## Query Optimization

### Index Selection Strategy

```javascript
// Bad: No index
db.orders.find({ userId: "user123", status: "pending" })
// Collection scan: 1M documents

// Good: Single field index
db.orders.createIndex({ userId: 1 })
db.orders.find({ userId: "user123", status: "pending" })
// Index scan: 1000 documents, then filter

// Better: Compound index
db.orders.createIndex({ userId: 1, status: 1 })
db.orders.find({ userId: "user123", status: "pending" })
// Index scan: 10 documents
```

### ESR Rule (Equality, Sort, Range)

Order compound index fields by:
1. **Equality** filters first
2. **Sort** fields second
3. **Range** filters last

```javascript
// Query
db.orders.find({
  userId: "user123",        // Equality
  createdAt: { $gte: date } // Range
}).sort({ status: 1 })      // Sort

// Optimal index: E-S-R
db.orders.createIndex({ userId: 1, status: 1, createdAt: -1 })
```

### Covered Queries

Query is "covered" when all fields are in the index (no document lookup needed).

```javascript
// Create index with all query fields
db.users.createIndex({ email: 1, name: 1, age: 1 })

// Covered query (fastest)
db.users.find(
  { email: "john@example.com" },
  { _id: 0, email: 1, name: 1, age: 1 }
)
// Only reads index, no document fetch
```

### Index Intersection

MongoDB can use multiple indexes for a single query.

```javascript
db.orders.createIndex({ userId: 1 })
db.orders.createIndex({ status: 1 })

// Uses both indexes
db.orders.find({ userId: "user123", status: "pending" })

// But compound index is better
db.orders.createIndex({ userId: 1, status: 1 })
```

---

## Explain Plans

### Basic Explain
```javascript
// Query plan
db.users.find({ email: "john@example.com" }).explain()

// Execution stats
db.users.find({ email: "john@example.com" }).explain("executionStats")

// All plans considered
db.users.find({ email: "john@example.com" }).explain("allPlansExecution")
```

### Understanding Explain Output

```javascript
{
  "executionStats": {
    "executionSuccess": true,
    "nReturned": 1,              // Documents returned
    "executionTimeMillis": 2,    // Query time
    "totalKeysExamined": 1,      // Index keys scanned
    "totalDocsExamined": 1,      // Documents scanned
    "executionStages": {
      "stage": "FETCH",          // Stage type
      "inputStage": {
        "stage": "IXSCAN",       // Index scan
        "keyPattern": { "email": 1 },
        "indexName": "email_1"
      }
    }
  }
}
```

### Common Stages
- **COLLSCAN**: Collection scan (bad - no index used)
- **IXSCAN**: Index scan (good)
- **FETCH**: Fetch documents from index
- **SORT**: In-memory sort (bad if large dataset)
- **SORT_KEY_GENERATOR**: Sort using index (good)

### Performance Metrics
```javascript
// Good query
totalKeysExamined: 1
totalDocsExamined: 1
nReturned: 1
// Ratio: 1:1:1 (perfect)

// Bad query
totalKeysExamined: 10000
totalDocsExamined: 10000
nReturned: 10
// Ratio: 10000:10000:10 (inefficient)
```

---

## Performance Best Practices

### 1. Index Strategy

```javascript
// ✅ DO: Create indexes for frequent queries
db.orders.createIndex({ userId: 1, createdAt: -1 })

// ✅ DO: Use compound indexes
db.products.createIndex({ category: 1, price: 1 })

// ❌ DON'T: Create too many indexes (slows writes)
// Max 5-10 indexes per collection

// ✅ DO: Use partial indexes for subset queries
db.orders.createIndex(
  { createdAt: -1 },
  { partialFilterExpression: { status: "pending" } }
)

// ✅ DO: Drop unused indexes
db.orders.dropIndex("old_index_1")
```

### 2. Query Optimization

```javascript
// ❌ BAD: Select all fields
db.users.find({ email: "john@example.com" })

// ✅ GOOD: Project only needed fields
db.users.find(
  { email: "john@example.com" },
  { name: 1, email: 1, _id: 0 }
)

// ❌ BAD: Large skip values
db.products.find().skip(10000).limit(20)

// ✅ GOOD: Use range queries for pagination
db.products.find({ _id: { $gt: lastId } }).limit(20)

// ❌ BAD: $where operator (slow)
db.users.find({ $where: "this.age > 30" })

// ✅ GOOD: Use query operators
db.users.find({ age: { $gt: 30 } })
```

### 3. Aggregation Optimization

```javascript
// ✅ DO: Filter early with $match
db.orders.aggregate([
  { $match: { status: "completed" } }, // Filter first
  { $group: { _id: "$userId", total: { $sum: "$amount" } } }
])

// ✅ DO: Use indexes in $match and $sort
db.orders.aggregate([
  { $match: { userId: "user123" } }, // Uses index
  { $sort: { createdAt: -1 } }       // Uses index
])

// ✅ DO: Limit early
db.orders.aggregate([
  { $match: { status: "completed" } },
  { $sort: { createdAt: -1 } },
  { $limit: 100 }
])
```

### 4. Connection Pooling

```javascript
// Spring Boot application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
      connection-pool:
        max-size: 100
        min-size: 10
        max-wait-time: 2000
        max-connection-life-time: 300000
        max-connection-idle-time: 60000
```

### 5. Monitoring

```javascript
// Current operations
db.currentOp()

// Slow queries (>100ms)
db.setProfilingLevel(1, { slowms: 100 })

// View profiler data
db.system.profile.find().sort({ ts: -1 }).limit(10)

// Server status
db.serverStatus()

// Collection stats
db.orders.stats()
```

---

## Real-World Example: E-commerce Optimization

### Before Optimization
```javascript
// Slow query (2000ms)
db.orders.find({
  userId: "user123",
  status: "completed",
  createdAt: { $gte: ISODate("2024-01-01") }
}).sort({ createdAt: -1 })

// Explain shows COLLSCAN
{
  "executionTimeMillis": 2000,
  "totalDocsExamined": 1000000,
  "nReturned": 50,
  "stage": "COLLSCAN"
}
```

### After Optimization
```javascript
// Create optimal index (ESR rule)
db.orders.createIndex({
  userId: 1,      // Equality
  status: 1,      // Equality
  createdAt: -1   // Range + Sort
})

// Fast query (5ms)
db.orders.find({
  userId: "user123",
  status: "completed",
  createdAt: { $gte: ISODate("2024-01-01") }
}).sort({ createdAt: -1 })

// Explain shows IXSCAN
{
  "executionTimeMillis": 5,
  "totalKeysExamined": 50,
  "totalDocsExamined": 50,
  "nReturned": 50,
  "stage": "FETCH",
  "inputStage": {
    "stage": "IXSCAN",
    "indexName": "userId_1_status_1_createdAt_-1"
  }
}
```

**Result**: 400x faster (2000ms → 5ms)

---

## Next Steps

Continue to [Part 3: Aggregation Framework](./03_MongoDB_Aggregation.md)
