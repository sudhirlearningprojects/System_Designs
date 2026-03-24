# Comparator vs Comparable in Java - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Comparable Interface](#comparable-interface)
3. [Comparator Interface](#comparator-interface)
4. [Key Differences](#key-differences)
5. [When to Use Which](#when-to-use-which)
6. [Practical Examples](#practical-examples)
7. [Java 8+ Features](#java-8-features)
8. [Best Practices](#best-practices)
9. [Common Interview Questions](#common-interview-questions)

---

## Introduction

Both `Comparable` and `Comparator` are interfaces used for sorting objects in Java, but they serve different purposes and are used in different scenarios.

### Quick Overview

| Aspect | Comparable | Comparator |
|--------|-----------|------------|
| **Package** | `java.lang` | `java.util` |
| **Method** | `compareTo(T o)` | `compare(T o1, T o2)` |
| **Purpose** | Natural ordering | Custom ordering |
| **Location** | Inside the class | External class |
| **Modification** | Requires source code access | No source code needed |
| **Sorting** | Single sorting sequence | Multiple sorting sequences |

---

## Comparable Interface

### Definition

```java
package java.lang;

public interface Comparable<T> {
    int compareTo(T o);
}
```

### Purpose
- Defines the **natural ordering** of objects
- Implemented **inside the class** being compared
- Provides **single sorting sequence**

### Return Values

```java
compareTo(T o) returns:
- Negative integer: if this < o
- Zero: if this == o
- Positive integer: if this > o
```

### Basic Example

```java
public class Employee implements Comparable<Employee> {
    private int id;
    private String name;
    private double salary;
    
    public Employee(int id, String name, double salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
    }
    
    // Natural ordering by ID
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
    
    // Getters and toString()
    public int getId() { return id; }
    public String getName() { return name; }
    public double getSalary() { return salary; }
    
    @Override
    public String toString() {
        return "Employee{id=" + id + ", name='" + name + "', salary=" + salary + "}";
    }
}
```

### Usage

```java
public class ComparableExample {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(3, "Alice", 75000));
        employees.add(new Employee(1, "Bob", 60000));
        employees.add(new Employee(2, "Charlie", 80000));
        
        // Sort using natural ordering (by ID)
        Collections.sort(employees);
        
        System.out.println("Sorted by ID (natural ordering):");
        employees.forEach(System.out::println);
    }
}
```

**Output:**
```
Sorted by ID (natural ordering):
Employee{id=1, name='Bob', salary=60000.0}
Employee{id=2, name='Charlie', salary=80000.0}
Employee{id=3, name='Alice', salary=75000.0}
```

---

### Comparable Examples with Different Types

#### 1. String Comparison (Natural Ordering)

```java
public class Student implements Comparable<Student> {
    private String name;
    private int rollNumber;
    
    public Student(String name, int rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }
    
    @Override
    public int compareTo(Student other) {
        // Natural ordering by name (alphabetically)
        return this.name.compareTo(other.name);
    }
    
    @Override
    public String toString() {
        return "Student{name='" + name + "', rollNumber=" + rollNumber + "}";
    }
}
```

#### 2. Multiple Field Comparison

```java
public class Product implements Comparable<Product> {
    private String name;
    private double price;
    private int quantity;
    
    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    @Override
    public int compareTo(Product other) {
        // First compare by price
        int priceComparison = Double.compare(this.price, other.price);
        
        if (priceComparison != 0) {
            return priceComparison;
        }
        
        // If prices are equal, compare by name
        return this.name.compareTo(other.name);
    }
    
    @Override
    public String toString() {
        return "Product{name='" + name + "', price=" + price + ", quantity=" + quantity + "}";
    }
}
```

---

## Comparator Interface

### Definition

```java
package java.util;

@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
    
    // Default and static methods (Java 8+)
    default Comparator<T> reversed() { ... }
    default Comparator<T> thenComparing(Comparator<? super T> other) { ... }
    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() { ... }
    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() { ... }
    // ... more methods
}
```

### Purpose
- Defines **custom ordering** of objects
- Implemented **outside the class** being compared
- Provides **multiple sorting sequences**
- Can sort objects without modifying their source code

### Return Values

```java
compare(T o1, T o2) returns:
- Negative integer: if o1 < o2
- Zero: if o1 == o2
- Positive integer: if o1 > o2
```

### Basic Example

```java
// Comparator for sorting by name
public class EmployeeNameComparator implements Comparator<Employee> {
    @Override
    public int compare(Employee e1, Employee e2) {
        return e1.getName().compareTo(e2.getName());
    }
}

// Comparator for sorting by salary
public class EmployeeSalaryComparator implements Comparator<Employee> {
    @Override
    public int compare(Employee e1, Employee e2) {
        return Double.compare(e1.getSalary(), e2.getSalary());
    }
}
```

### Usage

```java
public class ComparatorExample {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(3, "Alice", 75000));
        employees.add(new Employee(1, "Bob", 60000));
        employees.add(new Employee(2, "Charlie", 80000));
        
        // Sort by name
        Collections.sort(employees, new EmployeeNameComparator());
        System.out.println("Sorted by Name:");
        employees.forEach(System.out::println);
        
        System.out.println();
        
        // Sort by salary
        Collections.sort(employees, new EmployeeSalaryComparator());
        System.out.println("Sorted by Salary:");
        employees.forEach(System.out::println);
    }
}
```

**Output:**
```
Sorted by Name:
Employee{id=3, name='Alice', salary=75000.0}
Employee{id=1, name='Bob', salary=60000.0}
Employee{id=2, name='Charlie', salary=80000.0}

Sorted by Salary:
Employee{id=1, name='Bob', salary=60000.0}
Employee{id=3, name='Alice', salary=75000.0}
Employee{id=2, name='Charlie', salary=80000.0}
```

---

## Key Differences

### 1. Location of Implementation

**Comparable:**
```java
// Inside the class
public class Employee implements Comparable<Employee> {
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
}
```

**Comparator:**
```java
// Outside the class (separate class or anonymous)
public class EmployeeComparator implements Comparator<Employee> {
    @Override
    public int compare(Employee e1, Employee e2) {
        return e1.getName().compareTo(e2.getName());
    }
}
```

---

### 2. Number of Sorting Sequences

**Comparable:**
```java
// Only ONE natural ordering
public class Employee implements Comparable<Employee> {
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id); // Always sorts by ID
    }
}
```

**Comparator:**
```java
// MULTIPLE custom orderings
Comparator<Employee> byName = (e1, e2) -> e1.getName().compareTo(e2.getName());
Comparator<Employee> bySalary = (e1, e2) -> Double.compare(e1.getSalary(), e2.getSalary());
Comparator<Employee> byId = (e1, e2) -> Integer.compare(e1.getId(), e2.getId());

// Use different comparators as needed
Collections.sort(employees, byName);
Collections.sort(employees, bySalary);
Collections.sort(employees, byId);
```

---

### 3. Modification of Original Class

**Comparable:**
```java
// Requires modifying the original class
public class Employee implements Comparable<Employee> {
    // Must add compareTo() method
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
}
```

**Comparator:**
```java
// No modification needed - works with existing classes
// Can even sort classes from external libraries
Comparator<String> lengthComparator = (s1, s2) -> 
    Integer.compare(s1.length(), s2.length());

List<String> words = Arrays.asList("apple", "pie", "banana");
Collections.sort(words, lengthComparator);
```

---

### 4. Method Signature

**Comparable:**
```java
// Single parameter (comparing with 'this')
public int compareTo(Employee other) {
    return this.id - other.id;
}
```

**Comparator:**
```java
// Two parameters (comparing two objects)
public int compare(Employee e1, Employee e2) {
    return e1.getId() - e2.getId();
}
```

---

### 5. Sorting Methods

**Comparable:**
```java
List<Employee> employees = new ArrayList<>();
// ... add employees

// Simple sort (uses natural ordering)
Collections.sort(employees);
employees.sort(null); // null means natural ordering
```

**Comparator:**
```java
List<Employee> employees = new ArrayList<>();
// ... add employees

// Sort with custom comparator
Collections.sort(employees, new EmployeeNameComparator());
employees.sort(new EmployeeSalaryComparator());
```

---

## When to Use Which

### Use Comparable When:

1. ✅ You have **access to source code** of the class
2. ✅ There is a **single, natural ordering** for the objects
3. ✅ The ordering is **fundamental** to the class (e.g., Integer, String)
4. ✅ You want **default sorting behavior**

**Examples:**
- `Integer` (natural ordering: ascending)
- `String` (natural ordering: alphabetical)
- `Date` (natural ordering: chronological)
- Employee sorted by ID (primary key)

---

### Use Comparator When:

1. ✅ You **don't have access** to source code
2. ✅ You need **multiple sorting sequences**
3. ✅ You want to sort by **different criteria** at different times
4. ✅ You want to sort **third-party classes**
5. ✅ You need **complex sorting logic**

**Examples:**
- Sort employees by name, salary, or department
- Sort products by price, rating, or popularity
- Sort strings by length instead of alphabetically
- Sort dates in reverse chronological order

---

## Practical Examples

### Example 1: Employee Sorting (Both Approaches)

```java
public class Employee implements Comparable<Employee> {
    private int id;
    private String name;
    private double salary;
    private String department;
    
    public Employee(int id, String name, double salary, String department) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.department = department;
    }
    
    // Comparable: Natural ordering by ID
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getSalary() { return salary; }
    public String getDepartment() { return department; }
    
    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', salary=%.2f, dept='%s'}", 
            id, name, salary, department);
    }
}

// Comparator: Multiple custom orderings
public class EmployeeComparators {
    
    // Sort by name
    public static Comparator<Employee> byName() {
        return (e1, e2) -> e1.getName().compareTo(e2.getName());
    }
    
    // Sort by salary (descending)
    public static Comparator<Employee> bySalaryDesc() {
        return (e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary());
    }
    
    // Sort by department, then by name
    public static Comparator<Employee> byDepartmentThenName() {
        return Comparator.comparing(Employee::getDepartment)
                         .thenComparing(Employee::getName);
    }
    
    // Sort by salary range (custom logic)
    public static Comparator<Employee> bySalaryRange() {
        return (e1, e2) -> {
            String range1 = getSalaryRange(e1.getSalary());
            String range2 = getSalaryRange(e2.getSalary());
            return range1.compareTo(range2);
        };
    }
    
    private static String getSalaryRange(double salary) {
        if (salary < 50000) return "Low";
        if (salary < 80000) return "Medium";
        return "High";
    }
}

// Usage
public class EmployeeSortingDemo {
    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee(3, "Alice", 75000, "IT"),
            new Employee(1, "Bob", 60000, "HR"),
            new Employee(2, "Charlie", 90000, "IT"),
            new Employee(4, "Diana", 55000, "HR")
        );
        
        // Natural ordering (by ID)
        System.out.println("Natural ordering (by ID):");
        Collections.sort(employees);
        employees.forEach(System.out::println);
        
        System.out.println("\nBy Name:");
        employees.sort(EmployeeComparators.byName());
        employees.forEach(System.out::println);
        
        System.out.println("\nBy Salary (Descending):");
        employees.sort(EmployeeComparators.bySalaryDesc());
        employees.forEach(System.out::println);
        
        System.out.println("\nBy Department, then Name:");
        employees.sort(EmployeeComparators.byDepartmentThenName());
        employees.forEach(System.out::println);
    }
}
```

---

### Example 2: Sorting Collections

```java
public class SortingExamples {
    
    public static void main(String[] args) {
        
        // 1. TreeSet with Comparable (natural ordering)
        Set<Integer> numbers = new TreeSet<>();
        numbers.add(5);
        numbers.add(2);
        numbers.add(8);
        numbers.add(1);
        System.out.println("TreeSet (natural): " + numbers); // [1, 2, 5, 8]
        
        // 2. TreeSet with Comparator (custom ordering)
        Set<Integer> reverseNumbers = new TreeSet<>(Comparator.reverseOrder());
        reverseNumbers.add(5);
        reverseNumbers.add(2);
        reverseNumbers.add(8);
        reverseNumbers.add(1);
        System.out.println("TreeSet (reverse): " + reverseNumbers); // [8, 5, 2, 1]
        
        // 3. PriorityQueue with Comparable
        Queue<Integer> minHeap = new PriorityQueue<>();
        minHeap.addAll(Arrays.asList(5, 2, 8, 1));
        System.out.println("Min Heap: " + minHeap.poll()); // 1
        
        // 4. PriorityQueue with Comparator (max heap)
        Queue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.addAll(Arrays.asList(5, 2, 8, 1));
        System.out.println("Max Heap: " + maxHeap.poll()); // 8
        
        // 5. Sorting arrays
        Integer[] arr = {5, 2, 8, 1};
        Arrays.sort(arr); // Natural ordering
        System.out.println("Array sorted: " + Arrays.toString(arr));
        
        Arrays.sort(arr, Comparator.reverseOrder()); // Custom ordering
        System.out.println("Array reverse: " + Arrays.toString(arr));
    }
}
```

---

## Java 8+ Features

### Lambda Expressions

```java
// Old way (anonymous class)
Comparator<Employee> byName = new Comparator<Employee>() {
    @Override
    public int compare(Employee e1, Employee e2) {
        return e1.getName().compareTo(e2.getName());
    }
};

// Java 8+ (lambda)
Comparator<Employee> byName = (e1, e2) -> e1.getName().compareTo(e2.getName());
```

---

### Method References

```java
// Lambda
Comparator<Employee> byName = (e1, e2) -> e1.getName().compareTo(e2.getName());

// Method reference (even cleaner)
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
```

---

### Comparator Static Methods

```java
// 1. comparing() - single field
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
Comparator<Employee> bySalary = Comparator.comparing(Employee::getSalary);

// 2. comparingInt(), comparingLong(), comparingDouble() - primitives
Comparator<Employee> byId = Comparator.comparingInt(Employee::getId);
Comparator<Employee> bySalary = Comparator.comparingDouble(Employee::getSalary);

// 3. naturalOrder() and reverseOrder()
Comparator<String> natural = Comparator.naturalOrder();
Comparator<String> reverse = Comparator.reverseOrder();

// 4. nullsFirst() and nullsLast()
Comparator<Employee> nullSafe = Comparator.nullsFirst(
    Comparator.comparing(Employee::getName)
);
```

---

### Comparator Chaining

```java
// Multiple sorting criteria
Comparator<Employee> comparator = Comparator
    .comparing(Employee::getDepartment)           // First by department
    .thenComparing(Employee::getSalary)           // Then by salary
    .thenComparing(Employee::getName);            // Then by name

employees.sort(comparator);

// With reversed order
Comparator<Employee> comparator = Comparator
    .comparing(Employee::getDepartment)
    .thenComparing(Employee::getSalary, Comparator.reverseOrder())
    .thenComparing(Employee::getName);
```

---

### Complete Java 8+ Example

```java
public class Java8ComparatorExample {
    
    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
            new Employee(3, "Alice", 75000, "IT"),
            new Employee(1, "Bob", 60000, "HR"),
            new Employee(2, "Charlie", 90000, "IT"),
            new Employee(4, "Diana", 55000, "HR"),
            new Employee(5, null, 70000, "IT") // null name
        );
        
        // 1. Simple sorting
        employees.sort(Comparator.comparing(Employee::getName, 
            Comparator.nullsLast(Comparator.naturalOrder())));
        
        // 2. Multiple criteria
        employees.sort(
            Comparator.comparing(Employee::getDepartment)
                     .thenComparingDouble(Employee::getSalary)
                     .reversed()
        );
        
        // 3. Custom comparator with lambda
        employees.sort((e1, e2) -> {
            // Custom logic: sort by salary range
            int range1 = (int) (e1.getSalary() / 10000);
            int range2 = (int) (e2.getSalary() / 10000);
            return Integer.compare(range1, range2);
        });
        
        // 4. Stream sorting
        List<Employee> sortedByName = employees.stream()
            .sorted(Comparator.comparing(Employee::getName, 
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
        
        // 5. Top N employees by salary
        List<Employee> top3BySalary = employees.stream()
            .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
            .limit(3)
            .collect(Collectors.toList());
        
        System.out.println("Top 3 by salary:");
        top3BySalary.forEach(System.out::println);
    }
}
```

---

## Best Practices

### 1. Consistency with equals()

```java
public class Employee implements Comparable<Employee> {
    private int id;
    private String name;
    
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Employee)) return false;
        Employee other = (Employee) obj;
        return this.id == other.id; // Consistent with compareTo
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

**Rule:** If `compareTo()` returns 0, `equals()` should return true.

---

### 2. Null Safety

```java
// Bad - NullPointerException risk
Comparator<Employee> byName = (e1, e2) -> e1.getName().compareTo(e2.getName());

// Good - Null safe
Comparator<Employee> byName = Comparator.comparing(
    Employee::getName,
    Comparator.nullsLast(Comparator.naturalOrder())
);

// Or handle nulls explicitly
Comparator<Employee> byName = (e1, e2) -> {
    if (e1.getName() == null && e2.getName() == null) return 0;
    if (e1.getName() == null) return 1;
    if (e2.getName() == null) return -1;
    return e1.getName().compareTo(e2.getName());
};
```

---

### 3. Avoid Subtraction for Comparison

```java
// Bad - can cause integer overflow
@Override
public int compareTo(Employee other) {
    return this.id - other.id; // DANGEROUS!
}

// Good - use Integer.compare()
@Override
public int compareTo(Employee other) {
    return Integer.compare(this.id, other.id);
}

// For doubles
return Double.compare(this.salary, other.salary);

// For longs
return Long.compare(this.timestamp, other.timestamp);
```

---

### 4. Use Comparator.comparing() for Cleaner Code

```java
// Old way
Comparator<Employee> byName = new Comparator<Employee>() {
    public int compare(Employee e1, Employee e2) {
        return e1.getName().compareTo(e2.getName());
    }
};

// Better
Comparator<Employee> byName = (e1, e2) -> e1.getName().compareTo(e2.getName());

// Best
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
```

---

### 5. Reusable Comparators

```java
public class EmployeeComparators {
    public static final Comparator<Employee> BY_ID = 
        Comparator.comparingInt(Employee::getId);
    
    public static final Comparator<Employee> BY_NAME = 
        Comparator.comparing(Employee::getName);
    
    public static final Comparator<Employee> BY_SALARY_DESC = 
        Comparator.comparingDouble(Employee::getSalary).reversed();
    
    public static final Comparator<Employee> BY_DEPT_THEN_SALARY = 
        Comparator.comparing(Employee::getDepartment)
                 .thenComparingDouble(Employee::getSalary);
}

// Usage
employees.sort(EmployeeComparators.BY_NAME);
employees.sort(EmployeeComparators.BY_SALARY_DESC);
```

---

## Common Interview Questions

### Q1: What is the difference between Comparable and Comparator?

**Answer:**
- **Comparable** is for natural ordering, implemented inside the class with `compareTo()`
- **Comparator** is for custom ordering, implemented outside the class with `compare()`
- Comparable provides single sorting sequence, Comparator provides multiple
- Comparable requires source code access, Comparator doesn't

---

### Q2: Can a class implement both Comparable and use Comparator?

**Answer:** Yes!

```java
public class Employee implements Comparable<Employee> {
    // Natural ordering by ID
    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
}

// Custom comparators for different orderings
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
Comparator<Employee> bySalary = Comparator.comparing(Employee::getSalary);

// Use natural ordering
Collections.sort(employees);

// Use custom ordering
Collections.sort(employees, byName);
```

---

### Q3: How do you sort in descending order?

**Answer:**

```java
// Comparable - reverse natural order
Collections.sort(list, Collections.reverseOrder());

// Comparator - reverse custom order
Comparator<Employee> bySalaryDesc = 
    Comparator.comparing(Employee::getSalary).reversed();

// Or
Comparator<Employee> bySalaryDesc = 
    (e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary());
```

---

### Q4: What happens if compareTo() or compare() returns inconsistent results?

**Answer:**
- Violates the contract of comparison
- Can cause unpredictable sorting behavior
- TreeSet/TreeMap may not work correctly
- Can lead to infinite loops in sorting algorithms

```java
// Bad - inconsistent
public int compareTo(Employee other) {
    return new Random().nextInt(3) - 1; // Random: -1, 0, or 1
}

// Good - consistent
public int compareTo(Employee other) {
    return Integer.compare(this.id, other.id);
}
```

---

### Q5: How do you handle null values in sorting?

**Answer:**

```java
// Using Comparator.nullsFirst() or nullsLast()
Comparator<Employee> comparator = Comparator.comparing(
    Employee::getName,
    Comparator.nullsLast(Comparator.naturalOrder())
);

// Manual null handling
Comparator<Employee> comparator = (e1, e2) -> {
    if (e1 == null && e2 == null) return 0;
    if (e1 == null) return 1;  // nulls last
    if (e2 == null) return -1;
    return e1.getName().compareTo(e2.getName());
};
```

---

## Summary

### Quick Decision Guide

```
Do you have access to source code?
├─ YES
│  └─ Is there a single, natural ordering?
│     ├─ YES → Use Comparable
│     └─ NO → Use Comparator
└─ NO → Use Comparator

Do you need multiple sorting criteria?
├─ YES → Use Comparator
└─ NO → Use Comparable (if you have source code)
```

### Key Takeaways

✅ **Comparable**: Natural ordering, inside class, single sequence  
✅ **Comparator**: Custom ordering, outside class, multiple sequences  
✅ Use `Integer.compare()`, `Double.compare()` instead of subtraction  
✅ Make comparisons consistent with `equals()`  
✅ Handle null values properly  
✅ Use Java 8+ features for cleaner code  
✅ Create reusable comparators for common sorting needs  

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Author**: System Designs Collection
