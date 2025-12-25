# Minimum Platforms Required

## Problem Statement

Given arrival and departure times of all trains that reach a railway station, find the minimum number of platforms required so that no train waits.

**Input:** 
- arr[]: Array of arrival times
- dep[]: Array of departure times

**Output:** Minimum number of platforms needed

**Constraints:**
- Arrival time < Departure time for each train
- Times can be represented as integers (e.g., 9:00 = 900)

**Examples:**
```
Input: 
  arr[] = {9:00, 9:40, 9:50, 11:00, 15:00, 18:00}
  dep[] = {9:10, 12:00, 11:20, 11:30, 19:00, 20:00}

Output: 3

Explanation:
  9:00-9:10: Train 1 (1 platform)
  9:40-12:00: Train 2 (2 platforms)
  9:50-11:20: Train 3 (3 platforms) ← Peak
  11:00-11:30: Train 4 (still 3 platforms)
  15:00-19:00: Train 5
  18:00-20:00: Train 6

Maximum overlap: 3 trains (between 9:50 and 11:00)

Input:
  arr[] = {9:00, 9:40}
  dep[] = {9:10, 12:00}

Output: 1

Explanation: No overlap, one platform sufficient
```

---

## Solution Approaches

### Approach 1: Sort and Two Pointers (Optimal)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(1)

```java
public static int findPlatform(int arr[], int dep[], int n) {
    Arrays.sort(arr);
    Arrays.sort(dep);
    
    int platforms = 0;
    int maxPlatforms = 0;
    int i = 0, j = 0;
    
    while (i < n && j < n) {
        if (arr[i] <= dep[j]) {
            platforms++;
            maxPlatforms = Math.max(maxPlatforms, platforms);
            i++;
        } else {
            platforms--;
            j++;
        }
    }
    
    return maxPlatforms;
}
```

---

### Approach 2: Events with Sorting

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int findPlatformEvents(int arr[], int dep[], int n) {
    List<int[]> events = new ArrayList<>();
    
    for (int i = 0; i < n; i++) {
        events.add(new int[]{arr[i], 1});  // Arrival: +1
        events.add(new int[]{dep[i], -1}); // Departure: -1
    }
    
    // Sort by time, departures before arrivals at same time
    Collections.sort(events, (a, b) -> {
        if (a[0] != b[0]) return a[0] - b[0];
        return a[1] - b[1]; // Departure (-1) before arrival (1)
    });
    
    int platforms = 0;
    int maxPlatforms = 0;
    
    for (int[] event : events) {
        platforms += event[1];
        maxPlatforms = Math.max(maxPlatforms, platforms);
    }
    
    return maxPlatforms;
}
```

---

### Approach 3: Brute Force (Check All Intervals)

**Time Complexity:** O(n²)  
**Space Complexity:** O(1)

```java
public static int findPlatformBrute(int arr[], int dep[], int n) {
    int maxPlatforms = 0;
    
    for (int i = 0; i < n; i++) {
        int platforms = 1;
        
        for (int j = 0; j < n; j++) {
            if (i != j) {
                // Check if train j overlaps with train i
                if (arr[j] <= arr[i] && dep[j] > arr[i]) {
                    platforms++;
                }
            }
        }
        
        maxPlatforms = Math.max(maxPlatforms, platforms);
    }
    
    return maxPlatforms;
}
```

---

### Approach 4: Min Heap (Meeting Rooms II Pattern)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(n)

```java
public static int findPlatformHeap(int arr[], int dep[], int n) {
    // Create train intervals
    int[][] trains = new int[n][2];
    for (int i = 0; i < n; i++) {
        trains[i][0] = arr[i];
        trains[i][1] = dep[i];
    }
    
    // Sort by arrival time
    Arrays.sort(trains, (a, b) -> a[0] - b[0]);
    
    // Min heap to track departure times
    PriorityQueue<Integer> pq = new PriorityQueue<>();
    
    for (int[] train : trains) {
        // Remove trains that have departed
        while (!pq.isEmpty() && pq.peek() <= train[0]) {
            pq.poll();
        }
        
        pq.offer(train[1]);
    }
    
    return pq.size();
}
```

---

## Algorithm Walkthrough

### Example: arr[] = {900, 940, 950, 1100, 1500, 1800}, dep[] = {910, 1200, 1120, 1130, 1900, 2000}

**Two Pointers Approach:**

```
After sorting:
  arr[] = {900, 940, 950, 1100, 1500, 1800}
  dep[] = {910, 1120, 1130, 1200, 1900, 2000}

