# Spring Data JPA - Part 1: Core Annotations & Entity Mapping

[← Back to Index](README.md) | [← Previous: Spring Boot](02_Spring_Boot.md) | [Next: Part 2 - Relationships →](04_Spring_Data_JPA_Part2.md)

## Table of Contents
- [Theory: Understanding JPA & ORM](#theory-understanding-jpa--orm)
- [@Entity & @Table](#entity--table)
- [@Id - Primary Key Strategies](#id---primary-key-strategies)
- [@Column - Column Mapping](#column---column-mapping)
- [Temporal Types](#temporal-types)
- [Enums & LOBs](#enums--lobs)

---

## Theory: Understanding JPA & ORM

### What is ORM (Object-Relational Mapping)?

**Problem**: Impedance mismatch between object-oriented and relational models
- Objects have inheritance, polymorphism
- Databases have tables, foreign keys
- Manual mapping is tedious and error-prone

**Solution**: ORM frameworks automatically map objects to tables

### What is JPA (Java Persistence API)?

**JPA is a specification**, not an implementation:
- Defines standard annotations (@Entity, @Id, etc.)
- Provides EntityManager API
- Standardizes query language (JPQL)

**Implementations**:
- Hibernate (most popular)
- EclipseLink
- OpenJPA

### Spring Data JPA Architecture

```
Application Code
       ↓
Spring Data JPA Repository
       ↓
JPA Provider (Hibernate)
       ↓
JDBC Driver
       ↓
Database
```

### Key Concepts

**1. Entity**
- Java class mapped to database table
- Must have @Entity annotation
- Must have primary key (@Id)

**2. EntityManager**
- Manages entity lifecycle
- Performs CRUD operations
- Handles transactions

**3. Persistence Context**
- Cache of managed entities
- First-level cache (session-scoped)
- Tracks entity changes

**4. Transaction Management**
- All database operations must be in transaction
- @Transactional annotation
- ACID properties guaranteed

### Entity States

```
Transient (new User())
    ↓ persist()
Managed (tracked by EntityManager)
    ↓ commit()
Detached (no longer tracked)
    ↓ merge()
Managed again
    ↓ remove()
Removed (marked for deletion)
```

### Benefits of Spring Data JPA

✅ **Reduces Boilerplate**
- No DAO implementation needed
- Query methods from method names
- Automatic CRUD operations

✅ **Database Independence**
- Switch databases without code changes
- Dialect handling automatic
- Portable queries (JPQL)

✅ **Performance Optimization**
- Lazy loading
- Caching (1st and 2nd level)
- Batch operations

---

## @Entity & @Table

### @Entity Annotation

**Purpose**: Marks a class as a JPA entity (database table)

```java
@Entity
public class User {
    // Mapped to table "user" by default (class name)
}

@Entity(name = "UserEntity") // Custom entity name for JPQL queries
public class User {
    // Still mapped to "user" table
}
```

**Requirements**:
- Must have no-arg constructor (can be protected)
- Cannot be final class
- Cannot have final methods/fields (for lazy loading)
- Must have @Id field

### @Table Annotation

**Purpose**: Customizes table mapping

```java
@Entity
@Table(
    name = "users",                    // Custom table name
    schema = "public",                 // Database schema
    catalog = "mydb",                  // Database catalog
    uniqueConstraints = {              // Unique constraints
        @UniqueConstraint(
            name = "uk_email",
            columnNames = {"email"}
        ),
        @UniqueConstraint(
            name = "uk_username_email",
            columnNames = {"username", "email"}
        )
    },
    indexes = {                        // Database indexes
        @Index(
            name = "idx_username",
            columnList = "username"
        ),
        @Index(
            name = "idx_email_created",
            columnList = "email, created_date DESC"
        )
    }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdDate;
}
```

**Generated SQL**:
```sql
CREATE TABLE public.users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255),
    email VARCHAR(255),
    created_date TIMESTAMP,
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username_email UNIQUE (username, email)
);

CREATE INDEX idx_username ON public.users(username);
CREATE INDEX idx_email_created ON public.users(email, created_date DESC);
```

**When to Use**:
- ✅ Table name differs from class name
- ✅ Need unique constraints
- ✅ Need database indexes for performance
- ✅ Multi-schema applications

---

## @Id - Primary Key Strategies

### Why Primary Keys Matter

**Purpose**: Uniquely identify each row in a table
- Required for entity identity
- Used in relationships (foreign keys)
- Affects performance (indexed by default)

### 1. IDENTITY Strategy (Auto-increment)

**How it works**: Database generates ID using auto-increment

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**Generated SQL** (MySQL/PostgreSQL):
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);
```

**Pros**:
- ✅ Simple and intuitive
- ✅ Database handles generation
- ✅ Sequential IDs

**Cons**:
- ❌ ID only available after INSERT
- ❌ Batch inserts less efficient
- ❌ Not portable (database-specific)

**Best for**: MySQL, PostgreSQL, SQL Server

### 2. SEQUENCE Strategy (Database Sequence)

**How it works**: Uses database sequence object

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(
    name = "user_seq",              // Generator name
    sequenceName = "user_sequence", // Database sequence name
    allocationSize = 50,            // Batch size (performance)
    initialValue = 1000             // Starting value
)
private Long id;
```

**Generated SQL** (Oracle/PostgreSQL):
```sql
CREATE SEQUENCE user_sequence START WITH 1000 INCREMENT BY 50;

INSERT INTO users (id, ...) VALUES (NEXT VALUE FOR user_sequence, ...);
```

**Pros**:
- ✅ ID available before INSERT (pre-allocation)
- ✅ Efficient batch inserts
- ✅ Portable across databases
- ✅ Can share sequence across tables

**Cons**:
- ❌ Gaps in IDs (due to allocation)
- ❌ Requires sequence support

**Best for**: Oracle, PostgreSQL, H2

**allocationSize Explained**:
```
Hibernate fetches 50 IDs at once:
Sequence: 1000, 1050, 1100...
Hibernate uses: 1000, 1001, 1002... 1049
Next fetch: 1050, 1051... 1099

Benefit: Reduces database calls (50 inserts = 1 sequence call)
```

### 3. TABLE Strategy (ID Table)

**How it works**: Separate table stores next ID

```java
@Id
@GeneratedValue(strategy = GenerationType.TABLE, generator = "user_gen")
@TableGenerator(
    name = "user_gen",
    table = "id_generator",         // ID table name
    pkColumnName = "gen_name",      // Entity name column
    valueColumnName = "gen_value",  // Next ID column
    pkColumnValue = "user_id",      // This entity's row
    allocationSize = 50
)
private Long id;
```

**Generated SQL**:
```sql
CREATE TABLE id_generator (
    gen_name VARCHAR(255) PRIMARY KEY,
    gen_value BIGINT
);

INSERT INTO id_generator (gen_name, gen_value) VALUES ('user_id', 1000);

-- On insert:
UPDATE id_generator SET gen_value = gen_value + 50 WHERE gen_name = 'user_id';
```

**Pros**:
- ✅ Works on any database
- ✅ Portable solution
- ✅ Can share across schemas

**Cons**:
- ❌ Slower (table locks)
- ❌ Scalability issues
- ❌ Extra table maintenance

**Best for**: Database-agnostic applications (rare use)

### 4. AUTO Strategy (JPA Chooses)

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;
```

**Behavior**: JPA provider selects strategy based on database
- MySQL/PostgreSQL → IDENTITY
- Oracle → SEQUENCE
- Others → TABLE

**Use when**: Don't care about specific strategy

### 5. UUID Strategy (Universally Unique Identifier)

**How it works**: Generates 128-bit unique identifier

```java
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(
    name = "UUID",
    strategy = "org.hibernate.id.UUIDGenerator"
)
@Column(updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
private UUID id;

// Or with Java UUID
@Id
@Column(columnDefinition = "BINARY(16)")
private UUID id = UUID.randomUUID();
```

**Generated Value**: `550e8400-e29b-41d4-a716-446655440000`

**Pros**:
- ✅ Globally unique (no collisions)
- ✅ Generated in application (no DB call)
- ✅ Distributed systems friendly
- ✅ Merge data from multiple sources
- ✅ Security (non-sequential)

**Cons**:
- ❌ Larger storage (36 chars vs 8 bytes)
- ❌ Slower index performance
- ❌ Not human-readable
- ❌ Random order (index fragmentation)

**Best for**: Distributed systems, microservices, security-sensitive apps

### 6. Composite Primary Key

**Use case**: Multiple columns form primary key

**Option A: @EmbeddedId**
```java
@Embeddable
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
    
    // Must override equals() and hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId)) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(productId, that.productId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId);
    }
}

@Entity
public class OrderItem {
    @EmbeddedId
    private OrderItemId id;
    
    private Integer quantity;
    private BigDecimal price;
}

// Usage
OrderItemId id = new OrderItemId();
id.setOrderId(1L);
id.setProductId(100L);

OrderItem item = new OrderItem();
item.setId(id);
```

**Option B: @IdClass**
```java
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
    
    // equals() and hashCode() required
}

@Entity
@IdClass(OrderItemId.class)
public class OrderItem {
    @Id
    private Long orderId;
    
    @Id
    private Long productId;
    
    private Integer quantity;
}

// Usage (simpler)
OrderItem item = new OrderItem();
item.setOrderId(1L);
item.setProductId(100L);
```

**Comparison**:

| Aspect | @EmbeddedId | @IdClass |
|--------|------------|----------|
| Encapsulation | Better (single object) | Fields scattered |
| JPQL Queries | `WHERE o.id.orderId = 1` | `WHERE o.orderId = 1` |
| Simplicity | More code | Less code |
| Recommendation | Preferred | Legacy code |

### Primary Key Strategy Comparison

| Strategy | Performance | Portability | Batch Insert | Use Case |
|----------|------------|-------------|--------------|----------|
| IDENTITY | Good | Medium | Poor | Simple apps, MySQL |
| SEQUENCE | Excellent | Good | Excellent | High-performance, Oracle |
| TABLE | Poor | Excellent | Medium | Database-agnostic |
| UUID | Good | Excellent | Excellent | Distributed systems |
| Composite | Good | Excellent | Good | Many-to-many join tables |

### Best Practices

✅ **DO**:
- Use SEQUENCE for high-performance apps
- Use UUID for distributed systems
- Use IDENTITY for simple applications
- Use Composite for join tables
- Set allocationSize for SEQUENCE (50-100)

❌ **DON'T**:
- Use TABLE strategy (performance issues)
- Use business data as primary key
- Change primary key values
- Use String as primary key (unless UUID)

---

## @Column - Column Mapping

### Purpose

Customizes how entity fields map to database columns.

### Basic Usage

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    // Default: column name = field name
    private String name; // Maps to "name" column
    
    // Custom column name
    @Column(name = "product_name")
    private String productName; // Maps to "product_name" column
}
```

### All @Column Attributes

```java
@Column(
    name = "product_name",              // Column name in database
    nullable = false,                   // NOT NULL constraint
    unique = true,                      // UNIQUE constraint
    length = 100,                       // VARCHAR(100) for String
    precision = 10,                     // Total digits for BigDecimal
    scale = 2,                          // Decimal places for BigDecimal
    insertable = true,                  // Include in INSERT statements
    updatable = true,                   // Include in UPDATE statements
    columnDefinition = "VARCHAR(100) DEFAULT 'Unknown'", // Custom SQL
    table = "products"                  // For secondary tables
)
private String name;
```

### Detailed Attribute Explanations

#### 1. name
```java
@Column(name = "user_email")
private String email; // Database column: user_email
```

#### 2. nullable
```java
@Column(nullable = false) // NOT NULL constraint
private String username;

@Column(nullable = true)  // NULL allowed (default)
private String middleName;
```

**Generated SQL**:
```sql
CREATE TABLE users (
    username VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255)
);
```

#### 3. unique
```java
@Column(unique = true) // UNIQUE constraint
private String email;
```

**Generated SQL**:
```sql
CREATE TABLE users (
    email VARCHAR(255) UNIQUE
);
```

**Note**: For composite unique constraints, use @Table(uniqueConstraints)

#### 4. length (String fields)
```java
@Column(length = 50)   // VARCHAR(50)
private String username;

@Column(length = 500)  // VARCHAR(500)
private String bio;

@Column(length = 10000) // TEXT (if supported)
private String description;
```

**Default**: 255 characters

#### 5. precision & scale (Numeric fields)
```java
@Column(precision = 10, scale = 2) // DECIMAL(10,2)
private BigDecimal price; // Max: 99999999.99

@Column(precision = 19, scale = 4) // DECIMAL(19,4)
private BigDecimal amount; // Max: 999999999999999.9999
```

**precision**: Total number of digits
**scale**: Digits after decimal point

**Example**:
- precision=10, scale=2 → 12345678.90 ✅
- precision=10, scale=2 → 123456789.00 ❌ (11 digits)

#### 6. insertable & updatable
```java
@Column(insertable = false, updatable = false)
private String calculatedField; // Read-only from database

@Column(updatable = false)
private LocalDateTime createdDate; // Set once, never updated

@Column(insertable = false)
private LocalDateTime lastModified; // Updated by database trigger
```

**Use cases**:
- Database-generated values
- Audit fields managed by triggers
- Derived/calculated columns

#### 7. columnDefinition
```java
// Custom SQL type
@Column(columnDefinition = "TEXT")
private String longText;

// Default value
@Column(columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
private String status;

// JSON column (PostgreSQL)
@Column(columnDefinition = "jsonb")
private String metadata;

// Timestamp with timezone
@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
private LocalDateTime createdAt;
```

**Warning**: Database-specific, reduces portability

### Common Patterns

#### Email Field
```java
@Column(name = "email", nullable = false, unique = true, length = 100)
private String email;
```

#### Price Field
```java
@Column(name = "price", nullable = false, precision = 10, scale = 2)
private BigDecimal price;
```

#### Description Field
```java
@Column(name = "description", length = 1000)
private String description;
```

#### Audit Fields
```java
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;
```

### When @Column is Optional

```java
@Entity
public class User {
    @Id
    private Long id;
    
    // No @Column needed - uses defaults
    private String username;     // Column: username, VARCHAR(255), nullable
    private Integer age;         // Column: age, INTEGER, nullable
    private LocalDate birthDate; // Column: birth_date, DATE, nullable
}
```

**Use @Column when**:
- Custom column name needed
- Constraints required (nullable, unique)
- Custom length/precision needed
- Special SQL type needed

### Best Practices

✅ **DO**:
- Use nullable=false for required fields
- Set appropriate length for Strings
- Use precision/scale for money fields
- Mark audit fields as updatable=false

❌ **DON'T**:
- Overuse columnDefinition (reduces portability)
- Use unique=true for composite keys (use @Table)
- Forget to set length for large text
- Use String for money (use BigDecimal)

---

## Temporal Types

### Purpose

Map Java date/time types to database date/time columns.

### Old Date API (java.util.Date)

**Requires @Temporal annotation**

```java
import java.util.Date;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class Event {
    @Id
    private Long id;
    
    // DATE: Only date (yyyy-MM-dd)
    @Temporal(TemporalType.DATE)
    private Date birthDate; // Stores: 1990-05-15
    
    // TIME: Only time (HH:mm:ss)
    @Temporal(TemporalType.TIME)
    private Date startTime; // Stores: 14:30:00
    
    // TIMESTAMP: Date + time
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt; // Stores: 1990-05-15 14:30:00.123
}
```

**Generated SQL**:
```sql
CREATE TABLE event (
    id BIGINT PRIMARY KEY,
    birth_date DATE,              -- Only date
    start_time TIME,              -- Only time
    created_at TIMESTAMP          -- Date + time
);
```

### Java 8+ Date/Time API (Recommended)

**No @Temporal needed** - JPA 2.2+ supports automatically

```java
import java.time.*;

@Entity
public class Event {
    @Id
    private Long id;
    
    // Date only (yyyy-MM-dd)
    private LocalDate eventDate;           // 2024-02-05
    
    // Time only (HH:mm:ss.nnnnnnnnn)
    private LocalTime startTime;           // 14:30:00.123456789
    
    // Date + Time (no timezone)
    private LocalDateTime createdAt;       // 2024-02-05T14:30:00.123
    
    // Date + Time + Timezone
    private ZonedDateTime scheduledAt;     // 2024-02-05T14:30:00.123+05:30[Asia/Kolkata]
    
    // Date + Time + Offset
    private OffsetDateTime publishedAt;    // 2024-02-05T14:30:00.123+05:30
    
    // Timestamp (milliseconds since epoch)
    private Instant timestamp;             // 2024-02-05T09:00:00Z
    
    // Duration (time span)
    private Duration duration;             // PT2H30M (2 hours 30 minutes)
    
    // Period (date span)
    private Period period;                 // P1Y2M3D (1 year 2 months 3 days)
}
```

**Generated SQL** (PostgreSQL):
```sql
CREATE TABLE event (
    id BIGINT PRIMARY KEY,
    event_date DATE,
    start_time TIME,
    created_at TIMESTAMP,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    timestamp TIMESTAMP,
    duration BIGINT,              -- Stored as nanoseconds
    period VARCHAR(255)           -- Stored as ISO-8601 string
);
```

### Type Comparison

| Java Type | Database Type | Contains | Example |
|-----------|--------------|----------|----------|
| LocalDate | DATE | Date only | 2024-02-05 |
| LocalTime | TIME | Time only | 14:30:00 |
| LocalDateTime | TIMESTAMP | Date + Time | 2024-02-05 14:30:00 |
| ZonedDateTime | TIMESTAMP | Date + Time + Zone | 2024-02-05 14:30:00 Asia/Kolkata |
| OffsetDateTime | TIMESTAMP | Date + Time + Offset | 2024-02-05 14:30:00 +05:30 |
| Instant | TIMESTAMP | UTC timestamp | 2024-02-05 09:00:00 UTC |

### When to Use Each Type

**LocalDate** - Date without time
```java
private LocalDate birthDate;      // Birthday
private LocalDate expiryDate;     // Expiry date
private LocalDate holidayDate;    // Holiday
```

**LocalTime** - Time without date
```java
private LocalTime openingTime;    // Store opens at 09:00
private LocalTime closingTime;    // Store closes at 21:00
private LocalTime alarmTime;      // Daily alarm
```

**LocalDateTime** - Date + Time (no timezone)
```java
private LocalDateTime createdAt;  // Record creation
private LocalDateTime updatedAt;  // Record update
private LocalDateTime eventTime;  // Event in local time
```

**Use when**: All users in same timezone or timezone doesn't matter

**Instant** - UTC timestamp (Recommended for distributed systems)
```java
private Instant createdAt;        // Record creation (UTC)
private Instant lastLogin;        // Last login time (UTC)
private Instant processedAt;      // Processing time (UTC)
```

**Use when**: 
- Distributed systems
- Multiple timezones
- Need absolute point in time
- Microservices

**ZonedDateTime** - Date + Time + Timezone
```java
private ZonedDateTime meetingTime; // Meeting: 2024-02-05 14:00 Asia/Kolkata
private ZonedDateTime flightTime;  // Flight: 2024-02-05 10:30 America/New_York
```

**Use when**: Need to preserve timezone information

### Common Patterns

#### Audit Fields
```java
@Entity
public class AuditableEntity {
    @Column(updatable = false)
    private Instant createdAt;    // UTC timestamp
    
    private Instant updatedAt;    // UTC timestamp
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

#### Event Scheduling
```java
@Entity
public class Event {
    private LocalDate eventDate;           // Which day
    private LocalTime startTime;           // What time
    private Duration duration;             // How long
    
    public LocalDateTime getStartDateTime() {
        return LocalDateTime.of(eventDate, startTime);
    }
    
    public LocalDateTime getEndDateTime() {
        return getStartDateTime().plus(duration);
    }
}
```

### Timezone Handling Best Practices

✅ **DO**:
- Use Instant for timestamps in distributed systems
- Store in UTC, convert to local timezone in UI
- Use ZonedDateTime when timezone matters
- Use LocalDateTime for timezone-agnostic data

❌ **DON'T**:
- Mix old Date API with new API
- Store timezone in separate field
- Use LocalDateTime for global applications
- Forget to handle daylight saving time

### Migration from Old to New API

```java
// Old (java.util.Date)
@Temporal(TemporalType.TIMESTAMP)
private Date createdAt;

// New (java.time.Instant) - Recommended
private Instant createdAt;

// Conversion
Date oldDate = new Date();
Instant instant = oldDate.toInstant();
Date newDate = Date.from(instant);
```

---

## Enums & LOBs

### Enums - Mapping Enumerated Types

#### Why Use Enums?

**Benefits**:
- ✅ Type safety (compile-time checking)
- ✅ Limited set of values
- ✅ Self-documenting code
- ✅ IDE autocomplete

**Example**:
```java
public enum Status {
    ACTIVE, INACTIVE, DELETED, SUSPENDED
}

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}
```

#### @Enumerated Annotation

**Two Strategies**: STRING (Recommended) vs ORDINAL

### 1. EnumType.STRING (Recommended)

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status; // Stores: "ACTIVE", "INACTIVE", etc.
}
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    status VARCHAR(20)  -- Stores string value
);

INSERT INTO user (id, status) VALUES (1, 'ACTIVE');
```

**Pros**:
- ✅ Human-readable in database
- ✅ Safe to reorder enum values
- ✅ Safe to add new values
- ✅ Easy debugging

**Cons**:
- ❌ More storage (string vs integer)
- ❌ Slightly slower

### 2. EnumType.ORDINAL (Not Recommended)

```java
@Enumerated(EnumType.ORDINAL)
private Status status; // Stores: 0, 1, 2, 3
```

**Generated SQL**:
```sql
CREATE TABLE user (
    status INTEGER  -- Stores ordinal position
);

INSERT INTO user (id, status) VALUES (1, 0); -- 0 = ACTIVE
```

**Mapping**:
```java
ACTIVE = 0
INACTIVE = 1
DELETED = 2
SUSPENDED = 3
```

**Pros**:
- ✅ Less storage
- ✅ Faster

**Cons**:
- ❌ Breaks if enum order changes
- ❌ Not human-readable
- ❌ Dangerous for maintenance

**Example of Problem**:
```java
// Original enum
enum Status { ACTIVE, INACTIVE, DELETED }  // ACTIVE=0, INACTIVE=1, DELETED=2

// Later, someone adds PENDING at the beginning
enum Status { PENDING, ACTIVE, INACTIVE, DELETED }  // PENDING=0, ACTIVE=1...

// Now all existing data is corrupted!
// Database value 0 was ACTIVE, now it's PENDING
```

### Custom Enum Mapping (Advanced)

```java
public enum Status {
    ACTIVE("A"),
    INACTIVE("I"),
    DELETED("D");
    
    private final String code;
    
    Status(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}

@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Status, String> {
    
    @Override
    public String convertToDatabaseColumn(Status status) {
        return status == null ? null : status.getCode();
    }
    
    @Override
    public Status convertToEntityAttribute(String code) {
        if (code == null) return null;
        return Arrays.stream(Status.values())
            .filter(s -> s.getCode().equals(code))
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}

@Entity
public class User {
    @Convert(converter = StatusConverter.class)
    private Status status; // Stores: "A", "I", "D"
}
```

**Benefits**: Compact storage + safe enum changes

---

### LOBs - Large Objects

#### What are LOBs?

**LOB = Large Object**
- **CLOB**: Character Large Object (text)
- **BLOB**: Binary Large Object (files)

#### @Lob Annotation

### 1. CLOB - Text Data

```java
@Entity
public class Article {
    @Id
    private Long id;
    
    @Lob
    @Column(columnDefinition = "TEXT")  // PostgreSQL
    private String content;  // Large text content
    
    @Lob
    private String description;  // Auto-mapped to CLOB/TEXT
}
```

**Generated SQL** (PostgreSQL):
```sql
CREATE TABLE article (
    id BIGINT PRIMARY KEY,
    content TEXT,           -- Unlimited length
    description TEXT
);
```

**Generated SQL** (MySQL):
```sql
CREATE TABLE article (
    content LONGTEXT,       -- Up to 4GB
    description LONGTEXT
);
```

**Use cases**:
- Article content
- Blog posts
- Comments
- JSON data
- XML data

### 2. BLOB - Binary Data

```java
@Entity
public class Document {
    @Id
    private Long id;
    
    @Lob
    private byte[] fileData;  // File content
    
    @Lob
    @Column(columnDefinition = "BYTEA")  // PostgreSQL
    private byte[] thumbnail;  // Image thumbnail
    
    private String fileName;
    private String mimeType;
    private Long fileSize;
}
```

**Generated SQL**:
```sql
CREATE TABLE document (
    id BIGINT PRIMARY KEY,
    file_data BLOB,         -- Binary data
    thumbnail BYTEA,        -- PostgreSQL binary
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size BIGINT
);
```

**Use cases**:
- Images
- PDFs
- Videos
- Audio files
- Encrypted data

### LOB Performance Considerations

**Problems**:
- ❌ Slow to load (large data)
- ❌ Memory intensive
- ❌ Not indexed
- ❌ Backup/restore slow

**Solutions**:

**1. Lazy Loading**
```java
@Lob
@Basic(fetch = FetchType.LAZY)  // Don't load unless accessed
private byte[] fileData;
```

**2. Separate Table**
```java
@Entity
public class Document {
    @Id
    private Long id;
    private String fileName;
    
    @OneToOne(fetch = FetchType.LAZY)
    private DocumentContent content;  // Separate table
}

@Entity
public class DocumentContent {
    @Id
    private Long id;
    
    @Lob
    private byte[] data;
}
```

**3. External Storage (Recommended)**
```java
@Entity
public class Document {
    @Id
    private Long id;
    
    private String fileName;
    private String s3Key;        // Store in S3/Cloud Storage
    private String s3Bucket;
    
    // No @Lob - just reference
}
```

---

### @Transient - Exclude from Persistence

```java
@Entity
public class User {
    @Id
    private Long id;
    
    private String firstName;
    private String lastName;
    
    @Transient  // Not stored in database
    private String fullName;
    
    @Transient  // Calculated field
    private int age;
    
    private LocalDate birthDate;
    
    // Calculated at runtime
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
```

**Use cases**:
- Calculated fields
- Temporary data
- Helper fields
- Derived values

---

### @Version - Optimistic Locking

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    @Version  // Automatically managed by JPA
    private Long version;
}
```

**How it works**:
```java
// User A reads product (version = 1)
Product product = productRepository.findById(1L);
product.setPrice(new BigDecimal("100.00"));

// User B reads same product (version = 1)
Product product2 = productRepository.findById(1L);
product2.setPrice(new BigDecimal("150.00"));

// User A saves (version 1 → 2) ✅
productRepository.save(product);

// User B tries to save (version still 1) ❌
productRepository.save(product2);  // Throws OptimisticLockException
```

**Generated SQL**:
```sql
-- User A's update
UPDATE product 
SET price = 100.00, version = 2 
WHERE id = 1 AND version = 1;  -- Success

-- User B's update
UPDATE product 
SET price = 150.00, version = 2 
WHERE id = 1 AND version = 1;  -- Fails (version is now 2)
```

**Benefits**:
- ✅ Prevents lost updates
- ✅ No database locks
- ✅ Better concurrency
- ✅ Automatic management

**Use when**: Multiple users can update same record

---

### Best Practices Summary

✅ **DO**:
- Use EnumType.STRING for enums
- Use @Transient for calculated fields
- Use @Version for concurrent updates
- Store large files externally (S3)
- Use lazy loading for LOBs

❌ **DON'T**:
- Use EnumType.ORDINAL (dangerous)
- Store large files in database
- Forget @Version for concurrent entities
- Load LOBs eagerly
- Use String for large text (use @Lob)

---

[← Previous: Spring Boot](02_Spring_Boot.md) | [Next: Part 2 - Relationships →](04_Spring_Data_JPA_Part2.md)
