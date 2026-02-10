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

### Approach 1: Two-Pass with Arrays (Precomputation)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

#### Theory

The key insight is that water trapped at any position depends on:
1. **Highest bar to the left** (including current position)
2. **Highest bar to the right** (including current position)

Water at position `i` = `min(leftMax[i], rightMax[i]) - height[i]`

**Why this works:**
- Water level is limited by the shorter of the two boundaries
- If current height is already at or above water level, no water can be trapped
- We precompute left and right maximums to avoid redundant calculations

**Visual Intuition:**
```
For position i:
        leftMax[i]              rightMax[i]
            ↓                        ↓
    |       |                        |
    |   |   |       i                |   |
    | | | | |   | | * |          | | | | |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
              ← → ← water level → ← →
```

#### Implementation

```java
public static Integer computeSnowpack(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    int n = arr.length;
    Integer[] leftMax = new Integer[n];
    Integer[] rightMax = new Integer[n];
    
    // Fill leftMax: maximum height from left up to index i
    leftMax[0] = arr[0];
    for (int i = 1; i < n; i++) {
        leftMax[i] = Math.max(leftMax[i - 1], arr[i]);
    }
    
    // Fill rightMax: maximum height from right up to index i
    rightMax[n - 1] = arr[n - 1];
    for (int i = n - 2; i >= 0; i--) {
        rightMax[i] = Math.max(rightMax[i + 1], arr[i]);
    }
    
    // Calculate water at each position
    int water = 0;
    for (int i = 0; i < n; i++) {
        water += Math.min(leftMax[i], rightMax[i]) - arr[i];
    }
    
    return water;
}
```

#### Detailed Dry Run

**Input:** `{0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}`

**Step 1: Build leftMax array (left to right scan)**
```
Iteration | i | arr[i] | leftMax[i-1] | leftMax[i] = max(leftMax[i-1], arr[i])
----------|---|--------|--------------|------------------------------------
Init      | 0 |   0    |      -       | 0
   1      | 1 |   1    |      0       | max(0, 1) = 1
   2      | 2 |   3    |      1       | max(1, 3) = 3
   3      | 3 |   0    |      3       | max(3, 0) = 3
   4      | 4 |   1    |      3       | max(3, 1) = 3
   5      | 5 |   2    |      3       | max(3, 2) = 3
   6      | 6 |   0    |      3       | max(3, 0) = 3
   7      | 7 |   4    |      3       | max(3, 4) = 4
   8      | 8 |   2    |      4       | max(4, 2) = 4
   9      | 9 |   0    |      4       | max(4, 0) = 4
  10      | 10|   3    |      4       | max(4, 3) = 4
  11      | 11|   0    |      4       | max(4, 0) = 4

Result: leftMax = {0, 1, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4}
```

**Step 2: Build rightMax array (right to left scan)**
```
Iteration | i | arr[i] | rightMax[i+1] | rightMax[i] = max(rightMax[i+1], arr[i])
----------|---|--------|---------------|--------------------------------------
Init      | 11|   0    |       -       | 0
   1      | 10|   3    |       0       | max(0, 3) = 3
   2      | 9 |   0    |       3       | max(3, 0) = 3
   3      | 8 |   2    |       3       | max(3, 2) = 3
   4      | 7 |   4    |       3       | max(3, 4) = 4
   5      | 6 |   0    |       4       | max(4, 0) = 4
   6      | 5 |   2    |       4       | max(4, 2) = 4
   7      | 4 |   1    |       4       | max(4, 1) = 4
   8      | 3 |   0    |       4       | max(4, 0) = 4
   9      | 2 |   3    |       4       | max(4, 3) = 4
  10      | 1 |   1    |       4       | max(4, 1) = 4
  11      | 0 |   0    |       4       | max(4, 0) = 4

Result: rightMax = {4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 0}
```

