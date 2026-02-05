# Spring Data JPA - Part 2: Relationships & Associations

[← Back to Index](README.md) | [← Previous: Part 1](03_Spring_Data_JPA_Part1.md) | [Next: Part 3 - Repositories →](05_Spring_Data_JPA_Part3.md)

## Table of Contents
- [Theory: Understanding Relationships](#theory-understanding-relationships)
- [@OneToOne](#onetoone)
- [@OneToMany & @ManyToOne](#onetomany--manytoone)
- [@ManyToMany](#manytomany)
- [Cascade Types](#cascade-types)
- [Fetch Types](#fetch-types)

---

## Theory: Understanding Relationships

### Database Relationships

**1. One-to-One (1:1)**
- One record in Table A relates to one record in Table B
- Example: User ↔ UserProfile
- Implementation: Foreign key with UNIQUE constraint

**2. One-to-Many (1:N)**
- One record in Table A relates to many records in Table B
- Example: User → Orders (one user has many orders)
- Implementation: Foreign key in "many" side

**3. Many-to-One (N:1)**
- Many records in Table A relate to one record in Table B
- Example: Orders → User (many orders belong to one user)
- Inverse of One-to-Many

**4. Many-to-Many (N:M)**
- Many records in Table A relate to many records in Table B
- Example: Students ↔ Courses
- Implementation: Join table with two foreign keys

### Bidirectional vs Unidirectional

**Unidirectional**:
- Navigation in one direction only
- Simpler, less memory
- Example: User knows Orders, but Order doesn't know User

**Bidirectional**:
- Navigation in both directions
- More convenient for queries
- Must designate "owning" side with mappedBy
- Example: User ↔ Orders (both know each other)

### Owning Side vs Inverse Side

**Owning Side**:
- Contains the foreign key
- Changes here are persisted
- Does NOT have mappedBy

**Inverse Side**:
- References the owning side
- Has mappedBy attribute
- Changes here are ignored

```java
// Owning side (has foreign key)
@ManyToOne
@JoinColumn(name = "user_id")
private User user;

// Inverse side (no foreign key)
@OneToMany(mappedBy = "user")
private List<Order> orders;
```

### N+1 Query Problem

**Problem**: Lazy loading causes multiple queries
```java
List<User> users = userRepository.findAll(); // 1 query
for (User user : users) {
    user.getOrders().size(); // N queries (one per user)
}
// Total: 1 + N queries
```

**Solutions**:
1. JOIN FETCH in JPQL
2. @EntityGraph
3. Batch fetching
4. DTO projections

### Cascade Operations

Cascade propagates operations from parent to child:
- **PERSIST**: Save parent → saves children
- **MERGE**: Update parent → updates children
- **REMOVE**: Delete parent → deletes children
- **REFRESH**: Reload parent → reloads children
- **DETACH**: Detach parent → detaches children
- **ALL**: All of the above

**orphanRemoval**: Deletes child when removed from collection

---

## @OneToOne

### Understanding @OneToOne

**Use Case**: One entity relates to exactly one other entity
- User ↔ UserProfile
- Person ↔ Passport
- Employee ↔ ParkingSpot

### 1. Unidirectional @OneToOne

**Owner side only knows about relationship**

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    @OneToOne(
        cascade = CascadeType.ALL,      // Cascade all operations
        fetch = FetchType.LAZY,         // Lazy load profile
        orphanRemoval = true            // Delete profile if removed
    )
    @JoinColumn(
        name = "profile_id",            // Foreign key column
        referencedColumnName = "id",    // References profile.id
        unique = true,                  // Ensures 1:1 relationship
        nullable = false                // Profile is required
    )
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bio;
    private String avatarUrl;
    // No reference to User
}
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255),
    profile_id BIGINT UNIQUE NOT NULL,  -- Foreign key with UNIQUE
    FOREIGN KEY (profile_id) REFERENCES user_profile(id)
);

CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY,
    bio TEXT,
    avatar_url VARCHAR(255)
);
```

**Usage**:
```java
UserProfile profile = new UserProfile();
profile.setBio("Software Engineer");

User user = new User();
user.setUsername("john");
user.setProfile(profile);

userRepository.save(user); // Saves both user and profile
```

### 2. Bidirectional @OneToOne

**Both sides know about relationship**

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;
    
    // Helper method to maintain bidirectional relationship
    public void setProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }
}

@Entity
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Owning side (has foreign key)
}
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255)
);

CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY,
    bio TEXT,
    user_id BIGINT UNIQUE NOT NULL,  -- Foreign key here
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

**Key Points**:
- `mappedBy = "user"` on User side (inverse side)
- `@JoinColumn` on UserProfile side (owning side)
- Foreign key is in user_profile table

### 3. Shared Primary Key @OneToOne

**Both entities share same primary key**

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @Id
    private Long id;  // Same as User.id
    
    @OneToOne
    @MapsId  // Use User's ID as this entity's ID
    @JoinColumn(name = "id")
    private User user;
}
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY
);

CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY,  -- Same value as user.id
    FOREIGN KEY (id) REFERENCES user(id)
);
```

**Benefits**:
- No extra foreign key column
- Guaranteed 1:1 relationship
- Saves storage space

**Usage**:
```java
User user = new User();
user = userRepository.save(user);  // Get generated ID

UserProfile profile = new UserProfile();
profile.setUser(user);  // ID automatically set from user
profileRepository.save(profile);
```

### 4. @OneToOne with @JoinTable

**Using join table (rare)**

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToOne
    @JoinTable(
        name = "user_profile_mapping",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "profile_id")
    )
    private UserProfile profile;
}
```

**Generated SQL**:
```sql
CREATE TABLE user_profile_mapping (
    user_id BIGINT UNIQUE,
    profile_id BIGINT UNIQUE,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (profile_id) REFERENCES user_profile(id)
);
```

### @OneToOne Lazy Loading Issue

**Problem**: @OneToOne is often EAGER even with FetchType.LAZY

```java
@OneToOne(fetch = FetchType.LAZY)
private UserProfile profile;  // Still loads eagerly!
```

**Why?**: Hibernate needs to know if profile is null

**Solutions**:

**1. Use optional=false**
```java
@OneToOne(fetch = FetchType.LAZY, optional = false)
private UserProfile profile;  // Now truly lazy
```

**2. Use @MapsId (shared primary key)**
```java
@OneToOne
@MapsId
private UserProfile profile;  // Lazy by default
```

**3. Make it bidirectional with mappedBy**
```java
@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
private UserProfile profile;  // Lazy works
```

### Best Practices

✅ **DO**:
- Use bidirectional for navigation from both sides
- Use shared primary key for true 1:1
- Set optional=false for lazy loading
- Use helper methods for bidirectional sync

❌ **DON'T**:
- Use @OneToOne for optional relationships (use @ManyToOne instead)
- Forget to set both sides in bidirectional
- Rely on LAZY without optional=false

### When to Use @OneToOne

✅ **Use @OneToOne when**:
- Truly 1:1 relationship
- Both entities always exist together
- Need to split large entity

❌ **Consider alternatives when**:
- Relationship is optional (use @ManyToOne)
- One side rarely accessed (use separate table)
- Performance critical (consider denormalization)

---

## @OneToMany & @ManyToOne

### Understanding the Relationship

**Most common relationship type**
- User has many Orders
- Department has many Employees
- Category has many Products

**Key Rule**: @ManyToOne side is ALWAYS the owning side (has foreign key)

### 1. Bidirectional @OneToMany (Recommended)

**Both sides know about relationship**

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    @OneToMany(
        mappedBy = "user",              // Field name in Order entity
        cascade = CascadeType.ALL,      // Cascade all operations
        fetch = FetchType.LAZY,         // Lazy load orders (default)
        orphanRemoval = true            // Delete orders when removed from list
    )
    private List<Order> orders = new ArrayList<>();
    
    // Helper methods to maintain bidirectional relationship
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);  // Set both sides
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);  // Clear both sides
    }
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private BigDecimal total;
    
    @ManyToOne(
        fetch = FetchType.LAZY,         // Lazy load user (recommended)
        optional = false                // User is required (NOT NULL)
    )
    @JoinColumn(
        name = "user_id",               // Foreign key column name
        nullable = false,               // NOT NULL constraint
        foreignKey = @ForeignKey(name = "fk_order_user")  // Custom FK name
    )
    private User user;  // Owning side (has foreign key)
}
```

