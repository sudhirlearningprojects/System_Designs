# Database Concepts Guide - Part 2

*Advanced database concepts covering integrity, normalization, storage, and transactions*

## Table of Contents (Part 2)
6. [Integrity Constraints](#integrity-constraints)
7. [Functional Dependencies](#functional-dependencies)
8. [Normal Forms](#normal-forms)
9. [File Organization](#file-organization)
10. [Indexing (B and B+ Trees)](#indexing)

---

## 6. Integrity Constraints {#integrity-constraints}

### Types of Integrity Constraints

**1. Domain Constraints**
- **Definition**: Restrict values that can be stored in attributes
- **Examples**: Data types, value ranges, format restrictions
- **Implementation**: CHECK constraints, data types

```sql
CREATE TABLE Employee (
    EmpID INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL,
    Age INT CHECK (Age >= 18 AND Age <= 65),
    Email VARCHAR(100) CHECK (Email LIKE '%@%.%'),
    Salary DECIMAL(10,2) CHECK (Salary > 0)
);
```

**2. Entity Integrity Constraint**
- **Rule**: Primary key cannot be NULL
- **Reason**: Primary key must uniquely identify each tuple
- **Automatic**: Enforced by DBMS when PRIMARY KEY is declared

**3. Referential Integrity Constraint**
- **Rule**: Foreign key must reference existing primary key or be NULL
- **Purpose**: Maintain consistency between related tables
- **Actions on Violation**:
  - **RESTRICT**: Reject operation
  - **CASCADE**: Propagate change
  - **SET NULL**: Set foreign key to NULL
  - **SET DEFAULT**: Set foreign key to default value

```sql
CREATE TABLE Employee (
    EmpID INT PRIMARY KEY,
    DeptID INT,
    FOREIGN KEY (DeptID) REFERENCES Department(DeptID)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);
```

**4. Key Constraints**
- **Primary Key**: Unique, not null
- **Unique Key**: Unique, can be null
- **Candidate Key**: Potential primary keys

**5. Semantic Integrity Constraints**
- **Business Rules**: Domain-specific constraints
- **Examples**: 
  - Manager's salary > subordinate's salary
  - Course prerequisites must be satisfied
  - Account balance cannot be negative

### Assertion

**Definition**: General constraint not tied to specific table
```sql
CREATE ASSERTION ManagerSalary
CHECK (NOT EXISTS (
    SELECT * FROM Employee E1, Employee E2
    WHERE E1.EmpID = E2.ManagerID 
    AND E1.Salary <= E2.Salary
));
```

### Triggers

**Purpose**: Automatically execute code in response to database events
**Events**: INSERT, UPDATE, DELETE
**Timing**: BEFORE, AFTER, INSTEAD OF

```sql
CREATE TRIGGER UpdateInventory
AFTER INSERT ON OrderItem
FOR EACH ROW
BEGIN
    UPDATE Product 
    SET Stock = Stock - NEW.Quantity
    WHERE ProductID = NEW.ProductID;
END;
```

---

## 7. Functional Dependencies {#functional-dependencies}

### Definition

**Functional Dependency (FD)**: X → Y
- **Meaning**: Value of X uniquely determines value of Y
- **Example**: StudentID → StudentName
- **Notation**: X is determinant, Y is dependent

### Types of Functional Dependencies

**1. Trivial FD**
- **Definition**: Y ⊆ X
- **Example**: {StudentID, Name} → {Name}
- **Always True**: Every relation satisfies trivial FDs

**2. Non-trivial FD**
- **Definition**: Y ⊄ X
- **Example**: StudentID → Name
- **Meaningful**: Provides information about relation structure

**3. Completely Non-trivial FD**
- **Definition**: X ∩ Y = ∅
- **Example**: StudentID → {Name, Age}
- **Strongest Form**: No overlap between determinant and dependent

### Armstrong's Axioms

**Basic Rules** (Sound and Complete):

**1. Reflexivity**: If Y ⊆ X, then X → Y
**2. Augmentation**: If X → Y, then XZ → YZ
**3. Transitivity**: If X → Y and Y → Z, then X → Z

**Derived Rules**:

**4. Union**: If X → Y and X → Z, then X → YZ
**5. Decomposition**: If X → YZ, then X → Y and X → Z
**6. Pseudo-transitivity**: If X → Y and WY → Z, then WX → Z

### Closure of Attributes

**Definition**: X⁺ = set of all attributes functionally determined by X

**Algorithm to Find X⁺**:
```
1. Initialize: result = X
2. Repeat:
   For each FD A → B in F:
     If A ⊆ result, then result = result ∪ B
3. Until no change in result
4. Return result
```

**Example**:
Given: R(A,B,C,D,E), F = {A→BC, CD→E, B→D, E→A}
Find: {A}⁺

```
Step 1: result = {A}
Step 2: A→BC applies, result = {A,B,C}
Step 3: B→D applies, result = {A,B,C,D}
Step 4: CD→E applies, result = {A,B,C,D,E}
Answer: {A}⁺ = {A,B,C,D,E}
```

### Candidate Keys

**Definition**: Minimal set of attributes that functionally determines all attributes

**Algorithm to Find Candidate Keys**:
1. Find attributes that never appear on RHS of FDs (must be in every key)
2. Find attributes that never appear on LHS of FDs (never in any key)
3. For remaining attributes, test combinations

### Canonical Cover

**Definition**: Minimal set of FDs equivalent to original set

**Properties**:
- No extraneous attributes in FDs
- No redundant FDs
- All FDs have single attribute on RHS

**Algorithm**:
1. **Right Reduce**: Split FDs with multiple attributes on RHS
2. **Left Reduce**: Remove extraneous attributes from LHS
3. **Remove Redundant**: Remove FDs that can be derived from others

---

## 8. Normal Forms {#normal-forms}

### Purpose of Normalization

**Problems in Unnormalized Relations**:
- **Insertion Anomaly**: Cannot insert certain data without other data
- **Deletion Anomaly**: Deleting tuple loses other information
- **Update Anomaly**: Same information stored multiple times

### First Normal Form (1NF)

**Definition**: Relation is in 1NF if all attribute values are atomic

**Requirements**:
- No repeating groups
- No multi-valued attributes
- Each cell contains single value

**Example Violation**:
```
Student(ID, Name, Courses)
1, John, {Math, Physics}  // Multi-valued attribute
```

**1NF Solution**:
```
Student(ID, Name)
StudentCourse(StudentID, Course)
```

### Second Normal Form (2NF)

**Definition**: Relation is in 2NF if:
1. It is in 1NF
2. No non-prime attribute is partially dependent on candidate key

**Partial Dependency**: Non-prime attribute depends on proper subset of candidate key

**Example Violation**:
```
StudentCourse(StudentID, CourseID, StudentName, CourseName, Grade)
Key: {StudentID, CourseID}
StudentName depends only on StudentID (partial dependency)
```

**2NF Solution**:
```
Student(StudentID, StudentName)
Course(CourseID, CourseName)
Enrollment(StudentID, CourseID, Grade)
```

### Third Normal Form (3NF)

**Definition**: Relation is in 3NF if:
1. It is in 2NF
2. No non-prime attribute is transitively dependent on candidate key

**Transitive Dependency**: X → Y → Z (where Y is not a candidate key)

**Example Violation**:
```
Employee(EmpID, DeptID, DeptName)
EmpID → DeptID → DeptName (transitive dependency)
```

**3NF Solution**:
```
Employee(EmpID, DeptID)
Department(DeptID, DeptName)
```

### Boyce-Codd Normal Form (BCNF)

**Definition**: Relation is in BCNF if:
- For every non-trivial FD X → Y, X is a superkey

**Stronger than 3NF**: Every BCNF relation is in 3NF

**Example**: 3NF but not BCNF
```
StudentAdvisor(StudentID, Major, AdvisorID)
FDs: {StudentID, Major} → AdvisorID
     AdvisorID → Major
Key: {StudentID, Major}
```

**BCNF Violation**: AdvisorID → Major (AdvisorID is not superkey)

**BCNF Solution**:
```
StudentAdvisor(StudentID, AdvisorID)
AdvisorMajor(AdvisorID, Major)
```

### Fourth Normal Form (4NF)

**Multi-valued Dependency (MVD)**: X →→ Y
- **Meaning**: For each X value, there's a set of Y values independent of other attributes

**Definition**: Relation is in 4NF if:
1. It is in BCNF
2. No non-trivial multi-valued dependencies

**Example Violation**:
```
StudentSkillHobby(StudentID, Skill, Hobby)
StudentID →→ Skill (independent of Hobby)
StudentID →→ Hobby (independent of Skill)
```

**4NF Solution**:
```
StudentSkill(StudentID, Skill)
StudentHobby(StudentID, Hobby)
```

### Fifth Normal Form (5NF)

**Join Dependency**: Relation can be losslessly decomposed into multiple relations

**Definition**: Relation is in 5NF if it cannot be losslessly decomposed further

**Rare**: Most practical databases don't need 5NF

### Normalization Algorithm

**Step-by-Step Process**:

1. **Start with Universal Relation**: All attributes in one table
2. **Identify FDs**: Determine all functional dependencies
3. **Apply 1NF**: Remove multi-valued attributes
4. **Apply 2NF**: Remove partial dependencies
5. **Apply 3NF**: Remove transitive dependencies
6. **Apply BCNF**: Ensure all determinants are superkeys
7. **Check for MVDs**: Apply 4NF if needed

### Denormalization

**When to Denormalize**:
- **Performance**: Reduce joins for frequently accessed data
- **Read-heavy**: More reads than writes
- **Reporting**: Analytical queries need aggregated data

**Trade-offs**:
- **Pros**: Faster queries, simpler queries
- **Cons**: Data redundancy, update anomalies, storage overhead

---

## 9. File Organization {#file-organization}

### Storage Hierarchy

**Primary Storage** (Volatile):
- **CPU Registers**: Fastest, smallest
- **Cache Memory**: Very fast, small
- **Main Memory (RAM)**: Fast, moderate size

**Secondary Storage** (Non-volatile):
- **SSD**: Fast, expensive, limited writes
- **Hard Disk**: Slower, cheaper, unlimited writes
- **Optical Disk**: Slowest, cheapest, portable

### File Organization Methods

**1. Heap File Organization**

**Structure**: Records stored in order of insertion
**Advantages**:
- Simple insertion (append at end)
- Good for bulk loading
- No overhead for organization

**Disadvantages**:
- Linear search required
- No ordering
- Inefficient for searches

**Use Cases**: Log files, temporary data, bulk operations

**2. Sequential File Organization**

**Structure**: Records sorted by key attribute
**Advantages**:
- Efficient for range queries
- Binary search possible
- Good for batch processing

**Disadvantages**:
- Expensive insertions/deletions
- Requires reorganization
- Overflow handling needed

**Search Time**: O(log n) with binary search

**3. Hash File Organization**

**Structure**: Records placed based on hash function
**Hash Function**: h(key) = bucket address

**Advantages**:
- O(1) average search time
- Fast insertions/deletions
- Direct access to records

**Disadvantages**:
- No range queries
- Hash collisions
- Fixed bucket size

**Collision Handling**:
- **Open Addressing**: Linear probing, quadratic probing
- **Chaining**: Linked list in each bucket
- **Overflow Buckets**: Separate area for collisions

**4. Indexed File Organization**

**Structure**: Data file + index file
**Index**: Sorted list of (key, pointer) pairs

**Types**:
- **Primary Index**: On ordering key
- **Secondary Index**: On non-ordering key
- **Clustering Index**: On non-key ordering attribute

### Record Storage

**Fixed-Length Records**
```
Record 1: |Field1|Field2|Field3|
Record 2: |Field1|Field2|Field3|
Record 3: |Field1|Field2|Field3|
```

**Variable-Length Records**
```
Record: |Length|Field1|Separator|Field2|Separator|Field3|
```

**Slotted Page Structure**
```
|Header|Slot Directory|Free Space|Records|
```

### Block and Buffer Management

**Block**: Unit of data transfer between disk and memory
**Buffer**: Memory area for holding blocks
**Buffer Manager**: Manages buffer pool

**Buffer Replacement Policies**:
- **LRU**: Least Recently Used
- **FIFO**: First In, First Out
- **Clock**: Circular buffer with reference bits

**Pinning**: Prevent block from being replaced
**Dirty Bit**: Indicates if block was modified

---

## 10. Indexing (B and B+ Trees) {#indexing}

### Introduction to Indexing

**Index**: Data structure that improves query performance
**Trade-off**: Storage space and update overhead vs. query speed

**Types of Indexes**:
- **Primary**: On primary key
- **Secondary**: On non-key attributes
- **Clustered**: Data physically ordered by index key
- **Non-clustered**: Logical ordering only

### B-Tree Index

**Properties**:
- **Balanced**: All leaf nodes at same level
- **Multi-way**: Each node can have multiple keys
- **Sorted**: Keys in each node are sorted
- **Order m**: Maximum m children per node

**B-Tree Node Structure**:
```
|P1|K1|P2|K2|P3|...|Km-1|Pm|
```
- Ki: Keys (sorted)
- Pi: Pointers to child nodes or data records

**B-Tree Properties** (Order m):
1. Root has at least 2 children (unless it's a leaf)
2. Internal nodes have at least ⌈m/2⌉ children
3. Internal nodes have at most m children
4. Leaf nodes have at least ⌈m/2⌉-1 keys
5. Leaf nodes have at most m-1 keys
6. All leaves at same level

**B-Tree Operations**:

**Search Algorithm**:
```
1. Start at root
2. For current node:
   - Find position where key should be
   - If key found, return success
   - If leaf node and key not found, return failure
   - Otherwise, follow appropriate pointer to child
3. Repeat until found or reach leaf
```

**Insertion Algorithm**:
```
1. Find leaf node where key should be inserted
2. If leaf has space, insert key
3. If leaf is full:
   - Split leaf into two nodes
   - Promote middle key to parent
   - If parent is full, split parent recursively
   - May create new root (tree grows in height)
```

**Deletion Algorithm**:
```
1. Find and remove key
2. If node has enough keys, done
3. If node has too few keys:
   - Try to borrow from sibling
   - If sibling has minimum keys, merge with sibling
   - May require recursive merging up the tree
```

### B+ Tree Index

**Differences from B-Tree**:
- **Data only in leaves**: Internal nodes contain only keys
- **Leaf linking**: Leaves linked for sequential access
- **Key repetition**: Keys may appear in both internal and leaf nodes

**B+ Tree Node Structure**:

**Internal Node**:
```
|P1|K1|P2|K2|P3|...|Km-1|Pm|
```

**Leaf Node**:
```
|K1|D1|K2|D2|...|Kn|Dn|Next|
```
- Di: Data records or pointers to data
- Next: Pointer to next leaf node

**B+ Tree Properties**:
1. All data records in leaf nodes
2. Leaf nodes linked in sorted order
3. Internal nodes contain only routing information
4. More keys per internal node (better fanout)

**Advantages of B+ Trees**:
- **Range Queries**: Efficient sequential access through leaf links
- **Higher Fanout**: More keys per internal node
- **Consistent Performance**: All data access requires same number of I/Os
- **Better for Databases**: Most database systems use B+ trees

**B+ Tree Operations**:

**Range Query**:
```
1. Find starting leaf node
2. Follow leaf links until end condition
3. Collect all records in range
```

**Example B+ Tree** (Order 3):
```
        [10]
       /    \
    [5]      [15,20]
   /  \      /  |   \
[1,3] [5,7] [10,12] [15,18] [20,25]
```

### Index Selection

**Factors to Consider**:
- **Query Patterns**: Point queries vs. range queries
- **Update Frequency**: Read-heavy vs. write-heavy
- **Selectivity**: How many records match typical query
- **Storage Cost**: Index size vs. benefit

**Primary Index**:
- **One per table**: On primary key
- **Clustered**: Data physically ordered
- **Unique**: No duplicate keys

**Secondary Index**:
- **Multiple allowed**: On any attribute
- **Non-clustered**: Logical ordering only
- **May have duplicates**: Unless on unique attribute

**Composite Index**:
- **Multiple attributes**: (LastName, FirstName)
- **Order matters**: Leftmost prefix rule
- **Good for**: Multi-attribute queries

### Index Maintenance

**Insert Operation**:
1. Update data file
2. Update all relevant indexes
3. Handle index splits if necessary

**Delete Operation**:
1. Remove from data file
2. Remove from all indexes
3. Handle index merges if necessary

**Update Operation**:
1. If indexed attribute unchanged: No index update
2. If indexed attribute changed: Delete old + Insert new

**Index Reorganization**:
- **Periodic**: Rebuild indexes to optimize structure
- **Online**: Rebuild without blocking queries
- **Statistics**: Update for query optimizer

---

*Continue to Part 3 for Transactions, Concurrency Control, Recovery, and MCQ Questions*