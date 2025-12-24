# Snowpack (Trapping Rain Water)

## Problem Statement

Given an array of non-negative integers representing elevations from a vertical cross-section of hills, determine how many units of snow (or water) could be captured between the hills.

**Input:** Array of non-negative integers (elevations)  
**Output:** Total units of snow/water trapped

**Example:**
```
Elevation Map:
                                 ___
             ___                |   |        ___
            |   |        ___    |   |___    |   |
         ___|   |    ___|   |   |   |   |   |   |
     ___|___|___|___|___|___|___|___|___|___|___|___
     {0,  1,  3,  0,  1,  2,  0,  4,  2,  0,  3,  0}

With Snow (*):
                                 ___
             ___                |   |        ___
            |   | *   *  _*_  * |   |_*_  * |   |
         ___|   | *  _*_|   | * |   |   | * |   |
     ___|___|___|_*_|___|___|_*_|___|___|_*_|___|___
     {0,  1,  3,  0,  1,  2,  0,  4,  2,  0,  3,  0}

Result: 13 units
```

---

## Solution Approaches

### Approach 1: Two-Pass with Arrays

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer computeSnowpack(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    int n = arr.length;
    Integer[] leftMax = new Integer[n];
    Integer[] rightMax = new Integer[n];
    
    // Fill leftMax
    leftMax[0] = arr[0];
    for (int i = 1; i < n; i++) {
        leftMax[i] = Math.max(leftMax[i - 1], arr[i]);
    }
    
    // Fill rightMax
    rightMax[n - 1] = arr[n - 1];
    for (int i = n - 2; i >= 0; i--) {
        rightMax[i] = Math.max(rightMax[i + 1], arr[i]);
    }
    
    // Calculate water
    int water = 0;
    for (int i = 0; i < n; i++) {
        water += Math.min(leftMax[i], rightMax[i]) - arr[i];
    }
    
    return water;
}
```

---

### Approach 2: Two Pointers (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

```java
public static Integer computeSnowpackOptimal(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    int left = 0, right = arr.length - 1;
    int leftMax = 0, rightMax = 0;
    int water = 0;
    
    while (left < right) {
        if (arr[left] < arr[right]) {
            if (arr[left] >= leftMax) {
                leftMax = arr[left];
            } else {
                water += leftMax - arr[left];
            }
            left++;
        } else {
            if (arr[right] >= rightMax) {
                rightMax = arr[right];
            } else {
                water += rightMax - arr[right];
            }
            right--;
        }
    }
    
    return water;
}
```

---

### Approach 3: Stack-Based

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

```java
public static Integer computeSnowpackStack(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    Stack<Integer> stack = new Stack<>();
    int water = 0;
    
    for (int i = 0; i < arr.length; i++) {
        while (!stack.isEmpty() && arr[i] > arr[stack.peek()]) {
            int top = stack.pop();
            
            if (stack.isEmpty()) break;
            
            int distance = i - stack.peek() - 1;
            int height = Math.min(arr[i], arr[stack.peek()]) - arr[top];
            water += distance * height;
        }
        stack.push(i);
    }
    
    return water;
}
```

---

## Algorithm Walkthrough

### Example: {0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}

**Two-Pass Approach:**

```
Step 1: Build leftMax array
Index:    0  1  2  3  4  5  6  7  8  9  10 11
arr:      0  1  3  0  1  2  0  4  2  0  3  0
leftMax:  0  1  3  3  3  3  3  4  4  4  4  4

Step 2: Build rightMax array
Index:    0  1  2  3  4  5  6  7  8  9  10 11
arr:      0  1  3  0  1  2  0  4  2  0  3  0
rightMax: 4  4  4  4  4  4  4  4  3  3  3  0

Step 3: Calculate water at each position
water[i] = min(leftMax[i], rightMax[i]) - arr[i]

i=0:  min(0, 4) - 0 = 0
i=1:  min(1, 4) - 1 = 0
i=2:  min(3, 4) - 3 = 0
i=3:  min(3, 4) - 0 = 3  ✓
i=4:  min(3, 4) - 1 = 2  ✓
i=5:  min(3, 4) - 2 = 1  ✓
i=6:  min(3, 4) - 0 = 3  ✓
i=7:  min(4, 4) - 4 = 0
i=8:  min(4, 3) - 2 = 1  ✓
i=9:  min(4, 3) - 0 = 3  ✓
i=10: min(4, 3) - 3 = 0
i=11: min(4, 0) - 0 = 0

Total: 0+0+0+3+2+1+3+0+1+3+0+0 = 13
```

**Two-Pointer Approach:**

```
Initial: left=0, right=11, leftMax=0, rightMax=0, water=0

Step 1: arr[0]=0 < arr[11]=0 (equal, go left)
  arr[0]=0 >= leftMax=0 → leftMax=0
  left=1

Step 2: arr[1]=1 > arr[11]=0
  arr[11]=0 >= rightMax=0 → rightMax=0
  right=10

Step 3: arr[1]=1 < arr[10]=3
  arr[1]=1 >= leftMax=0 → leftMax=1
  left=2

