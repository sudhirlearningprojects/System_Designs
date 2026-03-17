# Design Hit Counter (LeetCode 362)

## Problem Statement

Design a hit counter which counts the number of hits received in the past 5 minutes (i.e., the past 300 seconds).

Your system should accept a timestamp parameter (in seconds granularity), and you may assume that calls are being made to the system in chronological order (i.e., timestamp is monotonically increasing). Several hits may arrive roughly at the same time.

Implement the `HitCounter` class:
- `HitCounter()` Initializes the object of the hit counter system.
- `void hit(int timestamp)` Records a hit that happened at timestamp (in seconds). Several hits may happen at the same timestamp.
- `int getHits(int timestamp)` Returns the number of hits in the past 5 minutes from timestamp (i.e., the past 300 seconds).

**Constraints:**
- 1 <= timestamp <= 2 * 10^9
- All the calls are being made to the system in chronological order (i.e., timestamp is monotonically increasing)
- At most 300 calls will be made to hit and getHits

## Approach 1: Queue with Timestamp Cleanup (Simple)

### Intuition
- Store all hits in a queue with timestamps
- On getHits(), remove hits older than 300 seconds
- Count remaining hits

### Implementation

```java
class HitCounter {
    private Queue<Integer> hits;
    
    public HitCounter() {
        hits = new LinkedList<>();
    }
    
    public void hit(int timestamp) {
        hits.offer(timestamp);
    }
    
    public int getHits(int timestamp) {
        // Remove hits older than 300 seconds
        while (!hits.isEmpty() && timestamp - hits.peek() >= 300) {
            hits.poll();
        }
        return hits.size();
    }
}
```

**Time Complexity**:
- hit: O(1)
- getHits: O(n) where n = number of hits in queue

**Space Complexity**: O(n) where n = total hits

### Pros
- Simple to implement
- Accurate hit counting
- Easy to understand

### Cons
- getHits can be O(n) if many old hits
- Space grows with total hits
- Not optimal for high traffic

---

## Approach 2: Circular Array (Fixed Space)

### Intuition
- Use array of size 300 (one slot per second)
- Each slot stores hit count for that second
- Use modulo to wrap around (circular buffer)

### Implementation

```java
class HitCounter {
    private int[] times;
    private int[] hits;
    
    public HitCounter() {
        times = new int[300];
        hits = new int[300];
    }
    
    public void hit(int timestamp) {
        int index = timestamp % 300;
        
        if (times[index] != timestamp) {
            // New time slot, reset
            times[index] = timestamp;
            hits[index] = 1;
        } else {
            // Same time slot, increment
            hits[index]++;
        }
    }
    
    public int getHits(int timestamp) {
        int total = 0;
        
        for (int i = 0; i < 300; i++) {
            if (timestamp - times[i] < 300) {
                total += hits[i];
            }
        }
        
        return total;
    }
}
```

**Time Complexity**:
- hit: O(1)
- getHits: O(300) = O(1)

**Space Complexity**: O(300) = O(1)

### Pros
- Fixed space O(300)
- O(1) hit operation
- O(1) getHits (constant 300 iterations)

### Cons
- Wastes space if hits are sparse
- Always iterates 300 slots
- Not suitable for different time windows

---

## Approach 3: HashMap with Timestamp Buckets

### Intuition
- Use HashMap to store timestamp -> hit count
- Only store timestamps that have hits
- Clean up old entries on getHits()

### Implementation

```java
class HitCounter {
    private Map<Integer, Integer> hitMap;
    
    public HitCounter() {
        hitMap = new HashMap<>();
    }
    
    public void hit(int timestamp) {
        hitMap.put(timestamp, hitMap.getOrDefault(timestamp, 0) + 1);
    }
    
    public int getHits(int timestamp) {
        int total = 0;
        
        // Remove old entries and count valid ones
        Iterator<Map.Entry<Integer, Integer>> it = hitMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            int time = entry.getKey();
            
            if (timestamp - time >= 300) {
                it.remove(); // Clean up old entries
            } else {
                total += entry.getValue();
            }
        }
        
        return total;
    }
}
```

**Time Complexity**:
- hit: O(1)
- getHits: O(k) where k = unique timestamps in map

**Space Complexity**: O(k) where k = unique timestamps

### Pros
- Space efficient (only stores timestamps with hits)
- Cleans up old data automatically
- Good for sparse hits

### Cons
- getHits iterates all entries
- Not optimal for dense hits
- HashMap overhead

---

## Approach 4: TreeMap with Range Query (Optimal for Queries)

### Intuition
- TreeMap maintains sorted order of timestamps
- Use tailMap() to get all entries >= (timestamp - 300)
- Efficient range queries

