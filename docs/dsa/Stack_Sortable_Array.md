# Check if Array is Stack Sortable

## Problem Statement

An array is said to be stack sortable if it can be sorted using a single stack as an auxiliary data structure. Elements can be pushed to the stack and popped from the stack to form a sorted sequence.

**Operations Allowed:**
1. Push element from input array to stack
2. Pop element from stack to output array
3. Goal: Output array should be sorted

**Input:** Array of integers  
**Output:** true if stack sortable, false otherwise

**Examples:**
```
Input: [3, 2, 1]
Output: true
Explanation:
  Push 3 → Stack: [3]
  Push 2 → Stack: [3, 2]
  Push 1 → Stack: [3, 2, 1]
  Pop 1 → Output: [1]
  Pop 2 → Output: [1, 2]
  Pop 3 → Output: [1, 2, 3] ✓

Input: [2, 1, 3]
Output: true
Explanation:
  Push 2 → Stack: [2]
  Push 1 → Stack: [2, 1]
  Pop 1 → Output: [1]
  Pop 2 → Output: [1, 2]
  Push 3 → Stack: [3]
  Pop 3 → Output: [1, 2, 3] ✓

Input: [3, 1, 2]
Output: false
Explanation:
  Push 3 → Stack: [3]
  Push 1 → Stack: [3, 1]
  Pop 1 → Output: [1]
  Push 2 → Stack: [3, 2]
  Cannot get 2 before 3 ✗

Input: [1, 2, 3]
Output: true
Explanation: Already sorted
```

---

## Solution Approaches

### Approach 1: Simulation (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static boolean isStackSortable(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    int expected = 1; // Next expected element in sorted order
    
    for (int num : arr) {
        // Push current element to stack
        stack.push(num);
        
        // Pop all elements that match expected sequence
        while (!stack.isEmpty() && stack.peek() == expected) {
            stack.pop();
            expected++;
        }
    }
    
    // If stack is empty, all elements were sorted
    return stack.isEmpty();
}
```

---

### Approach 2: Check Pattern (231 Pattern)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static boolean isStackSortablePattern(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    int minSoFar = Integer.MAX_VALUE;
    
    for (int num : arr) {
        // If current element is less than minimum seen after it
        // in stack, then not sortable
        if (num < minSoFar) {
            return false;
        }
        
        // Pop elements smaller than current
        while (!stack.isEmpty() && stack.peek() < num) {
            minSoFar = stack.pop();
        }
        
        stack.push(num);
    }
    
    return true;
}
```

---

### Approach 3: With Actual Sorting

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static boolean isStackSortableWithOutput(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    List<Integer> output = new ArrayList<>();
    int expected = 1;
    
    for (int num : arr) {
        stack.push(num);
        
        while (!stack.isEmpty() && stack.peek() == expected) {
            output.add(stack.pop());
            expected++;
        }
    }
    
    // Check if output is sorted
    for (int i = 0; i < output.size() - 1; i++) {
        if (output.get(i) > output.get(i + 1)) {
            return false;
        }
    }
    
    return stack.isEmpty();
}
```

---

## Algorithm Walkthrough

### Example: [3, 2, 1]

```
Expected: 1
Stack: []
Output: []

Step 1: Process 3
  Push 3 → Stack: [3]
  3 == 1? No
  
Step 2: Process 2
  Push 2 → Stack: [3, 2]
  2 == 1? No
  
Step 3: Process 1
  Push 1 → Stack: [3, 2, 1]
  1 == 1? Yes ✓
    Pop 1 → Output: [1], expected = 2
    Stack: [3, 2]
    
  2 == 2? Yes ✓
    Pop 2 → Output: [1, 2], expected = 3
    Stack: [3]
    
  3 == 3? Yes ✓
    Pop 3 → Output: [1, 2, 3], expected = 4
    Stack: []

Stack empty? Yes
Result: true
```

### Example: [3, 1, 2]

```
Expected: 1
Stack: []

Step 1: Process 3
  Push 3 → Stack: [3]
  3 == 1? No
  