**Generated SQL**:
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    total DECIMAL(10,2),
    user_id BIGINT NOT NULL,  -- Foreign key here
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE INDEX idx_order_user ON orders(user_id);  -- Auto-created
```

**Usage**:
```java
User user = new User();
user.setUsername("john");

Order order1 = new Order();
order1.setTotal(new BigDecimal("100.00"));

Order order2 = new Order();
order2.setTotal(new BigDecimal("200.00"));

user.addOrder(order1);  // Sets both sides
user.addOrder(order2);

userRepository.save(user);  // Saves user and all orders
```

### 2. Unidirectional @ManyToOne

**Only "many" side knows about relationship**

```java
@Entity
public class Order {
    @Id
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}

@Entity
public class User {
    @Id
    private Long id;
    // No reference to orders
}
```

**When to use**: When you only need to navigate from Order to User

### 3. Unidirectional @OneToMany (Not Recommended)

**Only "one" side knows about relationship**

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany
    @JoinColumn(name = "user_id")  // Foreign key in orders table
    private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {
    @Id
    private Long id;
    // No reference to user
}
```

**Problem**: Extra UPDATE statements
```sql
INSERT INTO orders (id, total) VALUES (1, 100.00);  -- Insert without user_id
UPDATE orders SET user_id = 1 WHERE id = 1;         -- Extra UPDATE!
```

