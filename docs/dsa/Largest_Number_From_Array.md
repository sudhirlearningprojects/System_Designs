# Largest Number from Array

## Problem Statement

Given a list of non-negative integers, arrange them to form the largest possible number.

**Input:** List of non-negative integers  
**Output:** Largest number as a string

**Examples:**
```
Input: [1, 34, 3, 98, 9, 76, 45, 4]
Output: "998764543431"

Input: [3, 30, 34, 5, 9]
Output: "9534330"

Input: [10, 2]
Output: "210"

Input: [0, 0]
Output: "0"

Input: [1]
Output: "1"
```

---

## Solution Approaches

### Approach 1: Custom Comparator (Optimal)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String largestNumber(int[] nums) {
    String[] strs = new String[nums.length];
    for (int i = 0; i < nums.length; i++) {
        strs[i] = String.valueOf(nums[i]);
    }
    
    Arrays.sort(strs, (a, b) -> (b + a).compareTo(a + b));
    
    if (strs[0].equals("0")) return "0";
    
    return String.join("", strs);
}
```

---

### Approach 2: Custom Comparator with StringBuilder

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String largestNumberBuilder(int[] nums) {
    String[] strs = new String[nums.length];
    for (int i = 0; i < nums.length; i++) {
        strs[i] = String.valueOf(nums[i]);
    }
    
    Arrays.sort(strs, (a, b) -> (b + a).compareTo(a + b));
    
    if (strs[0].equals("0")) return "0";
    
    StringBuilder sb = new StringBuilder();
    for (String s : strs) {
        sb.append(s);
    }
    
    return sb.toString();
}
```

---

### Approach 3: Using List

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static String largestNumberList(int[] nums) {
    List<String> list = new ArrayList<>();
    for (int num : nums) {
        list.add(String.valueOf(num));
    }
    
    Collections.sort(list, (a, b) -> (b + a).compareTo(a + b));
    
    if (list.get(0).equals("0")) return "0";
    
    return String.join("", list);
}
```

---

### Approach 4: Brute Force (Not Practical)

**Time Complexity:** O(n!)  
**Space Complexity:** O(n)

```java
public static String largestNumberBrute(int[] nums) {
    List<String> permutations = new ArrayList<>();
    generatePermutations(nums, 0, permutations);
    
    String max = "";
    for (String perm : permutations) {
        if (perm.compareTo(max) > 0) {
            max = perm;
        }
    }
    
    return max.equals("") ? "0" : max;
}

private static void generatePermutations(int[] nums, int start, List<String> result) {
    if (start == nums.length) {
        StringBuilder sb = new StringBuilder();
        for (int num : nums) {
            sb.append(num);
        }
        result.add(sb.toString());
        return;
    }
    
    for (int i = start; i < nums.length; i++) {
        swap(nums, start, i);
        generatePermutations(nums, start + 1, result);
        swap(nums, start, i);
    }
}

private static void swap(int[] nums, int i, int j) {
    int temp = nums[i];
    nums[i] = nums[j];
    nums[j] = temp;
}
```

---

## Algorithm Walkthrough

### Example: [1, 34, 3, 98, 9, 76, 45, 4]

**Custom Comparator Logic:**

```
Convert to strings: ["1", "34", "3", "98", "9", "76", "45", "4"]

Comparator: (a, b) -> (b + a).compareTo(a + b)

Compare "98" and "9":
  "9" + "98" = "998"
  "98" + "9" = "989"
  "998" > "989" → "9" comes first

Compare "9" and "76":
  "76" + "9" = "769"
  "9" + "76" = "976"
  "976" > "769" → "9" comes first

Compare "76" and "45":
  "45" + "76" = "4576"
  "76" + "45" = "7645"
  "7645" > "4576" → "76" comes first

After sorting: ["9", "98", "76", "45", "4", "34", "3", "1"]

Join: "998764543431"
```

### Detailed Comparison Examples

```
Compare "3" and "34":
  "34" + "3" = "343"
  "3" + "34" = "334"
  "343" > "334" → "34" comes first

Compare "3" and "30":
  "30" + "3" = "303"
  "3" + "30" = "330"
  "330" > "303" → "3" comes first

Compare "4" and "45":
  "45" + "4" = "454"
  "4" + "45" = "445"
  "454" > "445" → "45" comes first
```

---

## Complete Implementation

```java
import java.util.*;
import java.util.stream.Collectors;

public class Solution {
    
    // Approach 1: Custom comparator (Optimal)
    public static String largestNumber(int[] nums) {
        String[] strs = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            strs[i] = String.valueOf(nums[i]);
        }
        
        Arrays.sort(strs, (a, b) -> (b + a).compareTo(a + b));
        
        if (strs[0].equals("0")) return "0";
        
