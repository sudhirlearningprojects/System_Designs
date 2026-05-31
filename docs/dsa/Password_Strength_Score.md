# Password Strength Score - Weighted Unique Characters

## Problem Statement

Given a password string, calculate its strength score based on the count of **unique** characters in each category:

- Each unique lowercase letter = **1 point**
- Each unique uppercase letter = **2 points**
- Each unique digit = **3 points**
- Each unique special character (from `"!@#$"`) = **5 points**

Return the total strength score.

**Examples:**
```
Input:  "aAbB1!a"
Output: 1*2 + 2*2 + 3*1 + 5*1 = 2 + 4 + 3 + 5 = 14
Explanation: unique lower={a,b}, unique upper={A,B}, unique digit={1}, unique special={!}

Input:  "aaaa"
Output: 1
Explanation: unique lower={a} → 1*1 = 1

Input:  "aA1!"
Output: 1 + 2 + 3 + 5 = 11
```

---

## Solution 1: HashSet Approach

```java
int passwordStrength(String password) {
    Set<Character> lowerLs = new HashSet<>();
    Set<Character> upperLs = new HashSet<>();
    Set<Character> digits = new HashSet<>();
    Set<Character> specialChars = new HashSet<>();

    String special = "!@#$";

    for (char c : password.toCharArray()) {
        if (Character.isLowerCase(c)) lowerLs.add(c);
        else if (Character.isUpperCase(c)) upperLs.add(c);
        else if (Character.isDigit(c)) digits.add(c);
        else if (special.indexOf(c) != -1) specialChars.add(c);
    }

    return (lowerLs.size() * 1) + (upperLs.size() * 2) + (digits.size() * 3) + (specialChars.size() * 5);
}
```

**Drawback:** HashSet uses boxing (`char` → `Character`), object allocation, and hashing overhead.

---

## Solution 2: Bitmask Approach (Optimized)

```java
int passwordStrength(String password) {
    int lower = 0, upper = 0, digit = 0, special = 0;

    for (char c : password.toCharArray()) {
        if (Character.isLowerCase(c)) lower |= 1 << (c - 'a');
        else if (Character.isUpperCase(c)) upper |= 1 << (c - 'A');
        else if (Character.isDigit(c)) digit |= 1 << (c - '0');
        else if ("!@#$".indexOf(c) != -1) special |= 1 << ("!@#$".indexOf(c));
    }

    return Integer.bitCount(lower) * 1
         + Integer.bitCount(upper) * 2
         + Integer.bitCount(digit) * 3
         + Integer.bitCount(special) * 5;
}
```

---

## How the Bitmask Works

Each `int` has 32 bits. We use each bit to represent whether a specific character has been seen.

**For lowercase (26 letters → bits 0-25):**
```
'a' → bit 0,  'b' → bit 1,  ...,  'z' → bit 25
lower |= 1 << (c - 'a')   → sets the bit for that letter
```

**For digits (10 digits → bits 0-9):**
```
'0' → bit 0,  '1' → bit 1,  ...,  '9' → bit 9
digit |= 1 << (c - '0')   → sets the bit for that digit
```

**For special chars (`"!@#$"` → bits 0-3):**
```
'!' → bit 0,  '@' → bit 1,  '#' → bit 2,  '$' → bit 3
special |= 1 << (indexOf(c))  → sets the bit for that special char
```

**`Integer.bitCount(x)`** counts the number of 1-bits = number of unique characters seen.

---

## Trace with `"aAbB1!a"`

| char | Action | lower | upper | digit | special |
|------|--------|-------|-------|-------|---------|
| `'a'` | `lower |= 1<<0` | `0...001` | `0` | `0` | `0` |
| `'A'` | `upper |= 1<<0` | `0...001` | `0...001` | `0` | `0` |
| `'b'` | `lower |= 1<<1` | `0...011` | `0...001` | `0` | `0` |
| `'B'` | `upper |= 1<<1` | `0...011` | `0...011` | `0` | `0` |
| `'1'` | `digit |= 1<<1` | `0...011` | `0...011` | `0...010` | `0` |
| `'!'` | `special |= 1<<0` | `0...011` | `0...011` | `0...010` | `0...001` |
| `'a'` | `lower |= 1<<0` | `0...011` (unchanged!) | `0...011` | `0...010` | `0...001` |

**Final calculation:**
```
bitCount(011) * 1 = 2 * 1 = 2   (a, b)
bitCount(011) * 2 = 2 * 2 = 4   (A, B)
bitCount(010) * 3 = 1 * 3 = 3   (1)
bitCount(001) * 5 = 1 * 5 = 5   (!)
                    Total  = 14
```

**Key:** When `'a'` appears again, `lower |= 1<<0` doesn't change anything because bit 0 is already 1. This is how duplicates are automatically ignored without any extra check.

---

## Why Bitmask is Faster

| Aspect | HashSet | Bitmask |
|--------|---------|---------|
| Storage | 4 HashSet objects on heap | 4 `int` variables on stack |
| Per-char cost | Boxing `char`→`Character` + hash + equals | Single bitwise OR |
| Duplicate handling | Hash lookup + comparison | OR with already-set bit (no-op) |
| Count unique | `.size()` (internal counter) | `Integer.bitCount()` (1 CPU instruction) |
| GC pressure | Creates Character objects | Zero allocations |

---

## Complexity

| Approach | Time | Space |
|----------|------|-------|
| HashSet | O(n) | O(1) — bounded by 26+26+10+4 = 66 entries |
| Bitmask | O(n) | O(1) — 4 integers (16 bytes) |

Both are O(n) time and O(1) space, but bitmask has significantly lower constant factors.
