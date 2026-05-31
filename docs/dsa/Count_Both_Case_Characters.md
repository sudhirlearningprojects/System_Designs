# Count Characters Present in Both Lower and Upper Case

## Problem
Given a string, find the count of alphabets which are present in both lowercase and uppercase form.

**Example:** `"aAbBcD"` → Answer: `2` (a and b appear in both cases)

---

## Solution: Bitmask Approach (Optimal)

```java
int countBothCases(String s) {
    int lower = 0, upper = 0;
    for (char c : s.toCharArray()) {
        if (c >= 'a' && c <= 'z') lower |= 1 << (c - 'a');
        else if (c >= 'A' && c <= 'Z') upper |= 1 << (c - 'A');
    }
    return Integer.bitCount(lower & upper);
}
```

### How It Works

Each bit in an `int` represents a letter (bit 0 = 'a', bit 1 = 'b', ..., bit 25 = 'z').

**Trace with `"aAbBcD"`:**

| Char | Condition | Expression | Result |
|------|-----------|-----------|--------|
| `'a'` | lowercase | `c - 'a' = 0`, so `1 << 0 = 1` | `lower = 000...001` |
| `'A'` | uppercase | `c - 'A' = 0`, so `1 << 0 = 1` | `upper = 000...001` |
| `'b'` | lowercase | `c - 'a' = 1`, so `1 << 1 = 2` | `lower = 000...011` |
| `'B'` | uppercase | `c - 'A' = 1`, so `1 << 1 = 2` | `upper = 000...011` |
| `'c'` | lowercase | `c - 'a' = 2`, so `1 << 2 = 4` | `lower = 000...111` |
| `'D'` | uppercase | `c - 'A' = 3`, so `1 << 3 = 8` | `upper = 000...1011` |

**After loop:**
```
lower & upper = 000...0011  → bitCount = 2 (a, b are common)
```

### Key Operations
- `c - 'a'` → position (0-25) of the letter
- `1 << position` → number with only that bit set
- `|=` → turns ON that bit without affecting others
- `&` → intersection of both sets
- `Integer.bitCount()` → counts set bits (single CPU instruction)

---

## Alternative: HashSet Approach

```java
int countBothCases(String s) {
    Set<Character> lower = new HashSet<>(), upper = new HashSet<>();
    for (char c : s.toCharArray()) {
        if (Character.isLowerCase(c)) lower.add(c);
        else if (Character.isUpperCase(c)) upper.add(Character.toLowerCase(c));
    }
    lower.retainAll(upper);
    return lower.size();
}
```

---

## Complexity

| Approach | Time | Space |
|----------|------|-------|
| Bitmask | O(n) | O(1) — two integers |
| HashSet | O(n) | O(1) — at most 26 entries |

Bitmask is faster in practice due to no object allocation, no boxing/unboxing, and bitwise ops being single CPU instructions.

---

# Variation: Count Special Letters

## Problem
A letter `c` is called **special** if:
1. It appears **both** in lowercase and uppercase in the string
2. **Every** lowercase occurrence of `c` appears **before** the **first** uppercase occurrence of `c`

Return the number of special letters.

**Example 1:** `"aaAbcBC"` → Answer: `3` (a, b, c are all special)
**Example 2:** `"abc"` → Answer: `0` (no uppercase exists)
**Example 3:** `"AbBCab"` → Answer: `0` (lowercase `a` and `b` appear after their uppercase)

---

## Solution

```java
int numberOfSpecialChars(String word) {
    int[] lastLower = new int[26], firstUpper = new int[26];
    Arrays.fill(lastLower, -1);
    Arrays.fill(firstUpper, -1);

    for (int i = 0; i < word.length(); i++) {
        char c = word.charAt(i);
        if (Character.isLowerCase(c))
            lastLower[c - 'a'] = i;
        else if (Character.isUpperCase(c) && firstUpper[c - 'A'] == -1)
            firstUpper[c - 'A'] = i;
    }

    int count = 0;
    for (int i = 0; i < 26; i++) {
        if (lastLower[i] != -1 && firstUpper[i] != -1 && lastLower[i] < firstUpper[i])
            count++;
    }
    return count;
}
```

---

## How It Works

**Key Insight:** For a letter to be special, the **last** lowercase occurrence must come **before** the **first** uppercase occurrence. If this holds, then *all* lowercase occurrences are before the first uppercase (since last lowercase < first uppercase implies every lowercase < first uppercase).

**Algorithm:**
1. Single pass: track `lastLower[i]` = last index where lowercase of letter `i` appears
2. Single pass: track `firstUpper[i]` = first index where uppercase of letter `i` appears
3. A letter is special if both exist AND `lastLower[i] < firstUpper[i]`

**Why last lowercase vs first uppercase?**
- If even the *latest* lowercase is before the *earliest* uppercase, then all lowercase occurrences are guaranteed to be before any uppercase occurrence
- This is the tightest check — if this passes, the condition is satisfied

---

## Trace with `"aaAbcBC"`:

