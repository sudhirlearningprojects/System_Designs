# Three Sum - Find Triplet with Target Sum

## Problem Statement
Given an integer array and a target value, find if there exists a triplet (three elements) whose sum equals the target.

**Example 1**:
```
Input: nums = [1, 4, 45, 6, 10, 8], target = 22
Output: true
Explanation: Triplet (4, 10, 8) sums to 22
```

**Example 2**:
```
Input: nums = [1, 2, 3, 4, 5], target = 20
Output: false
Explanation: No triplet sums to 20
```

## Implementations

### Solution 1: Brute Force (Three Nested Loops)

```java
public class ThreeSumBruteForce {
    
    public static boolean hasThreeSum(int[] nums, int target) {
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            for (int j = i + 1; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (nums[i] + nums[j] + nums[k] == target) {
                        System.out.println("Triplet: " + nums[i] + ", " + nums[j] + ", " + nums[k]);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 4, 45, 6, 10, 8};
        int target = 22;
        System.out.println(hasThreeSum(nums, target)); // true
    }
}
```

**Time Complexity**: O(n³)  
**Space Complexity**: O(1)  
**Use Case**: Small arrays only

---

### Solution 2: Sorting + Two Pointers (Optimal)

```java
import java.util.*;

public class ThreeSumTwoPointers {
    
    public static boolean hasThreeSum(int[] nums, int target) {
        Arrays.sort(nums); // Sort array first
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            int left = i + 1;
            int right = n - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == target) {
                    System.out.println("Triplet: " + nums[i] + ", " + nums[left] + ", " + nums[right]);
                    return true;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return false;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 4, 45, 6, 10, 8};
        int target = 22;
        System.out.println(hasThreeSum(nums, target)); // true
    }
}
```

**Time Complexity**: O(n²)  
**Space Complexity**: O(1) or O(log n) for sorting  
**Use Case**: Best approach for interviews ⭐

---

### Solution 3: HashMap (Two Sum Approach)

```java
import java.util.*;

public class ThreeSumHashMap {
    
    public static boolean hasThreeSum(int[] nums, int target) {
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            Set<Integer> seen = new HashSet<>();
            int currentTarget = target - nums[i];
            
            for (int j = i + 1; j < n; j++) {
                int complement = currentTarget - nums[j];
                
                if (seen.contains(complement)) {
                    System.out.println("Triplet: " + nums[i] + ", " + complement + ", " + nums[j]);
                    return true;
                }
                seen.add(nums[j]);
            }
        }
        
        return false;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 4, 45, 6, 10, 8};
        int target = 22;
        System.out.println(hasThreeSum(nums, target)); // true
    }
}
```

**Time Complexity**: O(n²)  
**Space Complexity**: O(n)  
**Use Case**: When array cannot be modified (no sorting)

---

### Solution 4: Find All Triplets (Not Just Existence)

```java
import java.util.*;

public class ThreeSumAllTriplets {
    
    public static List<List<Integer>> findAllTriplets(int[] nums, int target) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;
        
        for (int i = 0; i < n - 2; i++) {
            // Skip duplicates for first element
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            
            int left = i + 1;
            int right = n - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    
                    // Skip duplicates for second element
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    // Skip duplicates for third element
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {-1, 0, 1, 2, -1, -4};
        int target = 0;
        List<List<Integer>> triplets = findAllTriplets(nums, target);
        System.out.println(triplets); // [[-1, -1, 2], [-1, 0, 1]]
    }
}
```

**Time Complexity**: O(n²)  
**Space Complexity**: O(1) excluding result  
**Use Case**: When all unique triplets are needed

---

### Solution 5: Count All Triplets (Including Duplicates)

```java
import java.util.*;

public class ThreeSumCount {
    
    public static int countTriplets(int[] nums, int target) {
        Arrays.sort(nums);
        int n = nums.length;
        int count = 0;
        
        for (int i = 0; i < n - 2; i++) {
            int left = i + 1;
            int right = n - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == target) {
                    // Count all combinations
                    if (nums[left] == nums[right]) {
                        int range = right - left + 1;
                        count += (range * (range - 1)) / 2;
                        break;
                    }
                    
                    int leftCount = 1, rightCount = 1;
                    while (left + 1 < right && nums[left] == nums[left + 1]) {
                        leftCount++;
                        left++;
                    }
                    while (right - 1 > left && nums[right] == nums[right - 1]) {
                        rightCount++;
                        right--;
                    }
                    
                    count += leftCount * rightCount;
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return count;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 2, 2, 3, 3};
        int target = 6;
        System.out.println("Count: " + countTriplets(nums, target)); // 8
    }
}
```

