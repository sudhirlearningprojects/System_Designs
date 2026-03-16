# Dynamic Programming Deep Dive (40 Problems)

## 1. Climbing Stairs (LC 70) ⭐⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/climbing-stairs/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/count-ways-to-reach-the-nth-stair/)

```java
public int climbStairs(int n) {
    if (n <= 2) return n;
    int a = 1, b = 2;
    for (int i = 3; i <= n; i++) { int c = a + b; a = b; b = c; }
    return b;
}
```

## 2. Coin Change (LC 322) ⭐⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/coin-change/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/coin-change/)

```java
public int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, amount + 1);
    dp[0] = 0;
    for (int i = 1; i <= amount; i++)
        for (int coin : coins)
            if (i >= coin) dp[i] = Math.min(dp[i], dp[i - coin] + 1);
    return dp[amount] > amount ? -1 : dp[amount];
}
```

## 3. Longest Increasing Subsequence (LC 300) ⭐⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/longest-increasing-subsequence/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/longest-increasing-subsequence/)

```java
public int lengthOfLIS(int[] nums) {
    int[] dp = new int[nums.length];
    int len = 0;
    for (int num : nums) {
        int i = Arrays.binarySearch(dp, 0, len, num);
        if (i < 0) i = -(i + 1);
        dp[i] = num;
        if (i == len) len++;
    }
    return len;
}
```

## 4. Word Break (LC 139) ⭐⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/word-break/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/word-break/)

```java
public boolean wordBreak(String s, List<String> wordDict) {
    Set<String> set = new HashSet<>(wordDict);
    boolean[] dp = new boolean[s.length() + 1];
    dp[0] = true;
    for (int i = 1; i <= s.length(); i++)
        for (int j = 0; j < i; j++)
            if (dp[j] && set.contains(s.substring(j, i))) { dp[i] = true; break; }
    return dp[s.length()];
}
```

## 5. House Robber (LC 198) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/house-robber/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/stickler-theif/)

```java
public int rob(int[] nums) {
    int prev1 = 0, prev2 = 0;
    for (int num : nums) { int tmp = prev1; prev1 = Math.max(prev2 + num, prev1); prev2 = tmp; }
    return prev1;
}
```

## 6. House Robber II (LC 213) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/house-robber-ii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/house-robber-ii/)

```java
public int rob(int[] nums) {
    if (nums.length == 1) return nums[0];
    return Math.max(robRange(nums, 0, nums.length - 2), robRange(nums, 1, nums.length - 1));
}
int robRange(int[] nums, int start, int end) {
    int prev1 = 0, prev2 = 0;
    for (int i = start; i <= end; i++) { int tmp = prev1; prev1 = Math.max(prev2 + nums[i], prev1); prev2 = tmp; }
    return prev1;
}
```

## 7. Unique Paths (LC 62) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/unique-paths/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/number-of-unique-paths/)

```java
public int uniquePaths(int m, int n) {
    int[] dp = new int[n];
    Arrays.fill(dp, 1);
    for (int i = 1; i < m; i++)
        for (int j = 1; j < n; j++)
            dp[j] += dp[j - 1];
    return dp[n - 1];
}
```

## 8. Longest Common Subsequence (LC 1143) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/longest-common-subsequence/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/longest-common-subsequence/)

```java
public int longestCommonSubsequence(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    int[][] dp = new int[m + 1][n + 1];
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++)
            dp[i][j] = s1.charAt(i-1) == s2.charAt(j-1) ? dp[i-1][j-1] + 1 : Math.max(dp[i-1][j], dp[i][j-1]);
    return dp[m][n];
}
```

## 9. Edit Distance (LC 72) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/edit-distance/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/edit-distance/)

```java
public int minDistance(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    int[][] dp = new int[m + 1][n + 1];
    for (int i = 0; i <= m; i++) dp[i][0] = i;
    for (int j = 0; j <= n; j++) dp[0][j] = j;
    for (int i = 1; i <= m; i++)
        for (int j = 1; j <= n; j++)
            dp[i][j] = s1.charAt(i-1) == s2.charAt(j-1) ? dp[i-1][j-1] : 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
    return dp[m][n];
}
```

