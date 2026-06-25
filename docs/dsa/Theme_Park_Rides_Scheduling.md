# Theme Park Rides Scheduling - Earliest Finish Time

## Problem Statement

You are given two categories of theme park attractions: **land rides** and **water rides**.

**Land rides:**
- `landStartTime[i]` – the earliest time the ith land ride can be boarded
- `landDuration[i]` – how long the ith land ride lasts

**Water rides:**
- `waterStartTime[j]` – the earliest time the jth water ride can be boarded
- `waterDuration[j]` – how long the jth water ride lasts

**Constraints:**
- A tourist must experience exactly one ride from each category, in either order
- A ride may be started at its opening time or any later moment
- If a ride is started at time `t`, it finishes at time `t + duration`
- Immediately after finishing one ride the tourist may board the other (if it is already open) or wait until it opens

**Goal:** Return the earliest possible time at which the tourist can finish both rides.

---

## Approach 1: Brute Force — O(n × m)

### Intuition

Try every pair `(land ride i, water ride j)` in both orders:
1. Land first → Water second
2. Water first → Land second

For each combination, compute the total finish time.

### Key Formula

If ride A finishes at time `f`, and ride B has opening time `s` with duration `d`:
- Ride B starts at `max(f, s)` and finishes at `max(f, s) + d`

### Code

```java
public int earliestFinish(int[] landStartTime, int[] landDuration, 
                          int[] waterStartTime, int[] waterDuration) {
    int result = Integer.MAX_VALUE;

    for (int i = 0; i < landStartTime.length; i++) {
        for (int j = 0; j < waterStartTime.length; j++) {
            // Option 1: Land first, then Water
            int landFinish = landStartTime[i] + landDuration[i];
            int total1 = Math.max(landFinish, waterStartTime[j]) + waterDuration[j];

            // Option 2: Water first, then Land
            int waterFinish = waterStartTime[j] + waterDuration[j];
            int total2 = Math.max(waterFinish, landStartTime[i]) + landDuration[i];

            result = Math.min(result, Math.min(total1, total2));
        }
    }

    return result;
}
```

### Complexity
| Metric | Value |
|--------|-------|
| Time   | O(n × m) |
| Space  | O(1) |

---

## Approach 2: Optimized — O((n + m) log(n + m))

### Intuition

The brute force checks every pair. We can optimize by fixing the "first ride" and efficiently finding the best "second ride".

**Key observation:** Given a first ride that finishes at time `f`, and we want to pick the best second ride from array `second[]`:

- **Case A** — second ride opens at or before `f` (i.e., `secondStart[j] <= f`):
  - Tourist starts immediately → finishes at `f + secondDuration[j]`
  - Want: **minimum duration** among rides with `startTime <= f`

- **Case B** — second ride opens after `f` (i.e., `secondStart[j] > f`):
  - Tourist waits → finishes at `secondStart[j] + secondDuration[j]` (i.e., `endTime[j]`)
  - Want: **minimum endTime** among rides with `startTime > f`

### Algorithm

1. Sort second rides by `startTime`
2. Build **prefix min duration** array (for Case A)
3. Build **suffix min endTime** array (for Case B)
4. For each first ride, binary search to split second rides into Case A and Case B
5. Take the minimum of both cases

### Visual Walkthrough

```
Second rides sorted by startTime:
Index:    0       1       2       3       4
Start:   [2]     [5]     [8]     [12]    [15]
End:     [7]     [9]     [11]    [18]    [17]
Dur:      5       4       3       6       2

prefixMinDur:  [5, 4, 3, 3, 2]   ← min duration up to index k
suffixMinEnd:  [7, 9, 11, 17, 17, ∞]  ← min endTime from index k onwards

If first ride finishes at f = 10:
  Binary search → first index with start > 10 is index 3 (start=12)
  
  Case A (indices 0..2, start <= 10): 
    best = f + prefixMinDur[2] = 10 + 3 = 13
  
  Case B (indices 3..4, start > 10): 
    best = suffixMinEnd[3] = 17
  
  Answer for this first ride = min(13, 17) = 13
```

### Code

