# Best Average Grade

## Problem Statement

Given a list of student test scores where each student may have multiple scores, find the best (highest) average grade among all students.

**Input:** 2D String array `[studentName, score]`  
**Output:** Integer (best average grade)

**Example:**
```
Input: [
  ["Rohan", "84"],
  ["Sachin", "102"],
  ["Ishan", "55"],
  ["Sachin", "18"]
]

Averages:
  Rohan: 84 / 1 = 84
  Sachin: (102 + 18) / 2 = 60
  Ishan: 55 / 1 = 55

Output: 84 (Rohan's average)
```

---

## Solution Approach

### Optimal: HashMap to Track Sum and Count

**Time Complexity:** O(n)  
**Space Complexity:** O(k) where k = unique students

```java
public static Integer bestAvgGrade(String[][] scores) {
    if (scores == null || scores.length == 0) return 0;
    
    Map<String, int[]> studentData = new HashMap<>();
    
    // Collect sum and count for each student
    for (String[] score : scores) {
        String name = score[0];
        int grade = Integer.parseInt(score[1]);
        
        studentData.putIfAbsent(name, new int[2]);
        studentData.get(name)[0] += grade;  // sum
        studentData.get(name)[1]++;         // count
    }
    
    // Find best average
    int bestAvg = Integer.MIN_VALUE;
    for (int[] data : studentData.values()) {
        int avg = data[0] / data[1];
        bestAvg = Math.max(bestAvg, avg);
    }
    
    return bestAvg;
}
```

---

## Algorithm Walkthrough

### Example: Given Test Case

```
Input:
[["Rohan", "84"], ["Sachin", "102"], ["Ishan", "55"], ["Sachin", "18"]]

Step 1: Build student data map
  Process ["Rohan", "84"]:
    studentData = {"Rohan": [84, 1]}
  
  Process ["Sachin", "102"]:
    studentData = {"Rohan": [84, 1], "Sachin": [102, 1]}
  
  Process ["Ishan", "55"]:
    studentData = {"Rohan": [84, 1], "Sachin": [102, 1], "Ishan": [55, 1]}
  
  Process ["Sachin", "18"]:
    studentData = {"Rohan": [84, 1], "Sachin": [120, 2], "Ishan": [55, 1]}

Step 2: Calculate averages
  Rohan: 84 / 1 = 84
  Sachin: 120 / 2 = 60
  Ishan: 55 / 1 = 55

Step 3: Find maximum
  max(84, 60, 55) = 84

Result: 84
```

---

## Complete Implementation

