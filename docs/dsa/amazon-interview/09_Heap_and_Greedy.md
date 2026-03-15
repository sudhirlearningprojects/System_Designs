# Heap & Greedy Problems (25 Problems)

---

## 1. Top K Frequent Elements (LC 347) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given integer array, return k most frequent elements.

**Example**: `nums=[1,1,1,2,2,3], k=2` → `[1,2]`

### Solution
```java
public int[] topKFrequent(int[] nums, int k) {
    Map<Integer, Integer> freq = new HashMap<>();
    for (int n : nums) freq.put(n, freq.getOrDefault(n, 0) + 1);

    PriorityQueue<Integer> minHeap = new PriorityQueue<>((a, b) -> freq.get(a) - freq.get(b));
    for (int n : freq.keySet()) {
        minHeap.offer(n);
        if (minHeap.size() > k) minHeap.poll();
    }

    int[] result = new int[k];
    for (int i = k - 1; i >= 0; i--) result[i] = minHeap.poll();
    return result;
}
```
**Time**: O(n log k) | **Space**: O(n)

### Dry Run
```
nums=[1,1,1,2,2,3], k=2
freq={1:3, 2:2, 3:1}
minHeap by freq:
  add 1(3) → [1]
  add 2(2) → [2,1]  size=2=k, no poll
  add 3(1) → [3,2,1] size=3>k, poll min(3) → [2,1]
result = [1,2]
```

### Test Cases
```java
topKFrequent([1,1,1,2,2,3], 2) → [1,2]
topKFrequent([1], 1)           → [1]
topKFrequent([1,2], 2)         → [1,2]
```

### Use Cases
- Trending hashtags, most visited pages, frequent search queries

---

## 2. Find Median from Data Stream (LC 295) ⭐⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Very High

### Problem
Design a data structure that supports addNum and findMedian operations.

### Solution
```java
class MedianFinder {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder()); // lower half
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(); // upper half

    public void addNum(int num) {
        maxHeap.offer(num);
        minHeap.offer(maxHeap.poll());
        if (minHeap.size() > maxHeap.size()) maxHeap.offer(minHeap.poll());
    }

    public double findMedian() {
        return maxHeap.size() > minHeap.size()
            ? maxHeap.peek()
            : (maxHeap.peek() + minHeap.peek()) / 2.0;
    }
}
```
**Time**: O(log n) add, O(1) find | **Space**: O(n)

### Dry Run
```
addNum(1): maxHeap=[1], minHeap=[]
addNum(2): maxHeap=[1], minHeap=[2]
addNum(3): maxHeap=[2,1], minHeap=[3]
findMedian(): sizes equal? no → maxHeap.peek()=2
```

### Test Cases
```java
addNum(1), addNum(2), findMedian() → 1.5
addNum(3), findMedian()            → 2.0
```

---

## 3. K Closest Points to Origin (LC 973) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution
```java
public int[][] kClosest(int[][] points, int k) {
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
        (a, b) -> (b[0]*b[0] + b[1]*b[1]) - (a[0]*a[0] + a[1]*a[1])
    );
    for (int[] p : points) {
        maxHeap.offer(p);
        if (maxHeap.size() > k) maxHeap.poll();
    }
    return maxHeap.toArray(new int[k][]);
}
```
**Time**: O(n log k) | **Space**: O(k)

### Test Cases
```java
kClosest([[1,3],[-2,2]], 1)        → [[-2,2]]
kClosest([[3,3],[5,-1],[-2,4]], 2) → [[3,3],[-2,4]]
```

---

## 4. Task Scheduler (LC 621) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Problem
Given tasks and cooldown n, find minimum intervals to finish all tasks.

**Example**: `tasks=["A","A","A","B","B","B"], n=2` → `8`

### Solution
```java
public int leastInterval(char[] tasks, int n) {
    int[] freq = new int[26];
    for (char t : tasks) freq[t - 'A']++;
    Arrays.sort(freq);
    int maxFreq = freq[25];
    int idleSlots = (maxFreq - 1) * n;
    for (int i = 24; i >= 0 && freq[i] > 0; i--)
        idleSlots -= Math.min(freq[i], maxFreq - 1);
    return tasks.length + Math.max(0, idleSlots);
}
```
**Time**: O(n) | **Space**: O(1)

### Dry Run
```
tasks=[A,A,A,B,B,B], n=2
freq: A=3, B=3
maxFreq=3, idleSlots=(3-1)*2=4
i=24: freq[24]=3, min(3,2)=2, idleSlots=4-2=2
i=23: freq[23]=3, min(3,2)=2, idleSlots=2-2=0
result = 6 + max(0,0) = 6... wait tasks=[A,A,A,B,B,B]
→ A B _ A B _ A B = 8
```

