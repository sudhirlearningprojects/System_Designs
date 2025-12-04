# Snow Pack Problem (Trapping Rain Water)

## Problem Statement

Given an array of non-negative integers representing mountain heights, calculate how much snow/water can be trapped between the peaks.

### Visual Example

```
Input: [3, 0, 2, 0, 4]

      █
  █ ░░█
  █░█░█
─────────
0 1 2 3 4

Output: 7 units (░ = trapped snow)
```

---

## Solutions

### Solution 1: Brute Force
**Time**: O(n²), **Space**: O(1)

#### Algorithm Explanation

**Core Idea**: For each position, water level = min(max_height_on_left, max_height_on_right)

**Step-by-Step**:
1. For each position i (skip first and last)
2. Scan left to find maximum height
3. Scan right to find maximum height
4. Water at position i = min(leftMax, rightMax) - height[i]
5. Add to total

**Example Walkthrough**: `[3, 0, 2, 0, 4]`

```
Position 1 (height=0):
  leftMax = max(3, 0) = 3
  rightMax = max(0, 2, 0, 4) = 4
  water = min(3, 4) - 0 = 3 ✓

Position 2 (height=2):
  leftMax = max(3, 0, 2) = 3
  rightMax = max(2, 0, 4) = 4
  water = min(3, 4) - 2 = 1 ✓

Position 3 (height=0):
  leftMax = max(3, 0, 2, 0) = 3
  rightMax = max(0, 4) = 4
  water = min(3, 4) - 0 = 3 ✓

Total = 3 + 1 + 3 = 7
```

**Visual**:
```
    █
█░░░█  ← Water fills to min(leftMax, rightMax)
█░█░█
█░█░█
```

```java
public static int trapSnowBruteForce(int[] heights) {
    if (heights == null || heights.length < 3) return 0;
    
    int totalSnow = 0;
    for (int i = 1; i < heights.length - 1; i++) {
        int leftMax = 0, rightMax = 0;
        for (int j = 0; j <= i; j++) leftMax = Math.max(leftMax, heights[j]);
        for (int j = i; j < heights.length; j++) rightMax = Math.max(rightMax, heights[j]);
        totalSnow += Math.min(leftMax, rightMax) - heights[i];
    }
    return totalSnow;
}
```

**Pros**: Simple, no extra space  
**Cons**: Slow for large arrays (recalculates max for each position)

### Solution 2: Dynamic Programming
**Time**: O(n), **Space**: O(n)

#### Algorithm Explanation

**Core Idea**: Pre-compute max heights to avoid redundant calculations

**Step-by-Step**:
1. **Build leftMax array**: leftMax[i] = max height from 0 to i
2. **Build rightMax array**: rightMax[i] = max height from i to n-1
3. **Calculate water**: For each position, water = min(leftMax[i], rightMax[i]) - height[i]

**Example Walkthrough**: `[3, 0, 2, 0, 4]`

```
Step 1: Build leftMax (scan left to right)
  leftMax[0] = 3
  leftMax[1] = max(3, 0) = 3
  leftMax[2] = max(3, 2) = 3
  leftMax[3] = max(3, 0) = 3
  leftMax[4] = max(3, 4) = 4
  Result: [3, 3, 3, 3, 4]

Step 2: Build rightMax (scan right to left)
  rightMax[4] = 4
  rightMax[3] = max(0, 4) = 4
  rightMax[2] = max(2, 4) = 4
  rightMax[1] = max(0, 4) = 4
  rightMax[0] = max(3, 4) = 4
  Result: [4, 4, 4, 4, 4]

Step 3: Calculate water at each position
  i=0: min(3, 4) - 3 = 0
  i=1: min(3, 4) - 0 = 3 ✓
  i=2: min(3, 4) - 2 = 1 ✓
  i=3: min(3, 4) - 0 = 3 ✓
  i=4: min(4, 4) - 4 = 0
  Total = 7
```

**Visual Representation**:
```
heights:  [3, 0, 2, 0, 4]
leftMax:  [3, 3, 3, 3, 4]  ← Max from left
rightMax: [4, 4, 4, 4, 4]  ← Max from right
water:    [0, 3, 1, 3, 0]  ← min(left, right) - height
```

```java
public static int trapSnowDP(int[] heights) {
    if (heights == null || heights.length < 3) return 0;
    
    int n = heights.length;
    int[] leftMax = new int[n], rightMax = new int[n];
    
    leftMax[0] = heights[0];
    for (int i = 1; i < n; i++) leftMax[i] = Math.max(leftMax[i - 1], heights[i]);
    
    rightMax[n - 1] = heights[n - 1];
    for (int i = n - 2; i >= 0; i--) rightMax[i] = Math.max(rightMax[i + 1], heights[i]);
    
    int totalSnow = 0;
    for (int i = 0; i < n; i++) totalSnow += Math.min(leftMax[i], rightMax[i]) - heights[i];
    return totalSnow;
}
```