**Step 3: Calculate water at each position**
```
i  | arr[i] | leftMax[i] | rightMax[i] | min(L,R) | water = min-arr[i] | Visual
---|--------|------------|-------------|----------|--------------------|---------
0  |   0    |     0      |      4      |    0     |    0 - 0 = 0      | Ground
1  |   1    |     1      |      4      |    1     |    1 - 1 = 0      | Bar
2  |   3    |     3      |      4      |    3     |    3 - 3 = 0      | Bar
3  |   0    |     3      |      4      |    3     |    3 - 0 = 3 ✓    | ***
4  |   1    |     3      |      4      |    3     |    3 - 1 = 2 ✓    | _**
5  |   2    |     3      |      4      |    3     |    3 - 2 = 1 ✓    | __*
6  |   0    |     3      |      4      |    3     |    3 - 0 = 3 ✓    | ***
7  |   4    |     4      |      4      |    4     |    4 - 4 = 0      | Bar
8  |   2    |     4      |      3      |    3     |    3 - 2 = 1 ✓    | __*
9  |   0    |     4      |      3      |    3     |    3 - 0 = 3 ✓    | ***
10 |   3    |     4      |      3      |    3     |    3 - 3 = 0      | Bar
11 |   0    |     4      |      0      |    0     |    0 - 0 = 0      | Ground

Total water = 0+0+0+3+2+1+3+0+1+3+0+0 = 13 units
```

**Visual Representation:**
```
Index:  0  1  2  3  4  5  6  7  8  9  10 11
Array:  0  1  3  0  1  2  0  4  2  0  3  0

                                 4
             3                   4        3
          1  3     1  2          4  2     3
       0  1  3  0  1  2  0       4  2  0  3  0
       ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

With water (*):
                                 4
             3  *  *  *  *       4  *  *  3
          1  3  *  1  2  *       4  2  *  3
       0  1  3  0  1  2  0       4  2  0  3  0
       ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       Water at: 3(3) 4(2) 5(1) 6(3) 8(1) 9(3)
```

---

### Approach 2: Two Pointers (Optimal)

**Time Complexity:** O(n)  
**Space Complexity:** O(1)

#### Theory

This approach optimizes space by eliminating the need for leftMax and rightMax arrays. Instead, we use two pointers moving from both ends.