**Time Complexity**: O(n²)  
**Space Complexity**: O(1)

---

## Dry Run Example 1

**Input**: `nums = [1, 4, 45, 6, 10, 8]`, `target = 22`

### Using Two Pointers Approach:

**Step 1: Sort Array**
```
Original: [1, 4, 45, 6, 10, 8]
Sorted:   [1, 4, 6, 8, 10, 45]
```

**Step 2: Iterate with Two Pointers**

```
i=0, nums[i]=1, need to find two numbers that sum to 21

  left=1, right=5
  sum = 1 + 4 + 45 = 50 > 22 → right--
  
  left=1, right=4
  sum = 1 + 4 + 10 = 15 < 22 → left++
  
  left=2, right=4
  sum = 1 + 6 + 10 = 17 < 22 → left++
  
  left=3, right=4
  sum = 1 + 8 + 10 = 19 < 22 → left++
  
  left=4, right=4 → left >= right, exit

i=1, nums[i]=4, need to find two numbers that sum to 18

  left=2, right=5
  sum = 4 + 6 + 45 = 55 > 22 → right--
  
  left=2, right=4
  sum = 4 + 6 + 10 = 20 < 22 → left++
  
  left=3, right=4
  sum = 4 + 8 + 10 = 22 == 22 ✓ FOUND!

Triplet: [4, 8, 10]
Result: true
```

---

## Dry Run Example 2

**Input**: `nums = [-1, 0, 1, 2, -1, -4]`, `target = 0`

### Using Two Pointers (Find All Unique Triplets):

**Step 1: Sort Array**
```
Original: [-1, 0, 1, 2, -1, -4]
Sorted:   [-4, -1, -1, 0, 1, 2]
```

**Step 2: Find All Triplets**

```
i=0, nums[i]=-4, need sum = 4

  left=1, right=5
  sum = -4 + (-1) + 2 = -3 < 0 → left++
  
  left=2, right=5
  sum = -4 + (-1) + 2 = -3 < 0 → left++
  
  left=3, right=5
  sum = -4 + 0 + 2 = -2 < 0 → left++
  
  left=4, right=5
  sum = -4 + 1 + 2 = -1 < 0 → left++
  
  left=5, right=5 → exit

i=1, nums[i]=-1, need sum = 1

  left=2, right=5
  sum = -1 + (-1) + 2 = 0 == 0 ✓ FOUND!
  Add [-1, -1, 2]
  Skip duplicates, left++, right--
  
  left=3, right=4
  sum = -1 + 0 + 1 = 0 == 0 ✓ FOUND!
  Add [-1, 0, 1]
  left++, right--
  
  left=4, right=3 → exit

i=2, nums[i]=-1, skip (duplicate of previous)

i=3, nums[i]=0, need sum = 0

  left=4, right=5
  sum = 0 + 1 + 2 = 3 > 0 → right--
  
  left=4, right=4 → exit

Result: [[-1, -1, 2], [-1, 0, 1]]
```

---

## Edge Test Cases

```java
// Test Case 1: Exact three elements
int[] arr1 = {1, 2, 3};
int target1 = 6;
boolean result1 = hasThreeSum(arr1, target1);
// Expected: true (1 + 2 + 3 = 6)

// Test Case 2: No triplet exists
int[] arr2 = {1, 2, 3, 4};
int target2 = 20;
boolean result2 = hasThreeSum(arr2, target2);
// Expected: false

// Test Case 3: Array with duplicates
int[] arr3 = {1, 1, 1, 1};
int target3 = 3;
boolean result3 = hasThreeSum(arr3, target3);
// Expected: true (1 + 1 + 1 = 3)

// Test Case 4: Negative numbers
int[] arr4 = {-5, -2, 0, 3, 4};
int target4 = -3;
boolean result4 = hasThreeSum(arr4, target4);
// Expected: true (-5 + (-2) + 4 = -3)

// Test Case 5: All negative numbers
int[] arr5 = {-10, -5, -3, -1};
int target5 = -18;
boolean result5 = hasThreeSum(arr5, target5);
// Expected: true (-10 + (-5) + (-3) = -18)

// Test Case 6: Mix of positive and negative
int[] arr6 = {-1, 0, 1, 2, -1, -4};
int target6 = 0;
boolean result6 = hasThreeSum(arr6, target6);
// Expected: true (-1 + 0 + 1 = 0)

// Test Case 7: Large numbers
int[] arr7 = {1000, 2000, 3000, 4000};
int target7 = 6000;
boolean result7 = hasThreeSum(arr7, target7);
// Expected: true (1000 + 2000 + 3000 = 6000)

// Test Case 8: Zero target
int[] arr8 = {0, 0, 0};
int target8 = 0;
boolean result8 = hasThreeSum(arr8, target8);
// Expected: true (0 + 0 + 0 = 0)

// Test Case 9: Array length less than 3
int[] arr9 = {1, 2};
int target9 = 5;
boolean result9 = hasThreeSum(arr9, target9);
// Expected: false (not enough elements)

// Test Case 10: First three elements sum to target
int[] arr10 = {5, 10, 15, 20, 25};
int target10 = 30;
boolean result10 = hasThreeSum(arr10, target10);
// Expected: true (5 + 10 + 15 = 30)

// Test Case 11: Last three elements sum to target
int[] arr11 = {1, 2, 3, 10, 20, 30};
int target11 = 60;
boolean result11 = hasThreeSum(arr11, target11);
// Expected: true (10 + 20 + 30 = 60)

// Test Case 12: Same element used multiple times
int[] arr12 = {2, 2, 2, 2};
int target12 = 6;
boolean result12 = hasThreeSum(arr12, target12);
// Expected: true (2 + 2 + 2 = 6)
```