## 10. Maximum Product Subarray (LC 152) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/maximum-product-subarray/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/maximum-product-subarray/)

```java
public int maxProduct(int[] nums) {
    int max = nums[0], min = nums[0], result = nums[0];
    for (int i = 1; i < nums.length; i++) {
        if (nums[i] < 0) { int tmp = max; max = min; min = tmp; }
        max = Math.max(nums[i], max * nums[i]);
        min = Math.min(nums[i], min * nums[i]);
        result = Math.max(result, max);
    }
    return result;
}
```

## 11. Partition Equal Subset Sum (LC 416) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/partition-equal-subset-sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/subset-sum-problem/)

```java
public boolean canPartition(int[] nums) {
    int sum = Arrays.stream(nums).sum();
    if (sum % 2 != 0) return false;
    int target = sum / 2;
    boolean[] dp = new boolean[target + 1];
    dp[0] = true;
    for (int num : nums)
        for (int j = target; j >= num; j--)
            dp[j] = dp[j] || dp[j - num];
    return dp[target];
}
```

## 12. Best Time to Buy and Sell Stock III (LC 123) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iii/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/buy-and-sell-a-share-at-most-twice/)

```java
public int maxProfit(int[] prices) {
    int buy1 = Integer.MIN_VALUE, sell1 = 0, buy2 = Integer.MIN_VALUE, sell2 = 0;
    for (int p : prices) {
        buy1 = Math.max(buy1, -p);
        sell1 = Math.max(sell1, buy1 + p);
        buy2 = Math.max(buy2, sell1 - p);
        sell2 = Math.max(sell2, buy2 + p);
    }
    return sell2;
}
```

## 13. Decode Ways (LC 91) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/decode-ways/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/total-decoding-messages/)

```java
public int numDecodings(String s) {
    if (s.charAt(0) == '0') return 0;
    int prev1 = 1, prev2 = 1;
    for (int i = 1; i < s.length(); i++) {
        int curr = 0;
        if (s.charAt(i) != '0') curr = prev1;
        int two = Integer.parseInt(s.substring(i - 1, i + 1));
        if (two >= 10 && two <= 26) curr += prev2;
        prev2 = prev1; prev1 = curr;
    }
    return prev1;
}
```

## 14. Longest Palindromic Subsequence (LC 516) ⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/longest-palindromic-subsequence/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/longest-palindromic-subsequence/)

```java
public int longestPalindromeSubseq(String s) {
    int n = s.length();
    int[][] dp = new int[n][n];
    for (int i = n - 1; i >= 0; i--) {
        dp[i][i] = 1;
        for (int j = i + 1; j < n; j++)
            dp[i][j] = s.charAt(i) == s.charAt(j) ? dp[i+1][j-1] + 2 : Math.max(dp[i+1][j], dp[i][j-1]);
    }
    return dp[0][n - 1];
}
```

## 15. Target Sum (LC 494) ⭐⭐⭐⭐

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/target-sum/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/target-sum/)

```java
public int findTargetSumWays(int[] nums, int target) {
    int sum = Arrays.stream(nums).sum();
    if (sum < Math.abs(target) || (sum + target) % 2 != 0) return 0;
    int subsetSum = (sum + target) / 2;
    int[] dp = new int[subsetSum + 1];
    dp[0] = 1;
    for (int num : nums)
        for (int j = subsetSum; j >= num; j--)
            dp[j] += dp[j - num];
    return dp[subsetSum];
}
```

## Pattern Summary
- **1D DP**: Climbing Stairs, House Robber, Decode Ways
- **2D DP**: LCS, Edit Distance, Unique Paths
- **Knapsack**: Coin Change, Partition Sum, Target Sum
- **LIS**: Binary search optimization O(n log n)
- **Stock**: State machine (buy/sell/cooldown)

**Next**: [Study Plans](Study_Plans.md)