| i | char | Action |
|---|------|--------|
| 0 | `'a'` | lastLower[0] = 0 |
| 1 | `'a'` | lastLower[0] = 1 |
| 2 | `'A'` | firstUpper[0] = 2 |
| 3 | `'b'` | lastLower[1] = 3 |
| 4 | `'c'` | lastLower[2] = 4 |
| 5 | `'B'` | firstUpper[1] = 5 |
| 6 | `'C'` | firstUpper[2] = 6 |

**Final check:**

| Letter | lastLower | firstUpper | lastLower < firstUpper? | Special? |
|--------|-----------|------------|------------------------|----------|
| a | 1 | 2 | 1 < 2 ✅ | Yes |
| b | 3 | 5 | 3 < 5 ✅ | Yes |
| c | 4 | 6 | 4 < 6 ✅ | Yes |

**Result:** `3`

---

## Trace with `"AbBCab"`:

| i | char | Action |
|---|------|--------|
| 0 | `'A'` | firstUpper[0] = 0 |
| 1 | `'b'` | lastLower[1] = 1 |
| 2 | `'B'` | firstUpper[1] = 2 |
| 3 | `'C'` | firstUpper[2] = 3 |
| 4 | `'a'` | lastLower[0] = 4 |
| 5 | `'b'` | lastLower[1] = 5 |

**Final check:**

| Letter | lastLower | firstUpper | lastLower < firstUpper? | Special? |
|--------|-----------|------------|------------------------|----------|
| a | 4 | 0 | 4 < 0 ❌ | No |
| b | 5 | 2 | 5 < 2 ❌ | No |
| c | -1 | 3 | no lowercase | No |

**Result:** `0`

---

## Complexity

| Time | Space |
|------|-------|
| O(n) | O(1) — two arrays of size 26 |

---

## Optimized Solution: Single Pass with Bitmasks

```java
int numberOfSpecialChars(String word) {
    int lower = 0, upper = 0, invalid = 0;

    for (char c : word.toCharArray()) {
        if (Character.isLowerCase(c)) {
            int bit = 1 << (c - 'a');
            lower |= bit;
            if ((upper & bit) != 0)  // lowercase appearing after uppercase → invalid
                invalid |= bit;
        } else if (Character.isUpperCase(c)) {
            upper |= 1 << (c - 'A');
        }
    }

    return Integer.bitCount(lower & upper & ~invalid);
}
```

### How It Works

Uses 3 bitmasks in a **single left-to-right pass**:
- `lower` — letters that have appeared in lowercase
- `upper` — letters that have appeared in uppercase
- `invalid` — letters where a lowercase appeared **after** an uppercase (violates the condition)

**Logic:**
1. When we see a lowercase letter, mark it in `lower`. If its uppercase was already seen (`upper & bit != 0`), mark it `invalid`
2. When we see an uppercase letter, mark it in `upper`
3. Final answer: letters present in both cases (`lower & upper`) minus invalid ones (`& ~invalid`)

**Why this works:**
- A lowercase after an uppercase means not all lowercases are before the first uppercase
- `~invalid` flips invalid bits, so `& ~invalid` removes them from the result

---

### Trace with `"aaAbcBC"`:

| char | lower | upper | invalid | Reason |
|------|-------|-------|---------|--------|
| `'a'` | `a` | `` | `` | lowercase, upper doesn't have 'a' yet |
| `'a'` | `a` | `` | `` | same |
| `'A'` | `a` | `a` | `` | uppercase seen |
| `'b'` | `a,b` | `a` | `` | lowercase, upper doesn't have 'b' yet |
| `'c'` | `a,b,c` | `a` | `` | lowercase, upper doesn't have 'c' yet |
| `'B'` | `a,b,c` | `a,b` | `` | uppercase seen |
| `'C'` | `a,b,c` | `a,b,c` | `` | uppercase seen |

**Result:** `bitCount(a,b,c & a,b,c & ~empty) = bitCount(a,b,c) = 3` ✅

---

### Trace with `"AbBCab"`:

| char | lower | upper | invalid | Reason |
|------|-------|-------|---------|--------|
| `'A'` | `` | `a` | `` | uppercase seen |
| `'b'` | `b` | `a` | `` | lowercase, upper doesn't have 'b' yet |
| `'B'` | `b` | `a,b` | `` | uppercase seen |
| `'C'` | `b` | `a,b,c` | `` | uppercase seen |
| `'a'` | `a,b` | `a,b,c` | `a` | lowercase 'a' after uppercase 'A' → invalid! |
| `'b'` | `a,b` | `a,b,c` | `a,b` | lowercase 'b' after uppercase 'B' → invalid! |

**Result:** `bitCount(a,b & a,b,c & ~(a,b)) = bitCount(empty) = 0` ✅

---

### Comparison

| Approach | Time | Space | Passes | Allocations |
|----------|------|-------|--------|-------------|
| Two arrays | O(n) | O(1) — 52 ints | 2 (fill + loop + check) | 2 arrays |
| Three bitmasks | O(n) | O(1) — 3 ints | 1 | None |

Both are O(n)/O(1), but the bitmask version is faster in practice:
- Single pass (better cache locality)
- No array allocation or `Arrays.fill`
- Pure bitwise ops (single CPU cycle each)
- `Integer.bitCount` is a single hardware instruction on modern CPUs
