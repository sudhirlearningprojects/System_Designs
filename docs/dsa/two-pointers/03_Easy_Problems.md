# Two Pointers - Easy Problems (40%)

## 📚 8 Easy Problems with Complete Solutions

---

## Problem 1: Two Sum II - Input Array Is Sorted

**Difficulty**: Easy  
**Pattern**: Opposite Direction  
**LeetCode**: #167

### Problem Statement

Given a 1-indexed array of integers `numbers` that is already sorted in non-decreasing order, find two numbers such that they add up to a specific target number. Return the indices of the two numbers (1-indexed).

**Constraints**:
- 2 ≤ numbers.length ≤ 3 × 10⁴
- -1000 ≤ numbers[i] ≤ 1000
- numbers is sorted in non-decreasing order
- -1000 ≤ target ≤ 1000
- Exactly one solution exists

### Examples

```
Input: numbers = [2,7,11,15], target = 9
Output: [1,2]
Explanation: 2 + 7 = 9, return [1, 2]

Input: numbers = [2,3,4], target = 6
Output: [1,3]

Input: numbers = [-1,0], target = -1
Output: [1,2]
```

### Solution

```java
public class TwoSumII {
    public int[] twoSum(int[] numbers, int target) {
        int left = 0;
        int right = numbers.length - 1;
        
        while (left < right) {
            int sum = numbers[left] + numbers[right];
            
            if (sum == target) {
                return new int[]{left + 1, right + 1}; // 1-indexed
            } else if (sum < target) {
                left++;  // Need larger sum
            } else {
                right--; // Need smaller sum
            }
        }
        
        return new int[]{-1, -1}; // No solution (won't happen per constraints)
    }
}
```

### Dry Run

**Input**: `numbers = [2, 7, 11, 15]`, `target = 9`

```
Initial: left = 0, right = 3
Array: [2, 7, 11, 15]
        ↑           ↑
      left        right

Step 1:
  sum = numbers[0] + numbers[3] = 2 + 15 = 17
  17 > 9, so right--
  
Step 2: left = 0, right = 2
Array: [2, 7, 11, 15]
        ↑      ↑
      left   right
  
  sum = numbers[0] + numbers[2] = 2 + 11 = 13
  13 > 9, so right--
  
Step 3: left = 0, right = 1
Array: [2, 7, 11, 15]
        ↑  ↑
      left right
  
  sum = numbers[0] + numbers[1] = 2 + 7 = 9
  9 == 9, found! Return [1, 2] (1-indexed)
```

### Complexity Analysis

- **Time Complexity**: O(n) - Each pointer moves at most n times
- **Space Complexity**: O(1) - Only two pointers used

### Test Cases

```java
@Test
public void testTwoSumII() {
    TwoSumII solution = new TwoSumII();
    
    // Test case 1: Basic case
    assertArrayEquals(new int[]{1, 2}, 
        solution.twoSum(new int[]{2, 7, 11, 15}, 9));
    
    // Test case 2: Target at beginning
    assertArrayEquals(new int[]{1, 2}, 
        solution.twoSum(new int[]{2, 3, 4}, 6));
    
    // Test case 3: Negative numbers
    assertArrayEquals(new int[]{1, 2}, 
        solution.twoSum(new int[]{-1, 0}, -1));
    
    // Test case 4: Large array
    assertArrayEquals(new int[]{1, 5}, 
        solution.twoSum(new int[]{1, 2, 3, 4, 5}, 6));
    
    // Test case 5: Target at end
    assertArrayEquals(new int[]{4, 5}, 
        solution.twoSum(new int[]{1, 2, 3, 4, 5}, 9));
}
```

---

## Problem 2: Remove Duplicates from Sorted Array

**Difficulty**: Easy  
**Pattern**: Same Direction  
**LeetCode**: #26

### Problem Statement

Given an integer array `nums` sorted in non-decreasing order, remove duplicates in-place such that each unique element appears only once. Return the number of unique elements.

**Constraints**:
- 1 ≤ nums.length ≤ 3 × 10⁴
- -100 ≤ nums[i] ≤ 100
- nums is sorted in non-decreasing order

### Examples

```
Input: nums = [1,1,2]
Output: 2, nums = [1,2,_]

Input: nums = [0,0,1,1,1,2,2,3,3,4]
Output: 5, nums = [0,1,2,3,4,_,_,_,_,_]
```

### Solution