Step 4: arr[2]=3 >= arr[10]=3
  arr[10]=3 >= rightMax=0 → rightMax=3
  right=9

Step 5: arr[2]=3 >= arr[9]=0
  arr[9]=0 < rightMax=3 → water += 3-0 = 3
  right=8

Step 6: arr[2]=3 > arr[8]=2
  arr[8]=2 < rightMax=3 → water += 3-2 = 1
  water=4, right=7

Step 7: arr[2]=3 < arr[7]=4
  arr[2]=3 >= leftMax=1 → leftMax=3
  left=3

Step 8: arr[3]=0 < arr[7]=4
  arr[3]=0 < leftMax=3 → water += 3-0 = 3
  water=7, left=4

Step 9: arr[4]=1 < arr[7]=4
  arr[4]=1 < leftMax=3 → water += 3-1 = 2
  water=9, left=5

Step 10: arr[5]=2 < arr[7]=4
  arr[5]=2 < leftMax=3 → water += 3-2 = 1
  water=10, left=6

Step 11: arr[6]=0 < arr[7]=4
  arr[6]=0 < leftMax=3 → water += 3-0 = 3
  water=13, left=7

left=7, right=7 → Stop

Total: 13
```

---

## Complete Implementation

```java
import java.io.*;
import java.util.*;

class Solution {
    
    // Approach 1: Two-pass with arrays
    public static Integer computeSnowpack(Integer[] arr) {
        if (arr == null || arr.length < 3) return 0;
        
        int n = arr.length;
        Integer[] leftMax = new Integer[n];
        Integer[] rightMax = new Integer[n];
        
        leftMax[0] = arr[0];
        for (int i = 1; i < n; i++) {
            leftMax[i] = Math.max(leftMax[i - 1], arr[i]);
        }
        
        rightMax[n - 1] = arr[n - 1];
        for (int i = n - 2; i >= 0; i--) {
            rightMax[i] = Math.max(rightMax[i + 1], arr[i]);
        }
        
        int water = 0;
        for (int i = 0; i < n; i++) {
            water += Math.min(leftMax[i], rightMax[i]) - arr[i];
        }
        
        return water;
    }
    
    // Approach 2: Two pointers (optimal)
    public static Integer computeSnowpackOptimal(Integer[] arr) {
        if (arr == null || arr.length < 3) return 0;
        
        int left = 0, right = arr.length - 1;
        int leftMax = 0, rightMax = 0;
        int water = 0;
        
        while (left < right) {
            if (arr[left] < arr[right]) {
                if (arr[left] >= leftMax) {
                    leftMax = arr[left];
                } else {
                    water += leftMax - arr[left];
                }
                left++;
            } else {
                if (arr[right] >= rightMax) {
                    rightMax = arr[right];
                } else {
                    water += rightMax - arr[right];
                }
                right--;
            }
        }
        
        return water;
    }
    
    // Approach 3: Stack-based
    public static Integer computeSnowpackStack(Integer[] arr) {
        if (arr == null || arr.length < 3) return 0;
        
        Stack<Integer> stack = new Stack<>();
        int water = 0;
        
        for (int i = 0; i < arr.length; i++) {
            while (!stack.isEmpty() && arr[i] > arr[stack.peek()]) {
                int top = stack.pop();
                
                if (stack.isEmpty()) break;
                
                int distance = i - stack.peek() - 1;
                int height = Math.min(arr[i], arr[stack.peek()]) - arr[top];
                water += distance * height;
            }
            stack.push(i);
        }
        
        return water;
    }
    
    public static boolean doTestsPass() {
        boolean result = true;
        
        result &= computeSnowpack(new Integer[]{0,1,3,0,1,2,0,4,2,0,3,0}) == 13;
        result &= computeSnowpack(new Integer[]{4,2,0,3,2,5}) == 9;
        result &= computeSnowpack(new Integer[]{3,0,2,0,4}) == 7;
        
        return result;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail.");
        }
        
        // Demo
        Integer[] arr = {0,1,3,0,1,2,0,4,2,0,3,0};
        System.out.println("Snowpack: " + computeSnowpack(arr));
        System.out.println("Optimal: " + computeSnowpackOptimal(arr));
        System.out.println("Stack: " + computeSnowpackStack(arr));
    }
}
```

---

## Test Cases

```java
@Test
public void testComputeSnowpack() {
    // Test case 1: Given example
    assertEquals(13, computeSnowpack(new Integer[]{0,1,3,0,1,2,0,4,2,0,3,0}));
    
    // Test case 2: Simple valley
    assertEquals(9, computeSnowpack(new Integer[]{4,2,0,3,2,5}));
    
    // Test case 3: Multiple valleys
    assertEquals(7, computeSnowpack(new Integer[]{3,0,2,0,4}));
    
    // Test case 4: No water trapped
    assertEquals(0, computeSnowpack(new Integer[]{1,2,3,4,5}));
    assertEquals(0, computeSnowpack(new Integer[]{5,4,3,2,1}));
    
    // Test case 5: Flat terrain
    assertEquals(0, computeSnowpack(new Integer[]{3,3,3,3}));
    
    // Test case 6: Single peak
    assertEquals(0, computeSnowpack(new Integer[]{0,1,0}));
    
    // Test case 7: Two elements
    assertEquals(0, computeSnowpack(new Integer[]{3,5}));
    
    // Test case 8: Empty array
    assertEquals(0, computeSnowpack(new Integer[]{}));
    
    // Test case 9: All zeros
    assertEquals(0, computeSnowpack(new Integer[]{0,0,0,0}));
    
    // Test case 10: Large valley
    assertEquals(6, computeSnowpack(new Integer[]{3,0,0,2,0,4}));
}
```

---

## Visual Representation

### Example 1: {4, 2, 0, 3, 2, 5}

```
Before:
    5
