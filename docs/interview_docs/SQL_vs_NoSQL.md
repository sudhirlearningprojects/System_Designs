# SQL vs NoSQL Databases - Complete Comparison

## Quick Summary

| Aspect | SQL (Relational) | NoSQL (Non-Relational) |
|--------|-----------------|------------------------|
| **Data Model** | Tables with rows/columns | Documents, Key-Value, Graph, Column-family |
| **Schema** | Fixed schema (predefined) | Flexible schema (dynamic) |
| **Scalability** | Vertical (scale up) | Horizontal (scale out) |
| **ACID** | ✅ Full ACID compliance | ⚠️ Eventual consistency (BASE) |
| **Joins** | ✅ Complex joins supported | ❌ Limited/No joins |
| **Use Case** | Structured data, transactions | Unstructured data, high volume |
| **Examples** | MySQL, PostgreSQL, Oracle | MongoDB, Cassandra, Redis |

---

## What is SQL Database?

**SQL (Structured Query Language)** databases are relational databases that store data in tables with predefined schemas.

**Key Characteristics**:
- Data stored in tables (rows and columns)
- Fixed schema
- ACID transactions
- Relationships via foreign keys
- Powerful query language (SQL)

**Popular SQL Databases**:
- MySQL
- PostgreSQL
- Oracle
- SQL Server
- SQLite

---

## What is NoSQL Database?

**NoSQL (Not Only SQL)** databases are non-relational databases designed for specific data models and flexible schemas.

**Key Characteristics**:
- Flexible schema
- Horizontal scalability
- High performance
- Distributed architecture
- Various data models

**Popular NoSQL Databases**:
- MongoDB (Document)
- Redis (Key-Value)
- Cassandra (Column-family)
- Neo4j (Graph)
- DynamoDB (Key-Value/Document)

---

## Data Model Comparison

### SQL - Relational Model

**Users Table**:
```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    age INT
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    product VARCHAR(100),
    amount DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Data**:
```
users:
+----+------------+-------------------+-----+
| id | name       | email             | age |
+----+------------+-------------------+-----+
| 1  | John Doe   | john@example.com  | 30  |
| 2  | Jane Smith | jane@example.com  | 25  |
+----+------------+-------------------+-----+

orders:
+----+---------+-----------+--------+
| id | user_id | product   | amount |
+----+---------+-----------+--------+
| 1  | 1       | Laptop    | 999.99 |
| 2  | 1       | Mouse     | 29.99  |
| 3  | 2       | Keyboard  | 79.99  |
+----+---------+-----------+--------+
```

**Query with Join**:
```sql
SELECT u.name, o.product, o.amount
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.id = 1;
```

---

### NoSQL - Document Model (MongoDB)

**Users Collection**:
```json
{
  "_id": "1",
  "name": "John Doe",
  "email": "john@example.com",
  "age": 30,
  "orders": [
    {
      "id": "1",
      "product": "Laptop",
      "amount": 999.99,
      "date": "2024-01-15"
    },
    {
      "id": "2",
      "product": "Mouse",
      "amount": 29.99,
      "date": "2024-01-16"
    }
  ],
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "zip": "10001"
  }
}
```

**Query**:
```javascript
db.users.find({ "_id": "1" })
```

**Benefit**: All data in one document, no joins needed!

---

## Schema Comparison

### SQL - Fixed Schema

**Must Define Schema First**:
```sql
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50)
);

-- Adding new field requires ALTER TABLE
ALTER TABLE products ADD COLUMN description TEXT;
```

**Problem**: Schema changes require migration, downtime.

---

### NoSQL - Flexible Schema

**No Schema Required**:
```javascript
// Document 1
{
  "_id": "1",
  "name": "Laptop",
  "price": 999.99,
  "category": "Electronics"
}

// Document 2 - Different structure, no problem!
{
  "_id": "2",
  "name": "Book",
  "price": 19.99,
  "category": "Books",
  "author": "John Smith",  // New field
  "isbn": "978-1234567890"  // Another new field
}
```

**Benefit**: Add fields anytime, no migration needed!

---

## Scalability

### SQL - Vertical Scaling (Scale Up)

```
Single Server:
┌─────────────────┐
│   MySQL Server  │
│   CPU: 8 cores  │
│   RAM: 32 GB    │
│   Disk: 1 TB    │
└─────────────────┘