**Better**: Use bidirectional @OneToMany/@ManyToOne

### 4. @OneToMany with @JoinTable

**Using join table (rare for @OneToMany)**

```java
@Entity
public class Department {
    @Id
    private Long id;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(
        name = "dept_employees",
        joinColumns = @JoinColumn(name = "dept_id"),
        inverseJoinColumns = @JoinColumn(name = "emp_id")
    )
    private List<Employee> employees = new ArrayList<>();
}
```

**Generated SQL**:
```sql
CREATE TABLE dept_employees (
    dept_id BIGINT,
    emp_id BIGINT,
    PRIMARY KEY (dept_id, emp_id)
);
```

**When to use**: When you can't add foreign key to child table

### Collection Types

```java
// List - Ordered, allows duplicates
@OneToMany(mappedBy = "user")
private List<Order> orders = new ArrayList<>();

// Set - Unordered, no duplicates (requires equals/hashCode)
@OneToMany(mappedBy = "user")
private Set<Order> orders = new HashSet<>();

// Map - Key-value pairs
@OneToMany(mappedBy = "user")
@MapKey(name = "id")  // Use order.id as key
private Map<Long, Order> ordersById = new HashMap<>();
```

**Recommendation**: Use List for most cases

### Ordering Collections

```java
// Order by column
@OneToMany(mappedBy = "user")
@OrderBy("createdDate DESC, id ASC")  // SQL ORDER BY
private List<Order> orders;

// Order by custom comparator
@OneToMany(mappedBy = "user")
@OrderColumn(name = "order_index")  // Maintains order in separate column
private List<Order> orders;
```

### Cascade Operations Explained

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<Order> orders;

// CascadeType.PERSIST
User user = new User();
Order order = new Order();
user.addOrder(order);
userRepository.save(user);  // Also saves order

// CascadeType.REMOVE
userRepository.delete(user);  // Also deletes all orders

// CascadeType.MERGE
User detachedUser = ...; // From cache/session
detachedUser.getOrders().get(0).setTotal(new BigDecimal("150.00"));
userRepository.save(detachedUser);  // Also updates order
```

### orphanRemoval Explained

```java
@OneToMany(mappedBy = "user", orphanRemoval = true)
private List<Order> orders;

