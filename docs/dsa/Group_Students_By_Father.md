# Group Students by Father Name

## Problem Statement
Given an ArrayList of students with their father names, group students based on their father name.

**Example**:
```
Input:
[
  {name: "Alice", fatherName: "John"},
  {name: "Bob", fatherName: "Mike"},
  {name: "Charlie", fatherName: "John"},
  {name: "David", fatherName: "Mike"},
  {name: "Eve", fatherName: "John"}
]

Output:
{
  "John": ["Alice", "Charlie", "Eve"],
  "Mike": ["Bob", "David"]
}
```

## Implementation

### Solution 1: Using HashMap (Most Efficient)

```java
import java.util.*;

class Student {
    String name;
    String fatherName;
    
    public Student(String name, String fatherName) {
        this.name = name;
        this.fatherName = fatherName;
    }
}

public class GroupStudentsByFather {
    
    public static Map<String, List<String>> groupByFather(List<Student> students) {
        Map<String, List<String>> grouped = new HashMap<>();
        
        for (Student student : students) {
            grouped.computeIfAbsent(student.fatherName, k -> new ArrayList<>())
                   .add(student.name);
        }
        
        return grouped;
    }
    
    public static void main(String[] args) {
        List<Student> students = Arrays.asList(
            new Student("Alice", "John"),
            new Student("Bob", "Mike"),
            new Student("Charlie", "John"),
            new Student("David", "Mike"),
            new Student("Eve", "John")
        );
        
        Map<String, List<String>> result = groupByFather(students);
        
        // Print result
        result.forEach((father, children) -> {
            System.out.println(father + ": " + children);
        });
    }
}
```

**Output**:
```
John: [Alice, Charlie, Eve]
Mike: [Bob, David]
```

### Solution 2: Using Java 8 Streams

```java
import java.util.*;
import java.util.stream.Collectors;

public class GroupStudentsByFatherStream {
    
    public static Map<String, List<String>> groupByFather(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                    s -> s.fatherName,
                    Collectors.mapping(s -> s.name, Collectors.toList())
                ));
    }
    
    public static void main(String[] args) {
        List<Student> students = Arrays.asList(
            new Student("Alice", "John"),
            new Student("Bob", "Mike"),
            new Student("Charlie", "John"),
            new Student("David", "Mike"),
            new Student("Eve", "John")
        );
        
        Map<String, List<String>> result = groupByFather(students);
        result.forEach((father, children) -> 
            System.out.println(father + ": " + children)
        );
    }
}
```

### Solution 3: Group Student Objects (Not Just Names)

```java
public class GroupStudentObjects {
    
    public static Map<String, List<Student>> groupByFather(List<Student> students) {
        Map<String, List<Student>> grouped = new HashMap<>();
        
        for (Student student : students) {
            grouped.computeIfAbsent(student.fatherName, k -> new ArrayList<>())
                   .add(student);
        }
        
        return grouped;
    }
    
    // Using Streams
    public static Map<String, List<Student>> groupByFatherStream(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(s -> s.fatherName));
    }
}
```

### Solution 4: Manual Approach (Without computeIfAbsent)

```java
public class GroupStudentsManual {
    
    public static Map<String, List<String>> groupByFather(List<Student> students) {
        Map<String, List<String>> grouped = new HashMap<>();
        
        for (Student student : students) {
            if (!grouped.containsKey(student.fatherName)) {
                grouped.put(student.fatherName, new ArrayList<>());
            }
            grouped.get(student.fatherName).add(student.name);
        }
        
        return grouped;
    }
}
```

## Dry Run Example

**Input**: 
```
students = [
  {name: "Alice", fatherName: "John"},
  {name: "Bob", fatherName: "Mike"},
  {name: "Charlie", fatherName: "John"}
]
```

### Step-by-Step Execution:

```
Initial: grouped = {}

Iteration 1: student = {name: "Alice", fatherName: "John"}
  - "John" not in map
  - Create new list: grouped = {"John": []}
  - Add "Alice": grouped = {"John": ["Alice"]}

Iteration 2: student = {name: "Bob", fatherName: "Mike"}
  - "Mike" not in map
  - Create new list: grouped = {"John": ["Alice"], "Mike": []}
  - Add "Bob": grouped = {"John": ["Alice"], "Mike": ["Bob"]}

Iteration 3: student = {name: "Charlie", fatherName: "John"}
  - "John" exists in map
  - Add "Charlie": grouped = {"John": ["Alice", "Charlie"], "Mike": ["Bob"]}

Final Result:
{
  "John": ["Alice", "Charlie"],
  "Mike": ["Bob"]
}
```

