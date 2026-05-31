# Longest Common Prefix

## Problem 1: Longest Common Prefix of Strings

Given an array of strings, find the longest common prefix.

**Example:** `["flower", "flow", "flight"]` → `"fl"`

```java
String longestCommonPrefix(String[] strs) {
    if (strs == null || strs.length == 0) return "";
    String prefix = strs[0];
    for (int i = 1; i < strs.length; i++) {
        while (!strs[i].startsWith(prefix)) {
            prefix = prefix.substring(0, prefix.length() - 1);
            if (prefix.isEmpty()) return "";
        }
    }
    return prefix;
}
```

**Time:** O(S) where S = sum of all characters  
**Space:** O(1)

---

## Problem 2: Longest Common Prefix of Integers

Given an array of integers, find the longest common prefix (as digits from left).

**Example:** `[12345, 12378, 123]` → `123`  
**Example:** `[1, 23, 456]` → `0` (no common prefix)

```java
int longestCommonPrefix(int[] nums) {
    if (nums == null || nums.length == 0) return 0;
    String prefix = String.valueOf(nums[0]);
    for (int i = 1; i < nums.length; i++) {
        String s = String.valueOf(nums[i]);
        while (!s.startsWith(prefix)) {
            prefix = prefix.substring(0, prefix.length() - 1);
            if (prefix.isEmpty()) return 0;
        }
    }
    return prefix.isEmpty() ? 0 : Integer.parseInt(prefix);
}
```

### Without String Conversion

```java
int longestCommonPrefix(int[] nums) {
    if (nums == null || nums.length == 0) return 0;
    int prefix = nums[0];
    for (int i = 1; i < nums.length; i++) {
        while (prefix != 0) {
            int num = nums[i];
            // make num same number of digits as prefix
            while (numDigits(num) > numDigits(prefix))
                num /= 10;
            if (num == prefix) break;
            prefix /= 10; // shrink prefix
        }
        if (prefix == 0) return 0;
    }
    return prefix;
}

int numDigits(int n) {
    if (n == 0) return 1;
    int count = 0;
    while (n > 0) { n /= 10; count++; }
    return count;
}
```

---

## Trace: Integer Version (No String)

**Input:** `[12345, 12378, 123]`, start with `prefix = 12345`

**Iteration 1:** `nums[1] = 12378`

| prefix | num (trimmed) | Match? |
|--------|---------------|--------|
| 12345 | 12378 | No |
| 1234 | 1237 | No |
| 123 | 123 | ✅ |

`prefix = 123`

**Iteration 2:** `nums[2] = 123`

| prefix | num (trimmed) | Match? |
|--------|---------------|--------|
| 123 | 123 | ✅ |

`prefix = 123`

**Result:** `123`

---

## Complexity

| Approach | Time | Space |
|----------|------|-------|
| String conversion | O(D × n) | O(D) for string |
| Pure integer | O(D × n) | O(1) |

Where D = max digits in any number, n = array length.

---

## Problem 3: Longest Common Prefix Length from Two Arrays

Given two integer arrays `arr1` and `arr2`, find the length of the longest common prefix among all pairs `(arr1[i], arr2[j])`.

**Example:** `arr1 = [1, 10, 100]`, `arr2 = [1000]` → `3` (pair `100` and `1000` share prefix `100`)  
**Example:** `arr1 = [1, 2, 3]`, `arr2 = [4, 4, 4]` → `0` (no common prefix)

---

### Brute Force: O(n × m × D)

Check every pair — too slow for large inputs.

---

### Optimal: Trie / HashSet of Prefixes — O((n + m) × D)

**Key Insight:** Instead of comparing all pairs, store all prefixes of one array in a HashSet, then check prefixes of the other array against it.

```java
int longestCommonPrefix(int[] arr1, int[] arr2) {
    Set<Integer> prefixes = new HashSet<>();

    // Store all prefixes of arr1
    for (int num : arr1) {
        while (num > 0) {
            prefixes.add(num);
            num /= 10;
        }
    }

    // Check prefixes of arr2 against the set
    int maxLen = 0;
    for (int num : arr2) {
        while (num > 0) {
            if (prefixes.contains(num)) {
                maxLen = Math.max(maxLen, numDigits(num));
                break; // longest prefix of this number found, no need to shrink further
            }
            num /= 10;
        }
    }
    return maxLen;
}

int numDigits(int n) {
    if (n == 0) return 1;
    int count = 0;
    while (n > 0) { n /= 10; count++; }
    return count;
}
```

---

### How It Works

1. For each number in `arr1`, insert ALL its prefixes into a HashSet
   - `12345` → inserts `12345, 1234, 123, 12, 1`
2. For each number in `arr2`, check from its full value downward
   - First match found is the longest possible prefix for that number
3. Track the maximum length across all matches

---

### Trace

**Input:** `arr1 = [1, 10, 100]`, `arr2 = [1000]`

**Step 1: Build prefix set from arr1**

| Number | Prefixes inserted |
|--------|-------------------|
| 1 | `{1}` |
| 10 | `{1, 10}` |
| 100 | `{1, 10, 100}` |

`prefixes = {1, 10, 100}`

**Step 2: Check arr2 against set**

| Number | Check | In set? | Action |
|--------|-------|---------|--------|
| 1000 | 1000 | No | shrink |
| 1000 | 100 | ✅ | length = 3, break |

**Result:** `3`

---

### Why We Break Early

When checking `arr2[j]`, we start from the full number and shrink. The first match is already the longest prefix for that pair — no need to check shorter prefixes.

---

### Complexity

| | Time | Space |
|--|------|-------|
| Build set | O(n × D) | O(n × D) |
| Query | O(m × D) | — |
| **Total** | **O((n + m) × D)** | **O(n × D)** |

Where n = `arr1.length`, m = `arr2.length`, D = max digits (~10 for int).

Compared to brute force O(n × m × D), this is significantly faster for large arrays.
