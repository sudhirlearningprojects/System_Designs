# Product of Array Except Self

## Overview
Given an array of integers, return an array where each element at index `i` is the product of all elements in the original array except the element at index `i`. The solution must run in O(n) time without using division.

**Example**: `[1, 2, 3, 4]` → `[24, 12, 8, 6]`
- Index 0: 2 × 3 × 4 = 24
- Index 1: 1 × 3 × 4 = 12
- Index 2: 1 × 2 × 4 = 8
- Index 3: 1 × 2 × 3 = 6

## Problem Constraints
- Cannot use division operator
- Must run in O(n) time
- Preferably O(1) extra space (output array doesn't count)

## Algorithm

### Approach: Left and Right Products

**Key Insight**: For each position `i`, the result is:
```
result[i] = (product of all elements to the left) × (product of all elements to the right)
```

**Steps**:
1. Create `left[]` array where `left[i]` = product of all elements before index `i`
2. Create `right[]` array where `right[i]` = product of all elements after index `i`
3. Result: `result[i] = left[i] × right[i]`

## Implementation

### Solution 1: Using Left and Right Arrays (O(n) space)

```java
public class ProductExceptSelf {
    
    public static int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] left = new int[n];
        int[] right = new int[n];
        int[] result = new int[n];
        
        // Build left array
        left[0] = 1;
        for (int i = 1; i < n; i++) {
            left[i] = left[i - 1] * nums[i - 1];
        }
        
        // Build right array
        right[n - 1] = 1;
        for (int i = n - 2; i >= 0; i--) {
            right[i] = right[i + 1] * nums[i + 1];
        }
        
        // Build result
        for (int i = 0; i < n; i++) {
            result[i] = left[i] * right[i];
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4};
        int[] result = productExceptSelf(nums);
        System.out.println(Arrays.toString(result)); // [24, 12, 8, 6]
    }
}
```

### Solution 2: Space Optimized (O(1) extra space)

```java
public class ProductExceptSelfOptimized {
    
    public static int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        
        // Build left products directly in result array
        result[0] = 1;
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] * nums[i - 1];
        }
        
        // Multiply with right products using a variable
        int rightProduct = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] = result[i] * rightProduct;
            rightProduct *= nums[i];
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4};
        int[] result = productExceptSelf(nums);
        System.out.println(Arrays.toString(result)); // [24, 12, 8, 6]
    }
}
```

### Solution 3: Using Division (Not Allowed in Interviews)

```java
// ❌ This approach is typically NOT allowed
public static int[] productExceptSelfDivision(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    int totalProduct = 1;
    int zeroCount = 0;
    
    // Calculate total product and count zeros
    for (int num : nums) {
        if (num == 0) {
            zeroCount++;
        } else {
            totalProduct *= num;
        }
    }
    
    // Handle different cases
    for (int i = 0; i < n; i++) {
        if (zeroCount > 1) {
            result[i] = 0;
        } else if (zeroCount == 1) {
            result[i] = (nums[i] == 0) ? totalProduct : 0;
        } else {
            result[i] = totalProduct / nums[i];
        }
    }
    
    return result;
}
```

## Dry Run Example

**Input**: `nums = [1, 2, 3, 4]`

### Step 1: Build Left Products
```
left[0] = 1                    (no elements to the left)
left[1] = 1 × nums[0] = 1 × 1 = 1
left[2] = 1 × nums[1] = 1 × 2 = 2
left[3] = 2 × nums[2] = 2 × 3 = 6

left = [1, 1, 2, 6]
```

### Step 2: Build Right Products
```
right[3] = 1                    (no elements to the right)
right[2] = 1 × nums[3] = 1 × 4 = 4
right[1] = 4 × nums[2] = 4 × 3 = 12
right[0] = 12 × nums[1] = 12 × 2 = 24

right = [24, 12, 4, 1]
```

### Step 3: Calculate Result
```
result[0] = left[0] × right[0] = 1 × 24 = 24
result[1] = left[1] × right[1] = 1 × 12 = 12
result[2] = left[2] × right[2] = 2 × 4 = 8
result[3] = left[3] × right[3] = 6 × 1 = 6

result = [24, 12, 8, 6]
```

### Visual Representation
```
Index:     0    1    2    3
nums:      1    2    3    4
          ↓    ↓    ↓    ↓
left:      1    1    2    6   (cumulative product from left)
right:    24   12    4    1   (cumulative product from right)
          ↓    ↓    ↓    ↓
result:   24   12    8    6   (left × right)
```

## Dry Run - Space Optimized Approach

**Input**: `nums = [2, 3, 4, 5]`

### Pass 1: Build Left Products in Result Array
```
result[0] = 1
result[1] = result[0] × nums[0] = 1 × 2 = 2
result[2] = result[1] × nums[1] = 2 × 3 = 6
result[3] = result[2] × nums[2] = 6 × 4 = 24

result = [1, 2, 6, 24]
```

### Pass 2: Multiply with Right Products
```
rightProduct = 1

i=3: result[3] = 24 × 1 = 24,  rightProduct = 1 × 5 = 5
i=2: result[2] = 6 × 5 = 30,   rightProduct = 5 × 4 = 20
i=1: result[1] = 2 × 20 = 40,  rightProduct = 20 × 3 = 60
i=0: result[0] = 1 × 60 = 60,  rightProduct = 60 × 2 = 120

result = [60, 40, 30, 24]
```

**Verification**:
- result[0] = 3 × 4 × 5 = 60 ✓
- result[1] = 2 × 4 × 5 = 40 ✓
- result[2] = 2 × 3 × 5 = 30 ✓
- result[3] = 2 × 3 × 4 = 24 ✓

## Edge Test Cases

```java
// Test Case 1: Two elements
int[] arr1 = {2, 3};
int[] result1 = productExceptSelf(arr1);
// Expected: [3, 2]

// Test Case 2: Contains zero
int[] arr2 = {1, 2, 0, 4};
int[] result2 = productExceptSelf(arr2);
// Expected: [0, 0, 8, 0]

// Test Case 3: Multiple zeros
int[] arr3 = {0, 0, 2, 3};
int[] result3 = productExceptSelf(arr3);
// Expected: [0, 0, 0, 0]

// Test Case 4: All ones
int[] arr4 = {1, 1, 1, 1};
int[] result4 = productExceptSelf(arr4);
// Expected: [1, 1, 1, 1]

// Test Case 5: Negative numbers
int[] arr5 = {-1, 2, -3, 4};
int[] result5 = productExceptSelf(arr5);
// Expected: [-24, 12, -8, 6]

// Test Case 6: Mix of positive and negative
int[] arr6 = {-2, -3, 4, 5};
int[] result6 = productExceptSelf(arr6);
// Expected: [-60, -40, 30, 24]

// Test Case 7: Large numbers
int[] arr7 = {10, 20, 30};
int[] result7 = productExceptSelf(arr7);
// Expected: [600, 300, 200]

// Test Case 8: Single zero at start
int[] arr8 = {0, 1, 2, 3};
int[] result8 = productExceptSelf(arr8);
// Expected: [6, 0, 0, 0]

// Test Case 9: Single zero at end
int[] arr9 = {1, 2, 3, 0};
int[] result9 = productExceptSelf(arr9);
// Expected: [0, 0, 0, 6]

// Test Case 10: Three elements
int[] arr10 = {5, 6, 7};
int[] result10 = productExceptSelf(arr10);
// Expected: [42, 35, 30]
```

## Time Complexity

### Solution 1 (With Extra Arrays): O(n)
```
- First loop (build left array): O(n)
- Second loop (build right array): O(n)
- Third loop (build result): O(n)
Total: O(n) + O(n) + O(n) = O(3n) = O(n)
```

### Solution 2 (Space Optimized): O(n)
```
- First loop (build left in result): O(n)
- Second loop (multiply with right): O(n)
Total: O(n) + O(n) = O(2n) = O(n)
```

### Solution 3 (Division): O(n)
```
- Calculate total product: O(n)
- Build result: O(n)
Total: O(2n) = O(n)
```

## Space Complexity

### Solution 1: O(n)
- `left[]` array: O(n)
- `right[]` array: O(n)
- `result[]` array: O(n) (doesn't count as per problem)
- Total extra space: O(2n) = O(n)

### Solution 2: O(1)
- Only `rightProduct` variable: O(1)
- `result[]` array: O(n) (doesn't count as per problem)
- Total extra space: O(1) ✓ Optimal

### Solution 3: O(1)
- Only variables: O(1)
- `result[]` array: O(n) (doesn't count as per problem)

## Real-World Use Cases

### 1. **Stock Market Analysis**
```java
// Calculate relative performance: each stock vs market average
// result[i] = performance of all stocks except stock i
int[] calculateRelativePerformance(int[] stockReturns) {
    return productExceptSelf(stockReturns);
}
```

### 2. **Team Performance Metrics**
```java
// Calculate team productivity without each member
// Helps identify individual contribution
int[] teamProductivityWithoutMember(int[] memberProductivity) {
    return productExceptSelf(memberProductivity);
}
```

### 3. **Manufacturing Defect Analysis**
```java
// Calculate total production if one machine is down
// result[i] = production capacity without machine i
int[] productionWithoutMachine(int[] machineCapacity) {
    return productExceptSelf(machineCapacity);
}
```

### 4. **Network Bandwidth Calculation**
```java
// Calculate available bandwidth excluding one node
int[] bandwidthExcludingNode(int[] nodeBandwidth) {
    return productExceptSelf(nodeBandwidth);
}
```

### 5. **Game Score Multipliers**
```java
// In a game, calculate bonus for each player
// Bonus = product of all other players' scores
int[] calculateBonus(int[] playerScores) {
    return productExceptSelf(playerScores);
}
```

### 6. **Resource Allocation**
```java
// Calculate resource availability if one resource is unavailable
int[] resourceAvailability(int[] resources) {
    return productExceptSelf(resources);
}
```

## Variations and Related Problems

### 1. Sum Except Self
```java
// Similar problem but with sum instead of product
public int[] sumExceptSelf(int[] nums) {
    int totalSum = Arrays.stream(nums).sum();
    int[] result = new int[nums.length];
    for (int i = 0; i < nums.length; i++) {
        result[i] = totalSum - nums[i];
    }
    return result;
}
```

### 2. Product with Division Allowed
```java
// When division is allowed (handle zeros carefully)
public int[] productWithDivision(int[] nums) {
    int product = 1, zeroCount = 0, zeroIndex = -1;
    
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] == 0) {
            zeroCount++;
            zeroIndex = i;
        } else {
            product *= nums[i];
        }
    }
    
    int[] result = new int[nums.length];
    if (zeroCount > 1) {
        return result; // All zeros
    } else if (zeroCount == 1) {
        result[zeroIndex] = product;
    } else {
        for (int i = 0; i < nums.length; i++) {
            result[i] = product / nums[i];
        }
    }
    return result;
}
```

### 3. Product of Last K Elements
```java
// Calculate product of last k elements for each position
public int[] productOfLastK(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n];
    
    for (int i = 0; i < n; i++) {
        int product = 1;
        for (int j = Math.max(0, i - k + 1); j <= i; j++) {
            product *= nums[j];
        }
        result[i] = product;
    }
    return result;
}
```

## Advantages

1. **Optimal Time**: O(n) - single pass through array
2. **Space Efficient**: O(1) extra space (optimized version)
3. **No Division**: Works with zeros and avoids division errors
4. **Simple Logic**: Easy to understand and implement

## Disadvantages

1. **Integer Overflow**: Large products can overflow
2. **Not Intuitive**: Requires understanding of prefix/suffix products
3. **Two Passes**: Needs two iterations (though still O(n))

## Common Mistakes

### 1. Using Division Without Handling Zeros
```java
// ❌ Wrong: Fails when array contains zero
int totalProduct = 1;
for (int num : nums) totalProduct *= num;
for (int i = 0; i < nums.length; i++) {
    result[i] = totalProduct / nums[i]; // Division by zero!
}
```

### 2. Incorrect Initialization
```java
// ❌ Wrong: Should initialize to 1, not 0
int[] left = new int[n]; // Defaults to 0
left[0] = 0; // Wrong!

// ✓ Correct
left[0] = 1;
```

### 3. Off-by-One Errors
```java
// ❌ Wrong: Index out of bounds
for (int i = 1; i <= n; i++) { // Should be i < n
    left[i] = left[i-1] * nums[i-1];
}
```

## Interview Tips

1. **Clarify Constraints**: Ask if division is allowed, if zeros are present
2. **Start with Brute Force**: Explain O(n²) solution first
3. **Optimize Step-by-Step**: Show progression from O(n) space to O(1)
4. **Handle Edge Cases**: Discuss zeros, negative numbers, overflow
5. **Explain Trade-offs**: Time vs space complexity
6. **Test with Examples**: Walk through small examples

## Follow-up Questions

**Q1**: What if division is allowed?
- Use total product and divide by each element (handle zeros)

**Q2**: What if we need to handle very large numbers?
- Use BigInteger or modulo arithmetic

**Q3**: Can we do it in one pass?
- No, we need at least two passes (left-to-right and right-to-left)

**Q4**: What if array is very large and doesn't fit in memory?
- Use streaming approach with two passes

**Q5**: How to handle integer overflow?
- Use long data type or check for overflow before multiplication

## Comparison with Alternatives

| Approach          | Time  | Space | Division | Handles Zeros |
|-------------------|-------|-------|----------|---------------|
| Brute Force       | O(n²) | O(1)  | No       | Yes           |
| Left-Right Arrays | O(n)  | O(n)  | No       | Yes           |
| Space Optimized   | O(n)  | O(1)  | No       | Yes           |
| Division Method   | O(n)  | O(1)  | Yes      | Complex       |

## Key Takeaways

1. **Core Concept**: result[i] = leftProduct[i] × rightProduct[i]
2. **Optimization**: Reuse result array to store left products
3. **Space Saving**: Use variable for right products instead of array
4. **No Division**: Avoids division operator and handles zeros naturally
5. **Two Passes**: Minimum required for O(n) solution
