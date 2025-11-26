# hashCode() and equals() Contract

## Table of Contents
1. [Introduction](#introduction)
2. [The Contract Rules](#the-contract-rules)
3. [Why This Contract Exists](#why-this-contract-exists)
4. [Correct Implementation](#correct-implementation)
5. [Contract Violations](#contract-violations)
6. [Real-World Examples](#real-world-examples)
7. [Best Practices](#best-practices)
8. [Common Pitfalls](#common-pitfalls)

---

## Introduction

### What is the Contract?

The **hashCode() and equals() contract** is a fundamental rule in Java that defines the relationship between these two methods.

**Simple Statement:**
> If two objects are equal according to equals(), they MUST have the same hashCode().

```java
if (obj1.equals(obj2) == true) {
    // This MUST be true
    obj1.hashCode() == obj2.hashCode()
}
```

**However, the reverse is NOT required:**
```java
if (obj1.hashCode() == obj2.hashCode()) {
    // obj1.equals(obj2) can be true OR false
    // Hash collision is allowed
}
```

---

## The Contract Rules

### Rule 1: Consistency

**hashCode() must consistently return the same value during execution if the object doesn't change.**

```java
Person p = new Person("John", 25);

int hash1 = p.hashCode();  // 12345
int hash2 = p.hashCode();  // 12345 (MUST be same)
int hash3 = p.hashCode();  // 12345 (MUST be same)

// All calls must return same value
assert hash1 == hash2 && hash2 == hash3;
```

**Violation Example:**
```java
class BadPerson {
    String name;
    
    @Override
    public int hashCode() {
        return new Random().nextInt();  // ❌ WRONG! Returns different value each time
    }
}

BadPerson p = new BadPerson("John");
System.out.println(p.hashCode());  // 12345
System.out.println(p.hashCode());  // 67890 (different!)
```

---

### Rule 2: Equality Implies Same Hash

**If equals() returns true, hashCode() MUST return the same value.**

```java
Person p1 = new Person("John", 25);
Person p2 = new Person("John", 25);

if (p1.equals(p2)) {  // true
    // This MUST be true
    assert p1.hashCode() == p2.hashCode();
}
```

**This is the MOST IMPORTANT rule.**

---

### Rule 3: Hash Collision is Allowed

**Different objects (not equal) can have the same hashCode().**

```java
Person p1 = new Person("John", 25);
Person p2 = new Person("Jane", 30);

// This is ALLOWED (but not ideal for performance)
p1.hashCode() == p2.hashCode()  // true (collision)
p1.equals(p2)                    // false

// Not a violation, just poor hash distribution
```

---

### Rule 4: null Handling

**equals() must handle null properly.**

```java
Person p = new Person("John", 25);

// Must return false, not throw exception
assert !p.equals(null);
```

---

### Rule 5: Reflexive, Symmetric, Transitive

**equals() must satisfy these properties:**

```java
Person p1 = new Person("John", 25);
Person p2 = new Person("John", 25);
Person p3 = new Person("John", 25);

// Reflexive: x.equals(x) must be true
assert p1.equals(p1);

// Symmetric: x.equals(y) == y.equals(x)
assert p1.equals(p2) == p2.equals(p1);

// Transitive: if x.equals(y) and y.equals(z), then x.equals(z)
assert p1.equals(p2) && p2.equals(p3) && p1.equals(p3);
```

---

## Why This Contract Exists

### HashMap/HashSet Depend On It

HashMap uses **both** hashCode() and equals() to store and retrieve entries:

```java
Map<Person, String> map = new HashMap<>();
Person p1 = new Person("John", 25);
map.put(p1, "Engineer");

Person p2 = new Person("John", 25);  // Equal to p1
String job = map.get(p2);  // Should return "Engineer"
```

**How HashMap Works:**

```
Step 1: Calculate hashCode()
p2.hashCode() → 12345

Step 2: Find bucket using hash
bucket_index = 12345 % 16 = 5

Step 3: In bucket[5], use equals() to find exact match
bucket[5]: [p1, "Engineer"] → [p3, "Doctor"] → null
           p2.equals(p1)? YES! → Return "Engineer"
```

**If Contract is Violated:**

```java
// Violation: equals() overridden, hashCode() NOT overridden
class BadPerson {
    String name;
    
    @Override
    public boolean equals(Object obj) {
        return name.equals(((BadPerson) obj).name);
    }
    // hashCode() uses Object's default (memory address)
}

Map<BadPerson, String> map = new HashMap<>();
BadPerson p1 = new BadPerson("John");
map.put(p1, "Engineer");

BadPerson p2 = new BadPerson("John");
System.out.println(map.get(p2));  // null ❌ (expected "Engineer")

// Why?
// p1.hashCode() = 12345 (Object's default based on memory)
// p2.hashCode() = 67890 (different object, different memory)
// Different hashCodes → different buckets → not found!
```

---

## Correct Implementation

### Example 1: Simple Person Class

```java
class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public boolean equals(Object obj) {
        // Step 1: Check if same reference
        if (this == obj) return true;
        
        // Step 2: Check if null or different class
        if (obj == null || getClass() != obj.getClass()) return false;
        
        // Step 3: Cast and compare fields
        Person person = (Person) obj;
        return age == person.age && 
               Objects.equals(name, person.name);
    }
    
    @Override
    public int hashCode() {
        // Use same fields as equals()
        return Objects.hash(name, age);
    }
}
```

**Testing:**
```java
Person p1 = new Person("John", 25);
Person p2 = new Person("John", 25);
Person p3 = new Person("Jane", 30);

// Test equals
System.out.println(p1.equals(p2));  // true
System.out.println(p1.equals(p3));  // false

// Test hashCode contract
System.out.println(p1.hashCode() == p2.hashCode());  // true ✓
System.out.println(p1.hashCode() == p3.hashCode());  // false (probably)

// Test with HashMap
Map<Person, String> map = new HashMap<>();
map.put(p1, "Engineer");
System.out.println(map.get(p2));  // "Engineer" ✓
```

---

### Example 2: Employee with ID

```java
class Employee {
    private Long id;
    private String name;
    private String department;
    
    public Employee(Long id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        // Only compare ID (natural key)
        return Objects.equals(id, employee.id);
    }
    
    @Override
    public int hashCode() {
        // Use same field as equals()
        return Objects.hash(id);
    }
}
```

**Testing:**
```java
Employee e1 = new Employee(1L, "John", "IT");
Employee e2 = new Employee(1L, "John Smith", "Engineering");  // Same ID, different name

System.out.println(e1.equals(e2));  // true (same ID)
System.out.println(e1.hashCode() == e2.hashCode());  // true ✓
```

---

### Example 3: Composite Key

```java
class OrderItem {
    private String orderId;
    private String productId;
    private int quantity;
    
    public OrderItem(String orderId, String productId, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        OrderItem item = (OrderItem) obj;
        // Composite key: orderId + productId (not quantity)
        return Objects.equals(orderId, item.orderId) &&
               Objects.equals(productId, item.productId);
    }
    
    @Override
    public int hashCode() {
        // Use same fields as equals()
        return Objects.hash(orderId, productId);
    }
}
```

**Testing:**
```java
OrderItem item1 = new OrderItem("ORD-123", "PROD-456", 5);
OrderItem item2 = new OrderItem("ORD-123", "PROD-456", 10);  // Different quantity

System.out.println(item1.equals(item2));  // true (same order + product)
System.out.println(item1.hashCode() == item2.hashCode());  // true ✓
```

---

## Contract Violations

### Violation 1: Override equals() but NOT hashCode()

```java
class BadPerson {
    String name;
    int age;
    
    // Override equals only
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BadPerson) {
            BadPerson p = (BadPerson) obj;
            return name.equals(p.name) && age == p.age;
        }
        return false;
    }
    
    // hashCode() NOT overridden - uses Object.hashCode()
    // ❌ VIOLATES CONTRACT!
}
```

**Problem:**
```java
BadPerson p1 = new BadPerson("John", 25);
BadPerson p2 = new BadPerson("John", 25);

System.out.println(p1.equals(p2));  // true
System.out.println(p1.hashCode() == p2.hashCode());  // false ❌ VIOLATION!

// HashMap breaks
Map<BadPerson, String> map = new HashMap<>();
map.put(p1, "Engineer");
System.out.println(map.get(p2));  // null ❌ (expected "Engineer")

// Why? Different hashCodes → different buckets → not found
```

---

### Violation 2: Override hashCode() but NOT equals()

```java
class BadPerson {
    String name;
    int age;
    
    // equals() NOT overridden - uses Object.equals() (reference equality)
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

**Problem:**
```java
BadPerson p1 = new BadPerson("John", 25);
BadPerson p2 = new BadPerson("John", 25);

System.out.println(p1.equals(p2));  // false (different references)
System.out.println(p1.hashCode() == p2.hashCode());  // true

// HashMap behavior
Map<BadPerson, String> map = new HashMap<>();
map.put(p1, "Engineer");
System.out.println(map.get(p2));  // null ❌

// Why? Same bucket, but equals() returns false → not found
```

---

### Violation 3: Inconsistent Fields

```java
class BadPerson {
    String name;
    int age;
    
    @Override
    public boolean equals(Object obj) {
        BadPerson p = (BadPerson) obj;
        // Uses BOTH name and age
        return name.equals(p.name) && age == p.age;
    }
    
    @Override
    public int hashCode() {
        // Uses ONLY name (inconsistent!)
        return name.hashCode();  // ❌ WRONG!
    }
}
```

**Problem:**
```java
BadPerson p1 = new BadPerson("John", 25);
BadPerson p2 = new BadPerson("John", 30);

System.out.println(p1.equals(p2));  // false (different age)
System.out.println(p1.hashCode() == p2.hashCode());  // true (same name)

// Not a contract violation, but poor performance
// Many collisions → slower HashMap operations
```

---

### Violation 4: Mutable hashCode()

```java
class BadPerson {
    String name;  // Mutable field
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object obj) {
        return name.equals(((BadPerson) obj).name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
```

**Problem:**
```java
BadPerson p = new BadPerson("John");
Map<BadPerson, String> map = new HashMap<>();
map.put(p, "Engineer");

// Modify key after insertion
p.setName("Jane");  // hashCode changes!

System.out.println(map.get(p));  // null ❌ (wrong bucket now!)

// Why? hashCode changed → looking in wrong bucket
```

---

## Real-World Examples

### Example 1: HashSet Duplicate Detection

```java
class Student {
    String id;
    String name;
    
    public Student(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Student) {
            return id.equals(((Student) obj).id);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

// Usage
Set<Student> students = new HashSet<>();
students.add(new Student("S001", "John"));
students.add(new Student("S001", "John Doe"));  // Same ID

System.out.println(students.size());  // 1 (duplicate detected)
```

**Without proper equals/hashCode:**
```java
class BadStudent {
    String id;
    String name;
    // No equals/hashCode override
}

Set<BadStudent> students = new HashSet<>();
students.add(new BadStudent("S001", "John"));
students.add(new BadStudent("S001", "John Doe"));

System.out.println(students.size());  // 2 ❌ (duplicates not detected!)
```

---

### Example 2: HashMap Cache

```java
class CacheKey {
    String userId;
    String resourceType;
    
    public CacheKey(String userId, String resourceType) {
        this.userId = userId;
        this.resourceType = resourceType;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CacheKey key = (CacheKey) obj;
        return Objects.equals(userId, key.userId) &&
               Objects.equals(resourceType, key.resourceType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, resourceType);
    }
}

// Usage
Map<CacheKey, Object> cache = new HashMap<>();

CacheKey key1 = new CacheKey("user123", "profile");
cache.put(key1, new UserProfile());

CacheKey key2 = new CacheKey("user123", "profile");
Object cached = cache.get(key2);  // Found! ✓

System.out.println(cached != null);  // true
```

---

### Example 3: Database Entity

```java
@Entity
class User {
    @Id
    private Long id;
    private String username;
    private String email;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        User user = (User) obj;
        // Use ID for equality (database primary key)
        return id != null && id.equals(user.id);
    }
    
    @Override
    public int hashCode() {
        // Use constant for entities without ID (not yet persisted)
        return id != null ? id.hashCode() : 0;
    }
}

// Usage
Set<User> users = new HashSet<>();
User u1 = new User(1L, "john", "john@example.com");
User u2 = new User(1L, "john_updated", "john_new@example.com");

users.add(u1);
users.add(u2);  // Same ID, not added

System.out.println(users.size());  // 1 (same entity)
```

---

### Example 4: Immutable Key

```java
final class ImmutableKey {
    private final String value;
    
    public ImmutableKey(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImmutableKey that = (ImmutableKey) obj;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

// Usage - Safe as HashMap key
Map<ImmutableKey, String> map = new HashMap<>();
ImmutableKey key = new ImmutableKey("config");
map.put(key, "value");

// Cannot modify key (immutable)
// key.setValue("new"); // Compile error

System.out.println(map.get(new ImmutableKey("config")));  // "value" ✓
```

---

## Best Practices

### 1. Always Override Both Methods

```java
// ✓ Good: Override both
@Override
public boolean equals(Object obj) {
    // Implementation
}

@Override
public int hashCode() {
    // Implementation
}

// ❌ Bad: Override only one
@Override
public boolean equals(Object obj) {
    // Implementation
}
// Missing hashCode()!
```

---

### 2. Use Objects.hash() and Objects.equals()

```java
// ✓ Good: Use utility methods
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Person person = (Person) obj;
    return age == person.age &&
           Objects.equals(name, person.name);  // Null-safe
}

@Override
public int hashCode() {
    return Objects.hash(name, age);  // Handles nulls
}

// ❌ Bad: Manual null checks
@Override
public boolean equals(Object obj) {
    Person p = (Person) obj;
    return (name == null ? p.name == null : name.equals(p.name)) && age == p.age;
}

@Override
public int hashCode() {
    return 31 * (name == null ? 0 : name.hashCode()) + age;
}
```

---

### 3. Use Same Fields in Both Methods

```java
class Person {
    String name;
    int age;
    String address;  // Not part of equality
    
    @Override
    public boolean equals(Object obj) {
        Person p = (Person) obj;
        // Uses: name, age
        return Objects.equals(name, p.name) && age == p.age;
    }
    
    @Override
    public int hashCode() {
        // Uses: name, age (SAME fields as equals)
        return Objects.hash(name, age);
    }
}
```

---

### 4. Make Keys Immutable

```java
// ✓ Good: Immutable key
final class GoodKey {
    private final String value;
    
    public GoodKey(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;  // No setter
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof GoodKey && 
               Objects.equals(value, ((GoodKey) obj).value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

// ❌ Bad: Mutable key
class BadKey {
    private String value;
    
    public void setValue(String value) {  // Setter allows mutation
        this.value = value;
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();  // Changes if value changes!
    }
}
```

---

### 5. Use IDE Generation

**IntelliJ IDEA:**
```
Right-click → Generate → equals() and hashCode()
Or: Alt+Insert → equals() and hashCode()
```

**Eclipse:**
```
Source → Generate hashCode() and equals()
```

**Generated Code:**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Person person = (Person) o;
    return age == person.age && Objects.equals(name, person.name);
}

@Override
public int hashCode() {
    return Objects.hash(name, age);
}
```

---

### 6. Consider Lombok

```java
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
class Person {
    private String name;
    private int age;
    
    // equals() and hashCode() generated automatically
}

// Or with specific fields
@EqualsAndHashCode(of = {"id"})
class User {
    private Long id;
    private String name;
    private String email;
    
    // Only 'id' used in equals/hashCode
}
```

---

## Common Pitfalls

### Pitfall 1: Using Mutable Fields

```java
// ❌ Bad
class BadKey {
    List<String> items = new ArrayList<>();
    
    @Override
    public int hashCode() {
        return items.hashCode();  // Changes when list modified!
    }
}

BadKey key = new BadKey();
key.items.add("A");
map.put(key, "value");

key.items.add("B");  // hashCode changes!
map.get(key);  // null (wrong bucket)
```

---

### Pitfall 2: Not Handling null

```java
// ❌ Bad
@Override
public boolean equals(Object obj) {
    Person p = (Person) obj;  // NullPointerException if obj is null!
    return name.equals(p.name);
}

// ✓ Good
@Override
public boolean equals(Object obj) {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Person p = (Person) obj;
    return Objects.equals(name, p.name);
}
```

---

### Pitfall 3: Using instanceof with Inheritance

```java
class Animal {
    String name;
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Animal) {  // ❌ Breaks symmetry with subclasses
            return name.equals(((Animal) obj).name);
        }
        return false;
    }
}

class Dog extends Animal {
    String breed;
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Dog) {
            Dog dog = (Dog) obj;
            return name.equals(dog.name) && breed.equals(dog.breed);
        }
        return false;
    }
}

Animal a = new Animal("Max");
Dog d = new Dog("Max", "Labrador");

a.equals(d);  // true (Animal's equals)
d.equals(a);  // false (Dog's equals)
// Violates symmetry!

// ✓ Better: Use getClass()
@Override
public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) return false;
    // ...
}
```

---

### Pitfall 4: Forgetting to Override in Subclass

```java
class Person {
    String name;
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Person && 
               name.equals(((Person) obj).name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

class Employee extends Person {
    String employeeId;
    
    // ❌ Forgot to override equals/hashCode
    // Uses parent's implementation (only compares name)
}

Employee e1 = new Employee("John", "E001");
Employee e2 = new Employee("John", "E002");

e1.equals(e2);  // true ❌ (should be false - different IDs)
```

---

## Testing the Contract

```java
public class ContractTest {
    
    @Test
    public void testEqualsContract() {
        Person p1 = new Person("John", 25);
        Person p2 = new Person("John", 25);
        Person p3 = new Person("John", 25);
        
        // Reflexive: x.equals(x)
        assertTrue(p1.equals(p1));
        
        // Symmetric: x.equals(y) == y.equals(x)
        assertTrue(p1.equals(p2) == p2.equals(p1));
        
        // Transitive: x.equals(y) && y.equals(z) → x.equals(z)
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p3));
        assertTrue(p1.equals(p3));
        
        // Consistent: multiple calls return same result
        assertTrue(p1.equals(p2));
        assertTrue(p1.equals(p2));
        
        // null: x.equals(null) returns false
        assertFalse(p1.equals(null));
    }
    
    @Test
    public void testHashCodeContract() {
        Person p1 = new Person("John", 25);
        Person p2 = new Person("John", 25);
        
        // If equals, hashCode must be same
        if (p1.equals(p2)) {
            assertEquals(p1.hashCode(), p2.hashCode());
        }
        
        // Consistent: multiple calls return same value
        int hash1 = p1.hashCode();
        int hash2 = p1.hashCode();
        assertEquals(hash1, hash2);
    }
    
    @Test
    public void testHashMapBehavior() {
        Map<Person, String> map = new HashMap<>();
        Person p1 = new Person("John", 25);
        map.put(p1, "Engineer");
        
        Person p2 = new Person("John", 25);
        assertEquals("Engineer", map.get(p2));
    }
    
    @Test
    public void testHashSetBehavior() {
        Set<Person> set = new HashSet<>();
        Person p1 = new Person("John", 25);
        Person p2 = new Person("John", 25);
        
        set.add(p1);
        set.add(p2);
        
        assertEquals(1, set.size());  // No duplicates
    }
}
```

---

## Summary

### The Contract in Simple Terms

**Rule:** If two objects are equal, they must have the same hash code.

```java
if (a.equals(b)) {
    assert a.hashCode() == b.hashCode();
}
```

### Key Points

1. **Always override both** equals() and hashCode() together
2. **Use same fields** in both methods
3. **Make keys immutable** when using as HashMap/HashSet keys
4. **Use Objects.hash()** for hashCode() implementation
5. **Handle null** properly in equals()
6. **Test with HashMap/HashSet** to verify contract

### Quick Checklist

✅ Override both equals() and hashCode()  
✅ Use same fields in both methods  
✅ Use Objects.hash() and Objects.equals()  
✅ Handle null in equals()  
✅ Check for same class (getClass())  
✅ Make keys immutable  
✅ Test with HashMap/HashSet  

❌ Don't override only one method  
❌ Don't use different fields  
❌ Don't use mutable fields in hashCode()  
❌ Don't return constant from hashCode()  
❌ Don't forget to handle null  

---

## References

- [Object.hashCode() JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--)
- [Object.equals() JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-)
- [Effective Java by Joshua Bloch - Item 11](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [Java Collections Framework](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/)

---

**Last Updated**: 2024
**Java Versions**: 5, 8, 11, 17, 21
