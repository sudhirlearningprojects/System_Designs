# Java hashCode() and equals() - Deep Dive Interview Guide

## Overview

The `equals()` and `hashCode()` methods are fundamental to Java's object identity and are crucial for collections like `HashMap`, `HashSet`, and `Hashtable`. Understanding their contract and proper implementation is essential for Java interviews.

---

## The equals() and hashCode() Contract

### Object.equals() Contract (5 Rules)

1. **Reflexive**: `x.equals(x)` must return `true`
2. **Symmetric**: If `x.equals(y)` returns `true`, then `y.equals(x)` must return `true`
3. **Transitive**: If `x.equals(y)` and `y.equals(z)` return `true`, then `x.equals(z)` must return `true`
4. **Consistent**: Multiple invocations must return the same result
5. **Null handling**: `x.equals(null)` must return `false`

### Object.hashCode() Contract (3 Rules)

1. **Consistency**: Multiple invocations on same object must return same hash code
2. **equals() implies same hashCode()**: If `x.equals(y)` is `true`, then `x.hashCode() == y.hashCode()`
3. **Different objects can have same hashCode()**: If `x.equals(y)` is `false`, hashCodes can be same or different

### Critical Rule
**If you override equals(), you MUST override hashCode()!**

---

## Default Implementation

### Object.equals() Default

```java
public boolean equals(Object obj) {
    return (this == obj);  // Reference equality only
}
```

### Object.hashCode() Default

```java
public native int hashCode();  // Memory address based (JVM dependent)
```

---

## Proper Implementation Examples

### Example 1: Simple Person Class

```java
public class Person {
    private String name;
    private int age;
    private String email;
    
    public Person(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
    
    @Override
    public boolean equals(Object obj) {
        // 1. Reference equality check
        if (this == obj) return true;
        
        // 2. Null check
        if (obj == null) return false;
        
        // 3. Class type check
        if (getClass() != obj.getClass()) return false;
        
        // 4. Field comparison
        Person person = (Person) obj;
        return age == person.age &&
               Objects.equals(name, person.name) &&
               Objects.equals(email, person.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age, email);
    }
}
```

### Example 2: Complex Employee Class

```java
public class Employee {
    private Long id;
    private String firstName;
    private String lastName;
    private Department department;
    private double salary;
    private LocalDate hireDate;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        return Objects.equals(id, employee.id) &&
               Objects.equals(firstName, employee.firstName) &&
               Objects.equals(lastName, employee.lastName) &&
               Objects.equals(department, employee.department) &&
               Double.compare(employee.salary, salary) == 0 &&
               Objects.equals(hireDate, employee.hireDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, department, salary, hireDate);
    }
}
```

---

## Common Implementation Mistakes

### Mistake 1: Only Override equals()

```java
public class BadPerson {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BadPerson person = (BadPerson) obj;
        return age == person.age && Objects.equals(name, person.name);
    }
    
    // Missing hashCode() override!
}

// Problem demonstration
BadPerson p1 = new BadPerson("John", 25);
BadPerson p2 = new BadPerson("John", 25);

System.out.println(p1.equals(p2));    // true
System.out.println(p1.hashCode() == p2.hashCode()); // false (likely)

// HashMap behavior
Map<BadPerson, String> map = new HashMap<>();
map.put(p1, "Person 1");
System.out.println(map.get(p2)); // null (should be "Person 1")
```

### Mistake 2: Inconsistent hashCode()

```java
public class InconsistentPerson {
    private String name;
    private int age;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InconsistentPerson person = (InconsistentPerson) obj;
        return Objects.equals(name, person.name); // Only name
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age); // Both name and age
    }
}

// Problem: Two objects can be equal but have different hash codes
InconsistentPerson p1 = new InconsistentPerson("John", 25);
InconsistentPerson p2 = new InconsistentPerson("John", 30);

System.out.println(p1.equals(p2));    // true (same name)
System.out.println(p1.hashCode() == p2.hashCode()); // false (different age)
```

### Mistake 3: Using instanceof Instead of getClass()

```java
public class Parent {
    private String name;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Parent)) return false; // Problem!
        Parent parent = (Parent) obj;
        return Objects.equals(name, parent.name);
    }
}

public class Child extends Parent {
    private int age;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Child)) return false;
        Child child = (Child) obj;
        return super.equals(obj) && age == child.age;
    }
}

// Symmetry violation
Parent parent = new Parent("John");
Child child = new Child("John", 25);

System.out.println(parent.equals(child)); // true
System.out.println(child.equals(parent)); // false (violates symmetry!)
```

---