```java
public class RemoveDuplicates {
    public int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;
        
        int slow = 0; // Position for next unique element
        
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
        }
        
        return slow + 1; // Length of unique elements
    }
}
```

### Dry Run

**Input**: `nums = [0, 0, 1, 1, 1, 2, 2, 3, 3, 4]`

```
Initial: slow = 0, fast = 1
[0, 0, 1, 1, 1, 2, 2, 3, 3, 4]
 ↑  ↑
slow fast

Step 1: fast = 1
  nums[1] == nums[0] (0 == 0), skip
  
Step 2: fast = 2
  nums[2] != nums[0] (1 != 0)
  slow++, nums[1] = nums[2]
  [0, 1, 1, 1, 1, 2, 2, 3, 3, 4]
     ↑     ↑
   slow  fast

Step 3: fast = 3
  nums[3] == nums[1] (1 == 1), skip
  
Step 4: fast = 4
  nums[4] == nums[1] (1 == 1), skip
  
Step 5: fast = 5
  nums[5] != nums[1] (2 != 1)
  slow++, nums[2] = nums[5]
  [0, 1, 2, 1, 1, 2, 2, 3, 3, 4]
        ↑           ↑
      slow        fast

Step 6: fast = 6
  nums[6] == nums[2] (2 == 2), skip
  
Step 7: fast = 7
  nums[7] != nums[2] (3 != 2)
  slow++, nums[3] = nums[7]
  [0, 1, 2, 3, 1, 2, 2, 3, 3, 4]
           ↑              ↑
         slow           fast

Step 8: fast = 8
  nums[8] == nums[3] (3 == 3), skip
  
Step 9: fast = 9
  nums[9] != nums[3] (4 != 3)
  slow++, nums[4] = nums[9]
  [0, 1, 2, 3, 4, 2, 2, 3, 3, 4]
              ↑                 ↑
            slow              fast

Result: slow + 1 = 5
Final array: [0, 1, 2, 3, 4, _, _, _, _, _]
```

### Complexity Analysis

- **Time Complexity**: O(n) - Single pass through array
- **Space Complexity**: O(1) - In-place modification

### Test Cases

```java
@Test
public void testRemoveDuplicates() {
    RemoveDuplicates solution = new RemoveDuplicates();
    
    // Test case 1: Basic case
    int[] nums1 = {1, 1, 2};
    assertEquals(2, solution.removeDuplicates(nums1));
    assertArrayEquals(new int[]{1, 2, 2}, nums1);
    
    // Test case 2: Multiple duplicates
    int[] nums2 = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
    assertEquals(5, solution.removeDuplicates(nums2));
    
    // Test case 3: No duplicates
    int[] nums3 = {1, 2, 3, 4, 5};
    assertEquals(5, solution.removeDuplicates(nums3));
    
    // Test case 4: All same
    int[] nums4 = {1, 1, 1, 1};
    assertEquals(1, solution.removeDuplicates(nums4));
    
    // Test case 5: Single element
    int[] nums5 = {1};
    assertEquals(1, solution.removeDuplicates(nums5));
}
```

---

## Problem 3: Valid Palindrome

**Difficulty**: Easy  
**Pattern**: Opposite Direction  
**LeetCode**: #125

### Problem Statement

A phrase is a palindrome if, after converting all uppercase letters to lowercase and removing all non-alphanumeric characters, it reads the same forward and backward.

Given a string `s`, return `true` if it is a palindrome, or `false` otherwise.

### Examples

```
Input: s = "A man, a plan, a canal: Panama"
Output: true
Explanation: "amanaplanacanalpanama" is a palindrome

Input: s = "race a car"
Output: false
Explanation: "raceacar" is not a palindrome

Input: s = " "
Output: true
```

### Solution

```java
public class ValidPalindrome {
    public boolean isPalindrome(String s) {
        int left = 0;
        int right = s.length() - 1;
        
        while (left < right) {
            // Skip non-alphanumeric from left
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) {
                left++;
            }
            
            // Skip non-alphanumeric from right
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) {
                right--;
            }
            
            // Compare characters (case-insensitive)
            if (Character.toLowerCase(s.charAt(left)) != 
                Character.toLowerCase(s.charAt(right))) {
                return false;
            }
            
            left++;
            right--;
        }
        
        return true;
    }
}
```

### Dry Run

**Input**: `s = "A man, a plan, a canal: Panama"`