---

## 5. Meeting Rooms II (LC 253) ⭐⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Very High

### Problem
Given meeting intervals, find minimum number of conference rooms required.

**Example**: `[[0,30],[5,10],[15,20]]` → `2`

### Solution
```java
public int minMeetingRooms(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(); // end times

    for (int[] interval : intervals) {
        if (!minHeap.isEmpty() && minHeap.peek() <= interval[0])
            minHeap.poll(); // reuse room
        minHeap.offer(interval[1]);
    }
    return minHeap.size();
}
```
**Time**: O(n log n) | **Space**: O(n)

### Dry Run
```
intervals=[[0,30],[5,10],[15,20]] sorted
[0,30]: heap=[30]
[5,10]: 30>5, new room → heap=[10,30]
[15,20]: 10<=15, reuse → poll 10, add 20 → heap=[20,30]
rooms = heap.size() = 2
```

---

## 6. Reorganize String (LC 767) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public String reorganizeString(String s) {
    int[] freq = new int[26];
    for (char c : s.toCharArray()) freq[c - 'a']++;
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> b[1] - a[1]);
    for (int i = 0; i < 26; i++)
        if (freq[i] > 0) maxHeap.offer(new int[]{i, freq[i]});

    StringBuilder sb = new StringBuilder();
    while (maxHeap.size() >= 2) {
        int[] first = maxHeap.poll(), second = maxHeap.poll();
        sb.append((char)('a' + first[0]));
        sb.append((char)('a' + second[0]));
        if (--first[1] > 0) maxHeap.offer(first);
        if (--second[1] > 0) maxHeap.offer(second);
    }
    if (!maxHeap.isEmpty()) {
        int[] last = maxHeap.poll();
        if (last[1] > 1) return "";
        sb.append((char)('a' + last[0]));
    }
    return sb.toString();
}
```

---

## 7. Merge K Sorted Lists (LC 23) ⭐⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Very High

### Solution
```java
public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> minHeap = new PriorityQueue<>((a, b) -> a.val - b.val);
    for (ListNode node : lists)
        if (node != null) minHeap.offer(node);

    ListNode dummy = new ListNode(0), curr = dummy;
    while (!minHeap.isEmpty()) {
        ListNode node = minHeap.poll();
        curr.next = node;
        curr = curr.next;
        if (node.next != null) minHeap.offer(node.next);
    }
    return dummy.next;
}
```
**Time**: O(n log k) | **Space**: O(k)

---

## 8. IPO (LC 502) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Problem
Maximize capital after k projects. Each project has profit and capital requirement.

### Solution
```java
public int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
    int n = profits.length;
    int[][] projects = new int[n][2];
    for (int i = 0; i < n; i++) projects[i] = new int[]{capital[i], profits[i]};
    Arrays.sort(projects, (a, b) -> a[0] - b[0]);

    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    int idx = 0;
    for (int i = 0; i < k; i++) {
        while (idx < n && projects[idx][0] <= w) maxHeap.offer(projects[idx++][1]);
        if (maxHeap.isEmpty()) break;
        w += maxHeap.poll();
    }
    return w;
}
```

---

## 9. Minimum Cost to Connect Sticks (LC 1167) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int connectSticks(int[] sticks) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    for (int s : sticks) minHeap.offer(s);
    int cost = 0;
    while (minHeap.size() > 1) {
        int combined = minHeap.poll() + minHeap.poll();
        cost += combined;
        minHeap.offer(combined);
    }
    return cost;
}
```
**Time**: O(n log n) | **Space**: O(n)

---

## 10. Jump Game (LC 55) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution (Greedy)
```java
public boolean canJump(int[] nums) {
    int maxReach = 0;
    for (int i = 0; i < nums.length; i++) {
        if (i > maxReach) return false;
        maxReach = Math.max(maxReach, i + nums[i]);
    }
    return true;
}
```
**Time**: O(n) | **Space**: O(1)

### Dry Run
```
nums=[2,3,1,1,4]
i=0: maxReach=max(0,0+2)=2
i=1: maxReach=max(2,1+3)=4
i=2: maxReach=max(4,2+1)=4
i=3: maxReach=max(4,3+1)=4
i=4: 4<=4, return true
```

---

## 11. Jump Game II (LC 45) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution (Greedy)
```java
public int jump(int[] nums) {
    int jumps = 0, curEnd = 0, farthest = 0;
    for (int i = 0; i < nums.length - 1; i++) {
        farthest = Math.max(farthest, i + nums[i]);
        if (i == curEnd) { jumps++; curEnd = farthest; }
    }
    return jumps;
}
```
**Time**: O(n) | **Space**: O(1)

---