Need more capacity?
↓
Upgrade to bigger server:
┌─────────────────┐
│   MySQL Server  │
│   CPU: 32 cores │
│   RAM: 128 GB   │
│   Disk: 4 TB    │
└─────────────────┘
```

**Limitation**: Hardware limits, expensive, single point of failure.

---

### NoSQL - Horizontal Scaling (Scale Out)

```
Initial Setup:
┌──────────┐
│  Node 1  │
└──────────┘

Need more capacity?
↓
Add more nodes:
┌──────────┐  ┌──────────┐  ┌──────────┐
│  Node 1  │  │  Node 2  │  │  Node 3  │
└──────────┘  └──────────┘  └──────────┘
     ↓             ↓             ↓
  Data sharded across nodes
```

**Benefit**: Unlimited scaling, fault tolerance, cost-effective.

---

## ACID vs BASE

### SQL - ACID Transactions

**ACID Properties**:
- **A**tomicity: All or nothing
- **C**onsistency: Data integrity maintained
- **I**solation: Transactions don't interfere
- **D**urability: Changes are permanent

**Example**:
```sql
BEGIN TRANSACTION;

UPDATE accounts SET balance = balance - 100 WHERE id = 1;
UPDATE accounts SET balance = balance + 100 WHERE id = 2;

COMMIT; -- Both succeed or both fail
```

**Guarantee**: Strong consistency, data always correct.

---

### NoSQL - BASE Model

**BASE Properties**:
- **B**asically **A**vailable: System always responds
- **S**oft state: State may change over time
- **E**ventual consistency: Data becomes consistent eventually

**Example (MongoDB)**:
```javascript
// Write to primary node
db.users.updateOne(
  { "_id": "1" },
  { $set: { "balance": 900 } }
)

// Read from replica might return old value temporarily
// Eventually all replicas will have updated value
```

**Trade-off**: High availability over immediate consistency.

---

## Query Examples

### SQL Queries

**Simple Select**:
```sql
SELECT * FROM users WHERE age > 25;
```

**Join Multiple Tables**:
```sql
SELECT u.name, o.product, p.amount
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN payments p ON o.id = p.order_id
WHERE u.age > 25 AND p.status = 'completed';
```

**Aggregation**:
```sql
SELECT category, COUNT(*), AVG(price)
FROM products
GROUP BY category
HAVING AVG(price) > 100;
```

**Complex Query**:
```sql
SELECT u.name, 
       COUNT(o.id) as order_count,
       SUM(o.amount) as total_spent
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.created_at > '2024-01-01'
GROUP BY u.id, u.name
HAVING COUNT(o.id) > 5
ORDER BY total_spent DESC
LIMIT 10;
```

---

### NoSQL Queries (MongoDB)

**Simple Find**:
```javascript
db.users.find({ age: { $gt: 25 } })
```

**Embedded Data (No Join)**:
```javascript
db.users.find({
  "age": { $gt: 25 },
  "orders.status": "completed"
})
```

**Aggregation**:
```javascript
db.products.aggregate([
  { $group: {
      _id: "$category",
      count: { $sum: 1 },
      avgPrice: { $avg: "$price" }
  }},
  { $match: { avgPrice: { $gt: 100 } }}
])
```

**Complex Aggregation**:
```javascript
db.users.aggregate([
  { $match: { created_at: { $gt: new Date("2024-01-01") } }},
  { $unwind: "$orders" },
  { $group: {
      _id: "$_id",
      name: { $first: "$name" },
      order_count: { $sum: 1 },
      total_spent: { $sum: "$orders.amount" }
  }},
  { $match: { order_count: { $gt: 5 } }},
  { $sort: { total_spent: -1 }},
  { $limit: 10 }
])
```

---

## Types of NoSQL Databases

### 1. Document Databases (MongoDB, CouchDB)

**Data Model**: JSON-like documents

```json
{
  "_id": "123",
  "name": "John Doe",
  "email": "john@example.com",
  "orders": [
    { "product": "Laptop", "price": 999 }
  ]
}
```

**Use Case**: Content management, user profiles, catalogs

---

### 2. Key-Value Stores (Redis, DynamoDB)

**Data Model**: Simple key-value pairs

```
Key: "user:123"
Value: "{ name: 'John', email: 'john@example.com' }"