### Implementation

```java
class HitCounter {
    private TreeMap<Integer, Integer> hitMap;
    
    public HitCounter() {
        hitMap = new TreeMap<>();
    }
    
    public void hit(int timestamp) {
        hitMap.put(timestamp, hitMap.getOrDefault(timestamp, 0) + 1);
    }
    
    public int getHits(int timestamp) {
        // Get all entries in [timestamp - 299, timestamp]
        int startTime = timestamp - 299;
        
        // Remove entries older than 300 seconds
        hitMap.headMap(startTime).clear();
        
        // Sum all remaining hits
        int total = 0;
        for (int count : hitMap.values()) {
            total += count;
        }
        
        return total;
    }
}
```

**Time Complexity**:
- hit: O(log k) where k = unique timestamps
- getHits: O(k) where k = entries in window

**Space Complexity**: O(k)

### Pros
- Efficient range queries
- Automatic sorting
- Clean old data efficiently

### Cons
- TreeMap overhead (log n operations)
- More complex than simple solutions
- Still iterates all entries in window

---

## Approach 5: Sliding Window with Deque (Optimized)

### Intuition
- Deque stores (timestamp, cumulative count) pairs
- Maintain sliding window of 300 seconds
- Use cumulative sum for O(1) range queries

### Implementation

```java
class HitCounter {
    class Hit {
        int timestamp;
        int count;
        
        Hit(int timestamp, int count) {
            this.timestamp = timestamp;
            this.count = count;
        }
    }
    
    private Deque<Hit> window;
    private int totalHits;
    
    public HitCounter() {
        window = new LinkedList<>();
        totalHits = 0;
    }
    
    public void hit(int timestamp) {
        if (!window.isEmpty() && window.getLast().timestamp == timestamp) {
            // Same timestamp, increment count
            window.getLast().count++;
        } else {
            // New timestamp
            window.addLast(new Hit(timestamp, 1));
        }
        totalHits++;
    }
    
    public int getHits(int timestamp) {
        // Remove hits older than 300 seconds
        while (!window.isEmpty() && timestamp - window.getFirst().timestamp >= 300) {
            totalHits -= window.removeFirst().count;
        }
        
        return totalHits;
    }
}
```

**Time Complexity**:
- hit: O(1) amortized
- getHits: O(1) amortized (each element removed once)

**Space Complexity**: O(k) where k = unique timestamps in window

### Pros
- O(1) amortized for both operations
- Space efficient
- Maintains running total

### Cons
- Slightly more complex
- Amortized, not worst-case O(1)

---

## Comparison Table

| Approach | hit() | getHits() | Space | Best For |
|----------|-------|-----------|-------|----------|
| Queue | O(1) | O(n) | O(n) | Low traffic |
| Circular Array | O(1) | O(1) | O(1) | Fixed window, dense hits |
| HashMap | O(1) | O(k) | O(k) | Sparse hits |
| TreeMap | O(log k) | O(k) | O(k) | Range queries |
| Deque + Total | O(1) | O(1) amortized | O(k) | High traffic, optimal |

---

## Test Cases

```java
public class HitCounterTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        HitCounter counter = new HitCounter();
        counter.hit(1);
        counter.hit(2);
        counter.hit(3);
        assert counter.getHits(4) == 3;
        counter.hit(300);
        assert counter.getHits(300) == 4;
        assert counter.getHits(301) == 3; // Hit at timestamp 1 expired
        
        // Test Case 2: Multiple hits at same timestamp
        HitCounter counter2 = new HitCounter();
        counter2.hit(1);
        counter2.hit(1);
        counter2.hit(1);
        assert counter2.getHits(1) == 3;
        assert counter2.getHits(300) == 3;
        assert counter2.getHits(301) == 0;
        
        // Test Case 3: Sliding window
        HitCounter counter3 = new HitCounter();
        counter3.hit(1);
        counter3.hit(100);
        counter3.hit(200);
        counter3.hit(300);
        assert counter3.getHits(300) == 4;
        assert counter3.getHits(301) == 3; // Hit at 1 expired
        counter3.hit(400);
        assert counter3.getHits(400) == 4; // Hits at 1, 100 expired
        assert counter3.getHits(500) == 2; // Only 400, 300 remain
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

### 1. What if the number of hits per second could be huge?

**Solution**: Use bucketing by second
```java
class HitCounter {
    private Map<Integer, Long> secondBuckets; // timestamp -> count
    
    public void hit(int timestamp) {
        secondBuckets.put(timestamp, 
            secondBuckets.getOrDefault(timestamp, 0L) + 1);
    }
    