        return String.join("", strs);
    }
    
    // Approach 2: With StringBuilder
    public static String largestNumberBuilder(int[] nums) {
        String[] strs = new String[nums.length];
        for (int i = 0; i < nums.length; i++) {
            strs[i] = String.valueOf(nums[i]);
        }
        
        Arrays.sort(strs, (a, b) -> (b + a).compareTo(a + b));
        
        if (strs[0].equals("0")) return "0";
        
        StringBuilder sb = new StringBuilder();
        for (String s : strs) {
            sb.append(s);
        }
        
        return sb.toString();
    }
    
    // Approach 3: Using streams
    public static String largestNumberStream(int[] nums) {
        String result = Arrays.stream(nums)
            .mapToObj(String::valueOf)
            .sorted((a, b) -> (b + a).compareTo(a + b))
            .collect(Collectors.joining(""));
        
        return result.startsWith("0") ? "0" : result;
    }
    
    public static boolean doTestsPass() {
        // Test 1
        int[] test1 = {1, 34, 3, 98, 9, 76, 45, 4};
        if (!largestNumber(test1).equals("998764543431")) return false;
        
        // Test 2
        int[] test2 = {3, 30, 34, 5, 9};
        if (!largestNumber(test2).equals("9534330")) return false;
        
        // Test 3
        int[] test3 = {10, 2};
        if (!largestNumber(test3).equals("210")) return false;
        
        // Test 4
        int[] test4 = {0, 0};
        if (!largestNumber(test4).equals("0")) return false;
        
        // Test 5
        int[] test5 = {1};
        if (!largestNumber(test5).equals("1")) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] nums = {1, 34, 3, 98, 9, 76, 45, 4};
        System.out.println("Input: " + Arrays.toString(nums));
        System.out.println("Largest number: " + largestNumber(nums));
    }
}
```

---

## Test Cases

```java
@Test
public void testLargestNumber() {
    // Test 1: Given example
    int[] test1 = {1, 34, 3, 98, 9, 76, 45, 4};
    assertEquals("998764543431", largestNumber(test1));
    
    // Test 2: With 30 and 3
    int[] test2 = {3, 30, 34, 5, 9};
    assertEquals("9534330", largestNumber(test2));
    
    // Test 3: Simple case
    int[] test3 = {10, 2};
    assertEquals("210", largestNumber(test3));
    
    // Test 4: All zeros
    int[] test4 = {0, 0, 0};
    assertEquals("0", largestNumber(test4));
    
    // Test 5: Single element
    int[] test5 = {1};
    assertEquals("1", largestNumber(test5));
    
    // Test 6: With zero
    int[] test6 = {0, 9, 8, 7};
    assertEquals("9870", largestNumber(test6));
    
    // Test 7: Same digits
    int[] test7 = {1, 11, 111};
    assertEquals("111111", largestNumber(test7));
    
    // Test 8: Descending order
    int[] test8 = {9, 8, 7, 6, 5};
    assertEquals("98765", largestNumber(test8));
    
    // Test 9: Ascending order
    int[] test9 = {1, 2, 3, 4, 5};
    assertEquals("54321", largestNumber(test9));
}
```

---

## Visual Representation

### Comparison Logic

```
Why "9" > "98"?

Option 1: "9" + "98" = "998"
Option 2: "98" + "9" = "989"

"998" > "989" lexicographically
Therefore: "9" should come before "98"

Why "3" > "30"?

Option 1: "3" + "30" = "330"
Option 2: "30" + "3" = "303"

"330" > "303"
Therefore: "3" should come before "30"

Why "34" > "3"?

Option 1: "34" + "3" = "343"
Option 2: "3" + "34" = "334"

"343" > "334"
Therefore: "34" should come before "3"
```

### Sorting Process

```
Input: [1, 34, 3, 98, 9, 76, 45, 4]

Step-by-step sorting (simplified):
Initial: ["1", "34", "3", "98", "9", "76", "45", "4"]

After comparing all pairs:
  "9" vs "98": "9" first
  "98" vs "76": "98" first
  "76" vs "45": "76" first
  "45" vs "4": "45" first
  "4" vs "34": "4" first
  "34" vs "3": "34" first
  "3" vs "1": "3" first

Final: ["9", "98", "76", "45", "4", "34", "3", "1"]

