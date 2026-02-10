# Java Streams - Grouping Students by Marks

## Student Class

```java
public class Student {
    private String name;
    private String course;
    private int marks;
    
    public Student(String name, String course, int marks) {
        this.name = name;
        this.course = course;
        this.marks = marks;
    }
    
    // Getters
    public String getName() { return name; }
    public String getCourse() { return course; }
    public int getMarks() { return marks; }
    
    @Override
    public String toString() {
        return "Student{name='" + name + "', course='" + course + "', marks=" + marks + "}";
    }
}
```

---

## Sample Data

```java
List<Student> students = Arrays.asList(
    new Student("Alice", "Math", 85),
    new Student("Bob", "Physics", 92),
    new Student("Charlie", "Math", 85),
    new Student("David", "Chemistry", 78),
    new Student("Eve", "Physics", 92),
    new Student("Frank", "Math", 78),
    new Student("Grace", "Chemistry", 85),
    new Student("Henry", "Physics", 65),
    new Student("Ivy", "Math", 92),
    new Student("Jack", "Chemistry", 65)
);
```

---

## Solution 1: Group by Exact Marks

### Using Collectors.groupingBy()

```java
import java.util.*;
import java.util.stream.Collectors;

public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks
        Map<Integer, List<Student>> groupedByMarks = students.stream()
            .collect(Collectors.groupingBy(Student::getMarks));
        
        // Print results
        groupedByMarks.forEach((marks, studentList) -> {
            System.out.println("Marks: " + marks);
            studentList.forEach(student -> System.out.println("  " + student));
        });
    }
}
```

**Output**:
```
Marks: 65
  Student{name='Henry', course='Physics', marks=65}
  Student{name='Jack', course='Chemistry', marks=65}
Marks: 78
  Student{name='David', course='Chemistry', marks=78}
  Student{name='Frank', course='Math', marks=78}
Marks: 85
  Student{name='Alice', course='Math', marks=85}
  Student{name='Charlie', course='Math', marks=85}
  Student{name='Grace', course='Chemistry', marks=85}
Marks: 92
  Student{name='Bob', course='Physics', marks=92}
  Student{name='Eve', course='Physics', marks=92}
  Student{name='Ivy', course='Math', marks=92}
```

---

## Solution 2: Group by Grade Range

### Define Grade Ranges

```java
public class StudentGrouping {
    
    public static String getGrade(int marks) {
        if (marks >= 90) return "A";
        if (marks >= 80) return "B";
        if (marks >= 70) return "C";
        if (marks >= 60) return "D";
        return "F";
    }
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by grade
        Map<String, List<Student>> groupedByGrade = students.stream()
            .collect(Collectors.groupingBy(s -> getGrade(s.getMarks())));
        
        // Print results (sorted by grade)
        groupedByGrade.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.println("Grade: " + entry.getKey());
                entry.getValue().forEach(student -> 
                    System.out.println("  " + student));
            });
    }
}
```

**Output**:
```
Grade: A
  Student{name='Bob', course='Physics', marks=92}
  Student{name='Eve', course='Physics', marks=92}
  Student{name='Ivy', course='Math', marks=92}
Grade: B
  Student{name='Alice', course='Math', marks=85}
  Student{name='Charlie', course='Math', marks=85}
  Student{name='Grace', course='Chemistry', marks=85}
Grade: C
  Student{name='David', course='Chemistry', marks=78}
  Student{name='Frank', course='Math', marks=78}
Grade: D
  Student{name='Henry', course='Physics', marks=65}
  Student{name='Jack', course='Chemistry', marks=65}
```

---

## Solution 3: Group by Marks Range (10-point intervals)

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks range (60-69, 70-79, 80-89, 90-100)
        Map<String, List<Student>> groupedByRange = students.stream()
            .collect(Collectors.groupingBy(s -> {
                int marks = s.getMarks();
                int range = (marks / 10) * 10;
                return range + "-" + (range + 9);
            }));
        
        // Print results
        groupedByRange.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                System.out.println("Range: " + entry.getKey());
                entry.getValue().forEach(student -> 
                    System.out.println("  " + student));
            });
    }
}
```

**Output**:
```
Range: 60-69
  Student{name='Henry', course='Physics', marks=65}
  Student{name='Jack', course='Chemistry', marks=65}
Range: 70-79
  Student{name='David', course='Chemistry', marks=78}
  Student{name='Frank', course='Math', marks=78}
