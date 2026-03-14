# Dynamic Programming - Medium Problems (50%)

## 📚 10 Medium Problems with Complete Solutions

---

## Problem 1: Longest Increasing Subsequence

**Difficulty**: Medium  
**Pattern**: Subsequence DP  
**LeetCode**: #300

### Problem Statement

Given an integer array `nums`, return the length of the longest strictly increasing subsequence.

### Examples

```
Input: nums = [10,9,2,5,3,7,101,18]
Output: 4
Explanation: [2,3,7,101] or [2,3,7,18]

Input: nums = [0,1,0,3,2,3]
Output: 4
```

### Solution

```java
public class LongestIncreasingSubsequence {
    // DP Solution: O(n²)
    public int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        
        int maxLen = 1;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        
        return maxLen;
    }
    
    // Binary Search Solution: O(n log n)
    public int lengthOfLISOptimized(int[] nums) {
        List<Integer> tails = new ArrayList<>();
        
        for (int num : nums) {
            int pos = binarySearch(tails, num);
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                tails.set(pos, num);
            }
        }
        
        return tails.size();
    }
    
    private int binarySearch(List<Integer> tails, int target) {
        int left = 0, right = tails.size();
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (tails.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(n²) for DP, O(n log n) for binary search
- **Space Complexity**: O(n)

---

## Problem 2: Coin Change

**Difficulty**: Medium  
**Pattern**: Knapsack DP  
**LeetCode**: #322

### Problem Statement

You are given an integer array `coins` representing coins of different denominations and an integer `amount`. Return the fewest number of coins needed to make up that amount. If impossible, return -1.

### Examples

```
Input: coins = [1,2,5], amount = 11
Output: 3
Explanation: 11 = 5 + 5 + 1

Input: coins = [2], amount = 3
Output: -1
```

### Solution

```java
public class CoinChange {
    public int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);
        dp[0] = 0;
        
        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1);
                }
            }
        }
        
        return dp[amount] > amount ? -1 : dp[amount];
    }
}
```

### Dry Run

**Input**: `coins = [1, 2, 5]`, `amount = 11`

```
dp[0] = 0

dp[1]: min(dp[1-1]+1) = min(0+1) = 1
dp[2]: min(dp[2-1]+1, dp[2-2]+1) = min(1+1, 0+1) = 1
dp[3]: min(dp[3-1]+1, dp[3-2]+1) = min(1+1, 1+1) = 2
dp[4]: min(dp[4-1]+1, dp[4-2]+1) = min(2+1, 1+1) = 2
dp[5]: min(dp[5-1]+1, dp[5-2]+1, dp[5-5]+1) = min(2+1, 2+1, 0+1) = 1
...
dp[11]: min(dp[11-1]+1, dp[11-2]+1, dp[11-5]+1) = min(3+1, 3+1, 2+1) = 3

Result: 3 (5+5+1)
```

### Complexity Analysis

- **Time Complexity**: O(amount × coins.length)
- **Space Complexity**: O(amount)

---

## Problem 3: Longest Common Subsequence

**Difficulty**: Medium  
**Pattern**: 2D Subsequence DP  
**LeetCode**: #1143

### Problem Statement

Given two strings `text1` and `text2`, return the length of their longest common subsequence. If there is no common subsequence, return 0.

### Examples

```
Input: text1 = "abcde", text2 = "ace" 
Output: 3
Explanation: "ace" is the longest common subsequence

Input: text1 = "abc", text2 = "abc"
Output: 3

Input: text1 = "abc", text2 = "def"
Output: 0
```

### Solution

```java
public class LongestCommonSubsequence {
    public int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i-1) == text2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
                }
            }
        }
        
        return dp[m][n];
    }
}
```

### Complexity Analysis

- **Time Complexity**: O(m × n)
- **Space Complexity**: O(m × n)

---

## Problem 4: Unique Paths

**Difficulty**: Medium  
**Pattern**: 2D Grid DP  
**LeetCode**: #62

### Problem Statement

A robot is located at the top-left corner of an `m x n` grid. The robot can only move down or right. How many possible unique paths are there to reach the bottom-right corner?

### Solution

```java
public class UniquePaths {
    public int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];
        
        // Initialize first row and column
        for (int i = 0; i < m; i++) dp[i][0] = 1;
        for (int j = 0; j < n; j++) dp[0][j] = 1;
        
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i-1][j] + dp[i][j-1];
            }
        }
        
        return dp[m-1][n-1];
    }
    
    // Space Optimized
    public int uniquePathsOptimized(int m, int n) {
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[j] += dp[j-1];
            }
        }
        
        return dp[n-1];
    }
}
```

---

## Problem 5: House Robber II

**Difficulty**: Medium  
**Pattern**: Linear DP with Constraint  
**LeetCode**: #213

### Problem Statement

Same as House Robber, but houses are arranged in a circle (first and last are adjacent). You cannot rob both first and last house.

### Solution

```java
public class HouseRobberII {
    public int rob(int[] nums) {
        if (nums.length == 1) return nums[0];
        
        return Math.max(
            robRange(nums, 0, nums.length - 2),
            robRange(nums, 1, nums.length - 1)
        );
    }
    