User user = userRepository.findById(1L);
Order order = user.getOrders().get(0);
user.getOrders().remove(order);  // Remove from collection
userRepository.save(user);  // Order is DELETED from database
```

**Difference from CASCADE.REMOVE**:
- `CASCADE.REMOVE`: Deletes children when parent is deleted
- `orphanRemoval`: Deletes children when removed from collection

### Performance Considerations

**Problem**: Loading collections can be slow

```java
// BAD: Loads all orders eagerly
@OneToMany(fetch = FetchType.EAGER)
private List<Order> orders;

// GOOD: Lazy load (default)
@OneToMany(fetch = FetchType.LAZY)
private List<Order> orders;
```

**N+1 Query Problem**:
```java
List<User> users = userRepository.findAll();  // 1 query
for (User user : users) {
    user.getOrders().size();  // N queries
}
```

**Solutions**:

**1. JOIN FETCH**
```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
List<User> findAllWithOrders();
```

**2. @EntityGraph**
```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

**3. Batch Fetching**
```java
@OneToMany(mappedBy = "user")
@BatchSize(size = 10)  // Fetch 10 collections at once
private List<Order> orders;
```

### Best Practices

✅ **DO**:
- Use bidirectional @OneToMany/@ManyToOne
- Always use helper methods for bidirectional
- Use LAZY fetching (default)
- Use orphanRemoval for true parent-child
- Initialize collections (= new ArrayList<>())

❌ **DON'T**:
- Use unidirectional @OneToMany (extra UPDATEs)
- Use EAGER fetching
- Forget to set both sides in bidirectional
- Use Set without proper equals/hashCode
- Cascade REMOVE without careful consideration

### Common Patterns

**1. Parent-Child with Cascade**
```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Order> orders;
```

**2. Independent Entities**
```java
@OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
private List<Order> orders;  // Don't delete orders when user deleted
```

**3. Audit Trail**
```java
@OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
private List<AuditLog> auditLogs;  // Never delete audit logs
```

---

## @ManyToMany

### Understanding @ManyToMany

**Use Case**: Many entities relate to many other entities
- Students ↔ Courses (student takes many courses, course has many students)
- Authors ↔ Books (author writes many books, book has many authors)
- Users ↔ Roles (user has many roles, role assigned to many users)

**Implementation**: Join table with two foreign keys

### Bidirectional @ManyToMany

```java
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToMany(
        cascade = {CascadeType.PERSIST, CascadeType.MERGE},
        fetch = FetchType.LAZY
    )
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
    
    // Helper methods
    public void enrollCourse(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }
    
    public void dropCourse(Course course) {
        courses.remove(course);
        course.getStudents().remove(this);
    }
}

@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

**Generated SQL**:
```sql
CREATE TABLE student (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE course (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255)
);

CREATE TABLE student_course (
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id)
);
```

**Usage**:
```java
Student student = new Student();
student.setName("John");

Course course1 = new Course();
course1.setTitle("Math");

Course course2 = new Course();
course2.setTitle("Physics");

student.enrollCourse(course1);
student.enrollCourse(course2);

studentRepository.save(student);
```

### @ManyToMany with Extra Columns

**Problem**: Can't add extra columns to join table

```java
// Want to add: enrollment_date, grade
// Can't do this with @ManyToMany!
```

**Solution**: Convert to two @OneToMany with join entity

```java
@Entity
public class Student {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private Set<Enrollment> enrollments = new HashSet<>();
}

@Entity
public class Course {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private Set<Enrollment> enrollments = new HashSet<>();
}

@Entity
public class Enrollment {
    @EmbeddedId
    private EnrollmentId id;
    
    @ManyToOne
    @MapsId("studentId")
    private Student student;
    
    @ManyToOne
    @MapsId("courseId")
    private Course course;
    
    private LocalDate enrollmentDate;
    private String grade;
}