**Pros**: Fast O(n), easy to understand  
**Cons**: Uses O(n) extra space for two arrays

### Solution 3: Two Pointers (Optimal)
**Time**: O(n), **Space**: O(1)

#### Algorithm Explanation

**Core Idea**: Move pointers from both ends, process the side with smaller height

**Key Insight**: We don't need to know the exact max on both sides. We only need to know which side is smaller!
- If heights[left] < heights[right], water at left is determined by leftMax
- If heights[right] < heights[left], water at right is determined by rightMax

**Step-by-Step**:
1. Start with two pointers: left=0, right=n-1
2. Track leftMax and rightMax as we move
3. Process the side with smaller height
4. If current height < max, add trapped water
5. Otherwise, update max
6. Move pointer inward

**Example Walkthrough**: `[3, 0, 2, 0, 4]`

```
Initial: left=0, right=4, leftMax=0, rightMax=0, snow=0

Iteration 1:
  heights[0]=3 < heights[4]=4 → Process left
  heights[0]=3 >= leftMax=0 → Update leftMax=3
  left=1
  State: left=1, right=4, leftMax=3, rightMax=0, snow=0

Iteration 2:
  heights[1]=0 < heights[4]=4 → Process left
  heights[1]=0 < leftMax=3 → Add water: 3-0=3
  left=2
  State: left=2, right=4, leftMax=3, rightMax=0, snow=3

Iteration 3:
  heights[2]=2 < heights[4]=4 → Process left
  heights[2]=2 < leftMax=3 → Add water: 3-2=1
  left=3
  State: left=3, right=4, leftMax=3, rightMax=0, snow=4

Iteration 4:
  heights[3]=0 < heights[4]=4 → Process left
  heights[3]=0 < leftMax=3 → Add water: 3-0=3
  left=4
  State: left=4, right=4, leftMax=3, rightMax=0, snow=7

loop ends (left >= right)
Total = 7 ✓
```

**Visual Flow**:
```
    █
█░░░█
█░█░█
█░█░█
↑   ↑
L   R

Move left pointer →
Calculate water based on leftMax (since left side is lower)
```

**Why It Works**:
```
If heights[left] < heights[right]:
  - We know there's a taller bar on the right
  - Water at left is limited by leftMax (not rightMax)
  - Safe to calculate water at left position
  
Same logic applies when processing right side
```

```java
public static int trapSnowTwoPointers(int[] heights) {
    if (heights == null || heights.length < 3) return 0;
    
    int left = 0, right = heights.length - 1;
    int leftMax = 0, rightMax = 0, totalSnow = 0;
    
    while (left < right) {
        if (heights[left] < heights[right]) {
            if (heights[left] >= leftMax) leftMax = heights[left];
            else totalSnow += leftMax - heights[left];
            left++;
        } else {
            if (heights[right] >= rightMax) rightMax = heights[right];
            else totalSnow += rightMax - heights[right];
            right--;
        }
    }
    return totalSnow;
}
```

**Pros**: O(n) time, O(1) space - OPTIMAL!  
**Cons**: Slightly harder to understand initially

### Solution 4: Stack-Based
**Time**: O(n), **Space**: O(n)

#### Algorithm Explanation

**Core Idea**: Calculate water layer by layer (horizontally) using a stack to track boundaries

**Key Difference**: Other solutions calculate water column by column (vertically), this calculates row by row (horizontally)

**Step-by-Step**:
1. Maintain a stack of indices in decreasing height order
2. When we find a taller bar, it forms a right boundary
3. Pop from stack to find the valley bottom
4. The remaining stack top is the left boundary
5. Calculate water in this bounded region

**Example Walkthrough**: `[3, 0, 2, 0, 4]`