    private int robRange(int[] nums, int start, int end) {
        int prev2 = 0, prev1 = 0;
        
        for (int i = start; i <= end; i++) {
            int curr = Math.max(prev1, prev2 + nums[i]);
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

---

## Problem 6: Decode Ways

**Difficulty**: Medium  
**Pattern**: Linear DP  
**LeetCode**: #91

### Problem Statement

A message containing letters from A-Z can be encoded into numbers using 'A' -> "1", 'B' -> "2", ..., 'Z' -> "26". Given a string `s` containing only digits, return the number of ways to decode it.

### Solution

```java
public class DecodeWays {
    public int numDecodings(String s) {
        if (s.charAt(0) == '0') return 0;
        
        int n = s.length();
        int prev2 = 1, prev1 = 1;
        
        for (int i = 1; i < n; i++) {
            int curr = 0;
            
            // Single digit
            if (s.charAt(i) != '0') {
                curr += prev1;
            }
            
            // Two digits
            int twoDigit = Integer.parseInt(s.substring(i-1, i+1));
            if (twoDigit >= 10 && twoDigit <= 26) {
                curr += prev2;
            }
            
            prev2 = prev1;
            prev1 = curr;
        }
        
        return prev1;
    }
}
```

---

## Problem 7: Word Break

**Difficulty**: Medium  
**Pattern**: Linear DP with Dictionary  
**LeetCode**: #139

### Problem Statement

Given a string `s` and a dictionary of strings `wordDict`, return `true` if `s` can be segmented into a space-separated sequence of dictionary words.

### Solution

```java
public class WordBreak {
    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> dict = new HashSet<>(wordDict);
        boolean[] dp = new boolean[s.length() + 1];
        dp[0] = true;
        
        for (int i = 1; i <= s.length(); i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && dict.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break;
                }
            }
        }
        
        return dp[s.length()];
    }
}
```

---

## Problem 8: Partition Equal Subset Sum

**Difficulty**: Medium  
**Pattern**: Knapsack DP  
**LeetCode**: #416

### Problem Statement

Given a non-empty array `nums` containing only positive integers, find if the array can be partitioned into two subsets such that the sum of elements in both subsets is equal.

### Solution

```java
public class PartitionEqualSubsetSum {
    public boolean canPartition(int[] nums) {
        int sum = 0;
        for (int num : nums) sum += num;
        
        if (sum % 2 != 0) return false;
        
        int target = sum / 2;
        boolean[] dp = new boolean[target + 1];
        dp[0] = true;
        
        for (int num : nums) {
            for (int j = target; j >= num; j--) {
                dp[j] = dp[j] || dp[j - num];
            }
        }
        
        return dp[target];
    }
}
```

---

## Problem 9: Longest Palindromic Substring

**Difficulty**: Medium  
**Pattern**: Interval DP  
**LeetCode**: #5

### Problem Statement

Given a string `s`, return the longest palindromic substring in `s`.

### Solution

```java
public class LongestPalindromicSubstring {
    public String longestPalindrome(String s) {
        int n = s.length();
        boolean[][] dp = new boolean[n][n];
        int start = 0, maxLen = 1;
        
        // Single characters
        for (int i = 0; i < n; i++) {
            dp[i][i] = true;
        }
        
        // Two characters
        for (int i = 0; i < n - 1; i++) {
            if (s.charAt(i) == s.charAt(i+1)) {
                dp[i][i+1] = true;
                start = i;
                maxLen = 2;
            }
        }
        
        // Longer substrings
        for (int len = 3; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                
                if (s.charAt(i) == s.charAt(j) && dp[i+1][j-1]) {
                    dp[i][j] = true;
                    start = i;
                    maxLen = len;
                }
            }
        }
        
        return s.substring(start, start + maxLen);
    }
}
```

---

## Problem 10: Maximum Product Subarray

**Difficulty**: Medium  
**Pattern**: Linear DP  
**LeetCode**: #152

### Problem Statement

Given an integer array `nums`, find a contiguous non-empty subarray with the largest product, and return the product.

### Solution

```java
public class MaximumProductSubarray {
    public int maxProduct(int[] nums) {
        int maxSoFar = nums[0];
        int maxEndingHere = nums[0];
        int minEndingHere = nums[0];
        
        for (int i = 1; i < nums.length; i++) {
            int temp = maxEndingHere;
            
            maxEndingHere = Math.max(nums[i], 
                Math.max(maxEndingHere * nums[i], minEndingHere * nums[i]));
            
            minEndingHere = Math.min(nums[i], 
                Math.min(temp * nums[i], minEndingHere * nums[i]));
            
            maxSoFar = Math.max(maxSoFar, maxEndingHere);
        }
        
        return maxSoFar;
    }
}
```

---

## 📊 Summary

| Problem | Pattern | Time | Space | Key Concept |
|---------|---------|------|-------|-------------|
| LIS | Subsequence | O(n²) | O(n) | Track longest ending at i |
| Coin Change | Knapsack | O(n×m) | O(n) | Min coins for amount |
| LCS | 2D Subsequence | O(m×n) | O(m×n) | Match characters |
| Unique Paths | 2D Grid | O(m×n) | O(n) | Sum paths from top/left |
| House Robber II | Linear + Constraint | O(n) | O(1) | Two cases: skip first or last |
| Decode Ways | Linear | O(n) | O(1) | Single or double digit |
| Word Break | Linear + Dict | O(n²) | O(n) | Check all substrings |
| Partition Sum | Knapsack | O(n×sum) | O(sum) | Subset sum = total/2 |
| Longest Palindrome | Interval | O(n²) | O(n²) | Expand from center |
| Max Product | Linear | O(n) | O(1) | Track max and min |

---

**Next**: [Hard Problems](05_Hard_Problems.md)
