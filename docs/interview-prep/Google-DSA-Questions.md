# Google SDE Interview — Most Asked Real-Life Problem-Solving DSA Questions

Based on patterns from Leetcode tagged questions, Glassdoor reports, Blind posts, and interview experiences (2022-2025).

---

## 📊 Topic Distribution (Google Interviews)

| Topic | Frequency | Difficulty Mix |
|-------|-----------|---------------|
| Arrays / Strings | 25% | Medium-Hard |
| Graphs (BFS/DFS) | 20% | Medium-Hard |
| Dynamic Programming | 15% | Medium-Hard |
| Trees / Binary Trees | 12% | Medium |
| Sliding Window / Two Pointers | 10% | Medium |
| Design / OOP | 8% | Medium-Hard |
| Greedy / Intervals | 5% | Medium |
| Backtracking / Recursion | 5% | Hard |

---

## 🔥 Top 50 Most Asked Questions (Categorized)

---

### Category 1: Arrays & Strings

#### 1. Median of Two Sorted Arrays
**Real-life context**: Merging search result rankings from two different indexes.

```
Given two sorted arrays nums1 and nums2, find the median of the combined sorted array.
Must be O(log(m+n)).

Input: nums1 = [1,3], nums2 = [2]
Output: 2.0
```
**Pattern**: Binary search on partition  
**Difficulty**: Hard  
**LC**: [#4](https://leetcode.com/problems/median-of-two-sorted-arrays/)

---

#### 2. Longest Substring Without Repeating Characters
**Real-life context**: Finding the longest unique session in user activity logs.

```
Given a string, find the length of the longest substring without repeating characters.

Input: "abcabcbb"
Output: 3 ("abc")
```
**Pattern**: Sliding window + HashMap  
**Difficulty**: Medium  
**LC**: [#3](https://leetcode.com/problems/longest-substring-without-repeating-characters/)

---

#### 3. Next Permutation
**Real-life context**: Generating next lexicographic configuration in A/B testing.

```
Rearrange numbers into the lexicographically next greater permutation.
If impossible, rearrange to lowest order (sorted ascending).

Input: [1,2,3] → Output: [1,3,2]
Input: [3,2,1] → Output: [1,2,3]
```
**Pattern**: Two-pointer + reverse  
**Difficulty**: Medium  
**LC**: [#31](https://leetcode.com/problems/next-permutation/)

---

#### 4. Trapping Rain Water
**Real-life context**: Computing storage capacity between data center racks, or bandwidth allocation between network peaks.

```
Given n non-negative integers representing elevation map bars of width 1,
compute how much water can be trapped after raining.

Input: [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6
```
**Pattern**: Two pointers / Monotonic stack  
**Difficulty**: Hard  
**LC**: [#42](https://leetcode.com/problems/trapping-rain-water/)

---

#### 5. Minimum Window Substring
**Real-life context**: Finding the shortest log entry containing all required error codes.

```
Given strings s and t, find the minimum window in s that contains all characters of t.

Input: s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"
```
**Pattern**: Sliding window + frequency map  
**Difficulty**: Hard  
**LC**: [#76](https://leetcode.com/problems/minimum-window-substring/)

---

#### 6. Product of Array Except Self
**Real-life context**: Computing impact scores where each element's contribution is product of all others (ad revenue attribution).

```
Return array where output[i] = product of all elements except nums[i].
Must be O(n) without division.

Input: [1,2,3,4]
Output: [24,12,8,6]
```
**Pattern**: Prefix/suffix products  
**Difficulty**: Medium  
**LC**: [#238](https://leetcode.com/problems/product-of-array-except-self/)

---

#### 7. Container With Most Water
**Real-life context**: Maximizing bandwidth between two servers given height constraints.

```
Find two lines that together with x-axis form a container holding most water.

Input: [1,8,6,2,5,4,8,3,7]
Output: 49
```
**Pattern**: Two pointers (greedy shrink)  
**Difficulty**: Medium  
**LC**: [#11](https://leetcode.com/problems/container-with-most-water/)

---

#### 8. Text Justification
**Real-life context**: Google Docs text rendering engine.

```
Format text so each line has exactly maxWidth characters, fully justified.
Last line is left-justified.

Input: words = ["This","is","an","example"], maxWidth = 16
Output: ["This    is    an", "example         "]
```
**Pattern**: Greedy simulation  
**Difficulty**: Hard  
**LC**: [#68](https://leetcode.com/problems/text-justification/)

---

### Category 2: Graphs

#### 9. Word Ladder
**Real-life context**: Finding shortest transformation path between API versions or migration steps.

```
Find shortest transformation sequence from beginWord to endWord,
changing one letter at a time, using words from wordList.

Input: beginWord="hit", endWord="cog", wordList=["hot","dot","dog","lot","log","cog"]
Output: 5 (hit → hot → dot → dog → cog)
```
**Pattern**: BFS + adjacency via character mutation  
**Difficulty**: Hard  
**LC**: [#127](https://leetcode.com/problems/word-ladder/)

---

#### 10. Number of Islands
**Real-life context**: Counting connected clusters in a distributed system network map.

```
Given a 2D grid of '1's (land) and '0's (water), count number of islands.

Input: grid = [
  ["1","1","0","0"],
  ["1","1","0","0"],
  ["0","0","1","0"],
  ["0","0","0","1"]
]
Output: 3
```
**Pattern**: BFS/DFS flood fill  
**Difficulty**: Medium  
**LC**: [#200](https://leetcode.com/problems/number-of-islands/)

---

#### 11. Course Schedule (Topological Sort)
**Real-life context**: Build dependency resolution (Bazel/Gradle), deployment ordering.

```
There are n courses. Some have prerequisites. Determine if all courses can be finished.
Return a valid ordering if possible.

Input: numCourses=4, prerequisites=[[1,0],[2,0],[3,1],[3,2]]
Output: [0,1,2,3] or [0,2,1,3]
```
**Pattern**: Topological sort (Kahn's BFS or DFS)  
**Difficulty**: Medium  
**LC**: [#207](https://leetcode.com/problems/course-schedule/), [#210](https://leetcode.com/problems/course-schedule-ii/)

---

#### 12. Shortest Path in a Grid with Obstacles Elimination
**Real-life context**: Network packet routing with limited retries through faulty nodes.

```
Given m×n grid with obstacles, find shortest path from (0,0) to (m-1,n-1).
You can eliminate at most k obstacles.

Input: grid = [[0,0,0],[1,1,0],[0,0,0],[0,1,1],[0,0,0]], k = 1
Output: 6
```
**Pattern**: BFS with state (row, col, remaining_eliminations)  
**Difficulty**: Hard  
**LC**: [#1293](https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/)

---

#### 13. Alien Dictionary
**Real-life context**: Inferring ordering rules from sorted data (locale sorting, custom sort).

```
Given a sorted dictionary of an alien language, derive the character ordering.

Input: ["wrt","wrf","er","ett","rftt"]
Output: "wertf"
```
**Pattern**: Topological sort from pairwise comparisons  
**Difficulty**: Hard  
**LC**: [#269](https://leetcode.com/problems/alien-dictionary/)

---

#### 14. Accounts Merge
**Real-life context**: Deduplicating user accounts that share common emails (Google identity resolution).

```
Given list of accounts (name + emails), merge accounts belonging to same person.

Input: [["John","john@gmail","john@work"],["John","john@gmail","john2@gmail"]]
Output: [["John","john2@gmail","john@gmail","john@work"]]
```
**Pattern**: Union-Find or DFS on email graph  
**Difficulty**: Medium  
**LC**: [#721](https://leetcode.com/problems/accounts-merge/)

---

#### 15. Minimum Knight Moves
**Real-life context**: Minimum steps for a robot/agent to reach a target on a grid.

```
In an infinite chess board, find minimum moves for knight to reach (x, y) from (0, 0).
```
**Pattern**: BFS with pruning (limit search space)  
**Difficulty**: Medium  
**LC**: [#1197](https://leetcode.com/problems/minimum-knight-moves/)

---

### Category 3: Dynamic Programming

#### 16. Longest Increasing Subsequence
**Real-life context**: Finding longest chain of increasing stock prices, version dependencies.

```
Find length of longest strictly increasing subsequence.

Input: [10,9,2,5,3,7,101,18]
Output: 4 ([2,3,7,101])
```
**Pattern**: DP + Binary Search (O(n log n))  
**Difficulty**: Medium  
**LC**: [#300](https://leetcode.com/problems/longest-increasing-subsequence/)

---

#### 17. Word Break
**Real-life context**: Google Search query segmentation, URL tokenization.

```
Given string s and dictionary, determine if s can be segmented into dictionary words.

Input: s = "leetcode", wordDict = ["leet","code"]
Output: true
```
**Pattern**: DP or BFS/DFS with memoization  
**Difficulty**: Medium  
**LC**: [#139](https://leetcode.com/problems/word-break/)

---

#### 18. Edit Distance
**Real-life context**: Spell check, diff computation, DNA sequence alignment.

```
Find minimum operations (insert, delete, replace) to convert word1 to word2.

Input: word1 = "horse", word2 = "ros"
Output: 3
```
**Pattern**: 2D DP  
**Difficulty**: Medium  
**LC**: [#72](https://leetcode.com/problems/edit-distance/)

---

#### 19. Maximum Profit in Job Scheduling
**Real-life context**: Scheduling ad campaigns, cloud VM allocation for max profit.

```
Given n jobs with startTime, endTime, profit, find max profit with no overlapping.

Input: startTime=[1,2,3,3], endTime=[3,4,5,6], profit=[50,10,40,70]
Output: 120 (job 1 + job 4)
```
**Pattern**: DP + Binary Search (sort by end time)  
**Difficulty**: Hard  
**LC**: [#1235](https://leetcode.com/problems/maximum-profit-in-job-scheduling/)

---

#### 20. Decode Ways
**Real-life context**: Parsing encoded messages (phone number to letters mapping).

```
A='1', B='2', ..., Z='26'. Count ways to decode a digit string.

Input: "226"
Output: 3 ("BZ", "VF", "BBF")
```
**Pattern**: 1D DP (like Fibonacci with constraints)  
**Difficulty**: Medium  
**LC**: [#91](https://leetcode.com/problems/decode-ways/)

---

#### 21. Burst Balloons
**Real-life context**: Optimal order to shut down dependent services to maximize cleanup value.

```
Given n balloons with numbers, burst them to maximize coins.
Coins for bursting balloon i = nums[left] * nums[i] * nums[right].

Input: [3,1,5,8]
Output: 167
```
**Pattern**: Interval DP  
**Difficulty**: Hard  
**LC**: [#312](https://leetcode.com/problems/burst-balloons/)

---

#### 22. Coin Change (Minimum Coins)
**Real-life context**: Minimum number of API calls to assemble required data, payment splitting.

```
Find fewest coins needed to make amount. Return -1 if impossible.

Input: coins = [1,5,10,25], amount = 30
Output: 2 (25+5)
```
**Pattern**: Unbounded knapsack DP  
**Difficulty**: Medium  
**LC**: [#322](https://leetcode.com/problems/coin-change/)

---

### Category 4: Trees

#### 23. Serialize and Deserialize Binary Tree
**Real-life context**: Storing/transmitting tree structures (DOM, file systems, org charts).

```
Design algorithm to serialize a binary tree to string and deserialize back.
```
**Pattern**: BFS (level-order) or DFS (preorder) with null markers  
**Difficulty**: Hard  
**LC**: [#297](https://leetcode.com/problems/serialize-and-deserialize-binary-tree/)

---

#### 24. Lowest Common Ancestor of a Binary Tree
**Real-life context**: Finding shared directory ancestor, common manager in org hierarchy.

```
Find LCA of two nodes p and q in a binary tree.
```
**Pattern**: Recursive DFS  
**Difficulty**: Medium  
**LC**: [#236](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/)

---

#### 25. Binary Tree Maximum Path Sum
**Real-life context**: Maximum total value route through a network of weighted nodes.

```
Find the maximum path sum (path doesn't need to pass through root).

Input: [-10,9,20,null,null,15,7]
Output: 42 (15 → 20 → 7)
```
**Pattern**: DFS post-order with global max  
**Difficulty**: Hard  
**LC**: [#124](https://leetcode.com/problems/binary-tree-maximum-path-sum/)

---

#### 26. Count Complete Tree Nodes
**Real-life context**: Efficient size calculation for heap-based priority queues.

```
Count nodes in a complete binary tree in O(log²n) time.
```
**Pattern**: Binary search on last level  
**Difficulty**: Medium  
**LC**: [#222](https://leetcode.com/problems/count-complete-tree-nodes/)

---

#### 27. Vertical Order Traversal
**Real-life context**: Rendering tree structures in columns (file explorer, org chart display).

```
Return vertical order traversal of binary tree nodes.
```
**Pattern**: BFS/DFS with column tracking + sorting  
**Difficulty**: Hard  
**LC**: [#987](https://leetcode.com/problems/vertical-order-traversal-of-a-binary-tree/)

---

### Category 5: Sliding Window / Two Pointers

#### 28. Sliding Window Maximum
**Real-life context**: Computing rolling max metric (peak traffic per time window in monitoring).

```
Given array and window size k, return max value in each window.

Input: nums = [1,3,-1,-3,5,3,6,7], k = 3
Output: [3,3,5,5,6,7]
```
**Pattern**: Monotonic deque  
**Difficulty**: Hard  
**LC**: [#239](https://leetcode.com/problems/sliding-window-maximum/)

---

#### 29. Longest Repeating Character Replacement
**Real-life context**: Finding longest stable signal with at most k corrections.

```
Find longest substring with same characters after at most k replacements.

Input: s = "AABABBA", k = 1
Output: 4
```
**Pattern**: Sliding window + frequency tracking  
**Difficulty**: Medium  
**LC**: [#424](https://leetcode.com/problems/longest-repeating-character-replacement/)

---

#### 30. Subarrays with K Different Integers
**Real-life context**: Finding user sessions with exactly K unique page types.

```
Return number of subarrays with exactly k distinct integers.

Input: nums = [1,2,1,2,3], k = 2
Output: 7
```
**Pattern**: Sliding window (atMost(k) - atMost(k-1))  
**Difficulty**: Hard  
**LC**: [#992](https://leetcode.com/problems/subarrays-with-k-different-integers/)

---

### Category 6: Design & Data Structures

#### 31. LRU Cache
**Real-life context**: In-memory cache eviction (Memcached, CDN cache).

```
Design a data structure with O(1) get and put with LRU eviction.
```
**Pattern**: HashMap + Doubly Linked List  
**Difficulty**: Medium  
**LC**: [#146](https://leetcode.com/problems/lru-cache/)

---

#### 32. Design Search Autocomplete System
**Real-life context**: Google Search suggestion dropdown.

```
Design autocomplete that returns top-3 hot sentences matching prefix.
Support new sentence recording.
```
**Pattern**: Trie + Priority Queue  
**Difficulty**: Hard  
**LC**: [#642](https://leetcode.com/problems/design-search-autocomplete-system/)

---

#### 33. Design Hit Counter
**Real-life context**: Rate limiting, metrics (requests per second tracking).

```
Design a hit counter that counts hits in the past 5 minutes.
Support hit(timestamp) and getHits(timestamp).
```
**Pattern**: Circular buffer or queue  
**Difficulty**: Medium  
**LC**: [#362](https://leetcode.com/problems/design-hit-counter/)

---

#### 34. Snapshot Array
**Real-life context**: Copy-on-write data structures, database MVCC.

```
Implement SnapshotArray with set(index, val), snap(), and get(index, snap_id).
```
**Pattern**: TreeMap/Binary Search per index  
**Difficulty**: Medium  
**LC**: [#1146](https://leetcode.com/problems/snapshot-array/)

---

#### 35. Time Based Key-Value Store
**Real-life context**: Versioned configuration store (get value at specific timestamp).

```
Design key-value store where each key can have multiple values with timestamps.
get(key, timestamp) returns value with largest timestamp <= given timestamp.
```
**Pattern**: HashMap + Binary Search on sorted timestamps  
**Difficulty**: Medium  
**LC**: [#981](https://leetcode.com/problems/time-based-key-value-store/)

---

### Category 7: Greedy / Intervals

#### 36. Merge Intervals
**Real-life context**: Calendar meeting consolidation, IP range merging, log aggregation.

```
Merge all overlapping intervals.

Input: [[1,3],[2,6],[8,10],[15,18]]
Output: [[1,6],[8,10],[15,18]]
```
**Pattern**: Sort + linear merge  
**Difficulty**: Medium  
**LC**: [#56](https://leetcode.com/problems/merge-intervals/)

---

#### 37. Meeting Rooms II (Minimum Conference Rooms)
**Real-life context**: VM scheduling, minimum parallel workers needed.

```
Find minimum number of conference rooms required for all meetings.

Input: [[0,30],[5,10],[15,20]]
Output: 2
```
**Pattern**: Sort starts/ends separately or min-heap  
**Difficulty**: Medium  
**LC**: [#253](https://leetcode.com/problems/meeting-rooms-ii/)

---

#### 38. Task Scheduler
**Real-life context**: CPU task scheduling with cooldown, rate-limited API call scheduling.

```
Given tasks and cooldown n, find minimum intervals to execute all tasks.

Input: tasks = ["A","A","A","B","B","B"], n = 2
Output: 8 (A→B→idle→A→B→idle→A→B)
```
**Pattern**: Greedy (most frequent first) or math formula  
**Difficulty**: Medium  
**LC**: [#621](https://leetcode.com/problems/task-scheduler/)

---

#### 39. Non-overlapping Intervals
**Real-life context**: Maximum number of non-conflicting bookings/events.

```
Find minimum number of intervals to remove to make the rest non-overlapping.

Input: [[1,2],[2,3],[3,4],[1,3]]
Output: 1
```
**Pattern**: Greedy (sort by end time, count overlaps)  
**Difficulty**: Medium  
**LC**: [#435](https://leetcode.com/problems/non-overlapping-intervals/)

---

### Category 8: Backtracking / Recursion

#### 40. Word Search II
**Real-life context**: Finding multiple keywords in a document grid (search engine indexing).

```
Given m×n board and list of words, find all words present on the board.
Adjacent cells (horizontal/vertical) only.

Input: board, words = ["oath","pea","eat","rain"]
Output: ["eat","oath"]
```
**Pattern**: Trie + DFS backtracking  
**Difficulty**: Hard  
**LC**: [#212](https://leetcode.com/problems/word-search-ii/)

---

#### 41. Generate Parentheses
**Real-life context**: Generating all valid configurations (code templates, expression trees).

```
Generate all combinations of n pairs of well-formed parentheses.

Input: n = 3
Output: ["((()))","(()())","(())()","()(())","()()()"]
```
**Pattern**: Backtracking with open/close count  
**Difficulty**: Medium  
**LC**: [#22](https://leetcode.com/problems/generate-parentheses/)

---

#### 42. N-Queens
**Real-life context**: Constraint satisfaction (resource allocation with mutual exclusion).

```
Place n queens on n×n chessboard so no two attack each other.
Return all solutions.
```
**Pattern**: Backtracking with column/diagonal tracking  
**Difficulty**: Hard  
**LC**: [#51](https://leetcode.com/problems/n-queens/)

---

### Category 9: Stack / Queue / Heap

#### 43. Basic Calculator (I, II, III)
**Real-life context**: Expression evaluation in spreadsheets, query parsers.

```
Implement a calculator that evaluates expressions with +, -, *, / and parentheses.

Input: "3+2*2"
Output: 7
```
**Pattern**: Stack with operator precedence  
**Difficulty**: Hard  
**LC**: [#224](https://leetcode.com/problems/basic-calculator/), [#227](https://leetcode.com/problems/basic-calculator-ii/), [#772](https://leetcode.com/problems/basic-calculator-iii/)

---

#### 44. Find Median from Data Stream
**Real-life context**: Rolling median for monitoring dashboards (P50 latency).

```
Design data structure that supports addNum(num) and findMedian() efficiently.
```
**Pattern**: Two heaps (max-heap + min-heap)  
**Difficulty**: Hard  
**LC**: [#295](https://leetcode.com/problems/find-median-from-data-stream/)

---

#### 45. K Closest Points to Origin
**Real-life context**: Finding nearest servers/users to a location (geo-spatial queries).

```
Find k closest points to origin (0,0).

Input: points = [[3,3],[5,-1],[-2,4]], k = 2
Output: [[3,3],[-2,4]]
```
**Pattern**: Max-heap of size k or Quickselect  
**Difficulty**: Medium  
**LC**: [#973](https://leetcode.com/problems/k-closest-points-to-origin/)

---

### Category 10: Binary Search

#### 46. Search in Rotated Sorted Array
**Real-life context**: Searching in circular buffers, ring-based data structures.

```
Search target in rotated sorted array in O(log n).

Input: nums = [4,5,6,7,0,1,2], target = 0
Output: 4
```
**Pattern**: Modified binary search  
**Difficulty**: Medium  
**LC**: [#33](https://leetcode.com/problems/search-in-rotated-sorted-array/)

---

#### 47. Koko Eating Bananas
**Real-life context**: Finding minimum processing speed to complete all tasks within deadline.

```
Koko has piles of bananas, h hours to eat all. Find minimum eating speed.

Input: piles = [3,6,7,11], h = 8
Output: 4
```
**Pattern**: Binary search on answer  
**Difficulty**: Medium  
**LC**: [#875](https://leetcode.com/problems/koko-eating-bananas/)

---

#### 48. Split Array Largest Sum
**Real-life context**: Load balancing — split work into k workers minimizing max workload.

```
Split array into k subarrays minimizing the largest sum among them.

Input: nums = [7,2,5,10,8], k = 2
Output: 18 ([7,2,5] and [10,8])
```
**Pattern**: Binary search on answer + greedy validation  
**Difficulty**: Hard  
**LC**: [#410](https://leetcode.com/problems/split-array-largest-sum/)

---

### Category 11: Trie / String Algorithms

#### 49. Implement Trie (Prefix Tree)
**Real-life context**: Autocomplete, spell checker, IP routing tables.

```
Implement Trie with insert, search, and startsWith operations.
```
**Pattern**: Tree with character-keyed children  
**Difficulty**: Medium  
**LC**: [#208](https://leetcode.com/problems/implement-trie-prefix-tree/)

---

#### 50. Palindrome Pairs
**Real-life context**: Finding complementary strings (key combinations, reversible operations).

```
Given list of unique words, find all pairs (i,j) where words[i] + words[j] is a palindrome.

Input: ["abcd","dcba","lls","s","sssll"]
Output: [[0,1],[1,0],[3,2],[2,4]]
```
**Pattern**: Trie or HashMap with palindrome suffix checks  
**Difficulty**: Hard  
**LC**: [#336](https://leetcode.com/problems/palindrome-pairs/)

---

## 🎯 Google-Specific Patterns to Master

| Pattern | Why Google Loves It | Example Problems |
|---------|--------------------|--------------------|
| **BFS on state space** | Models real distributed systems | Knight moves, word ladder, grid with obstacles |
| **Binary search on answer** | Optimizing thresholds at scale | Koko bananas, split array, capacity shipping |
| **Topological sort** | Dependency resolution (builds, deploys) | Course schedule, alien dictionary |
| **Union-Find** | Identity resolution, clustering | Accounts merge, redundant connection |
| **Monotonic stack/deque** | Sliding window optimizations | Sliding window max, daily temperatures |
| **Interval scheduling** | Resource allocation | Meeting rooms, task scheduler |
| **Trie** | Prefix matching (search, autocomplete) | Word search II, autocomplete |
| **DP on intervals** | Optimal partitioning | Burst balloons, matrix chain |

---

## 📝 Interview Tips (Google-Specific)

1. **Clarify constraints** — Always ask about input size, duplicates, edge cases
2. **Start with brute force** — Then optimize. Google values the optimization journey
3. **Talk through trade-offs** — Time vs space, multiple approaches
4. **Real-world connection** — Mention how the problem relates to real systems
5. **Test your solution** — Walk through examples, edge cases (empty, single element, max size)
6. **Code quality matters** — Clean variable names, modular functions, no dead code
7. **Complexity analysis** — State time and space complexity clearly
8. **Follow-up readiness** — Google often asks "what if the input is too large for memory?" or "what if this needs to work in a distributed system?"
