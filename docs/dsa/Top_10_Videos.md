# Top 10 Videos by Watch Rate

## Problem Statement

Given a list of tuples containing video names and their watch rates, return the top 10 videos by total watch rates. Video names may appear multiple times and their rates should be aggregated.

**Input:** List of tuples (video_name, watch_rate)  
**Output:** List of top 10 video names sorted by total watch rate (descending)

**Example:**
```
Input:  [('abc', 10), ('def', 15), ('ghi', 10), ('abc', 12), ('xyz', 100)]
Output: ['xyz', 'abc', 'def', 'ghi']

Explanation:
  xyz: 100
  abc: 10 + 12 = 22
  def: 15
  ghi: 10
```

---

## Minimal Solution

```java
import java.util.*;
import java.util.stream.*;

public class TopVideos {
    
    public static List<String> getTop10Videos(List<VideoRate> videos) {
        return videos.stream()
            .collect(Collectors.groupingBy(
                v -> v.name,
                Collectors.summingInt(v -> v.rate)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    static class VideoRate {
        String name;
        int rate;
        
        VideoRate(String name, int rate) {
            this.name = name;
            this.rate = rate;
        }
    }
    
    public static void main(String[] args) {
        List<VideoRate> videos = List.of(
            new VideoRate("abc", 10),
            new VideoRate("def", 15),
            new VideoRate("ghi", 10),
            new VideoRate("abc", 12),
            new VideoRate("xyz", 100)
        );
        
        System.out.println(getTop10Videos(videos));
        // Output: [xyz, abc, def, ghi]
    }
}
```

---

## Alternative: HashMap Approach

```java
public static List<String> getTop10VideosHashMap(List<VideoRate> videos) {
    Map<String, Integer> rateMap = new HashMap<>();
    
    for (VideoRate v : videos) {
        rateMap.merge(v.name, v.rate, Integer::sum);
    }
    
    return rateMap.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

---

## Python Solution

```python
def get_top_10_videos(videos):
    from collections import defaultdict
    
    rates = defaultdict(int)
    for name, rate in videos:
        rates[name] += rate
    
    return sorted(rates.keys(), key=lambda x: rates[x], reverse=True)[:10]

# Example
L = [('abc', 10), ('def', 15), ('ghi', 10), ('abc', 12), ('xyz', 100)]
print(get_top_10_videos(L))
# Output: ['xyz', 'abc', 'def', 'ghi']
```

---

## Python One-Liner

```python
def get_top_10_videos(videos):
    from collections import Counter
    return [name for name, _ in Counter(dict(videos)).most_common(10)]
```

**Note:** This assumes unique names in input. For duplicates:

```python
def get_top_10_videos(videos):
    from collections import defaultdict
    rates = defaultdict(int)
    for name, rate in videos:
        rates[name] += rate
    return [name for name, _ in sorted(rates.items(), key=lambda x: x[1], reverse=True)[:10]]
```

---

## Complete Java Implementation

```java
import java.util.*;
import java.util.stream.*;

public class Solution {
    
    static class VideoRate {
        String name;
        int rate;
        
        VideoRate(String name, int rate) {
            this.name = name;
            this.rate = rate;
        }
    }
    