Initial: i=0, j=0, platforms=0, maxPlatforms=0

Step 1: arr[0]=900 <= dep[0]=910
  Train arrives → platforms=1, maxPlatforms=1
  i=1

Step 2: arr[1]=940 > dep[0]=910
  Train departs → platforms=0
  j=1

Step 3: arr[1]=940 <= dep[1]=1120
  Train arrives → platforms=1, maxPlatforms=1
  i=2

Step 4: arr[2]=950 <= dep[1]=1120
  Train arrives → platforms=2, maxPlatforms=2
  i=3

Step 5: arr[3]=1100 <= dep[1]=1120
  Train arrives → platforms=3, maxPlatforms=3 ← Peak
  i=4

Step 6: arr[4]=1500 > dep[1]=1120
  Train departs → platforms=2
  j=2

Step 7: arr[4]=1500 > dep[2]=1130
  Train departs → platforms=1
  j=3

Step 8: arr[4]=1500 > dep[3]=1200
  Train departs → platforms=0
  j=4

Step 9: arr[4]=1500 <= dep[4]=1900
  Train arrives → platforms=1
  i=5

Step 10: arr[5]=1800 <= dep[4]=1900
  Train arrives → platforms=2
  i=6

i=6, loop ends

Result: maxPlatforms = 3
```

**Timeline Visualization:**
```
Time:  900  940  950  1100 1120 1130 1200 1500 1800 1900 2000
       |    |    |    |    |    |    |    |    |    |    |
T1:    [----]
T2:         [------------------------]
T3:              [----------]
T4:                   [-----]
T5:                                  [----------]
T6:                                       [----------]

Platforms needed at each moment:
900:  1 (T1)
940:  2 (T1, T2)
950:  3 (T1, T2, T3) ← Peak
1100: 3 (T2, T3, T4) ← Peak
1120: 2 (T2, T4)
1130: 1 (T2)
1500: 1 (T5)
1800: 2 (T5, T6)