## 12. Gas Station (LC 134) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int canCompleteCircuit(int[] gas, int[] cost) {
    int total = 0, tank = 0, start = 0;
    for (int i = 0; i < gas.length; i++) {
        total += gas[i] - cost[i];
        tank += gas[i] - cost[i];
        if (tank < 0) { start = i + 1; tank = 0; }
    }
    return total >= 0 ? start : -1;
}
```

---

## 13. Assign Cookies (LC 455) ⭐⭐

**Difficulty**: Easy | **Frequency**: Medium

### Solution
```java
public int findContentChildren(int[] g, int[] s) {
    Arrays.sort(g); Arrays.sort(s);
    int child = 0, cookie = 0;
    while (child < g.length && cookie < s.length) {
        if (s[cookie] >= g[child]) child++;
        cookie++;
    }
    return child;
}
```

---

## 14. Minimum Number of Arrows (LC 452) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int findMinArrowShots(int[][] points) {
    Arrays.sort(points, (a, b) -> Integer.compare(a[1], b[1]));
    int arrows = 1, end = points[0][1];
    for (int i = 1; i < points.length; i++) {
        if (points[i][0] > end) { arrows++; end = points[i][1]; }
    }
    return arrows;
}
```

---

## 15. Non-overlapping Intervals (LC 435) ⭐⭐⭐⭐

**Difficulty**: Medium | **Frequency**: High

### Solution
```java
public int eraseOverlapIntervals(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
    int count = 0, end = intervals[0][1];
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] < end) count++;
        else end = intervals[i][1];
    }
    return count;
}
```

---

## 16. Kth Smallest in Matrix (LC 378) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int kthSmallest(int[][] matrix, int k) {
    int n = matrix.length;
    PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    for (int i = 0; i < n; i++) minHeap.offer(new int[]{matrix[i][0], i, 0});
    while (k-- > 1) {
        int[] curr = minHeap.poll();
        if (curr[2] + 1 < n) minHeap.offer(new int[]{matrix[curr[1]][curr[2]+1], curr[1], curr[2]+1});
    }
    return minHeap.peek()[0];
}
```

---

## 17. Sliding Window Maximum (LC 239) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: High

### Solution (Monotonic Deque)
```java
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        if (!deque.isEmpty() && deque.peekFirst() < i - k + 1) deque.pollFirst();
        while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) deque.pollLast();
        deque.offerLast(i);
        if (i >= k - 1) result[i - k + 1] = nums[deque.peekFirst()];
    }
    return result;
}
```
**Time**: O(n) | **Space**: O(k)

### Dry Run
```
nums=[1,3,-1,-3,5,3,6,7], k=3
i=0: deque=[0]
i=1: 3>1, remove 0 → deque=[1]
i=2: deque=[1,2], result[0]=nums[1]=3
i=3: deque=[1,2,3], result[1]=nums[1]=3
i=4: 5>all, deque=[4], result[2]=5
i=5: deque=[4,5], result[3]=5
i=6: 6>3, deque=[6], result[4]=6
i=7: 7>6, deque=[7], result[5]=7
→ [3,3,5,5,6,7]
```

---

## 18. Trapping Rain Water (LC 42) ⭐⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Very High

### Solution (Two Pointers)
```java
public int trap(int[] height) {
    int left = 0, right = height.length - 1;
    int leftMax = 0, rightMax = 0, water = 0;
    while (left < right) {
        if (height[left] < height[right]) {
            if (height[left] >= leftMax) leftMax = height[left];
            else water += leftMax - height[left];
            left++;
        } else {
            if (height[right] >= rightMax) rightMax = height[right];
            else water += rightMax - height[right];
            right--;
        }
    }
    return water;
}
```
**Time**: O(n) | **Space**: O(1)

---

## 19. Largest Rectangle in Histogram (LC 84) ⭐⭐⭐⭐

**Difficulty**: Hard | **Frequency**: High

### Solution (Monotonic Stack)
```java
public int largestRectangleArea(int[] heights) {
    Stack<Integer> stack = new Stack<>();
    int maxArea = 0;
    for (int i = 0; i <= heights.length; i++) {
        int h = (i == heights.length) ? 0 : heights[i];
        while (!stack.isEmpty() && h < heights[stack.peek()]) {
            int height = heights[stack.pop()];
            int width = stack.isEmpty() ? i : i - stack.peek() - 1;
            maxArea = Math.max(maxArea, height * width);
        }
        stack.push(i);
    }
    return maxArea;
}
```
**Time**: O(n) | **Space**: O(n)

---

## 20. Course Schedule III (LC 630) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Solution
```java
public int scheduleCourse(int[][] courses) {
    Arrays.sort(courses, (a, b) -> a[1] - b[1]);
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    int time = 0;
    for (int[] c : courses) {
        time += c[0];
        maxHeap.offer(c[0]);
        if (time > c[1]) time -= maxHeap.poll();
    }
    return maxHeap.size();
}
```

---

## 21. Minimum Cost to Hire K Workers (LC 857) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Solution
```java
public double mincostToHireWorkers(int[] quality, int[] wage, int k) {
    int n = quality.length;
    double[][] workers = new double[n][2];
    for (int i = 0; i < n; i++) workers[i] = new double[]{(double)wage[i]/quality[i], quality[i]};
    Arrays.sort(workers, (a, b) -> Double.compare(a[0], b[0]));

    PriorityQueue<Double> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
    double qualitySum = 0, result = Double.MAX_VALUE;
    for (double[] w : workers) {
        qualitySum += w[1];
        maxHeap.offer(w[1]);
        if (maxHeap.size() > k) qualitySum -= maxHeap.poll();
        if (maxHeap.size() == k) result = Math.min(result, qualitySum * w[0]);
    }
    return result;
}
```

---

## 22. Find K Pairs with Smallest Sums (LC 373) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
    List<List<Integer>> result = new ArrayList<>();
    if (nums1.length == 0 || nums2.length == 0) return result;
    PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> (nums1[a[0]] + nums2[a[1]]) - (nums1[b[0]] + nums2[b[1]]));
    for (int i = 0; i < Math.min(k, nums1.length); i++) minHeap.offer(new int[]{i, 0});
    while (k-- > 0 && !minHeap.isEmpty()) {
        int[] curr = minHeap.poll();
        result.add(Arrays.asList(nums1[curr[0]], nums2[curr[1]]));
        if (curr[1] + 1 < nums2.length) minHeap.offer(new int[]{curr[0], curr[1] + 1});
    }
    return result;
}
```