```java
public class BestAverageGrade {
    
    // Approach 1: HashMap with int array (Recommended)
    public static Integer bestAvgGrade(String[][] scores) {
        if (scores == null || scores.length == 0) {
            return 0;
        }
        
        Map<String, int[]> studentData = new HashMap<>();
        
        // Collect sum and count for each student
        for (String[] score : scores) {
            String name = score[0];
            int grade = Integer.parseInt(score[1]);
            
            studentData.putIfAbsent(name, new int[2]);
            studentData.get(name)[0] += grade;  // sum
            studentData.get(name)[1]++;         // count
        }
        
        // Find best average
        int bestAvg = Integer.MIN_VALUE;
        for (int[] data : studentData.values()) {
            int avg = data[0] / data[1];
            bestAvg = Math.max(bestAvg, avg);
        }
        
        return bestAvg;
    }
    
    // Approach 2: Using custom class
    static class StudentStats {
        int sum;
        int count;
        
        void addScore(int score) {
            sum += score;
            count++;
        }
        
        int getAverage() {
            return sum / count;
        }
    }
    
    public static Integer bestAvgGradeWithClass(String[][] scores) {
        if (scores == null || scores.length == 0) {
            return 0;
        }
        
        Map<String, StudentStats> studentMap = new HashMap<>();
        
        for (String[] score : scores) {
            String name = score[0];
            int grade = Integer.parseInt(score[1]);
            
            studentMap.putIfAbsent(name, new StudentStats());
            studentMap.get(name).addScore(grade);
        }
        
        int bestAvg = Integer.MIN_VALUE;
        for (StudentStats stats : studentMap.values()) {
            bestAvg = Math.max(bestAvg, stats.getAverage());
        }
        
        return bestAvg;
    }
    
    // Approach 3: Using List for each student
    public static Integer bestAvgGradeWithList(String[][] scores) {
        if (scores == null || scores.length == 0) {
            return 0;
        }
        
        Map<String, List<Integer>> studentScores = new HashMap<>();
        
        for (String[] score : scores) {
            String name = score[0];
            int grade = Integer.parseInt(score[1]);
            
            studentScores.putIfAbsent(name, new ArrayList<>());
            studentScores.get(name).add(grade);
        }
        
        int bestAvg = Integer.MIN_VALUE;
        for (List<Integer> scores_list : studentScores.values()) {
            int sum = scores_list.stream().mapToInt(Integer::intValue).sum();
            int avg = sum / scores_list.size();
            bestAvg = Math.max(bestAvg, avg);
        }
        
        return bestAvg;
    }
    
    // Bonus: Return student name with best average
    public static String studentWithBestAvg(String[][] scores) {
        if (scores == null || scores.length == 0) {
            return null;
        }
        
        Map<String, int[]> studentData = new HashMap<>();
        
        for (String[] score : scores) {
            String name = score[0];
            int grade = Integer.parseInt(score[1]);
            
            studentData.putIfAbsent(name, new int[2]);
            studentData.get(name)[0] += grade;
            studentData.get(name)[1]++;
        }
        
        String bestStudent = null;
        int bestAvg = Integer.MIN_VALUE;
        
        for (Map.Entry<String, int[]> entry : studentData.entrySet()) {
            int avg = entry.getValue()[0] / entry.getValue()[1];
            if (avg > bestAvg) {
                bestAvg = avg;
                bestStudent = entry.getKey();
            }
        }
        
        return bestStudent;
    }
    
    // Bonus: Return all averages
    public static Map<String, Integer> allAverages(String[][] scores) {
        Map<String, int[]> studentData = new HashMap<>();
        
        for (String[] score : scores) {
            String name = score[0];
            int grade = Integer.parseInt(score[1]);
            
            studentData.putIfAbsent(name, new int[2]);
            studentData.get(name)[0] += grade;
            studentData.get(name)[1]++;
        }
        
        Map<String, Integer> averages = new HashMap<>();
        for (Map.Entry<String, int[]> entry : studentData.entrySet()) {
            int avg = entry.getValue()[0] / entry.getValue()[1];
            averages.put(entry.getKey(), avg);
        }
        
        return averages;
    }
    
    public static boolean pass() {
        String[][] s1 = {
            {"Rohan", "84"},
            {"Sachin", "102"},
            {"Ishan", "55"},
            {"Sachin", "18"}
        };
        
        return bestAvgGrade(s1) == 84;
    }
    
    public static void main(String[] args) {
        if (pass()) {
            System.out.println("Pass");
        } else {
            System.out.println("Some Fail");
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testBestAvgGrade() {
    // Given test case
    String[][] s1 = {
        {"Rohan", "84"},
        {"Sachin", "102"},
        {"Ishan", "55"},
        {"Sachin", "18"}
    };
    assertEquals(84, (int) bestAvgGrade(s1));
    
    // Single student
    String[][] s2 = {{"Alice", "90"}};
    assertEquals(90, (int) bestAvgGrade(s2));
    
    // All same scores
    String[][] s3 = {
        {"Bob", "80"},
        {"Bob", "80"},
        {"Bob", "80"}
    };
    assertEquals(80, (int) bestAvgGrade(s3));
    
    // Multiple students, same average
    String[][] s4 = {
        {"Alice", "100"},
        {"Bob", "50"},
        {"Bob", "50"}
    };
    assertEquals(100, (int) bestAvgGrade(s4));
    
    // Negative scores
    String[][] s5 = {
        {"Alice", "-10"},
        {"Bob", "20"}
    };
    assertEquals(20, (int) bestAvgGrade(s5));
    
    // Large numbers
    String[][] s6 = {
        {"Alice", "100"},
        {"Alice", "100"},
        {"Bob", "99"}
    };
    assertEquals(100, (int) bestAvgGrade(s6));
    
    // Empty array
    assertEquals(0, (int) bestAvgGrade(new String[][]{}));
}
```

---

## Visual Representation