Range: 80-89
  Student{name='Alice', course='Math', marks=85}
  Student{name='Charlie', course='Math', marks=85}
  Student{name='Grace', course='Chemistry', marks=85}
Range: 90-99
  Student{name='Bob', course='Physics', marks=92}
  Student{name='Eve', course='Physics', marks=92}
  Student{name='Ivy', course='Math', marks=92}
```

---

## Solution 4: Group by Marks with Count

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks and count students
        Map<Integer, Long> marksCount = students.stream()
            .collect(Collectors.groupingBy(
                Student::getMarks,
                Collectors.counting()
            ));
        
        // Print results
        marksCount.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> 
                System.out.println("Marks: " + entry.getKey() + 
                                 " -> Count: " + entry.getValue()));
    }
}
```

**Output**:
```
Marks: 65 -> Count: 2
Marks: 78 -> Count: 2
Marks: 85 -> Count: 3
Marks: 92 -> Count: 3
```

---

## Solution 5: Group by Marks with Names Only

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks, collect only names
        Map<Integer, List<String>> marksToNames = students.stream()
            .collect(Collectors.groupingBy(
                Student::getMarks,
                Collectors.mapping(Student::getName, Collectors.toList())
            ));
        
        // Print results
        marksToNames.forEach((marks, names) -> 
            System.out.println("Marks: " + marks + " -> Students: " + names));
    }
}
```

**Output**:
```
Marks: 65 -> Students: [Henry, Jack]
Marks: 78 -> Students: [David, Frank]
Marks: 85 -> Students: [Alice, Charlie, Grace]
Marks: 92 -> Students: [Bob, Eve, Ivy]
```

---

## Solution 6: Multi-level Grouping (Course → Marks)

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by course, then by marks
        Map<String, Map<Integer, List<Student>>> groupedByCourseAndMarks = 
            students.stream()
                .collect(Collectors.groupingBy(
                    Student::getCourse,
                    Collectors.groupingBy(Student::getMarks)
                ));
        
        // Print results
        groupedByCourseAndMarks.forEach((course, marksMap) -> {
            System.out.println("Course: " + course);
            marksMap.forEach((marks, studentList) -> {
                System.out.println("  Marks: " + marks);
                studentList.forEach(student -> 
                    System.out.println("    " + student.getName()));
            });
        });
    }
}
```

**Output**:
```
Course: Chemistry
  Marks: 65
    Jack
  Marks: 78
    David
  Marks: 85
    Grace
Course: Math
  Marks: 78
    Frank
  Marks: 85
    Alice
    Charlie
  Marks: 92
    Ivy
Course: Physics
  Marks: 65
    Henry
  Marks: 92
    Bob
    Eve
```

---

## Solution 7: Group by Marks with Average per Group

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by grade and calculate average marks
        Map<String, Double> gradeAverages = students.stream()
            .collect(Collectors.groupingBy(
                s -> getGrade(s.getMarks()),
                Collectors.averagingInt(Student::getMarks)
            ));
        
        // Print results
        gradeAverages.forEach((grade, avg) -> 
            System.out.printf("Grade: %s -> Average: %.2f%n", grade, avg));
    }
    
    public static String getGrade(int marks) {
        if (marks >= 90) return "A";
        if (marks >= 80) return "B";
        if (marks >= 70) return "C";
        if (marks >= 60) return "D";
        return "F";
    }
}
```

**Output**:
```
Grade: A -> Average: 92.00
Grade: B -> Average: 85.00
Grade: C -> Average: 78.00
Grade: D -> Average: 65.00
```

---

## Solution 8: Group by Marks with Statistics

```java
import java.util.IntSummaryStatistics;

public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by course and get statistics
        Map<String, IntSummaryStatistics> courseStats = students.stream()
            .collect(Collectors.groupingBy(
                Student::getCourse,
                Collectors.summarizingInt(Student::getMarks)
            ));
        
        // Print results
        courseStats.forEach((course, stats) -> {
            System.out.println("Course: " + course);
            System.out.println("  Count: " + stats.getCount());
            System.out.println("  Min: " + stats.getMin());
            System.out.println("  Max: " + stats.getMax());
            System.out.printf("  Average: %.2f%n", stats.getAverage());
            System.out.println("  Sum: " + stats.getSum());
        });
    }
}
```

**Output**:
```
Course: Chemistry
  Count: 3
  Min: 65
  Max: 85
  Average: 76.00
  Sum: 228
Course: Math
  Count: 4
  Min: 78
  Max: 92
  Average: 85.00
  Sum: 340