    // Approach 1: Stream API (Most concise)
    public static List<String> getTop10Videos(List<VideoRate> videos) {
        return videos.stream()
            .collect(Collectors.groupingBy(
                v -> v.name,
                Collectors.summingInt(v -> v.rate)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    // Approach 2: HashMap + Sorting
    public static List<String> getTop10VideosHashMap(List<VideoRate> videos) {
        Map<String, Integer> rateMap = new HashMap<>();
        
        for (VideoRate v : videos) {
            rateMap.merge(v.name, v.rate, Integer::sum);
        }
        
        return rateMap.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    // Approach 3: PriorityQueue (Min-Heap for top K)
    public static List<String> getTop10VideosPQ(List<VideoRate> videos) {
        Map<String, Integer> rateMap = new HashMap<>();
        
        for (VideoRate v : videos) {
            rateMap.merge(v.name, v.rate, Integer::sum);
        }
        
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
            Comparator.comparingInt(Map.Entry::getValue)
        );
        
        for (Map.Entry<String, Integer> entry : rateMap.entrySet()) {
            pq.offer(entry);
            if (pq.size() > 10) {
                pq.poll();
            }
        }
        
        List<String> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(0, pq.poll().getKey());
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        List<VideoRate> videos = List.of(
            new VideoRate("abc", 10),
            new VideoRate("def", 15),
            new VideoRate("ghi", 10),
            new VideoRate("abc", 12),
            new VideoRate("xyz", 100),
            new VideoRate("mno", 50),
            new VideoRate("pqr", 30),
            new VideoRate("stu", 25),
            new VideoRate("vwx", 20),
            new VideoRate("abc", 5)
        );
        
        System.out.println("Stream API: " + getTop10Videos(videos));
        System.out.println("HashMap:    " + getTop10VideosHashMap(videos));
        System.out.println("PriorityQueue: " + getTop10VideosPQ(videos));
        
        // Test with more than 10 unique videos
        List<VideoRate> manyVideos = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            manyVideos.add(new VideoRate("video" + i, i * 10));
        }
        
        System.out.println("\nTop 10 from 20 videos:");
        System.out.println(getTop10Videos(manyVideos));
    }
}
```

---

## Complexity Analysis

| Approach | Time | Space | Notes |
|----------|------|-------|-------|
| Stream API | O(n + k log k) | O(k) | k = unique videos |
| HashMap + Sort | O(n + k log k) | O(k) | Same as stream |
| PriorityQueue | O(n log 10) | O(k) | **Best for large k** |

**Where:**
- n = total number of entries
- k = number of unique videos

**PriorityQueue Advantage:**
- When k >> 10, PriorityQueue is more efficient
- O(n log 10) vs O(n + k log k)
- Maintains only top 10 in memory

---

## Test Cases

```java
@Test
public void testTop10Videos() {
    // Test 1: Basic case
    List<VideoRate> videos1 = List.of(
        new VideoRate("abc", 10),
        new VideoRate("def", 15),
        new VideoRate("ghi", 10),
        new VideoRate("abc", 12),
        new VideoRate("xyz", 100)
    );
    List<String> result1 = getTop10Videos(videos1);
    assertEquals("xyz", result1.get(0));
    assertEquals("abc", result1.get(1));
    
    // Test 2: Exactly 10 videos
    List<VideoRate> videos2 = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        videos2.add(new VideoRate("video" + i, i * 10));
    }
    assertEquals(10, getTop10Videos(videos2).size());
    
    // Test 3: More than 10 videos
    List<VideoRate> videos3 = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
        videos3.add(new VideoRate("video" + i, i * 10));
    }
    assertEquals(10, getTop10Videos(videos3).size());
    
    // Test 4: Less than 10 videos
    List<VideoRate> videos4 = List.of(
        new VideoRate("a", 10),
        new VideoRate("b", 20),
        new VideoRate("c", 30)
    );
    assertEquals(3, getTop10Videos(videos4).size());
    
    // Test 5: Duplicate aggregation
    List<VideoRate> videos5 = List.of(
        new VideoRate("same", 10),
        new VideoRate("same", 20),
        new VideoRate("same", 30)
    );
    List<String> result5 = getTop10Videos(videos5);
    assertEquals(1, result5.size());
    assertEquals("same", result5.get(0));
}
```

---

## Key Points

1. **Aggregate duplicates:** Sum watch rates for same video name
2. **Sort descending:** Highest watch rate first
3. **Limit to 10:** Return at most 10 videos
4. **Handle edge cases:** Less than 10 unique videos

---

## Edge Cases

1. **Empty list:** Return empty list
2. **Less than 10 videos:** Return all videos sorted
3. **Exactly 10 videos:** Return all 10 sorted
4. **More than 10 videos:** Return top 10
5. **All same video:** Return single video
6. **Tie in rates:** Order doesn't matter (or use name as tiebreaker)

---

## Optimization for Large Data

```java
// For very large datasets, use parallel stream
public static List<String> getTop10VideosParallel(List<VideoRate> videos) {
    return videos.parallelStream()
        .collect(Collectors.groupingByConcurrent(
            v -> v.name,
            Collectors.summingInt(v -> v.rate)
        ))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

---

## Summary

**Recommended Solution:** Stream API approach
- Clean and concise
- Functional style
- Easy to understand
- Good performance for most cases

**For very large datasets:** PriorityQueue approach
- O(n log 10) instead of O(n + k log k)
- Memory efficient (only stores top 10)