---

## 23. Ugly Number II (LC 264) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public int nthUglyNumber(int n) {
    int[] dp = new int[n];
    dp[0] = 1;
    int p2 = 0, p3 = 0, p5 = 0;
    for (int i = 1; i < n; i++) {
        int next = Math.min(dp[p2]*2, Math.min(dp[p3]*3, dp[p5]*5));
        dp[i] = next;
        if (next == dp[p2]*2) p2++;
        if (next == dp[p3]*3) p3++;
        if (next == dp[p5]*5) p5++;
    }
    return dp[n-1];
}
```

---

## 24. Hand of Straights (LC 846) ⭐⭐⭐

**Difficulty**: Medium | **Frequency**: Medium

### Solution
```java
public boolean isNStraightHand(int[] hand, int groupSize) {
    if (hand.length % groupSize != 0) return false;
    TreeMap<Integer, Integer> count = new TreeMap<>();
    for (int h : hand) count.put(h, count.getOrDefault(h, 0) + 1);
    while (!count.isEmpty()) {
        int first = count.firstKey();
        for (int i = first; i < first + groupSize; i++) {
            if (!count.containsKey(i)) return false;
            count.put(i, count.get(i) - 1);
            if (count.get(i) == 0) count.remove(i);
        }
    }
    return true;
}
```

---

## 25. Minimum Interval to Include Each Query (LC 2158) ⭐⭐⭐

**Difficulty**: Hard | **Frequency**: Medium

### Solution
```java
public int[] minInterval(int[][] intervals, int[] queries) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    int n = queries.length;
    Integer[] idx = new Integer[n];
    for (int i = 0; i < n; i++) idx[i] = i;
    Arrays.sort(idx, (a, b) -> queries[a] - queries[b]);

    PriorityQueue<int[]> minHeap = new PriorityQueue<>((a, b) -> a[0] - b[0]); // [size, end]
    int[] result = new int[n];
    int i = 0;
    for (int qi : idx) {
        int q = queries[qi];
        while (i < intervals.length && intervals[i][0] <= q) {
            int size = intervals[i][1] - intervals[i][0] + 1;
            minHeap.offer(new int[]{size, intervals[i][1]});
            i++;
        }
        while (!minHeap.isEmpty() && minHeap.peek()[1] < q) minHeap.poll();
        result[qi] = minHeap.isEmpty() ? -1 : minHeap.peek()[0];
    }
    return result;
}
```

---

## Pattern Summary

| Problem | Data Structure | Key Insight |
|---------|---------------|-------------|
| Top K Frequent | Min-Heap size k | Evict smallest |
| Median Stream | Max+Min Heap | Balance halves |
| Meeting Rooms II | Min-Heap end times | Reuse rooms |
| Sliding Window Max | Monotonic Deque | O(n) not O(nk) |
| Merge K Lists | Min-Heap | Always pick smallest |
| Task Scheduler | Greedy | Idle = (maxFreq-1)*n |