    public long getHits(int timestamp) {
        long total = 0;
        for (int i = timestamp - 299; i <= timestamp; i++) {
            total += secondBuckets.getOrDefault(i, 0L);
        }
        return total;
    }
}
```

### 2. What if hits are not in chronological order?

**Solution**: Use TreeMap to maintain sorted order
```java
class HitCounter {
    private TreeMap<Integer, Integer> hits;
    
    public void hit(int timestamp) {
        hits.put(timestamp, hits.getOrDefault(timestamp, 0) + 1);
    }
    
    public int getHits(int timestamp) {
        return hits.subMap(timestamp - 299, true, timestamp, true)
                   .values().stream().mapToInt(Integer::intValue).sum();
    }
}
```

### 3. How to make it thread-safe?

**Solution**: Add synchronization
```java
class HitCounter {
    private final Queue<Integer> hits = new LinkedList<>();
    private final Object lock = new Object();
    
    public void hit(int timestamp) {
        synchronized (lock) {
            hits.offer(timestamp);
        }
    }
    
    public int getHits(int timestamp) {
        synchronized (lock) {
            while (!hits.isEmpty() && timestamp - hits.peek() >= 300) {
                hits.poll();
            }
            return hits.size();
        }
    }
}
```

### 4. How to support different time windows?

**Solution**: Parameterize window size
```java
class HitCounter {
    private final int windowSize;
    private Queue<Integer> hits;
    
    public HitCounter(int windowSizeSeconds) {
        this.windowSize = windowSizeSeconds;
        this.hits = new LinkedList<>();
    }
    
    public int getHits(int timestamp) {
        while (!hits.isEmpty() && timestamp - hits.peek() >= windowSize) {
            hits.poll();
        }
        return hits.size();
    }
}
```

### 5. How to implement in distributed system?

**Solution**: Use Redis with sorted sets
```java
class DistributedHitCounter {
    private Jedis redis;
    private String key = "hit_counter";
    
    public void hit(int timestamp) {
        redis.zadd(key, timestamp, UUID.randomUUID().toString());
    }
    
    public long getHits(int timestamp) {
        // Remove old entries
        redis.zremrangeByScore(key, 0, timestamp - 300);
        
        // Count remaining
        return redis.zcard(key);
    }
}
```

---

## Real-world Optimizations

### 1. Rate Limiting Integration
```java
class RateLimitedHitCounter extends HitCounter {
    private static final int MAX_HITS_PER_MINUTE = 100;
    
    @Override
    public void hit(int timestamp) {
        int recentHits = getHits(timestamp);
        if (recentHits >= MAX_HITS_PER_MINUTE) {
            throw new RateLimitExceededException();
        }
        super.hit(timestamp);
    }
}
```

### 2. Metrics and Monitoring
```java
class MonitoredHitCounter extends HitCounter {
    private long totalHits = 0;
    private long totalGetHitsCalls = 0;
    
    @Override
    public void hit(int timestamp) {
        totalHits++;
        super.hit(timestamp);
    }
    
    @Override
    public int getHits(int timestamp) {
        totalGetHitsCalls++;
        return super.getHits(timestamp);
    }
    
    public double getAverageHitsPerSecond() {
        return totalHits / 300.0;
    }
}
```

### 3. Persistence
```java
class PersistentHitCounter extends HitCounter {
    private Database db;
    
    @Override
    public void hit(int timestamp) {
        super.hit(timestamp);
        db.insert("hits", timestamp);
    }
    
    public void restore() {
        List<Integer> hits = db.query("SELECT timestamp FROM hits WHERE timestamp > ?", 
                                      System.currentTimeMillis() / 1000 - 300);
        for (int timestamp : hits) {
            super.hit(timestamp);
        }
    }
}
```

---

## Common Mistakes

1. ❌ Not removing old hits (memory leak)
2. ❌ Using `timestamp - 300` instead of `timestamp - 299` for inclusive range
3. ❌ Not handling multiple hits at same timestamp
4. ❌ Assuming hits are always in order (follow-up question)
5. ❌ Not considering thread safety in production

---

## Related Problems

- [Logger Rate Limiter (LC 359)](./logger-rate-limiter.md)
- [Design Rate Limiter (System Design)](../../05-rate-limiting/design-rate-limiter.md)
- [Moving Average from Data Stream (LC 346)](./moving-average.md)
- [Design Log Storage System (LC 635)](./design-log-storage.md)

---

## Real-world Applications

1. **API Rate Limiting**: Track requests per user/IP
2. **Analytics**: Real-time traffic monitoring
3. **DDoS Protection**: Detect abnormal traffic patterns
4. **Metrics Collection**: Application performance monitoring
5. **Gaming**: Track player actions per time window