## Complex Interview Questions

### Question 1: HashMap Behavior with Broken equals/hashCode

```java
public class BrokenKey {
    private String value;
    
    public BrokenKey(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BrokenKey key = (BrokenKey) obj;
        return Objects.equals(value, key.value);
    }
    
    // Intentionally broken: always returns different hash code
    @Override
    public int hashCode() {
        return new Random().nextInt();
    }
}

public class HashMapTest {
    public static void main(String[] args) {
        Map<BrokenKey, String> map = new HashMap<>();
        BrokenKey key1 = new BrokenKey("test");
        BrokenKey key2 = new BrokenKey("test");
        
        map.put(key1, "value1");
        
        System.out.println(key1.equals(key2));     // ?
        System.out.println(map.get(key1));        // ?
        System.out.println(map.get(key2));        // ?
        System.out.println(map.containsKey(key2)); // ?
    }
}
```

**Answer:**
```
true     // equals() works correctly
value1   // Same object reference
null     // Different hash code, different bucket
false    // Can't find in HashMap
```

### Question 2: Mutable Objects as HashMap Keys

```java
public class MutableKey {
    private String value;
    
    public MutableKey(String value) {
        this.value = value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MutableKey key = (MutableKey) obj;
        return Objects.equals(value, key.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

public class MutableKeyTest {
    public static void main(String[] args) {
        Map<MutableKey, String> map = new HashMap<>();
        MutableKey key = new MutableKey("original");
        
        map.put(key, "value");
        System.out.println(map.get(key));        // ?
        
        key.setValue("modified");
        System.out.println(map.get(key));        // ?
        System.out.println(map.containsKey(key)); // ?
        System.out.println(map.size());          // ?
    }
}
```

**Answer:**
```
value    // Found in correct bucket
null     // Hash code changed, wrong bucket
false    // Can't find in HashMap
1        // Object still in map but unreachable
```

### Question 3: String Pool and equals/hashCode

```java
public class StringTest {
    public static void main(String[] args) {
        String s1 = "hello";
        String s2 = "hello";
        String s3 = new String("hello");
        String s4 = new String("hello");
        
        System.out.println(s1 == s2);           // ?
        System.out.println(s1.equals(s2));     // ?
        System.out.println(s1 == s3);          // ?
        System.out.println(s1.equals(s3));     // ?
        System.out.println(s3 == s4);          // ?
        System.out.println(s3.equals(s4));     // ?
        
        System.out.println(s1.hashCode() == s2.hashCode()); // ?
        System.out.println(s1.hashCode() == s3.hashCode()); // ?
        System.out.println(s3.hashCode() == s4.hashCode()); // ?
        
        // HashMap behavior
        Map<String, Integer> map = new HashMap<>();
        map.put(s1, 1);
        map.put(s3, 3);
        
        System.out.println(map.size());         // ?
        System.out.println(map.get(s2));       // ?
        System.out.println(map.get(s4));       // ?
    }
}
```

**Answer:**
```
true     // String pool - same reference
true     // Same content
false    // Different objects
true     // Same content
false    // Different objects
true     // Same content

true     // Same string, same hash code
true     // Same content, same hash code
true     // Same content, same hash code

1        // s3 overwrites s1 (same key)
3        // s2 maps to same key as s1
3        // s4 maps to same key as s3
```

### Question 4: Inheritance and equals/hashCode

```java
public class Point {
    private int x, y;
    
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

public class ColorPoint extends Point {
    private String color;
    
    public ColorPoint(int x, int y, String color) {
        super(x, y);
        this.color = color;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ColorPoint that = (ColorPoint) obj;
        return Objects.equals(color, that.color);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color);
    }
}

public class InheritanceTest {
    public static void main(String[] args) {
        Point p = new Point(1, 2);
        ColorPoint cp1 = new ColorPoint(1, 2, "red");
        ColorPoint cp2 = new ColorPoint(1, 2, "red");
        ColorPoint cp3 = new ColorPoint(1, 2, "blue");
        
        System.out.println(p.equals(cp1));      // ?
        System.out.println(cp1.equals(p));     // ?
        System.out.println(cp1.equals(cp2));   // ?
        System.out.println(cp1.equals(cp3));   // ?
        
        // Set behavior
        Set<Point> set = new HashSet<>();
        set.add(p);
        set.add(cp1);
        System.out.println(set.size());        // ?
    }
}
```

**Answer:**
```
false    // Different classes (getClass() check)
false    // Different classes (getClass() check)
true     // Same class, same coordinates, same color
false    // Same coordinates, different color

2        // Different classes, both added
```

