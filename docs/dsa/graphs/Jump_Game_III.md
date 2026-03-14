# Jump Game III
**LeetCode 1306** | Medium | BFS / DFS

## Problem
Given an array `arr` and a start index, you can jump from index `i` to `i + arr[i]` or `i - arr[i]`.
Return `true` if you can reach any index with value `0`.

```
Input: arr = [4,2,3,0,3,1,2], start = 5
Output: true  →  5→4→1→3 (arr[3]=0)

Input: arr = [4,2,3,0,3,1,2], start = 0
Output: true  →  0→4→1→3

Input: arr = [3,0,2,1,2], start = 2
Output: false
```

## Approach 1: BFS (Optimal)
**Time:** O(n) | **Space:** O(n)

```java
public boolean canReach(int[] arr, int start) {
    int n = arr.length;
    boolean[] visited = new boolean[n];
    Queue<Integer> q = new LinkedList<>();
    q.offer(start); visited[start] = true;

    while (!q.isEmpty()) {
        int i = q.poll();
        if (arr[i] == 0) return true;
        int[] next = {i + arr[i], i - arr[i]};
        for (int j : next)
            if (j >= 0 && j < n && !visited[j]) { visited[j] = true; q.offer(j); }
    }
    return false;
}
```

## Approach 2: DFS
**Time:** O(n) | **Space:** O(n)

```java
public boolean canReach(int[] arr, int start) {
    return dfs(arr, new boolean[arr.length], start);
}

private boolean dfs(int[] arr, boolean[] visited, int i) {
    if (i < 0 || i >= arr.length || visited[i]) return false;
    if (arr[i] == 0) return true;
    visited[i] = true;
    return dfs(arr, visited, i + arr[i]) || dfs(arr, visited, i - arr[i]);
}
```

## Approach 3: In-place Marking (No Extra Space)
Mark visited by negating values. Restore if needed (here we don't need to).

**Time:** O(n) | **Space:** O(n) stack for DFS

```java
public boolean canReach(int[] arr, int start) {
    if (start < 0 || start >= arr.length || arr[start] < 0) return false;
    if (arr[start] == 0) return true;
    arr[start] = -arr[start]; // mark visited
    return canReach(arr, start + arr[start]) || canReach(arr, start - arr[start]);
}
```

## Key Insight
Simple graph reachability: each index is a node with at most 2 outgoing edges (`i±arr[i]`).
BFS/DFS from `start`, check if any node with value 0 is reachable.
The visited array prevents infinite loops in cyclic paths.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `arr[start] == 0` | true | Already at a zero |
| No zero in array | false | Impossible to reach |
| Zero unreachable from start | false | Graph is disconnected |
| `arr = [0]` | true | Single element is zero |
| Cycle with no zero | false | BFS/DFS detects cycle via visited |
| `start` out of bounds | false | Invalid start |
| `arr = [1,0]`, `start=0` | true | 0→1 (arr[1]=0) |

---

## Dry Run

**Input:** `arr = [4,2,3,0,3,1,2], start = 5`

**BFS trace:**
```
n=7, visited=[F,F,F,F,F,F,F]
Queue: [5], visited[5]=true

--- Step 1 ---
Pop 5: arr[5]=1 ≠ 0
  Right: 5+1=6, not visited → enqueue, visited[6]=true
  Left:  5-1=4, not visited → enqueue, visited[4]=true
Queue: [6,4]

--- Step 2 ---
Pop 6: arr[6]=2 ≠ 0
  Right: 6+2=8 ≥ 7 → out of bounds, skip
  Left:  6-2=4, already visited, skip
Pop 4: arr[4]=3 ≠ 0
  Right: 4+3=7 ≥ 7 → out of bounds, skip
  Left:  4-3=1, not visited → enqueue, visited[1]=true
Queue: [1]

--- Step 3 ---
Pop 1: arr[1]=2 ≠ 0
  Right: 1+2=3, not visited → enqueue, visited[3]=true
  Left:  1-2=-1 < 0 → out of bounds, skip
Queue: [3]

--- Step 4 ---
Pop 3: arr[3]=0 → return true!

Answer: true  (path: 5→4→1→3)
```

---

## Follow-up Questions

**Q: What if you need to find the minimum number of jumps to reach a zero?**
Count BFS levels — each level = one jump. Return the level when a zero is first found.

**Q: What if `arr[i]` can be 0 at multiple indices?**
Return true as soon as any zero is reached — the current code already handles this.

**Q: Can the in-place negation approach (Approach 3) cause issues?**
Yes — it modifies the input array. If the caller needs the original array, use a separate visited array. Also, `arr[start] = -arr[start]` then `start + arr[start]` uses the negated value, which is correct since `-(-x) = x`.

**Q: What’s the maximum recursion depth for DFS?**
O(n) — each index visited at most once. For n=5×10⁴, use iterative BFS to avoid stack overflow.

**Related Problems:** LC 45 (Jump Game II), LC 1345 (Jump Game IV), LC 1696 (Jump Game VI)
