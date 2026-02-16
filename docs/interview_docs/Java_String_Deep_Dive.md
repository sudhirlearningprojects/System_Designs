# Java String Deep Dive - Interview Guide

## Overview

Java String is an **immutable** sequence of characters stored as a **char array** (Java 8) or **byte array** (Java 9+). Understanding String creation methods, memory management, and behavior is crucial for Java interviews.

---

## String Creation Methods

### 1. String Literal (String Pool)

```java
String s1 = "Hello";
String s2 = "Hello";
System.out.println(s1 == s2); // true (same reference)
```

**How it works:**
- Stored in **String Pool** (heap memory)
- JVM checks pool first before creating new string
- Same literal = same reference
- **Compile-time optimization**

### 2. new String() Constructor

```java
String s1 = new String("Hello");
String s2 = new String("Hello");
System.out.println(s1 == s2); // false (different objects)
```

**How it works:**
- Creates new object in heap (outside pool)
- Always creates new instance
- Different objects even with same content
- **Runtime object creation**

### 3. String.valueOf() Method

```java
String s1 = String.valueOf(123);      // "123"
String s2 = String.valueOf(true);     // "true"
String s3 = String.valueOf(null);     // "null"
String s4 = String.valueOf(new char[]{'A', 'B'}); // "AB"
```

**How it works:**
- Converts primitives/objects to string
- Returns string literal (uses pool)
- **Null-safe** (returns "null" for null input)

### 4. StringBuilder/StringBuffer

```java
StringBuilder sb = new StringBuilder();
sb.append("Hello").append(" World");
String s = sb.toString(); // Creates new String object
```

**How it works:**
- Mutable string building
- toString() creates new String object
- More efficient for concatenation

### 5. String.intern() Method

```java
String s1 = new String("Java");
String s2 = s1.intern(); // Moves to pool
String s3 = "Java";      // From pool
System.out.println(s2 == s3); // true
```

**How it works:**
- Forces string into String Pool
- Returns pool reference
- Useful for memory optimization

---

## Memory Layout Comparison

```java
// String Pool (Heap)
String s1 = "Java";     // Pool
String s2 = "Java";     // Same reference as s1

// Heap (Outside Pool)  
String s3 = new String("Java");  // New object
String s4 = new String("Java");  // Another new object

// Memory visualization:
// String Pool: ["Java"] ← s1, s2 point here
// Heap:        [String obj1] ← s3 points here
//              [String obj2] ← s4 points here
```

**Memory Locations:**
- **String Pool**: Special area in heap for literals
- **Heap**: Regular object storage
- **Stack**: Variable references

---

## String Immutability

```java
String s = "Hello";
s.concat(" World");     // Creates new object, doesn't modify s
System.out.println(s);  // "Hello" (unchanged)

String s2 = s.concat(" World");
System.out.println(s2); // "Hello World" (new object)
```

**Key Points:**
- Strings are **immutable** (cannot be changed)
- Operations create new objects
- Original string never changes
- Thread-safe by design

**Why Immutable?**
- **Security**: Prevents malicious modification
- **Thread Safety**: No synchronization needed
- **Caching**: Hashcode can be cached
- **String Pool**: Enables sharing

---

## String Concatenation Behavior

### Compile-time vs Runtime

```java
// Compile-time concatenation (optimized)
String s1 = "Hello" + "World";        // Becomes "HelloWorld" at compile time
String s2 = "HelloWorld";
System.out.println(s1 == s2);         // true (same pool reference)

// Runtime concatenation (creates new object)
String a = "Hello";
String b = "World";
String s3 = a + b;                    // New object created
System.out.println(s1 == s3);         // false (different objects)
```

### Performance Impact

```java
// Inefficient (creates multiple objects)
String result = "";
for(int i = 0; i < 1000; i++) {
    result += "A";  // Creates new object each iteration
}

// Efficient (single mutable buffer)
StringBuilder sb = new StringBuilder();
for(int i = 0; i < 1000; i++) {
    sb.append("A");  // Modifies existing buffer
}
String result = sb.toString();
```

---

## Major Output-Based Interview Questions

### Question 1: String Pool vs new String()

```java
public class StringTest1 {
    public static void main(String[] args) {
        String s1 = "Java";
        String s2 = "Java";
        String s3 = new String("Java");
        String s4 = new String("Java");
        
        System.out.println(s1 == s2);        // ?
        System.out.println(s1 == s3);        // ?
        System.out.println(s3 == s4);        // ?
        System.out.println(s1.equals(s3));   // ?
    }
}
```

**Output & Explanation:**
```
true   // s1 and s2 point to same pool object
false  // s1 (pool) vs s3 (heap)
false  // s3 and s4 are different heap objects
true   // equals() compares content, not reference
```

### Question 2: String Concatenation Optimization

```java
public class StringTest2 {
    public static void main(String[] args) {
        String s1 = "Hello";
        String s2 = "World";
        String s3 = "HelloWorld";
        String s4 = s1 + s2;              // Runtime concatenation
        String s5 = "Hello" + "World";    // Compile-time concatenation
        
        System.out.println(s3 == s4);  // ?
        System.out.println(s3 == s5);  // ?
    }
}
```

