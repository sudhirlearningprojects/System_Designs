# Database Indexing - Deep Dive Guide

## Table of Contents
1. [Introduction to Indexing](#introduction-to-indexing)
2. [B+ Tree Indexing](#b-tree-indexing)
3. [Indexing Algorithms](#indexing-algorithms)
4. [Index Types](#index-types)
5. [Performance Analysis](#performance-analysis)
6. [Real-World Examples](#real-world-examples)

---

## Introduction to Indexing

### What is an Index?

An index is a data structure that improves the speed of data retrieval operations on a database table at the cost of additional writes and storage space.

```
Without Index:
SELECT * FROM users WHERE id = 1000;
→ Full table scan: O(n) - checks all rows

With Index:
SELECT * FROM users WHERE id = 1000;
→ Index lookup: O(log n) - uses B+ tree
```

### Why Indexing?

```
Table: 1,000,000 rows
Without Index: 1,000,000 disk reads (worst case)
With B+ Tree Index: ~4 disk reads (log₁₀₀₀(1,000,000) ≈ 2-4)

Speed improvement: 250,000x faster!
```

---

## B+ Tree Indexing

### B+ Tree Structure

```
B+ Tree (Order m=4, max 3 keys per node):

                    [30|60]
                   /   |   \
                  /    |    \
            [10|20] [40|50] [70|80]
             /  |  \   |  \   |  \
           L1  L2  L3 L4  L5 L6  L7

Internal Nodes: Only keys (for routing)
Leaf Nodes: Keys + Data pointers + Next pointer

Leaf Level (Linked List):
[10|20] → [40|50] → [70|80] → NULL
```

### B+ Tree Properties

1. **Order (m)**: Maximum number of children per node
2. **Keys per node**: m-1 maximum keys
3. **Children per node**: m maximum children
4. **Balanced**: All leaf nodes at same level
5. **Sorted**: Keys in ascending order
6. **Linked leaves**: Leaf nodes form linked list

### B+ Tree Node Structure

```java
class BPlusTreeNode {
    int[] keys;           // Keys in node
    int numKeys;          // Current number of keys
    BPlusTreeNode[] children;  // Child pointers
    BPlusTreeNode next;   // Next leaf (only for leaf nodes)
    boolean isLeaf;       // Is this a leaf node?
    Object[] data;        // Data pointers (only for leaf nodes)
}
```

### How Many Edges in B+ Tree?

**Formula**: For a B+ Tree of order **m**:
- **Internal node**: m children (m edges)
- **Leaf node**: 0 children (0 edges)

**Example**: Order m=4
- Each internal node: 3-4 keys, 4 children (4 edges)
- Each leaf node: 3-4 keys, 0 children (0 edges)

**Total edges calculation**:
```
For n keys:
- Height: h = log_m(n)
- Internal nodes: n/m
- Total edges: approximately n edges

Example: 1,000,000 keys, order 100
- Height: log₁₀₀(1,000,000) ≈ 3
- Edges per internal node: 100
- Total internal nodes: ~10,000
- Total edges: ~1,000,000
```

### B+ Tree Operations

#### 1. Search Operation

```java
public Object search(int key) {
    BPlusTreeNode node = root;
    
    // Traverse from root to leaf
    while (!node.isLeaf) {
        int i = 0;
        while (i < node.numKeys && key >= node.keys[i]) {
            i++;
        }
        node = node.children[i];
    }
    
    // Search in leaf node
    for (int i = 0; i < node.numKeys; i++) {
        if (node.keys[i] == key) {
            return node.data[i];
        }
    }
    return null;
}

// Time Complexity: O(log n)
// Disk I/O: O(log_m n) where m is order
```

#### 2. Range Query

```java
public List<Object> rangeQuery(int startKey, int endKey) {
    List<Object> result = new ArrayList<>();
    
    // Find start leaf node
    BPlusTreeNode node = findLeafNode(startKey);
    
    // Traverse linked list of leaf nodes
    while (node != null) {
        for (int i = 0; i < node.numKeys; i++) {
            if (node.keys[i] >= startKey && node.keys[i] <= endKey) {
                result.add(node.data[i]);
            }
            if (node.keys[i] > endKey) {
                return result;
            }
        }
        node = node.next;  // Move to next leaf
    }
    return result;
}

// Time Complexity: O(log n + k) where k is result size
// Efficient due to linked leaf nodes!
```

#### 3. Insert Operation

```java
public void insert(int key, Object data) {
    if (root == null) {
        root = new BPlusTreeNode(true);
        root.keys[0] = key;
        root.data[0] = data;
        root.numKeys = 1;
        return;
    }
    
    // If root is full, split it
    if (root.numKeys == maxKeys) {
        BPlusTreeNode newRoot = new BPlusTreeNode(false);
        newRoot.children[0] = root;
        splitChild(newRoot, 0);
        root = newRoot;
    }
    
    insertNonFull(root, key, data);
}

private void insertNonFull(BPlusTreeNode node, int key, Object data) {
    int i = node.numKeys - 1;
    
    if (node.isLeaf) {
        // Insert in leaf node
        while (i >= 0 && key < node.keys[i]) {
            node.keys[i + 1] = node.keys[i];
            node.data[i + 1] = node.data[i];
            i--;
        }
        node.keys[i + 1] = key;
        node.data[i + 1] = data;
        node.numKeys++;
    } else {
        // Find child to insert
        while (i >= 0 && key < node.keys[i]) {
            i--;
        }
        i++;
        
        if (node.children[i].numKeys == maxKeys) {
            splitChild(node, i);
            if (key > node.keys[i]) {
                i++;
            }
        }
        insertNonFull(node.children[i], key, data);
    }
}

// Time Complexity: O(log n)
```

### B+ Tree vs B Tree

| Feature | B Tree | B+ Tree |
|---------|--------|---------|
| **Data Storage** | Internal + Leaf nodes | Only leaf nodes |
| **Leaf Linking** | No | Yes (linked list) |
| **Range Queries** | Slower | Faster |
| **Space** | Less | More |
| **Height** | Same | Same |
| **Use Case** | File systems | Databases |

---

## Indexing Algorithms

### 1. Hash Index

**Structure**: Hash table with buckets

```
Hash Index:
Key → Hash Function → Bucket → Data

Example:
hash(42) = 42 % 10 = 2
hash(52) = 52 % 10 = 2  (collision)

Bucket 0: []
Bucket 1: []
Bucket 2: [42→Data1, 52→Data2]
Bucket 3: []
...
```

**Implementation**:
```java
class HashIndex {
    private List<Entry>[] buckets;
    private int size;
    
    public void insert(int key, Object data) {
        int bucket = hash(key) % buckets.length;
        buckets[bucket].add(new Entry(key, data));
    }
    
    public Object search(int key) {
        int bucket = hash(key) % buckets.length;
        for (Entry e : buckets[bucket]) {
            if (e.key == key) return e.data;
        }
        return null;
    }
}

// Time Complexity:
// - Search: O(1) average, O(n) worst
// - Insert: O(1)
// - Range Query: O(n) - NOT SUPPORTED efficiently
```

**Pros & Cons**:
- ✅ O(1) exact match lookups
- ✅ Fast inserts
- ❌ No range queries
- ❌ No sorting
- ❌ Collisions

### 2. Bitmap Index

**Structure**: Bit array for each distinct value

```
Bitmap Index:
Table:
ID | Gender
1  | M
2  | F
3  | M
4  | F
5  | M

Bitmap for 'M': [1, 0, 1, 0, 1]
Bitmap for 'F': [0, 1, 0, 1, 0]

Query: WHERE Gender = 'M'
→ Return positions where bit = 1: [1, 3, 5]
```

**Implementation**:
```java
class BitmapIndex {
    private Map<Object, BitSet> bitmaps;
    
    public void build(List<Object> column) {
        bitmaps = new HashMap<>();
        for (int i = 0; i < column.size(); i++) {
            Object value = column.get(i);
            bitmaps.putIfAbsent(value, new BitSet());
            bitmaps.get(value).set(i);
        }
    }
    
    public BitSet search(Object value) {
        return bitmaps.get(value);
    }
    
    // AND operation for multiple conditions
    public BitSet and(Object value1, Object value2) {
        BitSet result = (BitSet) bitmaps.get(value1).clone();
        result.and(bitmaps.get(value2));
        return result;
    }
}

// Time Complexity:
// - Search: O(1)
// - AND/OR: O(n/64) - bitwise operations
// - Space: O(n * distinct_values)
```

**Use Cases**:
- Low cardinality columns (few distinct values)
- Data warehousing
- OLAP queries

### 3. R-Tree Index (Spatial)

**Structure**: Tree for spatial data (rectangles)

```
R-Tree:
        [MBR: (0,0)-(100,100)]
       /                    \
[MBR: (0,0)-(50,50)]  [MBR: (50,50)-(100,100)]
    /        \              /           \
  Rect1    Rect2         Rect3        Rect4
```

**Use Cases**:
- Geographic data (maps, GPS)
- CAD systems
- Game engines

### 4. Full-Text Index (Inverted Index)

**Structure**: Word → Document list

```
Inverted Index:
Documents:
Doc1: "hello world"
Doc2: "hello database"
Doc3: "world database"

Index:
"hello"    → [Doc1, Doc2]
"world"    → [Doc1, Doc3]
"database" → [Doc2, Doc3]

Query: "hello world"
→ Intersection of [Doc1, Doc2] and [Doc1, Doc3] = [Doc1]
```

**Implementation**:
```java
class InvertedIndex {
    private Map<String, Set<Integer>> index;
    
    public void addDocument(int docId, String text) {
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            index.putIfAbsent(word, new HashSet<>());
            index.get(word).add(docId);
        }
    }
    
    public Set<Integer> search(String word) {
        return index.getOrDefault(word.toLowerCase(), new HashSet<>());
    }
    
    public Set<Integer> searchPhrase(String phrase) {
        String[] words = phrase.toLowerCase().split("\\s+");
        Set<Integer> result = new HashSet<>(search(words[0]));
        for (int i = 1; i < words.length; i++) {
            result.retainAll(search(words[i]));
        }
        return result;
    }
}
```

### 5. LSM Tree (Log-Structured Merge Tree)

**Structure**: Multiple levels of sorted files

```
LSM Tree:
Memory (MemTable):
[10, 20, 30, 40]

Level 0 (SSTables):
File1: [5, 15, 25]
File2: [35, 45, 55]

Level 1:
File3: [1, 11, 21, 31, 41, 51]

Compaction: Merge files periodically
```

**Use Cases**:
- Write-heavy workloads
- NoSQL databases (Cassandra, RocksDB)
- Time-series data

### 6. Trie Index (Prefix Tree)

**Structure**: Tree for string prefixes

```
Trie:
        root
       /  |  \
      a   b   c
     /    |    \
    p     a     a
   /      |      \
  p      t       t
 /       |        \
le      man       ch

Words: "apple", "batman", "catch"
```

**Implementation**:
```java
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord;
    Object data;
}

class TrieIndex {
    private TrieNode root = new TrieNode();
    
    public void insert(String key, Object data) {
        TrieNode node = root;
        for (char c : key.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
        node.data = data;
    }
    
    public Object search(String key) {
        TrieNode node = root;
        for (char c : key.toCharArray()) {
            if (!node.children.containsKey(c)) return null;
            node = node.children.get(c);
        }
        return node.isEndOfWord ? node.data : null;
    }
    
    public List<String> prefixSearch(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode node = root;
        
        // Navigate to prefix
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) return results;
            node = node.children.get(c);
        }
        
        // Collect all words with this prefix
        collectWords(node, prefix, results);
        return results;
    }
}

// Time Complexity:
// - Insert: O(m) where m is key length
// - Search: O(m)
// - Prefix Search: O(m + k) where k is result size
```

---

## Index Types

### 1. Primary Index (Clustered)

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,  -- Clustered index
    name VARCHAR(100),
    email VARCHAR(100)
);

-- Data is physically sorted by id
-- Only ONE clustered index per table
```

**Structure**:
```
Clustered Index (B+ Tree):
Leaf nodes contain actual data rows

[10] → [id=10, name="Alice", email="alice@..."]
[20] → [id=20, name="Bob", email="bob@..."]
[30] → [id=30, name="Charlie", email="charlie@..."]
```

### 2. Secondary Index (Non-Clustered)

```sql
CREATE INDEX idx_email ON users(email);

-- Separate B+ Tree
-- Leaf nodes contain pointers to data
```

**Structure**:
```
Secondary Index (B+ Tree):
Leaf nodes contain row pointers

["alice@..."] → Pointer to row with id=10
["bob@..."]   → Pointer to row with id=20
["charlie@..."] → Pointer to row with id=30
```

### 3. Composite Index

```sql
CREATE INDEX idx_name_age ON users(name, age);

-- Index on multiple columns
-- Order matters: (name, age) ≠ (age, name)
```

**Usage**:
```sql
-- ✅ Uses index (leftmost prefix)
SELECT * FROM users WHERE name = 'Alice';
SELECT * FROM users WHERE name = 'Alice' AND age = 25;

-- ❌ Does NOT use index
SELECT * FROM users WHERE age = 25;
```

### 4. Unique Index

```sql
CREATE UNIQUE INDEX idx_email ON users(email);

-- Enforces uniqueness
-- Faster than non-unique index
```

### 5. Covering Index

```sql
CREATE INDEX idx_covering ON users(name, age, email);

-- Index contains all queried columns
-- No need to access table (index-only scan)
```

**Example**:
```sql
-- ✅ Index-only scan (fast)
SELECT name, age, email FROM users WHERE name = 'Alice';

-- ❌ Requires table access (slower)
SELECT name, age, email, address FROM users WHERE name = 'Alice';
```

### 6. Partial Index

```sql
CREATE INDEX idx_active_users ON users(name) 
WHERE status = 'active';

-- Index only subset of rows
-- Smaller index size
```

### 7. Expression Index

```sql
CREATE INDEX idx_lower_email ON users(LOWER(email));

-- Index on computed expression
```

---

## Performance Analysis

### Time Complexity Comparison

| Operation | No Index | Hash Index | B+ Tree | Bitmap |
|-----------|----------|------------|---------|--------|
| **Exact Match** | O(n) | O(1) | O(log n) | O(1) |
| **Range Query** | O(n) | O(n) | O(log n + k) | O(n) |
| **Insert** | O(1) | O(1) | O(log n) | O(1) |
| **Delete** | O(n) | O(1) | O(log n) | O(1) |
| **Sort** | O(n log n) | O(n log n) | O(n) | O(n log n) |

### Space Complexity

```
B+ Tree: O(n)
Hash Index: O(n)
Bitmap: O(n * distinct_values)
Inverted Index: O(total_words)
```

### Disk I/O Analysis

```
Example: 1,000,000 rows, B+ Tree order 100

Without Index:
- Disk reads: 1,000,000 (worst case)
- Time: 1,000,000 * 10ms = 10,000 seconds

With B+ Tree Index:
- Height: log₁₀₀(1,000,000) ≈ 3
- Disk reads: 3-4
- Time: 4 * 10ms = 40ms

Speed improvement: 250,000x faster!
```

---

## Real-World Examples

### Example 1: E-Commerce Product Search

```sql
-- Table
CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(200),
    category VARCHAR(50),
    price DECIMAL(10,2),
    stock INT,
    created_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_category ON products(category);
CREATE INDEX idx_price ON products(price);
CREATE INDEX idx_category_price ON products(category, price);
CREATE FULLTEXT INDEX idx_name ON products(name);

-- Queries
-- ✅ Uses idx_category
SELECT * FROM products WHERE category = 'Electronics';

-- ✅ Uses idx_category_price
SELECT * FROM products 
WHERE category = 'Electronics' AND price BETWEEN 100 AND 500;

-- ✅ Uses idx_name (full-text)
SELECT * FROM products WHERE MATCH(name) AGAINST('laptop');
```

### Example 2: Social Media Posts

```sql
-- Table
CREATE TABLE posts (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    content TEXT,
    created_at TIMESTAMP,
    likes INT,
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_created (created_at DESC),
    FULLTEXT INDEX idx_content (content)
);

-- Queries
-- ✅ Uses idx_user_created
SELECT * FROM posts 
WHERE user_id = 123 
ORDER BY created_at DESC 
LIMIT 20;

-- ✅ Uses idx_created
SELECT * FROM posts 
WHERE created_at > '2024-01-01' 
ORDER BY created_at DESC;
```

### Example 3: Geospatial Queries

```sql
-- Table with spatial index
CREATE TABLE locations (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    coordinates POINT NOT NULL,
    SPATIAL INDEX idx_coordinates (coordinates)
);

-- Query: Find nearby locations
SELECT id, name, 
       ST_Distance(coordinates, POINT(40.7128, -74.0060)) AS distance
FROM locations
WHERE ST_Distance(coordinates, POINT(40.7128, -74.0060)) < 10
ORDER BY distance;
```

---

## Best Practices

### 1. Index Selectivity

```sql
-- ✅ Good: High selectivity (many distinct values)
CREATE INDEX idx_email ON users(email);

-- ❌ Bad: Low selectivity (few distinct values)
CREATE INDEX idx_gender ON users(gender);  -- Only M/F
```

### 2. Index Maintenance

```sql
-- Rebuild fragmented indexes
ALTER INDEX idx_name REBUILD;

-- Update statistics
ANALYZE TABLE users;

-- Monitor index usage
SELECT * FROM sys.dm_db_index_usage_stats;
```

### 3. Avoid Over-Indexing

```
Too many indexes:
- Slower writes (INSERT/UPDATE/DELETE)
- More storage space
- Index maintenance overhead

Rule of thumb: 3-5 indexes per table
```

### 4. Use EXPLAIN to Analyze

```sql
EXPLAIN SELECT * FROM users WHERE email = 'alice@example.com';

-- Output shows:
-- - Index used
-- - Rows scanned
-- - Query cost
```

---

## Summary

### Key Takeaways

1. **B+ Tree**: Best for range queries, sorted data, databases
2. **Hash Index**: Best for exact matches, O(1) lookups
3. **Bitmap**: Best for low cardinality, data warehousing
4. **Inverted Index**: Best for full-text search
5. **R-Tree**: Best for spatial data

### Index Selection Guide

```
Use B+ Tree when:
├─ Range queries needed
├─ Sorting required
└─ General-purpose indexing

Use Hash Index when:
├─ Only exact matches
├─ No range queries
└─ High-speed lookups

Use Bitmap when:
├─ Low cardinality columns
├─ Data warehousing
└─ Complex boolean queries

Use Inverted Index when:
├─ Full-text search
├─ Document retrieval
└─ Keyword matching
```

### B+ Tree Edge Count

**For order m**:
- Internal node: m edges (m children)
- Leaf node: 0 edges
- Total edges ≈ n (number of keys)
- Height: log_m(n)

**Example**: 1M keys, order 100
- Height: 3
- Edges per node: 100
- Total edges: ~1,000,000