```
i=0, height=3:
  Stack empty, push 0
  Stack: [0]
  Snow: 0

i=1, height=0:
  heights[1]=0 < heights[0]=3, push 1
  Stack: [0, 1]
  Snow: 0

i=2, height=2:
  heights[2]=2 > heights[1]=0 → Found right boundary!
  
  Pop top=1 (valley bottom, height=0)
  Left boundary: stack.peek()=0 (height=3)
  Right boundary: i=2 (height=2)
  
  distance = 2 - 0 - 1 = 1
  boundedHeight = min(2, 3) - 0 = 2
  water = 1 × 2 = 2
  
  heights[2]=2 < heights[0]=3, push 2
  Stack: [0, 2]
  Snow: 2

i=3, height=0:
  heights[3]=0 < heights[2]=2, push 3
  Stack: [0, 2, 3]
  Snow: 2

i=4, height=4:
  heights[4]=4 > heights[3]=0 → Found right boundary!
  
  Pop top=3 (valley bottom, height=0)
  Left: stack.peek()=2 (height=2)
  Right: i=4 (height=4)
  distance = 4 - 2 - 1 = 1
  boundedHeight = min(4, 2) - 0 = 2
  water = 1 × 2 = 2
  Snow: 4
  
  heights[4]=4 > heights[2]=2 → Continue!
  Pop top=2 (valley bottom, height=2)
  Left: stack.peek()=0 (height=3)
  Right: i=4 (height=4)
  distance = 4 - 0 - 1 = 3
  boundedHeight = min(4, 3) - 2 = 1
  water = 3 × 1 = 3
  Snow: 7
  
  heights[4]=4 > heights[0]=3 → Continue!
  Pop top=0, stack empty, break
  Push 4
  Stack: [4]
  Snow: 7

Total = 7 ✓
```

**Visual - Layer by Layer**:
```
Layer 3:     █
         █░░░█  ← Calculate this layer
         
Layer 2: █░░░█
         █░█░█  ← Then this layer
         
Layer 1: █░█░█
         █░█░█  ← Finally this layer
```

**Stack Behavior**:
```
Stack maintains indices in decreasing height order:
[3, 0, 2, 0, 4]
     ↓
[0]           ← height=3
[0,1]         ← height=0 (smaller)
[0,2]         ← height=2 (pop 1, calculate water)
[0,2,3]       ← height=0 (smaller)
[4]           ← height=4 (pop all, calculate water)
```

```java
public static int trapSnowStack(int[] heights) {
    if (heights == null || heights.length < 3) return 0;
    
    Stack<Integer> stack = new Stack<>();
    int totalSnow = 0;
    
    for (int i = 0; i < heights.length; i++) {
        while (!stack.isEmpty() && heights[i] > heights[stack.peek()]) {
            int top = stack.pop();
            if (stack.isEmpty()) break;
            int distance = i - stack.peek() - 1;
            int boundedHeight = Math.min(heights[i], heights[stack.peek()]) - heights[top];
            totalSnow += distance * boundedHeight;
        }
        stack.push(i);
    }
    return totalSnow;
}
```

**Pros**: Calculates water horizontally, good for understanding water filling process  
**Cons**: Uses stack space, more complex logic

---

## Edge Cases

| Case | Input | Output | Description |
|------|-------|--------|-------------|
| Empty | `[]` | 0 | No elements |
| Single | `[5]` | 0 | Need at least 3 |
| Two | `[5, 5]` | 0 | Need at least 3 |
| Descending | `[5, 4, 3, 2, 1]` | 0 | No valley |
| Ascending | `[1, 2, 3, 4, 5]` | 0 | No valley |
| Flat | `[3, 3, 3, 3]` | 0 | No valley |
| Perfect Valley | `[5, 2, 1, 2, 1, 5]` | 14 | Deep valley |
| Multiple Zeros | `[3, 0, 0, 2, 0, 4]` | 10 | Consecutive zeros |
| All Zeros | `[0, 0, 0, 0]` | 0 | No height |
| Large Values | `[100, 0, 100]` | 100 | Large heights |
| Zigzag | `[5, 1, 5, 1, 5, 1, 5]` | 16 | Alternating |
| V-Shape | `[10,9,8,7,6,5,4,3,2,1,2,3,4,5,6,7,8,9,10]` | 81 | Symmetric valley |

---

## Comparison

| Solution | Time | Space | Best For |
|----------|------|-------|----------|
| Brute Force | O(n²) | O(1) | Learning |
| DP | O(n) | O(n) | Readability |
| Two Pointers | O(n) | O(1) | **Production** |
| Stack | O(n) | O(n) | Layer calculation |

---

## Running

```bash
# Compile
javac src/main/java/org/sudhir512kj/algorithms/SnowPackProblem.java

# Run
java -cp src/main/java org.sudhir512kj.algorithms.SnowPackProblem

# Test
mvn test -Dtest=SnowPackProblemTest
```

---

## Key Insights

1. Water level at position i = min(leftMax, rightMax) - height[i]
2. Two pointers optimal: O(n) time, O(1) space
3. Stack calculates horizontally, DP calculates vertically
4. Need at least 3 elements to trap water

---

## Related Problems

- Container With Most Water (LeetCode 11)
- Trapping Rain Water II (LeetCode 407)
- Pour Water (LeetCode 755)
