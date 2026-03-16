# Problem Statement Template

Use this template when adding new problems or updating existing ones.

---

## Problem Title (LC XXX) ⭐⭐⭐⭐⭐

**Difficulty**: Easy/Medium/Hard | **Frequency**: Low/Medium/High/Very High

### Problem Statement
[Clear description of what the problem asks]

[Explain any special terms or concepts]

**Constraints**:
- [List all constraints]
- [Input ranges]
- [Special conditions]

**Examples**:

**Example 1**:
```
Input: [input description]
Output: [expected output]
Explanation: [Step-by-step explanation of how we get the output]
```

**Example 2**:
```
Input: [different case]
Output: [expected output]
Explanation: [Why this output]
```

**Example 3** (Edge Case):
```
Input: [edge case]
Output: [expected output]
Explanation: [Edge case handling]
```

### Solution
```java
public ReturnType functionName(InputType input) {
    // Implementation with comments
    // explaining key steps
}
```
**Time**: O(?) | **Space**: O(?)

### Dry Run
```
Input: [specific example]
Step 1: [what happens]
Step 2: [next step]
...
Final: [result]
```

### Test Cases
```java
// Test Case 1: [Description - e.g., Standard case]
Input: [input]
Output: [output]
Expected: [what we expect and why]

// Test Case 2: [Description - e.g., Edge case - empty input]
Input: [input]
Output: [output]
Expected: [explanation]

// Test Case 3: [Description - e.g., Minimum size]
Input: [input]
Output: [output]
Expected: [explanation]

// Test Case 4: [Description - e.g., Maximum size]
Input: [input]
Output: [output]
Expected: [explanation]

// Test Case 5: [Description - e.g., Special case]
Input: [input]
Output: [output]
Expected: [explanation]

// Test Case 6: [Description - e.g., Negative numbers/special chars]
Input: [input]
Output: [output]
Expected: [explanation]
```

### Use Cases
- **Real-world application 1**: [Description]
- **Real-world application 2**: [Description]
- **Real-world application 3**: [Description]

### Key Insights
- [Important observation 1]
- [Important observation 2]
- [Pattern or technique used]

### Common Mistakes
- [Mistake 1 to avoid]
- [Mistake 2 to avoid]

### Follow-up Questions
- [Possible follow-up 1]
- [Possible follow-up 2]

---

## Example: Two Sum (LC 1) ⭐⭐⭐⭐⭐

**Difficulty**: Easy | **Frequency**: Very High

### Problem Statement
Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.

You may assume that each input would have exactly one solution, and you may not use the same element twice.

You can return the answer in any order.

**Constraints**:
- 2 <= nums.length <= 10^4
- -10^9 <= nums[i] <= 10^9
- -10^9 <= target <= 10^9
- Only one valid answer exists

**Follow-up**: Can you come up with an algorithm that is less than O(n²) time complexity?

**Examples**:

**Example 1**:
```
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].
```

**Example 2**:
```
Input: nums = [3,2,4], target = 6
Output: [1,2]
Explanation: nums[1] + nums[2] = 2 + 4 = 6, so return [1,2].
```

**Example 3**:
```
Input: nums = [3,3], target = 6
Output: [0,1]
Explanation: nums[0] + nums[1] = 3 + 3 = 6, return [0,1].
```

### Solution
```java
public int[] twoSum(int[] nums, int target) {
    // Use hash map to store value -> index mapping
    Map<Integer, Integer> map = new HashMap<>();
    
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        
        // Check if complement exists in map
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        
        // Store current number and its index
        map.put(nums[i], i);
    }
    
    return new int[]{}; // No solution found
}
```
**Time**: O(n) - Single pass through array | **Space**: O(n) - Hash map storage

### Dry Run
```
Input: nums = [2,7,11,15], target = 9

Iteration 1: i=0, nums[0]=2
  complement = 9 - 2 = 7
  map is empty, so add {2: 0}
  map = {2: 0}

Iteration 2: i=1, nums[1]=7
  complement = 9 - 7 = 2
  map contains 2 at index 0
  return [0, 1] ✓

Result: [0, 1]
```

### Test Cases
```java
// Test Case 1: Standard case
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Expected: First two elements sum to target

// Test Case 2: Solution not at beginning
Input: nums = [3,2,4], target = 6
Output: [1,2]
Expected: Elements at indices 1 and 2

// Test Case 3: Duplicate numbers
Input: nums = [3,3], target = 6
Output: [0,1]
Expected: Same value used twice (different indices)

// Test Case 4: Negative numbers
Input: nums = [-1,-2,-3,-4,-5], target = -8
Output: [2,4]
Expected: -3 + (-5) = -8

// Test Case 5: Zero in array
Input: nums = [0,4,3,0], target = 0
Output: [0,3]
Expected: 0 + 0 = 0

// Test Case 6: Large numbers
Input: nums = [1000000000,2,3,1000000000], target = 2000000000
Output: [0,3]
Expected: Handles large integers

// Test Case 7: Minimum size
Input: nums = [1,2], target = 3
Output: [0,1]
Expected: Minimum valid input (2 elements)
```

### Use Cases
- **E-commerce**: Finding products that together match a budget
- **Finance**: Pairing transactions that sum to a specific amount
- **Gaming**: Finding item combinations that reach target score
- **Data Analysis**: Identifying data points that sum to threshold

### Key Insights
- Hash map provides O(1) lookup for complement
- Single pass solution possible (no need for nested loops)
- Store values as we iterate (don't need to pre-populate map)
- Works with duplicates because we check before adding

### Common Mistakes
- Using nested loops (O(n²) instead of O(n))
- Not handling duplicate values correctly
- Forgetting to check if complement exists before accessing map
- Using same element twice (checking i != j)

### Follow-up Questions
- What if there are multiple solutions? Return all pairs
- What if no solution exists? Return empty array or throw exception
- What if we need to return values instead of indices?
- Can we solve with O(1) space? (Only if array is sorted - two pointers)
- What about Three Sum? Four Sum? (Extend to k-sum problem)

---

## Guidelines for Writing Problem Statements

### 1. Problem Statement Section
- Start with clear, concise description
- Explain any domain-specific terms
- State what input is given and what output is expected
- Include all constraints with exact ranges
- Add follow-up questions if applicable

### 2. Examples Section
- Provide at least 3 examples
- Example 1: Standard/typical case
- Example 2: Different scenario
- Example 3: Edge case
- Each example must have clear explanation

### 3. Solution Section
- Well-commented code
- Use meaningful variable names
- Include complexity analysis
- Explain the approach briefly

### 4. Dry Run Section
- Pick one example
- Show step-by-step execution
- Include variable states at each step
- Show final result

### 5. Test Cases Section
- Minimum 5-7 test cases
- Cover: standard, edge cases, minimum, maximum, special values
- Each test case has description, input, output, and expected behavior
- Include negative numbers, zeros, duplicates where applicable

### 6. Use Cases Section
- 3-4 real-world applications
- Be specific (not just "sorting data")
- Show how the algorithm solves real problems

### 7. Additional Sections
- Key Insights: Important observations
- Common Mistakes: What to avoid
- Follow-ups: Variations of the problem

---

## Quick Checklist

When adding/updating a problem, ensure:
- [ ] Clear problem statement with constraints
- [ ] At least 3 examples with explanations
- [ ] Optimized solution with comments
- [ ] Time and space complexity
- [ ] Dry run with specific example
- [ ] 5-7 comprehensive test cases
- [ ] Real-world use cases
- [ ] Key insights and common mistakes
- [ ] Follow-up questions

---

This template ensures consistency and completeness across all 300 problems!