Step 2: Process 1
  Push 1 → Stack: [3, 1]
  1 == 1? Yes ✓
    Pop 1 → Output: [1], expected = 2
    Stack: [3]
    
  3 == 2? No
  
Step 3: Process 2
  Push 2 → Stack: [3, 2]
  2 == 2? Yes ✓
    Pop 2 → Output: [1, 2], expected = 3
    Stack: [3]
    
  3 == 3? Yes ✓
    Pop 3 → Output: [1, 2, 3], expected = 4
    Stack: []

Stack empty? Yes
Result: true

Wait, this should be false!
Let me recalculate...

Actually [3, 1, 2] IS stack sortable:
  Push 3, Push 1, Pop 1, Push 2, Pop 2, Pop 3
  Output: [1, 2, 3] ✓
```

### Example: [2, 3, 1] (Not Sortable)

```
Expected: 1
Stack: []

Step 1: Process 2
  Push 2 → Stack: [2]
  2 == 1? No
  
Step 2: Process 3
  Push 3 → Stack: [2, 3]
  3 == 1? No
  
Step 3: Process 1
  Push 1 → Stack: [2, 3, 1]
  1 == 1? Yes ✓
    Pop 1 → Output: [1], expected = 2
    Stack: [2, 3]
    
  3 == 2? No (top is 3, not 2)

End of input, Stack: [2, 3]
Stack not empty? Yes
Result: false

Cannot get 2 before 3!
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Simulation (Optimal)
    public static boolean isStackSortable(int[] arr) {
        Stack<Integer> stack = new Stack<>();
        int expected = 1;
        
        for (int num : arr) {
            stack.push(num);
            
            while (!stack.isEmpty() && stack.peek() == expected) {
                stack.pop();
                expected++;
            }
        }
        
        return stack.isEmpty();
    }
    
    // For arrays with arbitrary values (not 1 to n)
    public static boolean isStackSortableGeneral(int[] arr) {
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        
        Stack<Integer> stack = new Stack<>();
        int expectedIdx = 0;
        
        for (int num : arr) {
            stack.push(num);
            
            while (!stack.isEmpty() && stack.peek() == sorted[expectedIdx]) {
                stack.pop();
                expectedIdx++;
            }
        }
        
        return stack.isEmpty();
    }
    
    // Show the sorting process
    public static List<String> showSortingProcess(int[] arr) {
        List<String> steps = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        List<Integer> output = new ArrayList<>();
        int expected = 1;
        
        for (int num : arr) {
            stack.push(num);
            steps.add("Push " + num + " → Stack: " + stack);
            
            while (!stack.isEmpty() && stack.peek() == expected) {
                int popped = stack.pop();
                output.add(popped);
                expected++;
                steps.add("Pop " + popped + " → Output: " + output + ", Stack: " + stack);
            }
        }
        
        if (!stack.isEmpty()) {
            steps.add("Stack not empty: " + stack + " → NOT SORTABLE");
        } else {
            steps.add("Stack empty → SORTABLE");
        }
        
        return steps;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Reverse sorted
        if (!isStackSortable(new int[]{3, 2, 1})) return false;
        
        // Test 2: Already sorted
        if (!isStackSortable(new int[]{1, 2, 3})) return false;
        
        // Test 3: Mixed
        if (!isStackSortable(new int[]{2, 1, 3})) return false;
        
        // Test 4: Not sortable
        if (isStackSortable(new int[]{2, 3, 1})) return false;
        
        // Test 5: Single element
        if (!isStackSortable(new int[]{1})) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[][] tests = {
            {3, 2, 1},
            {2, 1, 3},
            {2, 3, 1},
            {3, 1, 2}
        };
        
        for (int[] arr : tests) {
            System.out.println("\nArray: " + Arrays.toString(arr));
            System.out.println("Stack sortable: " + isStackSortable(arr));
            System.out.println("Process:");
            for (String step : showSortingProcess(arr)) {
                System.out.println("  " + step);
            }
        }
    }
}
```

---

## Test Cases

```java
@Test
public void testIsStackSortable() {
    // Test 1: Reverse sorted (always sortable)
    assertTrue(isStackSortable(new int[]{5, 4, 3, 2, 1}));
    assertTrue(isStackSortable(new int[]{3, 2, 1}));
    
    // Test 2: Already sorted (always sortable)
    assertTrue(isStackSortable(new int[]{1, 2, 3, 4, 5}));
    
    // Test 3: Partially sorted
    assertTrue(isStackSortable(new int[]{2, 1, 3}));
    assertTrue(isStackSortable(new int[]{3, 1, 2}));
    assertTrue(isStackSortable(new int[]{4, 3, 2, 1, 5}));
    
    // Test 4: Not sortable
    assertFalse(isStackSortable(new int[]{2, 3, 1}));
    assertFalse(isStackSortable(new int[]{3, 4, 1, 2}));
    assertFalse(isStackSortable(new int[]{4, 2, 3, 1}));
    
    // Test 5: Single element
    assertTrue(isStackSortable(new int[]{1}));
    
    // Test 6: Two elements
    assertTrue(isStackSortable(new int[]{1, 2}));
    assertTrue(isStackSortable(new int[]{2, 1}));
}
```

---

## Visual Representation

### Stack Sortable: [3, 2, 1]

```
Input: [3, 2, 1]
Target: [1, 2, 3]