## Edge Test Cases

```java
// Test Case 1: Empty list
List<Student> students1 = new ArrayList<>();
Map<String, List<String>> result1 = groupByFather(students1);
// Expected: {}

// Test Case 2: Single student
List<Student> students2 = Arrays.asList(
    new Student("Alice", "John")
);
Map<String, List<String>> result2 = groupByFather(students2);
// Expected: {"John": ["Alice"]}

// Test Case 3: All students have same father
List<Student> students3 = Arrays.asList(
    new Student("Alice", "John"),
    new Student("Bob", "John"),
    new Student("Charlie", "John")
);
Map<String, List<String>> result3 = groupByFather(students3);
// Expected: {"John": ["Alice", "Bob", "Charlie"]}

// Test Case 4: All students have different fathers
List<Student> students4 = Arrays.asList(
    new Student("Alice", "John"),
    new Student("Bob", "Mike"),
    new Student("Charlie", "David")
);
Map<String, List<String>> result4 = groupByFather(students4);
// Expected: {"John": ["Alice"], "Mike": ["Bob"], "David": ["Charlie"]}

// Test Case 5: Null father name (handle carefully)
List<Student> students5 = Arrays.asList(
    new Student("Alice", "John"),
    new Student("Bob", null),
    new Student("Charlie", "John")
);
Map<String, List<String>> result5 = groupByFather(students5);
// Expected: {"John": ["Alice", "Charlie"], null: ["Bob"]}

// Test Case 6: Duplicate student names with same father
List<Student> students6 = Arrays.asList(
    new Student("Alice", "John"),
    new Student("Alice", "John"),
    new Student("Bob", "John")
);
Map<String, List<String>> result6 = groupByFather(students6);
// Expected: {"John": ["Alice", "Alice", "Bob"]}

// Test Case 7: Case sensitivity
List<Student> students7 = Arrays.asList(
    new Student("Alice", "John"),
    new Student("Bob", "john"),
    new Student("Charlie", "JOHN")
);
Map<String, List<String>> result7 = groupByFather(students7);
// Expected: {"John": ["Alice"], "john": ["Bob"], "JOHN": ["Charlie"]}
```

## Time Complexity

**O(n)** where n = number of students
- Single iteration through the list
- HashMap operations (put, get, containsKey) are O(1) average case
- Adding to ArrayList is O(1) amortized

## Space Complexity

**O(n)** where n = number of students
- HashMap stores all students grouped by father
- In worst case (all different fathers), we have n entries
- Each entry contains a list with student names

## Variations

### 1. Group by Multiple Criteria

```java
// Group by father name and city
public static Map<String, Map<String, List<String>>> groupByFatherAndCity(
    List<Student> students) {
    
    Map<String, Map<String, List<String>>> grouped = new HashMap<>();
    
    for (Student student : students) {
        grouped.computeIfAbsent(student.fatherName, k -> new HashMap<>())
               .computeIfAbsent(student.city, k -> new ArrayList<>())
               .add(student.name);
    }
    
    return grouped;
}
```

### 2. Count Students per Father

```java
public static Map<String, Integer> countByFather(List<Student> students) {
    Map<String, Integer> count = new HashMap<>();
    
    for (Student student : students) {
        count.put(student.fatherName, count.getOrDefault(student.fatherName, 0) + 1);
    }
    
    return count;
}

// Using Streams
public static Map<String, Long> countByFatherStream(List<Student> students) {
    return students.stream()
            .collect(Collectors.groupingBy(s -> s.fatherName, Collectors.counting()));
}
```

### 3. Sort Students Within Each Group

```java
public static Map<String, List<String>> groupAndSort(List<Student> students) {
    Map<String, List<String>> grouped = new HashMap<>();
    
    for (Student student : students) {
        grouped.computeIfAbsent(student.fatherName, k -> new ArrayList<>())
               .add(student.name);
    }
    
    // Sort each list
    grouped.values().forEach(Collections::sort);
    
    return grouped;
}
```