4       
  2   3 2
    0

After (water = *):
    5
4 * * * *
  2 * 3 2
    0

Water calculation:
i=0: min(4,5) - 4 = 0
i=1: min(4,5) - 2 = 2  ✓
i=2: min(4,5) - 0 = 4  ✓
i=3: min(4,5) - 3 = 1  ✓
i=4: min(4,5) - 2 = 2  ✓
i=5: min(5,5) - 5 = 0

Total: 2+4+1+2 = 9
```

### Example 2: {3, 0, 2, 0, 4}

```
Before:
      4
3
    2
  0   0

After:
      4
3 * * *
  * 2 *
  0   0

Water calculation:
i=0: min(3,4) - 3 = 0
i=1: min(3,4) - 0 = 3  ✓
i=2: min(3,4) - 2 = 1  ✓
i=3: min(3,4) - 0 = 3  ✓
i=4: min(4,4) - 4 = 0

Total: 3+1+3 = 7
```

---

## Edge Cases

1. **Empty array:** `[]` → 0
2. **Single element:** `[5]` → 0
3. **Two elements:** `[3, 5]` → 0
4. **No valleys:** `[1, 2, 3, 4]` → 0
5. **Descending:** `[5, 4, 3, 2, 1]` → 0
6. **All same height:** `[3, 3, 3, 3]` → 0
7. **All zeros:** `[0, 0, 0, 0]` → 0
8. **Single valley:** `[3, 0, 3]` → 3
9. **Large numbers:** `[1000, 0, 1000]` → 1000

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Two-Pass Arrays | O(n) | O(n) | Simple, easy to understand |
| Two Pointers | O(n) | O(1) | Optimal space |
| Stack-Based | O(n) | O(n) | Good for layer-by-layer |

**Why O(n)?**
- Must examine each element at least once
- Cannot skip any position

**Key Insight:**
Water at position i = min(max_left, max_right) - height[i]

---

## Related Problems

1. **Container With Most Water** - Two pointers to find max area
2. **Rain Water Trapper II** - 2D version with priority queue
3. **Pour Water** - Simulate water pouring
4. **Largest Rectangle in Histogram** - Similar stack approach
5. **Maximal Rectangle** - 2D histogram problem

---

## Interview Tips

### Clarification Questions
1. Can heights be negative? (No, non-negative)
2. What if array is empty? (Return 0)
3. Can we modify the input array? (Prefer not to)
4. What's the maximum array size? (Affects space choice)
5. Are there duplicate heights? (Yes, allowed)

### Approach Explanation
1. "Water trapped depends on walls on both sides"
2. "For each position, find max height on left and right"
3. "Water level = min(left_max, right_max)"
4. "Two pointers optimize space to O(1)"

### Common Mistakes
- Forgetting to handle edge cases (empty, small arrays)
- Not considering that water needs walls on both sides
- Off-by-one errors in loop boundaries
- Incorrect min/max calculations

### Optimization Path
1. Start with brute force: O(n²) checking left/right for each
2. Optimize with preprocessing: O(n) time, O(n) space
3. Final optimization: Two pointers O(n) time, O(1) space

---

## Real-World Applications

1. **Civil Engineering** - Rainwater harvesting design
2. **Terrain Analysis** - Water accumulation in landscapes
3. **Game Development** - Flood simulation
4. **Architecture** - Roof drainage planning
5. **Urban Planning** - Flood risk assessment

---

## Key Takeaways

1. **Core Concept:** Water trapped = min(left_max, right_max) - current_height
2. **Two-Pass Solution:** Precompute left and right maximums
3. **Optimal Solution:** Two pointers with O(1) space
4. **Key Insight:** Process from both ends simultaneously
5. **Edge Cases:** Arrays with < 3 elements cannot trap water
6. **Time Complexity:** Always O(n) - must visit all elements
7. **Space Trade-off:** O(n) for clarity vs O(1) for optimization

---

## Additional Notes

**Why Two Pointers Work:**
- Move pointer with smaller height
- Smaller height determines water level
- No need to know exact max on other side
- Only need to know current side's max

**Intuition:**
```
If left < right:
  Water at left depends only on leftMax
  (rightMax is guaranteed ≥ right > left)
  
If right < left:
  Water at right depends only on rightMax
  (leftMax is guaranteed ≥ left > right)
```

**When to Use Each Approach:**
- **Two-Pass:** Easier to understand, debug, explain
- **Two-Pointers:** Interviews, production (optimal)
- **Stack:** When processing layer-by-layer matters