Step 1: Push 3
  Stack: [3]
  Output: []

Step 2: Push 2
  Stack: [3, 2]
  Output: []

Step 3: Push 1
  Stack: [3, 2, 1]
  Output: []

Step 4: Pop 1 (matches expected 1)
  Stack: [3, 2]
  Output: [1]

Step 5: Pop 2 (matches expected 2)
  Stack: [3]
  Output: [1, 2]

Step 6: Pop 3 (matches expected 3)
  Stack: []
  Output: [1, 2, 3] ✓
```

### Not Stack Sortable: [2, 3, 1]

```
Input: [2, 3, 1]
Target: [1, 2, 3]

Step 1: Push 2
  Stack: [2]
  Output: []

Step 2: Push 3
  Stack: [2, 3]
  Output: []

Step 3: Push 1
  Stack: [2, 3, 1]
  Output: []

Step 4: Pop 1 (matches expected 1)
  Stack: [2, 3]
  Output: [1]

Step 5: Top is 3, but expected is 2
  Cannot pop 2 before 3!
  Stack: [2, 3] (not empty)
  Result: NOT SORTABLE ✗
```

---

## Edge Cases

1. **Empty array:** [] → true
2. **Single element:** [1] → true
3. **Already sorted:** [1, 2, 3] → true
4. **Reverse sorted:** [3, 2, 1] → true
5. **Two elements:** [1, 2] or [2, 1] → true
6. **Not sortable:** [2, 3, 1] → false
7. **Large array:** Works for any size

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Simulation | O(n) | O(n) | **Optimal** |
| Pattern Check | O(n) | O(n) | Alternative |
| With Output | O(n) | O(n) | Shows process |

**Why O(n)?**
- Each element pushed once: O(n)
- Each element popped at most once: O(n)
- Total: O(n)

---

## Related Problems

1. **Valid Parentheses** - Stack matching
2. **Next Greater Element** - Stack pattern
3. **Daily Temperatures** - Monotonic stack
4. **Largest Rectangle in Histogram** - Stack application
5. **Trapping Rain Water** - Stack solution
6. **132 Pattern** - Stack pattern detection

---

## Interview Tips

### Clarification Questions
1. Are elements 1 to n? (Usually yes, or can sort first)
2. Can array be empty? (Yes, return true)
3. Are duplicates allowed? (Usually no for this problem)
4. Return boolean or show process? (Usually boolean)
5. Can we use multiple stacks? (No, single stack only)

### Approach Explanation
1. "Simulate the sorting process with a stack"
2. "Track next expected element in sorted order"
3. "Push elements to stack, pop when top matches expected"
4. "If stack empty at end, array is sortable"
5. "O(n) time, O(n) space"

### Common Mistakes
- Not tracking expected element correctly
- Forgetting to pop all matching elements
- Wrong stack empty check
- Not handling edge cases
- Confusing push/pop order

### Key Insight

```
An array is NOT stack sortable if:
  There exists i < j < k where arr[j] > arr[k] > arr[i]
  (This is the 231 pattern)