```
Step 1: left = 0, right = 30
  s[0] = 'A', s[30] = 'a'
  'a' == 'a' ✓, continue
  
Step 2: left = 1, right = 29
  s[1] = ' ' (skip), left = 2
  s[2] = 'm', s[29] = 'm'
  'm' == 'm' ✓, continue
  
Step 3: left = 3, right = 28
  s[3] = 'a', s[28] = 'a'
  'a' == 'a' ✓, continue
  
... (continue comparing)

All characters match → return true
```

### Complexity Analysis

- **Time Complexity**: O(n) - Single pass
- **Space Complexity**: O(1) - No extra space

### Test Cases

```java
@Test
public void testValidPalindrome() {
    ValidPalindrome solution = new ValidPalindrome();
    
    assertTrue(solution.isPalindrome("A man, a plan, a canal: Panama"));
    assertFalse(solution.isPalindrome("race a car"));
    assertTrue(solution.isPalindrome(" "));
    assertTrue(solution.isPalindrome("a"));
    assertTrue(solution.isPalindrome(""));
    assertFalse(solution.isPalindrome("0P"));
}
```

---

## Problem 4: Move Zeroes

**Difficulty**: Easy  
**Pattern**: Same Direction  
**LeetCode**: #283

### Problem Statement

Given an integer array `nums`, move all 0's to the end while maintaining the relative order of non-zero elements. Must be done in-place.

### Examples

```
Input: nums = [0,1,0,3,12]
Output: [1,3,12,0,0]

Input: nums = [0]
Output: [0]
```

### Solution

```java
public class MoveZeroes {
    public void moveZeroes(int[] nums) {
        int slow = 0; // Position for next non-zero
        
        // Move all non-zeros to front
        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0) {
                nums[slow] = nums[fast];
                slow++;
            }
        }
        
        // Fill remaining with zeros
        while (slow < nums.length) {
            nums[slow] = 0;
            slow++;
        }
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

### Test Cases

```java
@Test
public void testMoveZeroes() {
    MoveZeroes solution = new MoveZeroes();
    
    int[] nums1 = {0, 1, 0, 3, 12};
    solution.moveZeroes(nums1);
    assertArrayEquals(new int[]{1, 3, 12, 0, 0}, nums1);
    
    int[] nums2 = {0};
    solution.moveZeroes(nums2);
    assertArrayEquals(new int[]{0}, nums2);
}
```

---

## Problem 5: Reverse String

**Difficulty**: Easy  
**Pattern**: Opposite Direction  
**LeetCode**: #344

### Problem Statement

Write a function that reverses a string. The input string is given as an array of characters `s`. You must do this by modifying the input array in-place with O(1) extra memory.

### Solution

```java
public class ReverseString {
    public void reverseString(char[] s) {
        int left = 0;
        int right = s.length - 1;
        
        while (left < right) {
            // Swap characters
            char temp = s[left];
            s[left] = s[right];
            s[right] = temp;
            
            left++;
            right--;
        }
    }
}
```

### Test Cases

```java
@Test
public void testReverseString() {
    ReverseString solution = new ReverseString();
    
    char[] s1 = {'h', 'e', 'l', 'l', 'o'};
    solution.reverseString(s1);
    assertArrayEquals(new char[]{'o', 'l', 'l', 'e', 'h'}, s1);
}
```

---

## Problem 6: Squares of a Sorted Array

**Difficulty**: Easy  
**Pattern**: Opposite Direction  
**LeetCode**: #977

### Problem Statement

Given an integer array `nums` sorted in non-decreasing order, return an array of the squares of each number sorted in non-decreasing order.

### Examples

```
Input: nums = [-4,-1,0,3,10]
Output: [0,1,9,16,100]

Input: nums = [-7,-3,2,3,11]
Output: [4,9,9,49,121]
```

### Solution

```java
public class SortedSquares {
    public int[] sortedSquares(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        int left = 0;
        int right = n - 1;
        int pos = n - 1; // Fill from end
        
        while (left <= right) {
            int leftSquare = nums[left] * nums[left];
            int rightSquare = nums[right] * nums[right];
            
            if (leftSquare > rightSquare) {
                result[pos] = leftSquare;
                left++;
            } else {
                result[pos] = rightSquare;
                right--;
            }
            pos--;
        }
        
        return result;
    }
}
```

### Dry Run

**Input**: `nums = [-4, -1, 0, 3, 10]`

```
Initial: left = 0, right = 4, pos = 4
[-4, -1, 0, 3, 10]
  ↑            ↑
left         right