Course: Physics
  Count: 3
  Min: 65
  Max: 92
  Average: 83.00
  Sum: 249
```

---

## Solution 9: Partition by Pass/Fail (Passing marks = 75)

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Partition by pass/fail (passing marks = 75)
        Map<Boolean, List<Student>> passFailPartition = students.stream()
            .collect(Collectors.partitioningBy(s -> s.getMarks() >= 75));
        
        // Print results
        System.out.println("PASSED:");
        passFailPartition.get(true).forEach(s -> System.out.println("  " + s));
        
        System.out.println("\nFAILED:");
        passFailPartition.get(false).forEach(s -> System.out.println("  " + s));
    }
}
```

**Output**:
```
PASSED:
  Student{name='Alice', course='Math', marks=85}
  Student{name='Bob', course='Physics', marks=92}
  Student{name='Charlie', course='Math', marks=85}
  Student{name='David', course='Chemistry', marks=78}
  Student{name='Eve', course='Physics', marks=92}
  Student{name='Frank', course='Math', marks=78}
  Student{name='Grace', course='Chemistry', marks=85}
  Student{name='Ivy', course='Math', marks=92}

FAILED:
  Student{name='Henry', course='Physics', marks=65}
  Student{name='Jack', course='Chemistry', marks=65}
```

---

## Solution 10: Group by Marks and Sort

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks using TreeMap (sorted by key)
        Map<Integer, List<Student>> sortedGroupedByMarks = students.stream()
            .collect(Collectors.groupingBy(
                Student::getMarks,
                TreeMap::new,  // Use TreeMap for sorted keys
                Collectors.toList()
            ));
        
        // Print results (already sorted by marks)
        sortedGroupedByMarks.forEach((marks, studentList) -> {
            System.out.println("Marks: " + marks + " (" + studentList.size() + " students)");
            studentList.stream()
                .sorted(Comparator.comparing(Student::getName))
                .forEach(student -> System.out.println("  " + student.getName()));
        });
    }
}
```

**Output**:
```
Marks: 65 (2 students)
  Henry
  Jack
Marks: 78 (2 students)
  David
  Frank
Marks: 85 (3 students)
  Alice
  Charlie
  Grace
Marks: 92 (3 students)
  Bob
  Eve
  Ivy
```

---

## Solution 11: Top N Students per Marks Group

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks and get top 2 students by name
        Map<Integer, List<Student>> topStudentsPerMarks = students.stream()
            .collect(Collectors.groupingBy(
                Student::getMarks,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream()
                        .sorted(Comparator.comparing(Student::getName))
                        .limit(2)
                        .collect(Collectors.toList())
                )
            ));
        
        // Print results
        topStudentsPerMarks.forEach((marks, studentList) -> {
            System.out.println("Marks: " + marks);
            studentList.forEach(student -> System.out.println("  " + student.getName()));
        });
    }
}
```

**Output**:
```
Marks: 65
  Henry
  Jack
Marks: 78
  David
  Frank
Marks: 85
  Alice
  Charlie
Marks: 92
  Bob
  Eve
```

---

## Solution 12: Custom Grouping with TreeMap and Filtering

```java
public class StudentGrouping {
    
    public static void main(String[] args) {
        List<Student> students = getStudents();
        
        // Group by marks (only marks >= 80), sorted
        Map<Integer, List<String>> highScorers = students.stream()
            .filter(s -> s.getMarks() >= 80)
            .collect(Collectors.groupingBy(
                Student::getMarks,
                TreeMap::new,
                Collectors.mapping(
                    Student::getName,
                    Collectors.toList()
                )
            ));
        
        // Print results
        System.out.println("High Scorers (Marks >= 80):");
        highScorers.forEach((marks, names) -> 
            System.out.println("Marks: " + marks + " -> " + names));
    }
}
```

**Output**:
```
High Scorers (Marks >= 80):
Marks: 85 -> [Alice, Charlie, Grace]
Marks: 92 -> [Bob, Eve, Ivy]
```

---

## Complete Working Example