```
Input Data:
┌────────┬───────┐
│ Name   │ Score │
├────────┼───────┤
│ Rohan  │  84   │
│ Sachin │ 102   │
│ Ishan  │  55   │
│ Sachin │  18   │
└────────┴───────┘

HashMap Structure:
┌────────┬─────────────┬───────────┐
│ Name   │ Sum  │ Count│  Average  │
├────────┼──────┼──────┼───────────┤
│ Rohan  │  84  │  1   │  84 / 1 = 84
│ Sachin │ 120  │  2   │ 120 / 2 = 60
│ Ishan  │  55  │  1   │  55 / 1 = 55
└────────┴──────┴──────┴───────────┘

Best Average: 84
```

---

## Edge Cases

| Input | Output | Explanation |
|-------|--------|-------------|
| `[]` | `0` | Empty array |
| `[["A", "100"]]` | `100` | Single student |
| `[["A", "50"], ["A", "50"]]` | `50` | Same scores |
| `[["A", "-10"], ["B", "20"]]` | `20` | Negative scores |
| `[["A", "0"], ["B", "0"]]` | `0` | Zero scores |

---

## Common Mistakes

1. **Not Handling Multiple Scores:**
   ```java
   // WRONG - only stores last score
   map.put(name, grade);
   
   // CORRECT - accumulates all scores
   map.get(name)[0] += grade;
   map.get(name)[1]++;
   ```

2. **Integer Division Truncation:**
   ```java
   // Be aware: 5 / 2 = 2 (not 2.5)
   int avg = sum / count;
   
   // For floating point: cast to double
   double avg = (double) sum / count;
   ```

3. **Not Initializing Array:**
   ```java
   // WRONG - NullPointerException
   map.get(name)[0] += grade;
   
   // CORRECT
   map.putIfAbsent(name, new int[2]);
   ```

4. **Not Handling Empty Input:**
   ```java
   if (scores == null || scores.length == 0) return 0;
   ```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| HashMap (int[]) | O(n) | O(k) | k = unique students |
| HashMap (List) | O(n) | O(n) | Stores all scores |
| HashMap (Class) | O(n) | O(k) | More readable |

**Where:**
- n = total number of scores
- k = number of unique students

---

## Why int[] Over List?

```
int[] approach:
- Space: O(k) - only 2 integers per student
- Time: O(n) - single pass

List approach:
- Space: O(n) - stores all scores
- Time: O(n) - single pass + sum calculation

int[] is more space-efficient!
```

---

## Alternative Data Structures

### Using Pair/Tuple
```java
Map<String, Pair<Integer, Integer>> map = new HashMap<>();
// Pair<sum, count>
```

### Using Custom Class
```java
class StudentStats {
    int sum, count;
    int getAverage() { return sum / count; }
}
```

### Using Two Maps
```java
Map<String, Integer> sums = new HashMap<>();
Map<String, Integer> counts = new HashMap<>();
// Less efficient - two lookups
```

---

## Related Problems

- **LeetCode 1086:** High Five (similar concept)
- **Group and aggregate data**
- **Student grade calculations**
- **Average calculations with grouping**

---

## Interview Tips

1. **Clarify Requirements:**
   - Return integer or double?
   - Handle empty input?
   - Negative scores possible?
   - What if tie in averages?

2. **Start with HashMap:**
   - Track sum and count per student
   - Single pass through data

3. **Explain Data Structure:**
   - int[2]: [sum, count]
   - More space-efficient than List

4. **Walk Through Example:**
   - Show Sachin with two scores
   - Calculate average: 120/2 = 60

5. **Discuss Complexity:**
   - O(n) time - single pass
   - O(k) space - unique students

---

## Real-World Applications

- **Grade Management Systems:** Student performance tracking
- **Sports Analytics:** Player statistics
- **Employee Performance:** Review aggregation
- **Survey Analysis:** Response averaging
- **Financial Analysis:** Portfolio performance

---

## Optimization: Stream API

```java
public static Integer bestAvgGradeStream(String[][] scores) {
    return Arrays.stream(scores)
        .collect(Collectors.groupingBy(
            s -> s[0],
            Collectors.averagingInt(s -> Integer.parseInt(s[1]))
        ))
        .values()
        .stream()
        .mapToInt(Double::intValue)
        .max()
        .orElse(0);
}

// More concise but slightly less efficient
```

---

## Key Takeaways

✅ Use HashMap to group scores by student  
✅ Store [sum, count] as int[2] for space efficiency  
✅ Single pass: O(n) time, O(k) space  
✅ Calculate average: sum / count (integer division)  
✅ Track maximum average while iterating  
✅ Handle empty input gracefully  
✅ putIfAbsent() to initialize new students  
✅ More space-efficient than storing all scores in List