### 4. Filter and Group

```java
// Group only students whose father name starts with 'J'
public static Map<String, List<String>> groupFilteredByFather(List<Student> students) {
    return students.stream()
            .filter(s -> s.fatherName.startsWith("J"))
            .collect(Collectors.groupingBy(
                s -> s.fatherName,
                Collectors.mapping(s -> s.name, Collectors.toList())
            ));
}
```

## Real-World Use Cases

### 1. **Family Tree Management**
```java
// Group family members by parent
Map<String, List<String>> familyTree = groupByFather(familyMembers);
```

### 2. **School Administration**
```java
// Group students for parent-teacher meetings
Map<String, List<String>> parentMeetings = groupByFather(students);
```

### 3. **Contact Management**
```java
// Group contacts by company
Map<String, List<Contact>> contactsByCompany = groupByCompany(contacts);
```

### 4. **Order Processing**
```java
// Group orders by customer
Map<String, List<Order>> ordersByCustomer = groupByCustomer(orders);
```

### 5. **Log Analysis**
```java
// Group log entries by user
Map<String, List<LogEntry>> logsByUser = groupByUser(logs);
```

## Complete Working Example

```java
import java.util.*;
import java.util.stream.Collectors;

class Student {
    String name;
    String fatherName;
    
    public Student(String name, String fatherName) {
        this.name = name;
        this.fatherName = fatherName;
    }
    
    @Override
    public String toString() {
        return name + " (Father: " + fatherName + ")";
    }
}

public class CompleteExample {
    
    // Method 1: Using HashMap
    public static Map<String, List<String>> groupByFatherHashMap(List<Student> students) {
        Map<String, List<String>> grouped = new HashMap<>();
        for (Student student : students) {
            grouped.computeIfAbsent(student.fatherName, k -> new ArrayList<>())
                   .add(student.name);
        }
        return grouped;
    }
    
    // Method 2: Using Streams
    public static Map<String, List<String>> groupByFatherStream(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                    s -> s.fatherName,
                    Collectors.mapping(s -> s.name, Collectors.toList())
                ));
    }
    
    // Method 3: Group Student objects
    public static Map<String, List<Student>> groupStudentObjects(List<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(s -> s.fatherName));
    }
    
    public static void main(String[] args) {
        // Create sample data
        List<Student> students = Arrays.asList(
            new Student("Alice", "John"),
            new Student("Bob", "Mike"),
            new Student("Charlie", "John"),
            new Student("David", "Mike"),
            new Student("Eve", "John"),
            new Student("Frank", "Robert")
        );
        
        System.out.println("=== Method 1: HashMap ===");
        Map<String, List<String>> result1 = groupByFatherHashMap(students);
        result1.forEach((father, children) -> 
            System.out.println(father + ": " + children)
        );
        
        System.out.println("\n=== Method 2: Streams ===");
        Map<String, List<String>> result2 = groupByFatherStream(students);
        result2.forEach((father, children) -> 
            System.out.println(father + ": " + children)
        );
        
        System.out.println("\n=== Method 3: Student Objects ===");
        Map<String, List<Student>> result3 = groupStudentObjects(students);
        result3.forEach((father, studentList) -> {
            System.out.println(father + ":");
            studentList.forEach(s -> System.out.println("  - " + s.name));
        });
    }
}
```

**Output**:
```
=== Method 1: HashMap ===
John: [Alice, Charlie, Eve]
Mike: [Bob, David]
Robert: [Frank]

=== Method 2: Streams ===
John: [Alice, Charlie, Eve]
Mike: [Bob, David]
Robert: [Frank]

=== Method 3: Student Objects ===
John:
  - Alice
  - Charlie
  - Eve
Mike:
  - Bob
  - David
Robert:
  - Frank
```

## Key Takeaways

1. **HashMap.computeIfAbsent()** is the cleanest approach
2. **Streams** provide functional programming style
3. **Time Complexity**: O(n) - single pass through data
4. **Space Complexity**: O(n) - stores all students
5. **Use Case**: Common pattern for grouping data by a key