```java
import java.util.*;
import java.util.stream.Collectors;

class Student {
    private String name;
    private String course;
    private int marks;
    
    public Student(String name, String course, int marks) {
        this.name = name;
        this.course = course;
        this.marks = marks;
    }
    
    public String getName() { return name; }
    public String getCourse() { return course; }
    public int getMarks() { return marks; }
    
    @Override
    public String toString() {
        return "Student{name='" + name + "', course='" + course + "', marks=" + marks + "}";
    }
}

public class StudentGroupingDemo {
    
    public static void main(String[] args) {
        List<Student> students = Arrays.asList(
            new Student("Alice", "Math", 85),
            new Student("Bob", "Physics", 92),
            new Student("Charlie", "Math", 85),
            new Student("David", "Chemistry", 78),
            new Student("Eve", "Physics", 92),
            new Student("Frank", "Math", 78),
            new Student("Grace", "Chemistry", 85),
            new Student("Henry", "Physics", 65),
            new Student("Ivy", "Math", 92),
            new Student("Jack", "Chemistry", 65)
        );
        
        // 1. Simple grouping by marks
        System.out.println("=== Group by Marks ===");
        Map<Integer, List<Student>> byMarks = students.stream()
            .collect(Collectors.groupingBy(Student::getMarks));
        byMarks.forEach((marks, list) -> 
            System.out.println(marks + " -> " + list.size() + " students"));
        
        // 2. Group by grade
        System.out.println("\n=== Group by Grade ===");
        Map<String, List<Student>> byGrade = students.stream()
            .collect(Collectors.groupingBy(s -> getGrade(s.getMarks())));
        byGrade.forEach((grade, list) -> 
            System.out.println(grade + " -> " + list.size() + " students"));
        
        // 3. Multi-level grouping
        System.out.println("\n=== Group by Course and Marks ===");
        Map<String, Map<Integer, List<Student>>> byCourseAndMarks = students.stream()
            .collect(Collectors.groupingBy(
                Student::getCourse,
                Collectors.groupingBy(Student::getMarks)
            ));
        byCourseAndMarks.forEach((course, marksMap) -> {
            System.out.println(course + ":");
            marksMap.forEach((marks, list) -> 
                System.out.println("  " + marks + " -> " + list.size() + " students"));
        });
        
        // 4. Partition by pass/fail
        System.out.println("\n=== Pass/Fail (Passing = 75) ===");
        Map<Boolean, List<Student>> passFailMap = students.stream()
            .collect(Collectors.partitioningBy(s -> s.getMarks() >= 75));
        System.out.println("Passed: " + passFailMap.get(true).size());
        System.out.println("Failed: " + passFailMap.get(false).size());
    }
    
    private static String getGrade(int marks) {
        if (marks >= 90) return "A";
        if (marks >= 80) return "B";
        if (marks >= 70) return "C";
        if (marks >= 60) return "D";
        return "F";
    }
}
```

**Output**:
```
=== Group by Marks ===
65 -> 2 students
78 -> 2 students
85 -> 3 students
92 -> 3 students

=== Group by Grade ===
A -> 3 students
B -> 3 students
C -> 2 students
D -> 2 students

=== Group by Course and Marks ===
Chemistry:
  65 -> 1 students
  78 -> 1 students
  85 -> 1 students
Math:
  78 -> 1 students
  85 -> 2 students
  92 -> 1 students
Physics:
  65 -> 1 students
  92 -> 2 students

=== Pass/Fail (Passing = 75) ===
Passed: 8
Failed: 2
```

---

## Summary of Collectors

| Collector | Purpose | Example |
|-----------|---------|---------|
| `groupingBy(classifier)` | Group by key | `groupingBy(Student::getMarks)` |
| `groupingBy(classifier, downstream)` | Group and transform | `groupingBy(Student::getMarks, counting())` |
| `groupingBy(classifier, mapFactory, downstream)` | Group with custom map | `groupingBy(Student::getMarks, TreeMap::new, toList())` |
| `partitioningBy(predicate)` | Split into true/false | `partitioningBy(s -> s.getMarks() >= 75)` |
| `counting()` | Count elements | `groupingBy(Student::getMarks, counting())` |
| `mapping(mapper, downstream)` | Transform elements | `groupingBy(Student::getMarks, mapping(Student::getName, toList()))` |
| `averagingInt()` | Calculate average | `groupingBy(Student::getCourse, averagingInt(Student::getMarks))` |
| `summarizingInt()` | Get statistics | `groupingBy(Student::getCourse, summarizingInt(Student::getMarks))` |

---

## Key Takeaways

1. ✅ Use `Collectors.groupingBy()` for simple grouping
2. ✅ Use `TreeMap::new` for sorted keys
3. ✅ Use `Collectors.mapping()` to transform grouped values
4. ✅ Use `Collectors.counting()` to count elements
5. ✅ Use `Collectors.partitioningBy()` for binary classification
6. ✅ Use multi-level grouping for complex hierarchies
7. ✅ Combine with `filter()`, `sorted()`, `limit()` for advanced queries