### Question 5: Circular Reference in equals/hashCode

```java
public class Node {
    private String value;
    private Node next;
    
    public Node(String value) {
        this.value = value;
    }
    
    public void setNext(Node next) {
        this.next = next;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return Objects.equals(value, node.value) &&
               Objects.equals(next, node.next); // Potential infinite recursion!
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, next); // Potential infinite recursion!
    }
}

public class CircularTest {
    public static void main(String[] args) {
        Node n1 = new Node("A");
        Node n2 = new Node("B");
        n1.setNext(n2);
        n2.setNext(n1); // Circular reference
        
        Node n3 = new Node("A");
        Node n4 = new Node("B");
        n3.setNext(n4);
        n4.setNext(n3); // Circular reference
        
        System.out.println(n1.equals(n3)); // StackOverflowError!
    }
}
```

**Solution:**
```java
public class SafeNode {
    private String value;
    private SafeNode next;
    private static final ThreadLocal<Set<SafeNode>> visiting = 
        ThreadLocal.withInitial(HashSet::new);
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SafeNode node = (SafeNode) obj;
        
        // Prevent infinite recursion
        Set<SafeNode> currentlyVisiting = visiting.get();
        if (currentlyVisiting.contains(this)) {
            return true; // Assume equal if already visiting
        }
        
        try {
            currentlyVisiting.add(this);
            return Objects.equals(value, node.value) &&
                   Objects.equals(next, node.next);
        } finally {
            currentlyVisiting.remove(this);
            if (currentlyVisiting.isEmpty()) {
                visiting.remove();
            }
        }
    }
}
```

### Question 6: Performance Impact of Poor hashCode()

```java
public class PoorHashCode {
    private String name;
    private int age;
    
    public PoorHashCode(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PoorHashCode that = (PoorHashCode) obj;
        return age == that.age && Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return 1; // Terrible hash code - all objects have same hash!
    }
}

public class PerformanceTest {
    public static void main(String[] args) {
        Map<PoorHashCode, String> map = new HashMap<>();
        
        // Add 10,000 objects
        for (int i = 0; i < 10000; i++) {
            map.put(new PoorHashCode("Person" + i, i), "Value" + i);
        }
        
        // Lookup performance
        long start = System.nanoTime();
        String result = map.get(new PoorHashCode("Person5000", 5000));
        long end = System.nanoTime();
        
        System.out.println("Time taken: " + (end - start) + " ns");
        // With good hashCode(): ~100-500 ns
        // With poor hashCode(): ~50,000-100,000 ns (100x slower!)
    }
}
```

---

## Advanced Scenarios

### Scenario 1: Custom hashCode() Algorithm

```java
public class CustomHash {
    private String field1;
    private int field2;
    private double field3;
    
    @Override
    public int hashCode() {
        // Custom hash algorithm
        int result = 17; // Prime number
        result = 31 * result + (field1 != null ? field1.hashCode() : 0);
        result = 31 * result + field2;
        result = 31 * result + Double.hashCode(field3);
        return result;
    }
    
    // Alternative using bit manipulation
    @Override
    public int hashCode() {
        int hash = 0;
        if (field1 != null) {
            hash ^= field1.hashCode();
        }
        hash ^= Integer.hashCode(field2) << 1;
        hash ^= Double.hashCode(field3) << 2;
        return hash;
    }
}
```

### Scenario 2: Lazy hashCode() Calculation

```java
public class LazyHashCode {
    private final String value;
    private volatile int hashCode; // Cached hash code
    
    public LazyHashCode(String value) {
        this.value = value;
    }
    
    @Override
    public int hashCode() {
        int h = hashCode;
        if (h == 0 && value != null) {
            h = value.hashCode();
            hashCode = h;
        }
        return h;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LazyHashCode that = (LazyHashCode) obj;
        return Objects.equals(value, that.value);
    }
}
```

### Scenario 3: equals() with Tolerance for Floating Point

```java
public class Point3D {
    private double x, y, z;
    private static final double EPSILON = 1e-9;
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point3D point = (Point3D) obj;
        return Math.abs(x - point.x) < EPSILON &&
               Math.abs(y - point.y) < EPSILON &&
               Math.abs(z - point.z) < EPSILON;
    }
    
    @Override
    public int hashCode() {
        // Round to avoid precision issues
        long xBits = Double.doubleToLongBits(Math.round(x / EPSILON) * EPSILON);
        long yBits = Double.doubleToLongBits(Math.round(y / EPSILON) * EPSILON);
        long zBits = Double.doubleToLongBits(Math.round(z / EPSILON) * EPSILON);
        return Objects.hash(xBits, yBits, zBits);
    }
}
```

