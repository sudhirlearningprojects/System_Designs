# Limit Occurrences in Sorted Array

## Problem Statement

Given a **sorted** array of integers and a limit `N`, remove elements that appear more than `N` times. Return the new array.

**Examples:**
```
Input:  arr = [1, 1, 1, 2, 2, 2, 3, 3], limit = 2
Output: [1, 1, 2, 2, 3, 3]

Input:  arr = [1, 1, 1, 1], limit = 1
Output: [1]

Input:  arr = [1, 2, 3, 4, 5], limit = 1
Output: [1, 2, 3, 4, 5]  (no duplicates, unchanged)
```

---

## Solution: Two-Pointer In-Place

```java
public int[] limitOccurrences(int[] arr, int limit) {
    int write = 0;

    for (int read = 0; read < arr.length; read++) {
        if (write < limit || arr[read] != arr[write - limit]) {
            arr[write++] = arr[read];
        }
    }

    return Arrays.copyOf(arr, write);
}
```

---

## How It Works

Since the array is sorted, all duplicates are adjacent. We use two pointers:
- `read` — scans every element
- `write` — tracks where the next valid element should go

**The condition has two parts:**

1. **`write < limit`** — We haven't written `limit` elements yet, so always accept (avoids negative index).
2. **`arr[read] != arr[write - limit]`** — The current element differs from the one written `limit` positions back. If they're equal, we've already placed `limit` copies → skip.

**Why look back `limit` positions?**
If `arr[write - limit]` equals `arr[read]`, then positions `write-limit`, `write-limit+1`, ..., `write-1` are ALL the same value (because the array is sorted and we only write valid elements in order). That's already `limit` copies written.

---

## Trace: `arr = [1, 1, 1, 2, 2, 2, 3, 3]`, `limit = 2`

| read | arr[read] | write | Check | Action | Result so far |
|------|-----------|-------|-------|--------|---------------|
| 0 | 1 | 0 | write < 2 → true | write | [1] |
| 1 | 1 | 1 | write < 2 → true | write | [1, 1] |
| 2 | 1 | 2 | arr[2] == arr[0]? → 1 == 1 → yes | **skip** | [1, 1] |
| 3 | 2 | 2 | arr[3] == arr[0]? → 2 == 1 → no | write | [1, 1, 2] |
| 4 | 2 | 3 | arr[4] == arr[1]? → 2 == 1 → no | write | [1, 1, 2, 2] |
| 5 | 2 | 4 | arr[5] == arr[2]? → 2 == 2 → yes | **skip** | [1, 1, 2, 2] |
| 6 | 3 | 4 | arr[6] == arr[2]? → 3 == 2 → no | write | [1, 1, 2, 2, 3] |
| 7 | 3 | 5 | arr[7] == arr[3]? → 3 == 2 → no | write | [1, 1, 2, 2, 3, 3] |

**Output:** `[1, 1, 2, 2, 3, 3]` ✓

---

## Why This Works (Intuition)

Think of `arr[write - limit]` as a "window" of the last `limit` elements written:

```
limit = 2, after writing [1, 1, 2, 2]:
                              ↑     ↑
                        write-2   write-1
                        
These last 2 written elements form the window.
If arr[read] == arr[write-2], it means the window is full of this value.
```

---

## Special Case: limit = 1 (Remove All Duplicates)

```
Condition becomes: arr[read] != arr[write - 1]
→ Only write if current differs from the last written element.
→ Classic "remove duplicates from sorted array" problem.
```

## Special Case: limit = 2 (LeetCode 80)

This is exactly [LeetCode 80 - Remove Duplicates from Sorted Array II](https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/).

---

## Complexity

| Metric | Value |
|--------|-------|
| Time | O(n) — single pass |
| Space | O(1) in-place (O(n) for returned copy) |

---

## Comparison with HashMap Approach

| Aspect | Two-Pointer | HashMap |
|--------|-------------|---------|
| Requires sorted input | Yes | No |
| Space | O(1) | O(k) where k = unique elements |
| Speed | Faster (no hashing) | Slower (hash overhead) |
| In-place | Yes | No |

**Use two-pointer when input is sorted. Use HashMap when unsorted.**