**Key Insight:**
- At any moment, we know the maximum height seen so far from left (leftMax) and right (rightMax)
- If `arr[left] < arr[right]`, the water at left position is determined by leftMax (because we know there's a taller bar on the right)
- If `arr[right] <= arr[left]`, the water at right position is determined by rightMax (because we know there's a taller bar on the left)

**Why this works:**
```
Case 1: arr[left] < arr[right]
    leftMax         ?????         rightMax
       |                             |
       |    left                     |    right
       | |   *                        | |
       +-+---+---...---...---...-----+-+
              ^
              Water here depends only on leftMax
              (we know right side has taller bar)

Case 2: arr[right] <= arr[left]
    leftMax         ?????         rightMax
       |                             |
       |    left                     |    right
       | |                        *  | |
       +-+---...---...---...---+-----+-+
                                ^
                                Water here depends only on rightMax
                                (we know left side has taller bar)
```

#### Implementation

```java
public static Integer computeSnowpackOptimal(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    int left = 0, right = arr.length - 1;
    int leftMax = 0, rightMax = 0;
    int water = 0;
    
    while (left < right) {
        if (arr[left] < arr[right]) {
            // Process left side
            if (arr[left] >= leftMax) {
                leftMax = arr[left];  // Update max, no water
            } else {
                water += leftMax - arr[left];  // Trap water
            }
            left++;
        } else {
            // Process right side
            if (arr[right] >= rightMax) {
                rightMax = arr[right];  // Update max, no water
            } else {
                water += rightMax - arr[right];  // Trap water
            }
            right--;
        }
    }
    
    return water;
}
```

#### Detailed Dry Run

**Input:** `{0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}`

```
Step | L | R | arr[L] | arr[R] | leftMax | rightMax | Action | Water Added | Total
-----|---|---|--------|--------|---------|----------|--------|-------------|------
Init | 0 | 11|   0    |   0    |    0    |    0     |   -    |      -      |  0
  1  | 0 | 11|   0    |   0    |    0    |    0     | L>=lM  |      0      |  0
     | 1 | 11|        |        |    0    |    0     | L++    |             |
  2  | 1 | 11|   1    |   0    |    0    |    0     | L>=R   |             |
     | 1 | 11|   1    |   0    |    0    |    0     | R>=rM  |      0      |  0
     | 1 | 10|        |        |    0    |    0     | R--    |             |
  3  | 1 | 10|   1    |   3    |    0    |    0     | L<R    |             |
     | 1 | 10|   1    |   3    |    0    |    0     | L>=lM  |      0      |  0
     | 2 | 10|        |        |    1    |    0     | L++    |             |
  4  | 2 | 10|   3    |   3    |    1    |    0     | L>=R   |             |
     | 2 | 10|   3    |   3    |    1    |    0     | R>=rM  |      0      |  0
     | 2 | 9 |        |        |    1    |    3     | R--    |             |
  5  | 2 | 9 |   3    |   0    |    1    |    3     | L>=R   |             |
     | 2 | 9 |   3    |   0    |    1    |    3     | R<rM   |   3-0=3     |  3
     | 2 | 8 |        |        |    1    |    3     | R--    |             |
  6  | 2 | 8 |   3    |   2    |    1    |    3     | L>=R   |             |
     | 2 | 8 |   3    |   2    |    1    |    3     | R<rM   |   3-2=1     |  4
     | 2 | 7 |        |        |    1    |    3     | R--    |             |
  7  | 2 | 7 |   3    |   4    |    1    |    3     | L<R    |             |
     | 2 | 7 |   3    |   4    |    1    |    3     | L>=lM  |      0      |  4
     | 3 | 7 |        |        |    3    |    3     | L++    |             |
  8  | 3 | 7 |   0    |   4    |    3    |    3     | L<R    |             |
     | 3 | 7 |   0    |   4    |    3    |    3     | L<lM   |   3-0=3     |  7
     | 4 | 7 |        |        |    3    |    3     | L++    |             |
  9  | 4 | 7 |   1    |   4    |    3    |    3     | L<R    |             |
     | 4 | 7 |   1    |   4    |    3    |    3     | L<lM   |   3-1=2     |  9
     | 5 | 7 |        |        |    3    |    3     | L++    |             |
 10  | 5 | 7 |   2    |   4    |    3    |    3     | L<R    |             |
     | 5 | 7 |   2    |   4    |    3    |    3     | L<lM   |   3-2=1     | 10
     | 6 | 7 |        |        |    3    |    3     | L++    |             |
 11  | 6 | 7 |   0    |   4    |    3    |    3     | L<R    |             |
     | 6 | 7 |   0    |   4    |    3    |    3     | L<lM   |   3-0=3     | 13
     | 7 | 7 |        |        |    3    |    3     | L++    |             |

Loop ends: left (7) >= right (7)
Final water = 13 units
```

**Pointer Movement Visualization:**
```
Array: {0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}

Step 1-2:   L→              ←R
            0  1  3  0  1  2  0  4  2  0  3  0

Step 3-4:      L→           ←R
            0  1  3  0  1  2  0  4  2  0  3  0

Step 5-6:         L→     ←R
            0  1  3  0  1  2  0  4  2  0  3  0
                      (collecting water from right)

Step 7-11:        L→→→→→
            0  1  3  0  1  2  0  4  2  0  3  0
                      (collecting water from left)
```

---

### Approach 3: Stack-Based (Layer by Layer)

**Time Complexity:** O(n)  
**Space Complexity:** O(n)

#### Theory

This approach calculates water layer by layer (horizontally) rather than column by column (vertically).

**Key Insight:**
- Use a monotonic decreasing stack to track potential water containers
- When we find a bar taller than the stack top, we can calculate water trapped between the current bar and the bar at the bottom of the stack
- Water forms rectangular layers between boundaries

**Visual Intuition:**
```
Stack approach fills water horizontally:

        |               |           |               |
        |       |       |           |   [Layer 2]   |
        |   |   |   |   |           |   |===|===|   |
    |   |   |   |   |   |   |   =>  |   |   |   |   |
    |   |   |   |   |   |   |       |   |   |   |   |
    +---+---+---+---+---+---+       +---+---+---+---+
    ←left    top    right→         ← distance →
    
    Water = distance × height
    distance = right - left - 1
    height = min(arr[left], arr[right]) - arr[top]
```

**How it works:**
1. Maintain a stack of indices with decreasing heights
2. When current bar is taller than stack top:
   - Pop the top (this is the bottom of water container)
   - The new top is the left boundary
   - Current position is the right boundary
   - Calculate water between these boundaries

#### Implementation

```java
public static Integer computeSnowpackStack(Integer[] arr) {
    if (arr == null || arr.length < 3) return 0;
    
    Stack<Integer> stack = new Stack<>();
    int water = 0;
    
    for (int i = 0; i < arr.length; i++) {
        // While current bar is taller than stack top
        while (!stack.isEmpty() && arr[i] > arr[stack.peek()]) {
            int top = stack.pop();  // Bottom of water container
            
            if (stack.isEmpty()) break;  // No left boundary
            
            int distance = i - stack.peek() - 1;  // Width
            int height = Math.min(arr[i], arr[stack.peek()]) - arr[top];  // Height
            water += distance * height;
        }
        stack.push(i);
    }
    
    return water;
}
```

#### Detailed Dry Run

**Input:** `{0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}`

```
Step | i | arr[i] | Stack (indices) | Action | Calculation | Water | Total
-----|---|--------|-----------------|--------|-------------|-------|------
  1  | 0 |   0    |      []         | Push 0 |      -      |   -   |  0
     |   |        |      [0]        |        |             |       |
  2  | 1 |   1    |      [0]        | 1>0    |             |       |
     |   |        |                 | Pop 0  | No left     |   0   |  0
     |   |        |      []         | Push 1 |             |       |
     |   |        |      [1]        |        |             |       |
  3  | 2 |   3    |      [1]        | 3>1    |             |       |
     |   |        |                 | Pop 1  | No left     |   0   |  0
     |   |        |      []         | Push 2 |             |       |
     |   |        |      [2]        |        |             |       |
  4  | 3 |   0    |      [2]        | 0<3    | Push 3     |   -   |  0
     |   |        |      [2,3]      |        |             |       |
  5  | 4 |   1    |      [2,3]      | 1>0    |             |       |
     |   |        |                 | Pop 3  | top=3       |       |
     |   |        |      [2]        | left=2, right=4     |       |
     |   |        |                 | dist=4-2-1=1        |       |
     |   |        |                 | h=min(1,3)-0=1      | 1×1=1 |  1
     |   |        |      [2]        | 1<3    | Push 4     |       |
     |   |        |      [2,4]      |        |             |       |
  6  | 5 |   2    |      [2,4]      | 2>1    |             |       |
     |   |        |                 | Pop 4  | top=4       |       |
     |   |        |      [2]        | left=2, right=5     |       |
     |   |        |                 | dist=5-2-1=2        |       |
     |   |        |                 | h=min(2,3)-1=1      | 2×1=2 |  3
     |   |        |      [2]        | 2<3    | Push 5     |       |
     |   |        |      [2,5]      |        |             |       |
  7  | 6 |   0    |      [2,5]      | 0<2    | Push 6     |   -   |  3
     |   |        |      [2,5,6]    |        |             |       |
  8  | 7 |   4    |      [2,5,6]    | 4>0    |             |       |
     |   |        |                 | Pop 6  | top=6       |       |
     |   |        |      [2,5]      | left=5, right=7     |       |
     |   |        |                 | dist=7-5-1=1        |       |
     |   |        |                 | h=min(4,2)-0=2      | 1×2=2 |  5
     |   |        |      [2,5]      | 4>2    |             |       |
     |   |        |                 | Pop 5  | top=5       |       |
     |   |        |      [2]        | left=2, right=7     |       |
     |   |        |                 | dist=7-2-1=4        |       |
     |   |        |                 | h=min(4,3)-2=1      | 4×1=4 |  9
     |   |        |      [2]        | 4>3    |             |       |
     |   |        |                 | Pop 2  | No left     |   0   |  9
     |   |        |      []         | Push 7 |             |       |
     |   |        |      [7]        |        |             |       |
  9  | 8 |   2    |      [7]        | 2<4    | Push 8     |   -   |  9
     |   |        |      [7,8]      |        |             |       |
 10  | 9 |   0    |      [7,8]      | 0<2    | Push 9     |   -   |  9
     |   |        |      [7,8,9]    |        |             |       |
 11  | 10|   3    |      [7,8,9]    | 3>0    |             |       |
     |   |        |                 | Pop 9  | top=9       |       |
     |   |        |      [7,8]      | left=8, right=10    |       |
     |   |        |                 | dist=10-8-1=1       |       |
     |   |        |                 | h=min(3,2)-0=2      | 1×2=2 | 11
     |   |        |      [7,8]      | 3>2    |             |       |
     |   |        |                 | Pop 8  | top=8       |       |
     |   |        |      [7]        | left=7, right=10    |       |
     |   |        |                 | dist=10-7-1=2       |       |
     |   |        |                 | h=min(3,4)-2=1      | 2×1=2 | 13
     |   |        |      [7]        | 3<4    | Push 10    |       |
     |   |        |      [7,10]     |        |             |       |
 12  | 11|   0    |      [7,10]     | 0<3    | Push 11    |   -   | 13
     |   |        |      [7,10,11]  |        |             |       |

Loop ends
Final water = 13 units
```

**Layer-by-Layer Visualization:**
```
Array: {0, 1, 3, 0, 1, 2, 0, 4, 2, 0, 3, 0}

Step 5 (i=4): Fill between indices 2 and 4
        3
        3  *  1
        ━━━━━━━  (1 unit)

Step 6 (i=5): Fill between indices 2 and 5
        3
        3  *  1  2
        ━━━━━━━━━━  (2 units)

Step 8 (i=7): Fill between indices 5 and 7
                    4
              2  *  4
              ━━━━━━  (2 units)

Step 8 (i=7): Fill between indices 2 and 7
        3           4
        3  *  *  *  4
        ━━━━━━━━━━━━━  (4 units)

Step 11 (i=10): Fill between indices 8 and 10
                    4     3
                    4  2  3
                    ━━━━━━  (2 units)

Step 11 (i=10): Fill between indices 7 and 10
                    4  *  3
                    4  2  3
                    ━━━━━━━━  (2 units)
```

---

## Comparison of Approaches

| Approach | Time | Space | Pros | Cons | Best For |
|----------|------|-------|------|------|----------|
| Two-Pass Arrays | O(n) | O(n) | Simple to understand, easy to debug | Extra space for arrays | Learning, interviews |
| Two Pointers | O(n) | O(1) | Optimal space, single pass | Slightly harder to understand | Production code |
| Stack-Based | O(n) | O(n) | Calculates layer by layer, elegant | Most complex logic | Advanced interviews |

---

## Algorithm Walkthrough - Simplified Example

### Example: {4, 2, 0, 3, 2, 5}

**Visual:**
```
                        5
    4           3       5
    4   2       3   2   5
    4   2   0   3   2   5
    ━━━━━━━━━━━━━━━━━━
    0   1   2   3   4   5

With water:
                        5
    4   *   *   3   *   5
    4   2   *   3   2   5
    4   2   0   3   2   5
    ━━━━━━━━━━━━━━━━━━
    Water: 0+2+4+1+3+0 = 9 units
```

**Approach 1: Two-Pass**
```
Step 1: leftMax  = {4, 4, 4, 4, 4, 5}
Step 2: rightMax = {5, 5, 5, 5, 5, 5}
Step 3: Water calculation
  i=0: min(4,5)-4 = 0
  i=1: min(4,5)-2 = 2 ✓
  i=2: min(4,5)-0 = 4 ✓
  i=3: min(4,5)-3 = 1 ✓
  i=4: min(4,5)-2 = 2 ✓
  i=5: min(5,5)-5 = 0
  Total = 9
```

**Approach 2: Two Pointers**
```
L=0, R=5: arr[0]=4, arr[5]=5, 4<5 → leftMax=4, L++
L=1, R=5: arr[1]=2, arr[5]=5, 2<5 → water+=4-2=2, L++
L=2, R=5: arr[2]=0, arr[5]=5, 0<5 → water+=4-0=4, L++
L=3, R=5: arr[3]=3, arr[5]=5, 3<5 → water+=4-3=1, L++
L=4, R=5: arr[4]=2, arr[5]=5, 2<5 → water+=4-2=2, L++
L=5, R=5: Stop
Total = 2+4+1+2 = 9
```

**Approach 3: Stack**
```
i=0: Push 0, stack=[0]
i=1: 2<4, Push 1, stack=[0,1]
i=2: 0<2, Push 2, stack=[0,1,2]
i=3: 3>0, Pop 2
     left=1, right=3, dist=1, h=min(3,2)-0=2, water=2
     3>2, Pop 1
     left=0, right=3, dist=2, h=min(3,4)-2=1, water=2+2=4
     3<4, Push 3, stack=[0,3]
i=4: 2<3, Push 4, stack=[0,3,4]
i=5: 5>2, Pop 4
     left=3, right=5, dist=1, h=min(5,3)-2=1, water=4+1=5
     5>3, Pop 3
     left=0, right=5, dist=4, h=min(5,4)-3=1, water=5+4=9
     5>4, Pop 0, no left
     Push 5, stack=[5]
Total = 9
```

---

## Key Insights and Patterns

### 1. Water Level Principle
```
Water at position i = min(max_left, max_right) - height[i]
```
The water level is always limited by the shorter boundary.

### 2. Monotonic Stack Pattern
The stack-based approach uses a **monotonic decreasing stack**:
- Maintains indices in decreasing order of heights
- When a taller bar appears, it triggers water calculation
- Common in problems involving "next greater element"

### 3. Two Pointer Optimization
When you need both left and right information:
- If you can determine the answer using only one side, use two pointers
- Move the pointer with the smaller value
- Eliminates need for preprocessing arrays

### 4. Edge Cases
```java
// No water possible
{1, 2, 3, 4, 5}  // Strictly increasing
{5, 4, 3, 2, 1}  // Strictly decreasing
{3, 3, 3, 3}     // All same height
{5}              // Single element
{5, 3}           // Two elements

// Water trapped
{3, 0, 2}        // Simple valley = 2
{3, 0, 0, 2}     // Wide valley = 4
{5, 2, 1, 2, 1, 5}  // Multiple valleys = 14
```

### 5. Common Mistakes
```java
// ❌ Wrong: Using current height as boundary
water += arr[i] - arr[i];  // Always 0!

// ✓ Correct: Using max boundaries
water += Math.min(leftMax, rightMax) - arr[i];

// ❌ Wrong: Not checking if water is negative
water += leftMax - arr[i];  // Could be negative!

// ✓ Correct: Water is always non-negative
if (arr[i] < leftMax) {
    water += leftMax - arr[i];
}
```

---

## Related Problems

1. **Container With Most Water** (LeetCode 11)
   - Similar two-pointer approach
   - Find maximum area, not total water

2. **Largest Rectangle in Histogram** (LeetCode 84)
   - Uses similar stack-based approach
   - Monotonic stack pattern

3. **Maximal Rectangle** (LeetCode 85)
   - Extension of histogram problem
   - 2D version of water trapping

4. **Pour Water** (LeetCode 755)
   - Simulation of water pouring
   - Uses similar water level concepts

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