Key: "session:abc"
Value: "{ userId: 123, expires: 1234567890 }"
```

**Use Case**: Caching, session storage, real-time data

---

### 3. Column-Family Stores (Cassandra, HBase)

**Data Model**: Columns grouped into families

```
Row Key: "user123"
Column Family: "profile"
  - name: "John Doe"
  - email: "john@example.com"
Column Family: "orders"
  - order1: "Laptop"
  - order2: "Mouse"
```

**Use Case**: Time-series data, analytics, IoT

---

### 4. Graph Databases (Neo4j, Amazon Neptune)

**Data Model**: Nodes and relationships

```
(User:John)-[:FRIENDS_WITH]->(User:Jane)
(User:John)-[:PURCHASED]->(Product:Laptop)
(Product:Laptop)-[:CATEGORY]->(Category:Electronics)
```

**Use Case**: Social networks, recommendation engines, fraud detection

---

## Real-World Examples

### E-commerce Application

#### SQL Approach

**Schema**:
```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100)
);

CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(10,2),
    stock INT
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    total DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id INT PRIMARY KEY,
    order_id INT,
    product_id INT,
    quantity INT,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

**Query**:
```sql
SELECT u.name, o.id, p.name, oi.quantity
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE u.id = 123;
```

---

#### NoSQL Approach (MongoDB)

**Document**:
```json
{
  "_id": "user123",
  "name": "John Doe",
  "email": "john@example.com",
  "orders": [
    {
      "orderId": "order456",
      "total": 1029.98,
      "items": [
        {
          "productId": "prod789",
          "name": "Laptop",
          "price": 999.99,
          "quantity": 1
        },
        {
          "productId": "prod790",
          "name": "Mouse",
          "price": 29.99,
          "quantity": 1
        }
      ]
    }
  ]
}
```

**Query**:
```javascript
db.users.findOne({ "_id": "user123" })
```

**Benefit**: Single query, no joins, faster reads!

---

## Performance Comparison

### SQL Performance

**Strengths**:
- ✅ Complex queries with joins
- ✅ Aggregations
- ✅ Transactions
- ✅ Data integrity

**Weaknesses**:
- ❌ Slow with large datasets
- ❌ Vertical scaling limits
- ❌ Join overhead

**Example**:
```sql
-- Complex join on 10M+ rows - can be slow
SELECT u.name, COUNT(o.id)
FROM users u
JOIN orders o ON u.id = o.user_id
GROUP BY u.id;
```

---

### NoSQL Performance

**Strengths**:
- ✅ Fast reads (no joins)
- ✅ Horizontal scaling
- ✅ High throughput
- ✅ Low latency

**Weaknesses**:
- ❌ Complex queries harder
- ❌ No joins (denormalization needed)
- ❌ Eventual consistency

**Example**:
```javascript
// Fast read - single document lookup
db.users.findOne({ "_id": "user123" })
// Returns all user data including orders in milliseconds
```

---

## When to Use SQL

### ✅ Use SQL When:

1. **Complex Relationships**
   - Multiple tables with foreign keys
   - Need complex joins

2. **ACID Transactions Required**
   - Banking, financial systems
   - Inventory management
   - Booking systems

3. **Structured Data**
   - Fixed schema
   - Data integrity critical

4. **Complex Queries**
   - Aggregations, reporting
   - Business intelligence

5. **Small to Medium Scale**
   - < 1TB data
   - Vertical scaling sufficient

**Examples**:
- Banking systems
- ERP systems
- Accounting software
- Reservation systems
- Traditional web applications

---

## When to Use NoSQL

### ✅ Use NoSQL When:

1. **Massive Scale**
   - Billions of records
   - Petabytes of data
   - Need horizontal scaling

2. **Flexible Schema**
   - Rapidly changing requirements
   - Unstructured data
   - Different data formats

3. **High Performance**
   - Low latency required
   - High throughput
   - Real-time applications

4. **Simple Queries**
   - Key-based lookups
   - No complex joins

5. **Distributed Systems**
   - Multi-region deployment
   - High availability

**Examples**:
- Social media platforms
- IoT applications
- Real-time analytics
- Content management
- Gaming leaderboards
- Session storage

---

## Hybrid Approach (Polyglot Persistence)

**Use Both!** Different databases for different needs.