**Output & Explanation:**
```
false  // s4 created at runtime (new StringBuilder().append(s1).append(s2).toString())
true   // s5 compile-time constant folding (becomes "HelloWorld" literal)
```

### Question 3: String intern() Method

```java
public class StringTest3 {
    public static void main(String[] args) {
        String s1 = new String("Java");
        String s2 = s1.intern();
        String s3 = "Java";
        
        System.out.println(s1 == s2);  // ?
        System.out.println(s2 == s3);  // ?
        System.out.println(s1 == s3);  // ?
    }
}
```

**Output & Explanation:**
```
false  // s1 (heap) vs s2 (pool after intern())
true   // both s2 and s3 from pool
false  // s1 (heap) vs s3 (pool)
```

### Question 4: String Modification Attempts

```java
public class StringTest4 {
    public static void main(String[] args) {
        String s = "Hello";
        s.concat(" World");    // Return value ignored
        s += " Java";          // Creates new object, assigns to s
        
        System.out.println(s);  // ?
    }
}
```

**Output & Explanation:**
```
Hello Java  // concat() result ignored (immutability), += creates new object
```

### Question 5: StringBuilder vs String Performance

```java
public class StringTest5 {
    public static void main(String[] args) {
        String s1 = "A";
        s1 += "B";    // Creates new String("AB")
        s1 += "C";    // Creates new String("ABC")
        
        StringBuilder sb = new StringBuilder("A");
        sb.append("B");    // Modifies internal buffer
        sb.append("C");    // Modifies internal buffer
        
        System.out.println(s1);                    // ?
        System.out.println(sb.toString());        // ?
        System.out.println(s1 == sb.toString());  // ?
    }
}
```

**Output & Explanation:**
```
ABC
ABC
false  // Different objects (s1 from concatenation, sb.toString() creates new object)
```

### Question 6: String Comparison Edge Cases

```java
public class StringTest6 {
    public static void main(String[] args) {
        String s1 = "123";
        String s2 = String.valueOf(123);
        String s3 = Integer.toString(123);
        String s4 = "" + 123;
        
        System.out.println(s1 == s2);  // ?
        System.out.println(s1 == s3);  // ?
        System.out.println(s1 == s4);  // ?
    }
}
```

**Output & Explanation:**
```
true   // valueOf(123) returns "123" from pool
true   // Integer.toString(123) returns "123" from pool
false  // "" + 123 uses StringBuilder, creates new object
```

### Question 7: String with null Values

```java
public class StringTest7 {
    public static void main(String[] args) {
        String s1 = null;
        String s2 = "null";
        String s3 = String.valueOf(null);
        String s4 = null + "";
        
        System.out.println(s1);        // ?
        System.out.println(s2);        // ?
        System.out.println(s3);        // ?
        System.out.println(s1 == s2);  // ?
    }
}
```

**Output & Explanation:**
```
null
null
null
false  // null reference vs "null" string object
```

### Question 8: String in Switch Statement

```java
public class StringTest8 {
    public static void main(String[] args) {
        String s = new String("Java");
        
        switch(s) {
            case "Java":
                System.out.println("Match");
                break;
            default:
                System.out.println("No match");
        }
    }
}
```

**Output & Explanation:**
```
Match  // switch uses equals() internally, not == comparison
```

### Question 9: String Constant Pool Size

```java
public class StringTest9 {
    public static void main(String[] args) {
        String s1 = "A" + "B";        // Compile-time: "AB"
        String s2 = "AB";
        String s3 = new String("AB").intern();
        
        System.out.println(s1 == s2);  // ?
        System.out.println(s2 == s3);  // ?
        
        // How many objects in pool?
        // Answer: 1 ("AB")
    }
}
```

**Output & Explanation:**
```
true   // Both from pool
true   // intern() returns pool reference
```

### Question 10: Complex String Creation

```java
public class StringTest10 {
    public static void main(String[] args) {
        String s1 = "Hello";
        String s2 = new String("Hello");
        String s3 = new String("Hello").intern();
        String s4 = s2.intern();
        
        System.out.println(s1 == s2);  // ?
        System.out.println(s1 == s3);  // ?
        System.out.println(s1 == s4);  // ?
        System.out.println(s3 == s4);  // ?
    }
}
```

**Output & Explanation:**
```
false  // Pool vs heap
true   // Both from pool (intern() returns pool reference)
true   // Both from pool
true   // Both from pool
```

---

## String Methods Deep Dive

### Commonly Asked Methods