---

## Collection Behavior Deep Dive

### HashMap Internal Working

```java
// Simplified HashMap behavior
public class HashMapBehavior {
    public static void main(String[] args) {
        Map<Person, String> map = new HashMap<>();
        
        Person p1 = new Person("John", 25);
        Person p2 = new Person("John", 25);
        
        // Step 1: Calculate hash code
        int hash1 = p1.hashCode();
        int hash2 = p2.hashCode();
        
        // Step 2: Calculate bucket index
        int bucket1 = hash1 & (16 - 1); // Assuming capacity = 16
        int bucket2 = hash2 & (16 - 1);
        
        // Step 3: Store in bucket
        map.put(p1, "Person 1");
        
        // Step 4: Lookup
        // Same hash code -> same bucket -> equals() check -> found
        String result = map.get(p2);
        
        System.out.println("Hash1: " + hash1);
        System.out.println("Hash2: " + hash2);
        System.out.println("Bucket1: " + bucket1);
        System.out.println("Bucket2: " + bucket2);
        System.out.println("Result: " + result);
    }
}
```

### HashSet Duplicate Detection

```java
public class HashSetBehavior {
    public static void main(String[] args) {
        Set<Person> set = new HashSet<>();
        
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        
        set.add(p1);
        boolean added = set.add(p2); // false if equals() returns true
        
        System.out.println("Set size: " + set.size());
        System.out.println("Second add successful: " + added);
    }
}
```

---

## Best Practices and Guidelines

### 1. Always Override Both Methods

```java
// Good
public class GoodClass {
    @Override
    public boolean equals(Object obj) { /* implementation */ }
    
    @Override
    public int hashCode() { /* implementation */ }
}
```

### 2. Use Objects.equals() and Objects.hash()

```java
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    MyClass that = (MyClass) obj;
    return Objects.equals(field1, that.field1) &&
           Objects.equals(field2, that.field2);
}

@Override
public int hashCode() {
    return Objects.hash(field1, field2);
}
```

### 3. Consider Using Records (Java 14+)

```java
// Automatic equals() and hashCode()
public record Person(String name, int age, String email) {}
```

### 4. Use IDE Generation

```java
// Most IDEs can generate proper equals() and hashCode()
// IntelliJ IDEA: Alt + Insert -> equals() and hashCode()
// Eclipse: Source -> Generate hashCode() and equals()
```

---

## Common Interview Traps

### Trap 1: "Why override both methods?"

**Wrong Answer:** "Because it's a best practice."

**Correct Answer:** "Because of the contract. If two objects are equal according to equals(), they must have the same hash code. Collections like HashMap rely on this contract for correct behavior."

### Trap 2: "What happens if you don't override hashCode()?"

**Wrong Answer:** "Nothing happens."

**Correct Answer:** "Objects that are equal according to equals() may have different hash codes, causing HashMap/HashSet to store duplicates and fail to find existing objects."

### Trap 3: "Can two different objects have the same hash code?"

**Wrong Answer:** "No, hash codes must be unique."

**Correct Answer:** "Yes, this is called a hash collision. Hash codes are int values (32-bit), so there are only 2^32 possible values. With more objects, collisions are inevitable and handled by chaining or open addressing."

---

## Performance Considerations

### Hash Code Distribution

```java
// Poor distribution - all objects hash to same bucket
@Override
public int hashCode() {
    return 1; // Terrible!
}

// Good distribution - spreads objects across buckets
@Override
public int hashCode() {
    return Objects.hash(field1, field2, field3);
}
```

### Expensive equals() Operations

```java
public class ExpensiveEquals {
    private List<String> data;
    
    @Override
    public boolean equals(Object obj) {
        // Expensive list comparison
        ExpensiveEquals other = (ExpensiveEquals) obj;
        return Objects.equals(data, other.data); // O(n) operation
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data); // Also expensive but cached
    }
}
```

---

## Key Interview Takeaways

1. **Contract Compliance**: Always maintain equals/hashCode contract
2. **Override Both**: Never override just one method
3. **Consistency**: Use same fields in both methods
4. **Performance**: Good hash code distribution is crucial
5. **Immutability**: Prefer immutable objects as keys
6. **Null Safety**: Handle null values properly
7. **Inheritance**: Be careful with equals() in inheritance hierarchies
8. **Collections**: Understanding impact on HashMap, HashSet behavior
9. **Testing**: Always test equals() for reflexive, symmetric, transitive properties
10. **Modern Approach**: Consider using Records for simple data classes