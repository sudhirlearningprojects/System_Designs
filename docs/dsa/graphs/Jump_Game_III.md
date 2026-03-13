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