@Embeddable
public class EnrollmentId implements Serializable {
    private Long studentId;
    private Long courseId;
}
```

### Why Use Set Instead of List

```java
// BAD: List with @ManyToMany
@ManyToMany
private List<Course> courses = new ArrayList<>();
// Problem: Hibernate deletes ALL and re-inserts ALL on update

// GOOD: Set with @ManyToMany
@ManyToMany
private Set<Course> courses = new HashSet<>();
// Only inserts/deletes changed items
```

### equals() and hashCode() Required

```java
@Entity
public class Course {
    @Id
    private Long id;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        Course course = (Course) o;
        return id != null && id.equals(course.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

### Best Practices

✅ **DO**:
- Use Set, not List
- Implement equals/hashCode
- Use helper methods for both sides
- Avoid CASCADE.REMOVE

❌ **DON'T**:
- Use @ManyToMany with extra columns (use join entity)
- Use List (causes delete-all-insert-all)
- Forget to set both sides

---

## Cascade Types

### Understanding Cascade Operations

**Cascade**: Propagate operations from parent to child entities

### CascadeType.PERSIST

**What**: Save parent → saves children

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
private List<Order> orders;

User user = new User();
Order order = new Order();
user.addOrder(order);

userRepository.save(user);  // Also saves order
```

**Generated SQL**:
```sql
INSERT INTO user (id, username) VALUES (1, 'john');
INSERT INTO orders (id, user_id, total) VALUES (1, 1, 100.00);
```

### CascadeType.MERGE

**What**: Update parent → updates children

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.MERGE)
private List<Order> orders;

User detachedUser = getUserFromCache();
detachedUser.getOrders().get(0).setTotal(new BigDecimal("200.00"));

userRepository.save(detachedUser);  // Also updates order
```

### CascadeType.REMOVE

**What**: Delete parent → deletes children

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
private List<Order> orders;

userRepository.delete(user);  // Also deletes all orders
```

**Generated SQL**:
```sql
DELETE FROM orders WHERE user_id = 1;
DELETE FROM user WHERE id = 1;
```

### CascadeType.REFRESH

**What**: Reload parent → reloads children

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.REFRESH)
private List<Order> orders;

entityManager.refresh(user);  // Also refreshes orders from DB
```

### CascadeType.DETACH

**What**: Detach parent → detaches children

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.DETACH)
private List<Order> orders;

entityManager.detach(user);  // Also detaches orders
```

### CascadeType.ALL

**What**: All cascade operations

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<Order> orders;
// Equivalent to: {PERSIST, MERGE, REMOVE, REFRESH, DETACH}
```

### orphanRemoval

**What**: Delete child when removed from collection

```java
@OneToMany(mappedBy = "user", orphanRemoval = true)
private List<Order> orders;

User user = userRepository.findById(1L);
user.getOrders().remove(0);  // Remove from collection
userRepository.save(user);  // Order DELETED from DB
```

### orphanRemoval vs CASCADE.REMOVE

| Feature | orphanRemoval | CASCADE.REMOVE |
|---------|---------------|----------------|
| Delete parent | Deletes children | Deletes children |
| Remove from collection | Deletes child | Does nothing |
| Set to null | Deletes child | Does nothing |
| Use case | True parent-child | Dependent entities |

**Example**:
```java
// With orphanRemoval=true
user.getOrders().clear();  // Deletes all orders

// With CASCADE.REMOVE only
user.getOrders().clear();  // Orders remain in DB (orphaned)
userRepository.delete(user);  // Now deletes orders
```

### Best Practices

✅ **Use CascadeType.PERSIST, MERGE** for most cases
✅ **Use orphanRemoval=true** for true parent-child
❌ **Avoid CASCADE.REMOVE** unless truly dependent
❌ **Never use CASCADE.ALL** on @ManyToMany

---

## Fetch Types

### Understanding Fetch Types

**Fetch Type**: When to load related entities

### LAZY vs EAGER

| Aspect | LAZY | EAGER |
|--------|------|-------|
| Load time | On access | Immediately |
| Memory | Less | More |
| Queries | Multiple (N+1) | Single (JOIN) |
| Default | @OneToMany, @ManyToMany | @ManyToOne, @OneToOne |

### FetchType.LAZY

**What**: Load related entities only when accessed

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

User user = userRepository.findById(1L);  // SELECT user
user.getOrders().size();  // SELECT orders (now)
```

**Generated SQL**:
```sql
-- First query
SELECT * FROM user WHERE id = 1;

-- Second query (when orders accessed)
SELECT * FROM orders WHERE user_id = 1;
```

### FetchType.EAGER

**What**: Load related entities immediately

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.EAGER)
    private User user;
}

Order order = orderRepository.findById(1L);  // SELECT with JOIN
```

**Generated SQL**:
```sql
SELECT o.*, u.* 
FROM orders o 
LEFT JOIN user u ON o.user_id = u.id 
WHERE o.id = 1;
```

### N+1 Query Problem

**Problem**: LAZY loading causes multiple queries

```java
List<User> users = userRepository.findAll();  // 1 query
for (User user : users) {
    System.out.println(user.getOrders().size());  // N queries
}
// Total: 1 + N queries
```

**Generated SQL**:
```sql
SELECT * FROM user;  -- 1 query
SELECT * FROM orders WHERE user_id = 1;  -- Query 1
SELECT * FROM orders WHERE user_id = 2;  -- Query 2
SELECT * FROM orders WHERE user_id = 3;  -- Query 3
-- ... N queries
```

### Solution 1: JOIN FETCH

```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
List<User> findAllWithOrders();
```

**Generated SQL**:
```sql
SELECT u.*, o.* 
FROM user u 
LEFT JOIN orders o ON u.id = o.user_id;
-- Only 1 query!
```

### Solution 2: @EntityGraph

```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();

// Or with named graph
@Entity
@NamedEntityGraph(name = "User.orders",
    attributeNodes = @NamedAttributeNode("orders"))
public class User { }

@EntityGraph("User.orders")
List<User> findAll();
```

**Generated SQL**: Same as JOIN FETCH

### Solution 3: @BatchSize

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 10)
    private List<Order> orders;
}
```

**Generated SQL**:
```sql
SELECT * FROM user;  -- 1 query
SELECT * FROM orders WHERE user_id IN (1,2,3,4,5,6,7,8,9,10);  -- Batch 1
SELECT * FROM orders WHERE user_id IN (11,12,13,...);  -- Batch 2
-- Total: 1 + (N/10) queries
```

### @EntityGraph Deep Dive

**attributePaths**: Simple syntax
```java
@EntityGraph(attributePaths = {"orders", "profile"})
User findById(Long id);

// Nested paths
@EntityGraph(attributePaths = {"orders.items"})
User findById(Long id);
```

**EntityGraphType**:
```java
// FETCH: Load specified + EAGER attributes
@EntityGraph(attributePaths = {"orders"}, type = EntityGraphType.FETCH)

// LOAD: Load specified, others as defined
@EntityGraph(attributePaths = {"orders"}, type = EntityGraphType.LOAD)
```

### Performance Comparison

| Solution | Queries | Memory | Complexity |
|----------|---------|--------|------------|
| EAGER | 1 | High | Low |
| LAZY | 1+N | Low | Low |
| JOIN FETCH | 1 | Medium | Medium |
| @EntityGraph | 1 | Medium | Low |
| @BatchSize | 1+N/B | Low | Low |

### Best Practices

✅ **DO**:
- Use LAZY as default
- Use JOIN FETCH for specific queries
- Use @EntityGraph for flexibility
- Use @BatchSize for collections

❌ **DON'T**:
- Use EAGER everywhere
- Ignore N+1 problem
- Fetch unnecessary data

---

[← Previous: Part 1](03_Spring_Data_JPA_Part1.md) | [Next: Part 3 - Repositories →](05_Spring_Data_JPA_Part3.md)