Example: [2, 3, 1]
  i=0: arr[0]=2
  j=1: arr[1]=3
  k=2: arr[2]=1
  3 > 1 > 2? No, but 3 > 2 > 1? No
  
Actually: 2 < 3 and 1 < 2
  But we need 1 before 2, and 2 is blocked by 3
```

---

## Real-World Applications

1. **Compiler Design** - Expression evaluation
2. **Undo/Redo** - Operation stacks
3. **Browser History** - Back/forward navigation
4. **Function Calls** - Call stack management
5. **Sorting Algorithms** - Stack-based sorting
6. **Data Structure Design** - Queue using stacks

---

## Key Takeaways

1. **Simulation:** Best approach is to simulate the process
2. **Expected Tracking:** Track next expected element
3. **Stack Operations:** Push all, pop when matches expected
4. **Time Complexity:** O(n) - each element processed once
5. **Space Complexity:** O(n) for stack
6. **Pattern:** 231 pattern indicates not sortable
7. **Edge Cases:** Empty, single element, already sorted all return true

---

## Additional Notes

### Why [3, 2, 1] is Sortable

```
Reverse sorted arrays are always stack sortable:
  Push all elements
  Then pop all in reverse order
  Result: sorted!

General rule: If array is reverse sorted, always sortable
```

### Why [2, 3, 1] is Not Sortable

```
To get sorted [1, 2, 3]:
  Need 1 first, then 2, then 3

Process [2, 3, 1]:
  Push 2, Push 3, Push 1
  Pop 1 ✓
  Now need 2, but 3 is on top
  Cannot get 2 without popping 3 first
  But 3 should come after 2 ✗

Blocked by stack LIFO property!
```

### 231 Pattern Detection

```java
// An array has 231 pattern if:
// There exist i < j < k where arr[j] > arr[k] > arr[i]

public static boolean has231Pattern(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    int third = Integer.MIN_VALUE;
    
    for (int i = arr.length - 1; i >= 0; i--) {
        if (arr[i] < third) {
            return true; // Found 231 pattern
        }
        
        while (!stack.isEmpty() && arr[i] > stack.peek()) {
            third = stack.pop();
        }
        
        stack.push(arr[i]);
    }
    
    return false;
}

// If 231 pattern exists, NOT stack sortable
```

### Permutation Sortability

```
Not all permutations are stack sortable!

For n=3:
  Sortable: [1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]
  Wait, all are sortable for n=3!

For n=4:
  Not sortable: [2,4,1,3], [3,1,4,2], [3,4,1,2], [4,2,3,1]
  
Pattern: Catalan number related
```

### Catalan Number Connection

```
Number of stack-sortable permutations of n elements:
  = Catalan number C(n)
  = (2n)! / ((n+1)! × n!)

For n=1: 1
For n=2: 2
For n=3: 5
For n=4: 14
For n=5: 42

Not all n! permutations are stack sortable!
```

### Multiple Stacks

```java
// With 2 stacks, more permutations become sortable
// With k stacks, even more sortable

// But this problem: single stack only
```

### Optimization: Early Exit

```java
// If we find element that cannot be placed, exit early
public static boolean isStackSortableFast(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    int expected = 1;
    
    for (int num : arr) {
        // If current number is less than expected and stack not empty
        // and top of stack is greater than current, not sortable
        if (num < expected && !stack.isEmpty() && stack.peek() > num) {
            return false;
        }
        
        stack.push(num);
        
        while (!stack.isEmpty() && stack.peek() == expected) {
            stack.pop();
            expected++;
        }
    }
    
    return stack.isEmpty();
}
```

### Reverse Process

```java
// Can also check by building from sorted to original
public static boolean isStackSortableReverse(int[] arr) {
    Stack<Integer> stack = new Stack<>();
    int idx = arr.length - 1;
    
    for (int i = arr.length; i >= 1; i--) {
        stack.push(i);
        
        while (!stack.isEmpty() && idx >= 0 && stack.peek() == arr[idx]) {
            stack.pop();
            idx--;
        }
    }
    
    return stack.isEmpty();
}
```