```
E-commerce Application:
├── PostgreSQL (SQL)
│   └── Orders, Payments, Inventory (ACID transactions)
├── MongoDB (NoSQL)
│   └── Product catalog, User profiles (flexible schema)
├── Redis (NoSQL)
│   └── Session storage, Caching (high performance)
└── Elasticsearch (NoSQL)
    └── Product search (full-text search)
```

**Example Architecture**:
```
User Request
    ↓
API Gateway
    ↓
┌─────────────┬──────────────┬──────────────┐
│             │              │              │
PostgreSQL   MongoDB       Redis         Elasticsearch
(Orders)     (Products)    (Cache)       (Search)
```

---

## Migration Considerations

### SQL to NoSQL

**Challenges**:
- Denormalization required
- No foreign keys
- Application-level joins
- Eventual consistency

**Example**:

**SQL (Normalized)**:
```sql
users: id, name, email
orders: id, user_id, total
order_items: id, order_id, product_id, quantity
```

**NoSQL (Denormalized)**:
```json
{
  "userId": "123",
  "name": "John",
  "orders": [
    {
      "orderId": "456",
      "items": [
        { "productId": "789", "quantity": 1 }
      ]
    }
  ]
}
```

---

### NoSQL to SQL

**Challenges**:
- Schema design
- Data normalization
- Relationship mapping
- Data migration

---

## Cost Comparison

### SQL Databases

**Costs**:
- License fees (Oracle, SQL Server)
- Expensive hardware (vertical scaling)
- DBA salaries
- Backup/recovery infrastructure

**Example**:
- Oracle Enterprise: $47,500 per processor
- SQL Server Enterprise: $14,256 per core

---

### NoSQL Databases

**Costs**:
- Open source (MongoDB, Cassandra)
- Commodity hardware (horizontal scaling)
- Cloud-based pricing (pay per use)
- Lower operational costs

**Example**:
- MongoDB Atlas: $0.08/hour (M10 cluster)
- DynamoDB: $0.25 per GB/month

---

## Popular Databases Comparison

| Database | Type | Best For | Scalability | ACID |
|----------|------|----------|-------------|------|
| **MySQL** | SQL | General purpose | Vertical | ✅ |
| **PostgreSQL** | SQL | Complex queries | Vertical | ✅ |
| **MongoDB** | NoSQL (Document) | Flexible schema | Horizontal | ⚠️ |
| **Redis** | NoSQL (Key-Value) | Caching | Horizontal | ❌ |
| **Cassandra** | NoSQL (Column) | Time-series | Horizontal | ❌ |
| **Neo4j** | NoSQL (Graph) | Relationships | Vertical | ✅ |
| **DynamoDB** | NoSQL (Key-Value) | Serverless | Horizontal | ⚠️ |

---

## Key Takeaways

1. **SQL**: Structured data, ACID transactions, complex queries
2. **NoSQL**: Flexible schema, horizontal scaling, high performance
3. **Not either/or**: Use both (polyglot persistence)
4. **SQL for**: Banking, ERP, traditional apps
5. **NoSQL for**: Social media, IoT, real-time apps
6. **Scalability**: SQL (vertical), NoSQL (horizontal)
7. **Consistency**: SQL (strong), NoSQL (eventual)
8. **Schema**: SQL (fixed), NoSQL (flexible)

---

## Quick Decision Tree

```
Need ACID transactions? 
├─ Yes → SQL
└─ No → Continue

Need complex joins?
├─ Yes → SQL
└─ No → Continue

Need horizontal scaling?
├─ Yes → NoSQL
└─ No → SQL

Flexible schema needed?
├─ Yes → NoSQL
└─ No → SQL

High write throughput?
├─ Yes → NoSQL
└─ No → SQL
```

---

## Summary Table

| Feature | SQL | NoSQL |
|---------|-----|-------|
| Data Model | Tables | Documents/Key-Value/Graph |
| Schema | Fixed | Flexible |
| Scalability | Vertical | Horizontal |
| Transactions | ACID | BASE |
| Joins | Yes | No/Limited |
| Consistency | Strong | Eventual |
| Use Case | Structured, transactional | Unstructured, high-scale |
| Learning Curve | Moderate | Easy to start |
| Maturity | Very mature | Relatively new |

**Bottom Line**: Choose based on your specific requirements. Many modern applications use both!