```java
String s = "Hello World";

// Length and character access
s.length();           // 11
s.charAt(0);          // 'H'
s.indexOf('o');       // 4 (first occurrence)
s.lastIndexOf('o');   // 7 (last occurrence)

// Substring operations
s.substring(0, 5);    // "Hello" (creates new String)
s.substring(6);       // "World" (creates new String)

// Case operations
s.toUpperCase();      // "HELLO WORLD" (creates new String)
s.toLowerCase();      // "hello world" (creates new String)

// Trimming and replacement
s.trim();             // Removes leading/trailing whitespace
s.replace('o', 'a');  // "Hella Warld" (creates new String)

// Comparison
s.equals("Hello World");      // true (content comparison)
s.equalsIgnoreCase("hello world"); // true
s.compareTo("Hello World");   // 0 (lexicographic comparison)

// Checking operations
s.startsWith("Hello"); // true
s.endsWith("World");   // true
s.contains("lo Wo");   // true
s.isEmpty();           // false
```

---

## String vs StringBuilder vs StringBuffer

| Feature | String | StringBuilder | StringBuffer |
|---------|--------|---------------|--------------|
| **Mutability** | Immutable | Mutable | Mutable |
| **Thread Safety** | Yes (immutable) | No | Yes (synchronized) |
| **Performance** | Slow (concatenation) | Fast | Medium |
| **Memory** | Creates new objects | Resizable buffer | Resizable buffer |
| **Use Case** | Few operations | Single-threaded | Multi-threaded |

### Performance Comparison

```java
// String concatenation (inefficient)
String s = "";
for(int i = 0; i < 10000; i++) {
    s += "A";  // O(n²) time complexity
}

// StringBuilder (efficient)
StringBuilder sb = new StringBuilder();
for(int i = 0; i < 10000; i++) {
    sb.append("A");  // O(n) time complexity
}
String result = sb.toString();
```

---

## Advanced String Concepts

### String Interning

```java
// Manual interning
String s1 = new String("Java").intern();
String s2 = "Java";
System.out.println(s1 == s2); // true

// Automatic interning (literals)
String s3 = "Python";
String s4 = "Python";
System.out.println(s3 == s4); // true
```

### String Pool Memory Management

```java
// JVM Parameters for String Pool
// -XX:StringTableSize=1000000  (increase pool size)
// -XX:+PrintStringTableStatistics (print pool stats)

// Pool size affects performance
String s1 = "A";  // Hash calculation and pool lookup
String s2 = "A";  // Pool hit (faster)
```

### String Encoding (Java 9+)

```java
// Compact Strings (Java 9+)
String latin = "Hello";     // Uses byte[] (1 byte per char)
String unicode = "नमस्ते";   // Uses byte[] (2 bytes per char)

// Memory savings: ~50% for Latin-1 strings
```

---

## Common Interview Traps

### Trap 1: String Pool Confusion

```java
String s1 = "A";
String s2 = "B";
String s3 = "AB";
String s4 = s1 + s2;  // NOT from pool (runtime concatenation)

System.out.println(s3 == s4); // false (trap: many expect true)
```

### Trap 2: String Modification

```java
public void modifyString(String s) {
    s = s + " Modified";  // Creates new object, doesn't affect original
}

String original = "Hello";
modifyString(original);
System.out.println(original); // "Hello" (unchanged - trap!)
```

### Trap 3: String Comparison

```java
String s1 = new String("Java");
String s2 = new String("Java");

if(s1 == s2) {  // false (reference comparison)
    System.out.println("Equal");
} else {
    System.out.println("Not Equal"); // This executes (trap!)
}
```

---

## Best Practices

### 1. Use String Literals When Possible

```java
// Good
String s = "Hello";

// Avoid (unless specifically needed)
String s = new String("Hello");
```

### 2. Use StringBuilder for Multiple Concatenations

```java
// Bad
String result = "";
for(String item : items) {
    result += item + ", ";
}

// Good
StringBuilder sb = new StringBuilder();
for(String item : items) {
    sb.append(item).append(", ");
}
String result = sb.toString();
```

### 3. Use equals() for Content Comparison

```java
// Bad
if(s1 == s2) { ... }

// Good
if(s1.equals(s2)) { ... }

// Better (null-safe)
if(Objects.equals(s1, s2)) { ... }
```

### 4. Consider String.intern() for Memory Optimization

```java
// When dealing with many duplicate strings
String s = getUserInput().intern(); // Reduces memory usage
```

---

## Key Interview Takeaways

1. **String literals** → String Pool (shared references)
2. **new String()** → Heap (new objects always)
3. **Immutability** → Operations create new objects
4. **== vs equals()** → Reference vs content comparison
5. **intern()** → Moves string to pool
6. **StringBuilder** → Efficient for multiple concatenations
7. **Compile-time constants** → Automatically pooled
8. **Runtime concatenation** → Creates new objects
9. **String Pool** → Memory optimization technique
10. **Performance** → StringBuilder > String for concatenation

---

## Quick Reference

### String Creation Methods
- `"literal"` → Pool
- `new String()` → Heap
- `String.valueOf()` → Pool
- `StringBuilder.toString()` → Heap
- `String.intern()` → Pool

### Memory Locations
- **String Pool**: Heap (special area)
- **String Objects**: Heap (regular area)
- **References**: Stack

### Performance Tips
- Use literals for constants
- Use StringBuilder for concatenation
- Use intern() for duplicate strings
- Avoid unnecessary new String() calls