Step 1:
  leftSquare = 16, rightSquare = 100
  100 > 16, result[4] = 100, right--
  result = [_, _, _, _, 100]
  
Step 2: left = 0, right = 3, pos = 3
  leftSquare = 16, rightSquare = 9
  16 > 9, result[3] = 16, left++
  result = [_, _, _, 16, 100]
  
Step 3: left = 1, right = 3, pos = 2
  leftSquare = 1, rightSquare = 9
  9 > 1, result[2] = 9, right--
  result = [_, _, 9, 16, 100]
  
Step 4: left = 1, right = 2, pos = 1
  leftSquare = 1, rightSquare = 0
  1 > 0, result[1] = 1, left++
  result = [_, 1, 9, 16, 100]
  
Step 5: left = 2, right = 2, pos = 0
  leftSquare = 0, rightSquare = 0
  result[0] = 0
  result = [0, 1, 9, 16, 100]
```

### Complexity Analysis

- **Time Complexity**: O(n)
- **Space Complexity**: O(n) for result array

### Test Cases

```java
@Test
public void testSortedSquares() {
    SortedSquares solution = new SortedSquares();
    
    assertArrayEquals(new int[]{0, 1, 9, 16, 100}, 
        solution.sortedSquares(new int[]{-4, -1, 0, 3, 10}));
    
    assertArrayEquals(new int[]{4, 9, 9, 49, 121}, 
        solution.sortedSquares(new int[]{-7, -3, 2, 3, 11}));
}
```

---

## Problem 7: Merge Sorted Array

**Difficulty**: Easy  
**Pattern**: Opposite Direction  
**LeetCode**: #88

### Problem Statement

You are given two integer arrays `nums1` and `nums2`, sorted in non-decreasing order, and two integers `m` and `n`, representing the number of elements in `nums1` and `nums2` respectively.

Merge `nums2` into `nums1` as one sorted array. The final sorted array should be stored inside `nums1`.

### Solution

```java
public class MergeSortedArray {
    public void merge(int[] nums1, int m, int[] nums2, int n) {
        int p1 = m - 1;     // Pointer for nums1
        int p2 = n - 1;     // Pointer for nums2
        int p = m + n - 1;  // Pointer for merged position
        
        // Merge from end to beginning
        while (p2 >= 0) {
            if (p1 >= 0 && nums1[p1] > nums2[p2]) {
                nums1[p] = nums1[p1];
                p1--;
            } else {
                nums1[p] = nums2[p2];
                p2--;
            }
            p--;
        }
    }
}
```

### Test Cases

```java
@Test
public void testMerge() {
    MergeSortedArray solution = new MergeSortedArray();
    
    int[] nums1 = {1, 2, 3, 0, 0, 0};
    solution.merge(nums1, 3, new int[]{2, 5, 6}, 3);
    assertArrayEquals(new int[]{1, 2, 2, 3, 5, 6}, nums1);
}
```

---

## Problem 8: Remove Element

**Difficulty**: Easy  
**Pattern**: Same Direction  
**LeetCode**: #27

### Problem Statement

Given an integer array `nums` and an integer `val`, remove all occurrences of `val` in-place. Return the number of elements not equal to `val`.

### Solution

```java
public class RemoveElement {
    public int removeElement(int[] nums, int val) {
        int slow = 0;
        
        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != val) {
                nums[slow] = nums[fast];
                slow++;
            }
        }
        
        return slow;
    }
}
```

### Test Cases

```java
@Test
public void testRemoveElement() {
    RemoveElement solution = new RemoveElement();
    
    int[] nums1 = {3, 2, 2, 3};
    assertEquals(2, solution.removeElement(nums1, 3));
    
    int[] nums2 = {0, 1, 2, 2, 3, 0, 4, 2};
    assertEquals(5, solution.removeElement(nums2, 2));
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| Two Sum II | Opposite | O(n) | O(1) | Sorted array pair finding |
| Remove Duplicates | Same | O(n) | O(1) | In-place modification |
| Valid Palindrome | Opposite | O(n) | O(1) | Character comparison |
| Move Zeroes | Same | O(n) | O(1) | Partitioning |
| Reverse String | Opposite | O(n) | O(1) | Swapping |
| Sorted Squares | Opposite | O(n) | O(n) | Merge from ends |
| Merge Sorted Array | Opposite | O(m+n) | O(1) | Merge backwards |
| Remove Element | Same | O(n) | O(1) | Filtering |

---

**Next**: [Medium Problems](04_Medium_Problems.md)