Maximum: 3 platforms
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Two Pointers (Optimal)
    public static int findPlatform(int arr[], int dep[], int n) {
        Arrays.sort(arr);
        Arrays.sort(dep);
        
        int platforms = 0;
        int maxPlatforms = 0;
        int i = 0, j = 0;
        
        while (i < n && j < n) {
            if (arr[i] <= dep[j]) {
                platforms++;
                maxPlatforms = Math.max(maxPlatforms, platforms);
                i++;
            } else {
                platforms--;
                j++;
            }
        }
        
        return maxPlatforms;
    }
    
    // Approach 2: Events
    public static int findPlatformEvents(int arr[], int dep[], int n) {
        List<int[]> events = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            events.add(new int[]{arr[i], 1});
            events.add(new int[]{dep[i], -1});
        }
        
        Collections.sort(events, (a, b) -> {
            if (a[0] != b[0]) return a[0] - b[0];
            return a[1] - b[1];
        });
        
        int platforms = 0;
        int maxPlatforms = 0;
        
        for (int[] event : events) {
            platforms += event[1];
            maxPlatforms = Math.max(maxPlatforms, platforms);
        }
        
        return maxPlatforms;
    }
    
    // Approach 3: Min Heap
    public static int findPlatformHeap(int arr[], int dep[], int n) {
        int[][] trains = new int[n][2];
        for (int i = 0; i < n; i++) {
            trains[i][0] = arr[i];
            trains[i][1] = dep[i];
        }
        
        Arrays.sort(trains, (a, b) -> a[0] - b[0]);
        
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        
        for (int[] train : trains) {
            while (!pq.isEmpty() && pq.peek() <= train[0]) {
                pq.poll();
            }
            pq.offer(train[1]);
        }
        
        return pq.size();
    }
    
    public static boolean doTestsPass() {
        // Test 1
        int[] arr1 = {900, 940, 950, 1100, 1500, 1800};
        int[] dep1 = {910, 1200, 1120, 1130, 1900, 2000};
        if (findPlatform(arr1, dep1, 6) != 3) return false;
        
        // Test 2
        int[] arr2 = {900, 940};
        int[] dep2 = {910, 1200};
        if (findPlatform(arr2, dep2, 2) != 1) return false;
        
        // Test 3: All overlap
        int[] arr3 = {900, 910, 920};
        int[] dep3 = {1000, 1010, 1020};
        if (findPlatform(arr3, dep3, 3) != 3) return false;
        
        // Test 4: No overlap
        int[] arr4 = {900, 1000, 1100};
        int[] dep4 = {930, 1030, 1130};
        if (findPlatform(arr4, dep4, 3) != 1) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass");
        } else {
            System.out.println("Tests fail");
        }
        
        // Demo
        int[] arr = {900, 940, 950, 1100, 1500, 1800};
        int[] dep = {910, 1200, 1120, 1130, 1900, 2000};
        
        System.out.println("Minimum platforms needed: " + findPlatform(arr, dep, 6));
    }
}
```

---

## Test Cases

```java
@Test
public void testFindPlatform() {
    // Test 1: Given example
    int[] arr1 = {900, 940, 950, 1100, 1500, 1800};
    int[] dep1 = {910, 1200, 1120, 1130, 1900, 2000};
    assertEquals(3, findPlatform(arr1, dep1, 6));
    
    // Test 2: No overlap
    int[] arr2 = {900, 940};
    int[] dep2 = {910, 1200};
    assertEquals(1, findPlatform(arr2, dep2, 2));
    
    // Test 3: All trains overlap
    int[] arr3 = {900, 910, 920};
    int[] dep3 = {1000, 1010, 1020};
    assertEquals(3, findPlatform(arr3, dep3, 3));
    
    // Test 4: Sequential trains (no overlap)
    int[] arr4 = {900, 1000, 1100};
    int[] dep4 = {930, 1030, 1130};
    assertEquals(1, findPlatform(arr4, dep4, 3));
    
    // Test 5: Single train
    int[] arr5 = {900};
    int[] dep5 = {1000};
    assertEquals(1, findPlatform(arr5, dep5, 1));
    
    // Test 6: Same arrival and departure times
    int[] arr6 = {900, 900};
    int[] dep6 = {910, 920};
    assertEquals(2, findPlatform(arr6, dep6, 2));
    
    // Test 7: Departure equals next arrival
    int[] arr7 = {900, 910};
    int[] dep7 = {910, 920};
    assertEquals(1, findPlatform(arr7, dep7, 2));
}
```

---

## Visual Representation

### Timeline Diagram

```
Example: arr[] = {900, 940, 950, 1100}, dep[] = {910, 1200, 1120, 1130}

Timeline:
900   940   950   1100  1120  1130  1200
|     |     |     |     |     |     |
[T1---]
      [T2--------------------------]
            [T3---------]
                  [T4----]

Platform allocation:
Time    Event       Platforms   Trains
900     T1 arrives  1           [T1]
910     T1 departs  0           []
940     T2 arrives  1           [T2]
950     T3 arrives  2           [T2, T3]
1100    T4 arrives  3 ← Max     [T2, T3, T4]
1120    T3 departs  2           [T2, T4]
1130    T4 departs  1           [T2]
1200    T2 departs  0           []

Maximum platforms needed: 3
```

### Two Pointers Movement

```
Sorted arrays:
arr: [900, 940, 950, 1100]
dep: [910, 1120, 1130, 1200]

i→   [900, 940, 950, 1100]
j→   [910, 1120, 1130, 1200]