---

## Algorithm Comparison

| Algorithm | Time | Space | Modifies Array | Best For |
|-----------|------|-------|----------------|----------|
| Brute Force | O(n³) | O(1) | No | n < 100 |
| Two Pointers | O(n²) | O(1) | Yes (sorting) | **Interviews** ⭐ |
| HashMap | O(n²) | O(n) | No | Immutable arrays |
| All Triplets | O(n²) | O(1) | Yes | Find all solutions |

---

## Variations

### Variation 1: Three Sum Closest

```java
public int threeSumClosest(int[] nums, int target) {
    Arrays.sort(nums);
    int closest = nums[0] + nums[1] + nums[2];
    
    for (int i = 0; i < nums.length - 2; i++) {
        int left = i + 1, right = nums.length - 1;
        
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            
            if (Math.abs(sum - target) < Math.abs(closest - target)) {
                closest = sum;
            }
            
            if (sum < target) {
                left++;
            } else if (sum > target) {
                right--;
            } else {
                return sum; // Exact match
            }
        }
    }
    
    return closest;
}

// Example: nums = [-1, 2, 1, -4], target = 1
// Result: 2 (sum of -1 + 2 + 1 = 2)
```

---

### Variation 2: Three Sum Smaller

```java
// Count triplets with sum < target
public int threeSumSmaller(int[] nums, int target) {
    Arrays.sort(nums);
    int count = 0;
    
    for (int i = 0; i < nums.length - 2; i++) {
        int left = i + 1, right = nums.length - 1;
        
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            
            if (sum < target) {
                count += right - left; // All elements between left and right work
                left++;
            } else {
                right--;
            }
        }
    }
    
    return count;
}

// Example: nums = [-2, 0, 1, 3], target = 2
// Result: 2 (triplets: [-2, 0, 1] and [-2, 0, 3])
```

---

### Variation 3: Three Sum with Multiplicity

```java
// Count triplets where i < j < k and nums[i] + nums[j] + nums[k] == target
public int threeSumMulti(int[] nums, int target) {
    long MOD = 1_000_000_007;
    long[] count = new long[101]; // Assuming nums[i] in [0, 100]
    
    for (int num : nums) {
        count[num]++;
    }
    
    long result = 0;
    
    for (int i = 0; i <= 100; i++) {
        for (int j = i; j <= 100; j++) {
            int k = target - i - j;
            if (k < 0 || k > 100) continue;
            
            if (i == j && j == k) {
                result += count[i] * (count[i] - 1) * (count[i] - 2) / 6;
            } else if (i == j && j != k) {
                result += count[i] * (count[i] - 1) / 2 * count[k];
            } else if (i < j && j < k) {
                result += count[i] * count[j] * count[k];
            }
        }
    }
    
    return (int) (result % MOD);
}
```

---

## Complete Working Example