Result: "998764543431"
```

---

## Edge Cases

1. **All zeros:** [0, 0, 0] → "0"
2. **Single element:** [5] → "5"
3. **Two elements:** [10, 2] → "210"
4. **Same digits:** [1, 11, 111] → "111111"
5. **With zero:** [0, 9, 8] → "980"
6. **Already sorted:** [9, 8, 7] → "987"
7. **Reverse sorted:** [1, 2, 3] → "321"
8. **Large numbers:** [999, 99, 9] → "99999999"

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Custom Comparator | O(n log n) | O(n) | **Optimal** |
| Brute Force | O(n!) | O(n) | Not practical |

**Why O(n log n)?**
- Sorting: O(n log n)
- Each comparison: O(k) where k = average string length
- Total: O(n log n × k)

**Space Complexity:**
- String array: O(n)
- Result string: O(n × k)

---

## Related Problems

1. **Largest Number At Least Twice** - Find largest element
2. **Reorder Data in Log Files** - Custom sorting
3. **Sort Array by Parity** - Custom comparator
4. **Custom Sort String** - Custom order
5. **Relative Sort Array** - Custom ordering
6. **Sort Characters By Frequency** - Frequency sorting

---

## Interview Tips

### Clarification Questions
1. Can numbers be negative? (Usually no, non-negative)
2. Can array be empty? (Handle edge case)
3. Return string or number? (String to handle large results)
4. What if all zeros? (Return "0", not "000")
5. Can numbers have leading zeros? (Usually no)

### Approach Explanation
1. "Convert numbers to strings for comparison"
2. "Use custom comparator: compare concatenations"
3. "For two strings a and b, if a+b > b+a, then a comes first"
4. "Sort using this comparator"
5. "Handle all-zeros case"

### Common Mistakes
- Not handling all-zeros case
- Wrong comparator logic (a+b vs b+a)
- Not converting to strings
- Integer overflow for large results
- Not considering edge cases

### Why This Comparator Works?

```java
(a, b) -> (b + a).compareTo(a + b)

Transitivity proof:
  If a+b > b+a and b+c > c+b
  Then a+c > c+a

This ensures consistent sorting order
```

---

## Real-World Applications

1. **Price Display** - Showing best price combinations
2. **Phone Numbers** - Arranging digits optimally
3. **Product Codes** - Creating optimal identifiers
4. **Lottery Numbers** - Maximizing number value
5. **Data Compression** - Optimal encoding
6. **Game Scores** - Maximizing score display

---

## Key Takeaways

1. **Custom Comparator:** Key to solving this problem
2. **String Comparison:** Compare concatenations (a+b vs b+a)
3. **Edge Case:** All zeros should return "0"
4. **Time Complexity:** O(n log n) from sorting
5. **Space Complexity:** O(n) for string array
6. **Lexicographic Order:** String comparison handles different lengths
7. **Transitivity:** Comparator must be transitive for correct sorting

---

## Additional Notes

### Why Not Digit-by-Digit Comparison?

```java
// Wrong approach:
// Compare first digits only
"9" vs "98": 9 == 9, unclear

// Correct approach:
// Compare concatenations
"9" + "98" = "998"
"98" + "9" = "989"
"998" > "989" → "9" first ✓
```

### Proof of Correctness

```
Claim: If a+b > b+a, then a should come before b

Proof by contradiction:
  Assume b should come before a
  Then result would be ...b...a...
  But ...a...b... gives larger number
  Contradiction!

Therefore, comparator is correct.
```

### Handling Large Numbers

```java
// Result can be very large
// Use String, not int/long

int[] nums = {999, 999, 999, 999};
// Result: "999999999999" (12 digits)
// Exceeds int (10 digits) and long (19 digits) for many cases

// String handles arbitrary length ✓
```

### Alternative: Using PriorityQueue

```java
public static String largestNumberPQ(int[] nums) {
    PriorityQueue<String> pq = new PriorityQueue<>(
        (a, b) -> (b + a).compareTo(a + b)
    );
    
    for (int num : nums) {
        pq.offer(String.valueOf(num));
    }
    
    if (pq.peek().equals("0")) return "0";
    
    StringBuilder sb = new StringBuilder();
    while (!pq.isEmpty()) {
        sb.append(pq.poll());
    }
    
    return sb.toString();
}
```

### Why Lexicographic Comparison Works

```
String comparison is lexicographic:
  "998" vs "989"
  
  Compare char by char:
    '9' == '9'
    '9' == '8' ✗
    '9' > '8' → "998" > "989" ✓

Works for any length strings!
```

### Edge Case: All Zeros

```java
// Without check:
[0, 0, 0] → "000" ✗

// With check:
if (strs[0].equals("0")) return "0";
[0, 0, 0] → "0" ✓

// After sorting, if first element is "0",
// all elements must be "0"
```

### Comparison with Other Sorting Problems

```
Normal sorting: Compare values directly
  [3, 30, 34] → [3, 30, 34]

This problem: Compare concatenations
  [3, 30, 34] → [34, 3, 30]
  Because: "343" > "334" > "3034"

Custom comparator is essential!
```

### Time Complexity Deep Dive

```
Sorting: O(n log n) comparisons
Each comparison: O(k) where k = string length
  - Concatenation: O(k)
  - String comparison: O(k)

Total: O(n log n × k)

For practical purposes:
  k is bounded (max ~10 digits)
  Effectively O(n log n)
```

### Space Optimization

```java
// Can we do better than O(n) space?

No, because:
  1. Need to store strings for comparison
  2. Result string is O(n × k)
  3. Cannot sort in-place (need string conversion)

O(n) space is optimal for this problem
```