Compare arr[i] with dep[j]:
- If arr[i] <= dep[j]: Train arrives, platforms++, i++
- If arr[i] > dep[j]: Train departs, platforms--, j++
```

---

## Edge Cases

1. **Single train:** Return 1
2. **No overlap:** Return 1
3. **All overlap:** Return n
4. **Same arrival times:** Multiple trains arrive together
5. **Departure = Next arrival:** Train leaves as next arrives
6. **Empty arrays:** Return 0
7. **Sorted vs unsorted input:** Algorithm handles both

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Two Pointers | O(n log n) | O(1) | **Optimal** |
| Events | O(n log n) | O(n) | Clear logic |
| Min Heap | O(n log n) | O(n) | Meeting Rooms II |
| Brute Force | O(n²) | O(1) | Not practical |

**Why O(n log n)?**
- Sorting: O(n log n)
- Two pointers: O(n)
- Total: O(n log n)

**Space Optimization:**
- Two pointers: O(1) extra space (sorts in-place)
- Events/Heap: O(n) for data structures

---

## Related Problems

1. **Meeting Rooms II** - Identical problem with meetings
2. **Merge Intervals** - Interval merging
3. **Insert Interval** - Insert into sorted intervals
4. **Non-overlapping Intervals** - Remove minimum intervals
5. **My Calendar I/II/III** - Booking system
6. **Car Pooling** - Capacity constraint variant

---

## Interview Tips

### Clarification Questions
1. Can arrival equal departure? (Usually no, arrival < departure)
2. Are times sorted? (Usually no, need to sort)
3. What if departure of one = arrival of another? (One platform sufficient)
4. Time format? (Usually integers like 900 for 9:00)
5. Can we modify input arrays? (Usually yes for sorting)

### Approach Explanation
1. "Sort both arrival and departure arrays"
2. "Use two pointers to track arrivals and departures"
3. "When train arrives, increment platform count"
4. "When train departs, decrement platform count"
5. "Track maximum platforms needed at any time"

### Common Mistakes
- Not sorting the arrays
- Wrong comparison (< vs <=)
- Not tracking maximum correctly
- Confusing arrival/departure logic
- Forgetting edge cases (single train, no overlap)

### Why Sorting Works
```
Key insight: We don't need to track which specific train
uses which platform. We only need the maximum overlap.

Sorting allows us to process events chronologically and
track the running count of trains at the station.
```

---

## Real-World Applications

1. **Railway Stations** - Platform allocation
2. **Airport Gates** - Gate assignment
3. **Meeting Rooms** - Conference room booking
4. **Parking Lots** - Parking spot allocation
5. **CPU Scheduling** - Process scheduling
6. **Resource Management** - Any shared resource allocation

---

## Key Takeaways

1. **Interval Overlap:** Classic problem pattern
2. **Two Pointers:** Optimal O(n log n) solution
3. **Sorting:** Key to efficient solution
4. **Event Processing:** Arrivals (+1), Departures (-1)
5. **Maximum Overlap:** Track running count maximum
6. **Space Optimization:** Can sort in-place for O(1) extra space
7. **Similar Pattern:** Meeting Rooms II, Car Pooling

---

## Additional Notes

### Why arr[i] <= dep[j] (not <)?

```
If arrival time equals departure time:
  arr[i] = dep[j]

Interpretation: New train arrives as old train departs
  → Can use same platform
  → Use <= to count arrival first

Alternative: Use < and handle ties differently
```

### Events Approach Tie-Breaking

```java
// Sort: departures before arrivals at same time
Collections.sort(events, (a, b) -> {
    if (a[0] != b[0]) return a[0] - b[0];
    return a[1] - b[1]; // -1 (departure) before 1 (arrival)
});

Why? If train departs at 910 and another arrives at 910,
process departure first to free up platform.
```

### Min Heap Intuition

```
Heap stores departure times of trains currently at station.

For each arriving train:
  1. Remove all trains that have departed (dep <= arrival)
  2. Add current train's departure time
  3. Heap size = platforms needed at this moment

Maximum heap size = answer
```

### Comparison with Meeting Rooms II

```
Meeting Rooms II (LeetCode 253):
  Input: intervals = [[0,30],[5,10],[15,20]]
  Output: 2

Same problem, different context:
  - Platforms → Meeting rooms
  - Trains → Meetings
  - Arrival/Departure → Start/End times

Solution is identical!
```

### Extension: Track Which Platform

```java
class PlatformAssignment {
    int platformNumber;
    int trainId;
    int arrivalTime;
    int departureTime;
}

// Use min heap with platform numbers
// Assign freed platform to next train
```

### Time Representation

```java
// Convert "HH:MM" to minutes
int timeToMinutes(String time) {
    String[] parts = time.split(":");
    return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
}

// Example: "09:40" → 9*60 + 40 = 580
```

### Why Not Greedy by Earliest Departure?

```
Greedy: Assign train to platform with earliest departure
Problem: Doesn't minimize platforms, just assigns them

Example:
  T1: [1, 10]
  T2: [2, 5]
  T3: [6, 9]

Greedy might use 2 platforms when 1 suffices (T1, then T2+T3)
Our algorithm correctly finds minimum: 2 (T1+T2 overlap)
```
