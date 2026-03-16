# Amazon Interview - Dynamic Programming Problems

## Problem 16: Climbing Stairs (LeetCode 70) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High  
**Pattern**: DP - Fibonacci

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/climbing-stairs/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/count-ways-to-reach-the-nth-stair/)

### Solution
```java
public int climbStairs(int n) {
    if (n <= 2) return n;
    
    int prev2 = 1, prev1 = 2;
    for (int i = 3; i <= n; i++) {
        int current = prev1 + prev2;
        prev2 = prev1;
        prev1 = current;
    }
    return prev1;
}
```
**Time**: O(n), **Space**: O(1)

---

## Problem 17: Coin Change (LeetCode 322) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: DP - Unbounded Knapsack

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/coin-change/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/coin-change/)

### Solution
```java
public int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, amount + 1);
    dp[0] = 0;
    
    for (int i = 1; i <= amount; i++) {
        for (int coin : coins) {
            if (i >= coin) {
                dp[i] = Math.min(dp[i], dp[i - coin] + 1);
            }
        }
    }
    
    return dp[amount] > amount ? -1 : dp[amount];
}
```
**Time**: O(n * amount), **Space**: O(amount)

---

## Problem 18: Word Break (LeetCode 139) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: DP

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/word-break/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/word-break/)

### Solution
```java
public boolean wordBreak(String s, List<String> wordDict) {
    Set<String> wordSet = new HashSet<>(wordDict);
    boolean[] dp = new boolean[s.length() + 1];
    dp[0] = true;
    
    for (int i = 1; i <= s.length(); i++) {
        for (int j = 0; j < i; j++) {
            if (dp[j] && wordSet.contains(s.substring(j, i))) {
                dp[i] = true;
                break;
            }
        }
    }
    
    return dp[s.length()];
}
```
**Time**: O(n³), **Space**: O(n)

---

## Problem 19: Longest Palindromic Substring (LeetCode 5) ⭐⭐⭐⭐

**Difficulty**: Medium  
**Frequency**: High  
**Pattern**: DP / Expand Around Center

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/longest-palindromic-substring/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/longest-palindrome-in-a-string/)

### Solution
```java
public String longestPalindrome(String s) {
    if (s == null || s.length() < 1) return "";
    
    int start = 0, end = 0;
    for (int i = 0; i < s.length(); i++) {
        int len1 = expandAroundCenter(s, i, i);
        int len2 = expandAroundCenter(s, i, i + 1);
        int len = Math.max(len1, len2);
        
        if (len > end - start) {
            start = i - (len - 1) / 2;
            end = i + len / 2;
        }
    }
    
    return s.substring(start, end + 1);
}

private int expandAroundCenter(String s, int left, int right) {
    while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
        left--;
        right++;
    }
    return right - left - 1;
}
```
**Time**: O(n²), **Space**: O(1)

---

## Problem 20: Best Time to Buy and Sell Stock (LeetCode 121) ⭐⭐⭐⭐

**Difficulty**: Easy  
**Frequency**: High  
**Pattern**: DP / Greedy

**Practice Links**:
- 🔗 [LeetCode](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/)
- 🔗 [GeeksforGeeks](https://www.geeksforgeeks.org/problems/stock-buy-and-sell/)

### Solution
```java
public int maxProfit(int[] prices) {
    int minPrice = Integer.MAX_VALUE;
    int maxProfit = 0;
    
    for (int price : prices) {
        if (price < minPrice) {
            minPrice = price;
        } else {
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
    }
    
    return maxProfit;
}
```
**Time**: O(n), **Space**: O(1)

---

**Next**: [Linked Lists Problems](04_Linked_Lists.md)
