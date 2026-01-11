# PostgreSQL Complete Guide - Part 1: Fundamentals & CRUD Operations

## 📋 Table of Contents
1. [Introduction](#introduction)
2. [Installation & Setup](#installation--setup)
3. [Database & Tables](#database--tables)
4. [CRUD Operations](#crud-operations)
5. [Data Types](#data-types)
6. [Constraints](#constraints)

---

## Introduction

PostgreSQL is an advanced, open-source relational database known for ACID compliance, extensibility, and SQL standards conformance.

### Key Features
- **ACID Compliant**: Full transaction support
- **Advanced Data Types**: JSON, Arrays, UUID, Geometric types
- **Extensible**: Custom functions, operators, data types
- **Concurrent Access**: MVCC for high concurrency
- **Full-Text Search**: Built-in text search capabilities
- **Foreign Data Wrappers**: Query external data sources

### When to Use PostgreSQL
✅ Complex transactions and ACID requirements  
✅ Complex queries with JOINs  
✅ Data integrity and consistency  
✅ Advanced data types (JSON, Arrays)  
✅ Full-text search  
✅ Geospatial data (PostGIS)  

❌ Simple key-value storage (use Redis)  
❌ Flexible schema requirements (use MongoDB)  

---

## Installation & Setup

### Using Docker (Recommended)
```bash
# Pull PostgreSQL image
docker pull postgres:16

# Run PostgreSQL container
docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=ecommerce \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16

# Connect to PostgreSQL
docker exec -it postgres psql -U admin -d ecommerce
```

### Using Homebrew (macOS)
```bash
brew install postgresql@16
brew services start postgresql@16
psql postgres
```

### Connection String
```
# Local connection
postgresql://localhost:5432/ecommerce

# With authentication
postgresql://admin:password@localhost:5432/ecommerce

# Cloud (AWS RDS, Azure, GCP)
postgresql://username:password@host:5432/dbname?sslmode=require
```

---

## Database & Tables

### Create Database
```sql
-- Create database
CREATE DATABASE ecommerce;

-- Connect to database
\c ecommerce

-- List databases
\l

-- Drop database
DROP DATABASE ecommerce;
```

### Create Tables
```sql
-- Users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    age INTEGER,
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER DEFAULT 0,
    category VARCHAR(100),
    tags TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order items table
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);
```

### Table Management
```sql
-- List tables
\dt

-- Describe table
\d users

-- Alter table
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users DROP COLUMN phone;
ALTER TABLE users RENAME COLUMN name TO full_name;

-- Drop table
DROP TABLE users;
DROP TABLE IF EXISTS users CASCADE;
```

---

## CRUD Operations

### Insert Data

```sql
-- Insert single row
INSERT INTO users (email, name, age)
VALUES ('john@example.com', 'John Doe', 30);

-- Insert multiple rows
INSERT INTO users (email, name, age) VALUES
    ('jane@example.com', 'Jane Smith', 28),
    ('bob@example.com', 'Bob Johnson', 35);

-- Insert and return inserted row
INSERT INTO users (email, name, age)
VALUES ('alice@example.com', 'Alice Brown', 25)
RETURNING *;

-- Insert from SELECT
INSERT INTO users_backup
SELECT * FROM users WHERE age > 30;

-- Insert with conflict handling (UPSERT)
INSERT INTO users (email, name, age)
VALUES ('john@example.com', 'John Updated', 31)
ON CONFLICT (email) 
DO UPDATE SET name = EXCLUDED.name, age = EXCLUDED.age;
```

### Read Data

```sql
-- Select all columns
SELECT * FROM users;

-- Select specific columns
SELECT id, email, name FROM users;

-- WHERE clause
SELECT * FROM users WHERE age > 30;
SELECT * FROM users WHERE email = 'john@example.com';

-- Multiple conditions
SELECT * FROM users 
WHERE age > 25 AND status = 'active';

SELECT * FROM users 
WHERE age < 20 OR age > 60;

-- IN operator
SELECT * FROM users 
WHERE status IN ('active', 'pending');

-- BETWEEN operator
SELECT * FROM users 
WHERE age BETWEEN 25 AND 35;

-- LIKE operator (pattern matching)
SELECT * FROM users WHERE email LIKE '%@gmail.com';
SELECT * FROM users WHERE name LIKE 'John%';
SELECT * FROM users WHERE name ILIKE 'john%'; -- case-insensitive

-- IS NULL / IS NOT NULL
SELECT * FROM users WHERE age IS NULL;
SELECT * FROM users WHERE age IS NOT NULL;

-- ORDER BY
SELECT * FROM users ORDER BY age DESC;
SELECT * FROM users ORDER BY name ASC, age DESC;

-- LIMIT and OFFSET (pagination)
SELECT * FROM users LIMIT 10;
SELECT * FROM users LIMIT 10 OFFSET 20;

-- DISTINCT
SELECT DISTINCT category FROM products;

-- COUNT
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM users WHERE age > 30;
```

### Update Data

```sql
-- Update single row
UPDATE users 
SET age = 31, updated_at = CURRENT_TIMESTAMP
WHERE email = 'john@example.com';

-- Update multiple rows
UPDATE users 
SET status = 'inactive'
WHERE age < 18;

-- Update with calculation
UPDATE products 
SET price = price * 1.1
WHERE category = 'electronics';

-- Update and return
UPDATE users 
SET age = age + 1
WHERE id = 1
RETURNING *;

-- Update from another table
UPDATE products p
SET stock = stock - oi.quantity
FROM order_items oi
WHERE p.id = oi.product_id AND oi.order_id = 123;
```

### Delete Data

```sql
-- Delete single row
DELETE FROM users WHERE email = 'john@example.com';

-- Delete multiple rows
DELETE FROM users WHERE age < 18;

-- Delete all rows
DELETE FROM users;

-- Delete and return
DELETE FROM users WHERE id = 1 RETURNING *;

-- Delete with JOIN
DELETE FROM order_items
WHERE order_id IN (
    SELECT id FROM orders WHERE status = 'cancelled'
);
```

---

## Data Types

### Numeric Types
```sql
CREATE TABLE numeric_examples (
    -- Integer types
    small_int SMALLINT,           -- -32768 to 32767
    normal_int INTEGER,           -- -2147483648 to 2147483647
    big_int BIGINT,              -- -9223372036854775808 to 9223372036854775807
    
    -- Auto-increment
    id SERIAL,                    -- Auto-incrementing integer
    big_id BIGSERIAL,            -- Auto-incrementing bigint
    
    -- Decimal types
    exact_decimal DECIMAL(10, 2), -- Exact decimal (10 digits, 2 after decimal)
    exact_numeric NUMERIC(10, 2), -- Same as DECIMAL
    
    -- Floating point
    real_num REAL,                -- 6 decimal digits precision
    double_num DOUBLE PRECISION   -- 15 decimal digits precision
);
```

### String Types
```sql
CREATE TABLE string_examples (
    -- Fixed length
    char_col CHAR(10),           -- Fixed 10 characters
    
    -- Variable length
    varchar_col VARCHAR(255),    -- Variable up to 255 characters
    text_col TEXT,               -- Unlimited length
    
    -- Case-insensitive
    citext_col CITEXT            -- Case-insensitive text (requires extension)
);
```

### Date/Time Types
```sql
CREATE TABLE datetime_examples (
    date_col DATE,                      -- Date only (YYYY-MM-DD)
    time_col TIME,                      -- Time only (HH:MM:SS)
    timestamp_col TIMESTAMP,            -- Date + Time
    timestamptz_col TIMESTAMPTZ,        -- Timestamp with timezone
    interval_col INTERVAL               -- Time interval
);

-- Insert examples
INSERT INTO datetime_examples VALUES (
    '2024-01-15',
    '14:30:00',
    '2024-01-15 14:30:00',
    '2024-01-15 14:30:00+00',
    '2 days 3 hours'
);

-- Date/Time functions
SELECT CURRENT_DATE;
SELECT CURRENT_TIME;
SELECT CURRENT_TIMESTAMP;
SELECT NOW();
SELECT AGE('2024-01-15', '1990-05-20');
```

### Boolean Type
```sql
CREATE TABLE boolean_examples (
    is_active BOOLEAN DEFAULT TRUE
);

INSERT INTO boolean_examples VALUES (TRUE);
INSERT INTO boolean_examples VALUES (FALSE);
INSERT INTO boolean_examples VALUES ('yes');  -- TRUE
INSERT INTO boolean_examples VALUES ('no');   -- FALSE
```

### JSON Types
```sql
CREATE TABLE json_examples (
    data JSON,                   -- JSON data (stored as text)
    data_binary JSONB            -- Binary JSON (faster, indexable)
);

-- Insert JSON
INSERT INTO json_examples (data_binary) VALUES 
('{"name": "John", "age": 30, "tags": ["developer", "postgres"]}');

-- Query JSON
SELECT data_binary->>'name' FROM json_examples;
SELECT data_binary->'tags'->0 FROM json_examples;
SELECT * FROM json_examples WHERE data_binary->>'age' = '30';
```

### Array Types
```sql
CREATE TABLE array_examples (
    tags TEXT[],
    numbers INTEGER[]
);

-- Insert arrays
INSERT INTO array_examples VALUES 
(ARRAY['tag1', 'tag2', 'tag3'], ARRAY[1, 2, 3]);

INSERT INTO array_examples VALUES 
('{"tag1", "tag2"}', '{1, 2, 3}');

-- Query arrays
SELECT * FROM array_examples WHERE 'tag1' = ANY(tags);
SELECT * FROM array_examples WHERE tags @> ARRAY['tag1'];
```

### UUID Type
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE uuid_examples (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255)
);

INSERT INTO uuid_examples (name) VALUES ('Test');
```

---

## Constraints

### Primary Key
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255)
);

-- Composite primary key
CREATE TABLE user_roles (
    user_id INTEGER,
    role_id INTEGER,
    PRIMARY KEY (user_id, role_id)
);
```

### Foreign Key
```sql
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    -- With actions
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    user_id INTEGER REFERENCES users(id) ON UPDATE CASCADE
);
```

### Unique Constraint
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    -- Named constraint
    username VARCHAR(50),
    CONSTRAINT unique_username UNIQUE (username)
);

-- Composite unique
CREATE TABLE user_profiles (
    user_id INTEGER,
    platform VARCHAR(50),
    UNIQUE (user_id, platform)
);
```

### Check Constraint
```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    price DECIMAL(10, 2) CHECK (price > 0),
    stock INTEGER CHECK (stock >= 0),
    age INTEGER CHECK (age >= 18 AND age <= 100),
    status VARCHAR(50) CHECK (status IN ('active', 'inactive', 'pending'))
);
```

### Not Null Constraint
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);
```

### Default Values
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_verified BOOLEAN DEFAULT FALSE
);
```

---

## Practical Examples

### E-commerce Schema
```sql
-- Create complete e-commerce schema
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE addresses (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    stock INTEGER DEFAULT 0 CHECK (stock >= 0),
    category VARCHAR(100),
    tags TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending' 
        CHECK (status IN ('pending', 'processing', 'shipped', 'delivered', 'cancelled')),
    shipping_address_id INTEGER REFERENCES addresses(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL
);

-- Insert sample data
INSERT INTO users (email, password_hash, name) VALUES
    ('john@example.com', '$2b$10$hash', 'John Doe'),
    ('jane@example.com', '$2b$10$hash', 'Jane Smith');

INSERT INTO products (sku, name, price, stock, category, tags) VALUES
    ('LAPTOP-001', 'MacBook Pro 16', 2499.99, 25, 'electronics', ARRAY['laptop', 'apple']),
    ('PHONE-001', 'iPhone 15 Pro', 999.99, 50, 'electronics', ARRAY['phone', 'apple']);

-- Query examples
SELECT * FROM users WHERE email = 'john@example.com';

SELECT p.* FROM products p 
WHERE p.category = 'electronics' AND p.stock > 0
ORDER BY p.price DESC;

SELECT u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name;
```

---

## Next Steps

Continue to [Part 2: Indexing & Query Optimization](./02_PostgreSQL_Indexing_Performance.md)