```java
import java.util.Arrays;

public class ThemeParkRides {

    public int earliestFinish(int[] landStartTime, int[] landDuration,
                              int[] waterStartTime, int[] waterDuration) {
        return Math.min(
            solve(landStartTime, landDuration, waterStartTime, waterDuration),
            solve(waterStartTime, waterDuration, landStartTime, landDuration)
        );
    }

    private int solve(int[] firstStart, int[] firstDur, int[] secondStart, int[] secondDur) {
        int n = firstStart.length;
        int m = secondStart.length;

        // Pair (startTime, endTime) for second rides, sorted by startTime
        int[][] second = new int[m][2];
        for (int j = 0; j < m; j++) {
            second[j][0] = secondStart[j];
            second[j][1] = secondStart[j] + secondDur[j];
        }
        Arrays.sort(second, (a, b) -> a[0] - b[0]);

        // Suffix min endTime: best for Case B
        int[] suffixMinEnd = new int[m + 1];
        suffixMinEnd[m] = Integer.MAX_VALUE;
        for (int k = m - 1; k >= 0; k--) {
            suffixMinEnd[k] = Math.min(suffixMinEnd[k + 1], second[k][1]);
        }

        // Prefix min duration: best for Case A
        int[] prefixMinDur = new int[m];
        prefixMinDur[0] = second[0][1] - second[0][0];
        for (int k = 1; k < m; k++) {
            prefixMinDur[k] = Math.min(prefixMinDur[k - 1], second[k][1] - second[k][0]);
        }

        int ans = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int f = firstStart[i] + firstDur[i];

            // Binary search: first index where second[idx][0] > f
            int lo = 0, hi = m;
            while (lo < hi) {
                int mid = (lo + hi) / 2;
                if (second[mid][0] > f) hi = mid;
                else lo = mid + 1;
            }

            // Case B: rides with startTime > f
            int candidate = suffixMinEnd[lo];

            // Case A: rides with startTime <= f → finish = f + minDuration
            if (lo > 0) {
                candidate = Math.min(candidate, f + prefixMinDur[lo - 1]);
            }

            ans = Math.min(ans, candidate);
        }
        return ans;
    }
}
```

### Complexity
| Metric | Value |
|--------|-------|
| Time   | O((n + m) log m) |
| Space  | O(m) |

---

## Dry Run Example

### Input
```
landStartTime  = [1, 5]
landDuration   = [3, 2]
waterStartTime = [2, 4, 6]
waterDuration  = [4, 1, 3]
```

### solve(land → water)

**Second rides (water) sorted by startTime:**
| Index | Start | End | Duration |
|-------|-------|-----|----------|
| 0     | 2     | 6   | 4        |
| 1     | 4     | 5   | 1        |
| 2     | 6     | 9   | 3        |

**prefixMinDur:** `[4, 1, 1]`
**suffixMinEnd:** `[5, 5, 9, ∞]`

**First ride i=0:** f = 1 + 3 = 4
- Binary search: first start > 4 → index 2 (start=6)
- Case B: suffixMinEnd[2] = 9
- Case A: f + prefixMinDur[1] = 4 + 1 = **5** ✓

**First ride i=1:** f = 5 + 2 = 7
- Binary search: first start > 7 → index 3 (none)
- Case B: suffixMinEnd[3] = ∞
- Case A: f + prefixMinDur[2] = 7 + 1 = **8**

**Best from land→water = 5**

### solve(water → land)

**Second rides (land) sorted by startTime:**
| Index | Start | End | Duration |
|-------|-------|-----|----------|
| 0     | 1     | 4   | 3        |
| 1     | 5     | 7   | 2        |

**prefixMinDur:** `[3, 2]`
**suffixMinEnd:** `[4, 7, ∞]`

**First ride j=0 (water):** f = 2 + 4 = 6
- Binary search: first start > 6 → index 2 (none)
- Case A: f + prefixMinDur[1] = 6 + 2 = **8**

**First ride j=1 (water):** f = 4 + 1 = 5
- Binary search: first start > 5 → index 2 (none)
- Case A: f + prefixMinDur[1] = 5 + 2 = **7**

**First ride j=2 (water):** f = 6 + 3 = 9
- Binary search: first start > 9 → index 2 (none)
- Case A: f + prefixMinDur[1] = 9 + 2 = **11**

**Best from water→land = 7**

### Final Answer
```
min(5, 7) = 5
```

The tourist takes land ride 0 (start=1, finish=4), then water ride 1 (start=4, finish=5).

---

## Edge Cases

| Case | Description | Handling |
|------|-------------|----------|
| Single ride per category | Only one pair to check | Works normally |
| All rides open at time 0 | No waiting needed for second ride | Case A always applies |
| Second ride opens far in future | Tourist must wait | Case B gives optimal |
| Same start times | Multiple rides at same time | Binary search handles correctly |
| Very large durations | First ride finishes very late | Case A dominates |

---

## Why This Works

The optimization works because we decompose the "best second ride" decision into two independent sub-problems:

1. **Case A (no waiting):** Only the duration of the second ride matters → precompute minimum duration prefix
2. **Case B (must wait):** The finish time is fixed regardless of when first ride ends → precompute minimum endTime suffix

Sorting by start time lets us binary search to partition rides into these two cases for any given finish time `f`.

---

## Pattern Recognition

This problem combines:
- **Greedy**: Pick the optimal second ride for each first ride
- **Sort + Binary Search**: Efficiently partition candidates
- **Prefix/Suffix Arrays**: Precompute answers for range queries

Similar problems:
- Meeting Rooms II (interval scheduling)
- Two City Scheduling
- Job Scheduling with deadlines
- Minimum Cost to Hire K Workers

---

## Summary

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Brute Force | O(n × m) | O(1) | Try all pairs |
| Optimized | O((n+m) log m) | O(m) | Sort + Binary Search + Prefix/Suffix |

The optimized approach reduces from quadratic to linearithmic by avoiding redundant comparisons through precomputation and binary search.