```java
import java.util.*;

public class ThreeSumComplete {
    
    // Method 1: Two Pointers (Optimal)
    public static boolean hasThreeSumTwoPointers(int[] nums, int target) {
        if (nums.length < 3) return false;
        
        Arrays.sort(nums);
        
        for (int i = 0; i < nums.length - 2; i++) {
            int left = i + 1;
            int right = nums.length - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == target) {
                    System.out.println("Triplet found: [" + nums[i] + ", " + 
                                     nums[left] + ", " + nums[right] + "]");
                    return true;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return false;
    }
    
    // Method 2: HashMap
    public static boolean hasThreeSumHashMap(int[] nums, int target) {
        if (nums.length < 3) return false;
        
        for (int i = 0; i < nums.length - 2; i++) {
            Set<Integer> seen = new HashSet<>();
            int currentTarget = target - nums[i];
            
            for (int j = i + 1; j < nums.length; j++) {
                int complement = currentTarget - nums[j];
                
                if (seen.contains(complement)) {
                    System.out.println("Triplet found: [" + nums[i] + ", " + 
                                     complement + ", " + nums[j] + "]");
                    return true;
                }
                seen.add(nums[j]);
            }
        }
        
        return false;
    }
    
    // Method 3: Find All Unique Triplets
    public static List<List<Integer>> findAllTriplets(int[] nums, int target) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        
        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;
            
            int left = i + 1;
            int right = nums.length - 1;
            
            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];
                
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    
                    while (left < right && nums[left] == nums[left + 1]) left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    
                    left++;
                    right--;
                } else if (sum < target) {
                    left++;
                } else {
                    right--;
                }
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Test 1: Basic Case ===");
        int[] nums1 = {1, 4, 45, 6, 10, 8};
        int target1 = 22;
        System.out.println("Two Pointers: " + hasThreeSumTwoPointers(nums1, target1));
        System.out.println("HashMap: " + hasThreeSumHashMap(nums1.clone(), target1));
        
        System.out.println("\n=== Test 2: No Triplet ===");
        int[] nums2 = {1, 2, 3, 4, 5};
        int target2 = 20;
        System.out.println("Result: " + hasThreeSumTwoPointers(nums2, target2));
        
        System.out.println("\n=== Test 3: Negative Numbers ===");
        int[] nums3 = {-1, 0, 1, 2, -1, -4};
        int target3 = 0;
        System.out.println("All Triplets: " + findAllTriplets(nums3, target3));
        
        System.out.println("\n=== Test 4: Duplicates ===");
        int[] nums4 = {1, 1, 1, 1};
        int target4 = 3;
        System.out.println("Result: " + hasThreeSumTwoPointers(nums4, target4));
        
        System.out.println("\n=== Test 5: Edge Case - Less than 3 elements ===");
        int[] nums5 = {1, 2};
        int target5 = 5;
        System.out.println("Result: " + hasThreeSumTwoPointers(nums5, target5));
    }
}
```

**Output**:
```
=== Test 1: Basic Case ===
Triplet found: [4, 8, 10]
Two Pointers: true
Triplet found: [4, 10, 8]
HashMap: true

=== Test 2: No Triplet ===
Result: false

=== Test 3: Negative Numbers ===
All Triplets: [[-1, -1, 2], [-1, 0, 1]]

=== Test 4: Duplicates ===
Triplet found: [1, 1, 1]
Result: true

=== Test 5: Edge Case - Less than 3 elements ===
Result: false
```

---

## Real-World Use Cases

### 1. **Financial Analysis**
```java
// Find three stocks whose combined price equals budget
boolean canAffordStocks(int[] stockPrices, int budget) {
    return hasThreeSum(stockPrices, budget);
}
```

### 2. **Resource Allocation**
```java
// Allocate three resources with total capacity = requirement
boolean allocateResources(int[] capacities, int requirement) {
    return hasThreeSum(capacities, requirement);
}
```

### 3. **Chemistry - Compound Formation**
```java
// Find three elements whose atomic weights sum to target
boolean canFormCompound(int[] atomicWeights, int targetWeight) {
    return hasThreeSum(atomicWeights, targetWeight);
}
```

### 4. **Game Development**
```java
// Find three items whose combined power equals boss health
boolean canDefeatBoss(int[] itemPowers, int bossHealth) {
    return hasThreeSum(itemPowers, bossHealth);
}
```

---

## Key Takeaways

1. **Two Pointers is optimal**: O(n²) time, O(1) space
2. **Sort first**: Essential for two-pointer approach
3. **HashMap alternative**: When array can't be modified
4. **Skip duplicates**: For finding unique triplets
5. **Edge cases**: Array length < 3, all same elements
6. **Pattern recognition**: Reduce to Two Sum problem

---

## Interview Tips

1. **Ask clarifying questions**:
   - Can array be modified (sorted)?
   - Are duplicates allowed?
   - Need all triplets or just existence?
   - Can same element be used multiple times?

2. **Start with brute force**: Explain O(n³) approach first

3. **Optimize step-by-step**: Move to O(n²) with sorting

4. **Handle edge cases**: Empty array, length < 3, duplicates

5. **Test with examples**: Use small arrays to verify logic

6. **Discuss trade-offs**: Time vs space, sorting vs hashing
