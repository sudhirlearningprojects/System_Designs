# Top K Frequent Elements

## Problem Statement
Given an integer array and an integer k, return the k most frequent elements. The answer can be returned in any order.

**Example 1**:
```
Input: nums = [1,1,1,2,2,3], k = 2
Output: [1,2]
Explanation: 1 appears 3 times, 2 appears 2 times, 3 appears 1 time
```

**Example 2**:
```
Input: nums = [1], k = 1
Output: [1]
```

## Implementations

### Solution 1: HashMap + Sorting (Simple Approach)

```java
import java.util.*;

public class TopKFrequentSorting {
    
    public static int[] topKFrequent(int[] nums, int k) {
        // Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // Sort by frequency
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        
        // Get top k elements
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = entries.get(i).getKey();
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k))); // [1, 2]
    }
}
```

**Time Complexity**: O(n log n) - sorting dominates  
**Space Complexity**: O(n) - HashMap storage

---

### Solution 2: HashMap + Min Heap (Optimal)

```java
import java.util.*;

public class TopKFrequentHeap {
    
    public static int[] topKFrequent(int[] nums, int k) {
        // Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // Min heap of size k (keep k most frequent)
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap = 
            new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
        
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                minHeap.poll(); // Remove least frequent
            }
        }
        
        // Extract elements from heap
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll().getKey();
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k))); // [2, 1] or [1, 2]
    }
}
```

**Time Complexity**: O(n log k) - heap operations  
**Space Complexity**: O(n) - HashMap + O(k) heap

---

### Solution 3: Bucket Sort (Most Optimal)

```java
import java.util.*;

public class TopKFrequentBucket {
    
    public static int[] topKFrequent(int[] nums, int k) {
        // Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // Bucket sort: index = frequency, value = list of numbers
        List<Integer>[] buckets = new List[nums.length + 1];
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            int freq = entry.getValue();
            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }
            buckets[freq].add(entry.getKey());
        }
        
        // Collect top k from highest frequency buckets
        int[] result = new int[k];
        int index = 0;
        for (int i = buckets.length - 1; i >= 0 && index < k; i--) {
            if (buckets[i] != null) {
                for (int num : buckets[i]) {
                    result[index++] = num;
                    if (index == k) break;
                }
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k))); // [1, 2]
    }
}
```

**Time Complexity**: O(n) - linear time  
**Space Complexity**: O(n) - HashMap + buckets

---

### Solution 4: HashMap + Max Heap (Alternative)

```java
import java.util.*;

public class TopKFrequentMaxHeap {
    
    public static int[] topKFrequent(int[] nums, int k) {
        // Count frequencies
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        // Max heap - get k largest
        PriorityQueue<Map.Entry<Integer, Integer>> maxHeap = 
            new PriorityQueue<>((a, b) -> b.getValue() - a.getValue());
        
        maxHeap.addAll(freqMap.entrySet());
        
        // Extract top k
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll().getKey();
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k))); // [1, 2]
    }
}
```

**Time Complexity**: O(n log n) - heap construction  
**Space Complexity**: O(n) - HashMap + heap

---

### Solution 5: Using Java Streams

```java
import java.util.*;
import java.util.stream.Collectors;

public class TopKFrequentStream {
    
    public static int[] topKFrequent(int[] nums, int k) {
        return Arrays.stream(nums)
                .boxed()
                .collect(Collectors.groupingBy(n -> n, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(k)
                .mapToInt(Map.Entry::getKey)
                .toArray();
    }
    
    public static void main(String[] args) {
        int[] nums = {1, 1, 1, 2, 2, 3};
        int k = 2;
        System.out.println(Arrays.toString(topKFrequent(nums, k))); // [1, 2]
    }
}
```

**Time Complexity**: O(n log n) - sorting  
**Space Complexity**: O(n) - intermediate collections

---

## Dry Run Example 1

**Input**: `nums = [1, 1, 1, 2, 2, 3]`, `k = 2`

### Using Min Heap Approach:

**Step 1: Build Frequency Map**
```
Iteration 1: num=1, freqMap = {1: 1}
Iteration 2: num=1, freqMap = {1: 2}
Iteration 3: num=1, freqMap = {1: 3}
Iteration 4: num=2, freqMap = {1: 3, 2: 1}
Iteration 5: num=2, freqMap = {1: 3, 2: 2}
Iteration 6: num=3, freqMap = {1: 3, 2: 2, 3: 1}

Final freqMap: {1: 3, 2: 2, 3: 1}
```

**Step 2: Build Min Heap (size k=2)**
```
Process (1, 3):
  minHeap = [(1, 3)]
  size = 1 <= k, no removal

Process (2, 2):
  minHeap = [(2, 2), (1, 3)]  // Min heap by frequency
  size = 2 <= k, no removal

Process (3, 1):
  minHeap = [(3, 1), (2, 2), (1, 3)]
  size = 3 > k, remove min (3, 1)
  minHeap = [(2, 2), (1, 3)]

Final minHeap: [(2, 2), (1, 3)]
```

**Step 3: Extract Result**
```
Poll: (2, 2) → result[0] = 2
Poll: (1, 3) → result[1] = 1

Result: [2, 1] or [1, 2] (order may vary)
```

---

## Dry Run Example 2

**Input**: `nums = [4, 4, 4, 5, 5, 6, 6, 6, 6, 7]`, `k = 3`

### Using Bucket Sort Approach:

**Step 1: Build Frequency Map**
```
freqMap = {
  4: 3,  // appears 3 times
  5: 2,  // appears 2 times
  6: 4,  // appears 4 times
  7: 1   // appears 1 time
}
```

**Step 2: Create Buckets (index = frequency)**
```
buckets[0] = null
buckets[1] = [7]        // 7 appears 1 time
buckets[2] = [5]        // 5 appears 2 times
buckets[3] = [4]        // 4 appears 3 times
buckets[4] = [6]        // 6 appears 4 times
buckets[5..10] = null
```

**Step 3: Collect Top K from Highest Frequency**
```
Start from buckets[10] down to buckets[0]

i=10: buckets[10] = null, skip
i=9:  buckets[9] = null, skip
...
i=4:  buckets[4] = [6]
      result[0] = 6, index = 1

i=3:  buckets[3] = [4]
      result[1] = 4, index = 2

i=2:  buckets[2] = [5]
      result[2] = 5, index = 3
      index == k, STOP

Result: [6, 4, 5]
```

**Verification**:
- 6 appears 4 times (most frequent) ✓
- 4 appears 3 times (2nd most frequent) ✓
- 5 appears 2 times (3rd most frequent) ✓

---

## Edge Test Cases

```java
// Test Case 1: Single element
int[] arr1 = {1};
int k1 = 1;
int[] result1 = topKFrequent(arr1, k1);
// Expected: [1]

// Test Case 2: All elements same
int[] arr2 = {5, 5, 5, 5};
int k2 = 1;
int[] result2 = topKFrequent(arr2, k2);
// Expected: [5]

// Test Case 3: All elements unique, k = array length
int[] arr3 = {1, 2, 3, 4, 5};
int k3 = 5;
int[] result3 = topKFrequent(arr3, k3);
// Expected: [1, 2, 3, 4, 5] (any order)

// Test Case 4: k = 1 (most frequent only)
int[] arr4 = {1, 1, 1, 2, 2, 3};
int k4 = 1;
int[] result4 = topKFrequent(arr4, k4);
// Expected: [1]

// Test Case 5: Multiple elements with same frequency
int[] arr5 = {1, 1, 2, 2, 3, 3};
int k5 = 2;
int[] result5 = topKFrequent(arr5, k5);
// Expected: Any 2 elements (e.g., [1, 2] or [2, 3] or [1, 3])

// Test Case 6: Negative numbers
int[] arr6 = {-1, -1, -2, -2, -2, 3};
int k6 = 2;
int[] result6 = topKFrequent(arr6, k6);
// Expected: [-2, -1]

// Test Case 7: Large k (k = unique elements)
int[] arr7 = {1, 1, 2, 2, 3, 3};
int k7 = 3;
int[] result7 = topKFrequent(arr7, k7);
// Expected: [1, 2, 3] (any order)

// Test Case 8: Two elements
int[] arr8 = {1, 2};
int k8 = 2;
int[] result8 = topKFrequent(arr8, k8);
// Expected: [1, 2] (any order)

// Test Case 9: Large array with few unique elements
int[] arr9 = new int[1000];
Arrays.fill(arr9, 0, 500, 1);
Arrays.fill(arr9, 500, 1000, 2);
int k9 = 2;
int[] result9 = topKFrequent(arr9, k9);
// Expected: [1, 2] or [2, 1]

// Test Case 10: Zero values
int[] arr10 = {0, 0, 0, 1, 1, 2};
int k10 = 2;
int[] result10 = topKFrequent(arr10, k10);
// Expected: [0, 1]
```

---

## Complexity Comparison

| Approach | Time Complexity | Space Complexity | Best For |
|----------|----------------|------------------|----------|
| Sorting | O(n log n) | O(n) | Simple implementation |
| Min Heap | O(n log k) | O(n + k) | When k << n |
| Bucket Sort | O(n) | O(n) | **Optimal solution** |
| Max Heap | O(n log n) | O(n) | When k is large |
| Streams | O(n log n) | O(n) | Readable code |

---

## When to Use Each Approach

### Use **Bucket Sort** when:
- Need optimal O(n) time complexity
- Frequency range is bounded by array length
- Best for interviews and production

### Use **Min Heap** when:
- k is much smaller than n (k << n)
- Memory is a concern (only stores k elements in heap)
- Need to process stream of data

### Use **Sorting** when:
- Simplicity is priority
- Array size is small
- Quick prototype needed

### Use **Max Heap** when:
- k is close to n
- Need all elements sorted by frequency

---

## Real-World Use Cases

### 1. **E-commerce: Top Selling Products**
```java
// Find top k best-selling products
int[] productIds = {101, 101, 102, 103, 101, 102, 104};
int[] topProducts = topKFrequent(productIds, 3);
```

### 2. **Social Media: Trending Hashtags**
```java
// Find top k trending hashtags
int[] hashtagIds = {1, 1, 1, 2, 2, 3, 4, 4, 4, 4};
int[] trending = topKFrequent(hashtagIds, 2);
```

### 3. **Web Analytics: Most Visited Pages**
```java
// Find top k most visited pages
int[] pageIds = getPageVisits();
int[] topPages = topKFrequent(pageIds, 10);
```

### 4. **Music Streaming: Top Songs**
```java
// Find top k most played songs
int[] songIds = getUserPlayHistory();
int[] topSongs = topKFrequent(songIds, 5);
```

### 5. **Network Security: Most Common Attack IPs**
```java
// Find top k attacking IP addresses
int[] ipHashes = getAttackLogs();
int[] topAttackers = topKFrequent(ipHashes, 20);
```

### 6. **Search Engine: Popular Queries**
```java
// Find top k search queries
int[] queryIds = getSearchLogs();
int[] popularQueries = topKFrequent(queryIds, 100);
```

---

## Complete Working Example

```java
import java.util.*;

public class TopKFrequentComplete {
    
    // Bucket Sort - O(n) time
    public static int[] topKFrequentBucket(int[] nums, int k) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        List<Integer>[] buckets = new List[nums.length + 1];
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            int freq = entry.getValue();
            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }
            buckets[freq].add(entry.getKey());
        }
        
        int[] result = new int[k];
        int index = 0;
        for (int i = buckets.length - 1; i >= 0 && index < k; i--) {
            if (buckets[i] != null) {
                for (int num : buckets[i]) {
                    result[index++] = num;
                    if (index == k) break;
                }
            }
        }
        return result;
    }
    
    // Min Heap - O(n log k) time
    public static int[] topKFrequentHeap(int[] nums, int k) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }
        
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap = 
            new PriorityQueue<>((a, b) -> a.getValue() - b.getValue());
        
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }
        
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll().getKey();
        }
        return result;
    }
    
    public static void main(String[] args) {
        // Test Case 1
        int[] nums1 = {1, 1, 1, 2, 2, 3};
        int k1 = 2;
        System.out.println("Test 1 - Bucket Sort: " + 
            Arrays.toString(topKFrequentBucket(nums1, k1)));
        System.out.println("Test 1 - Min Heap: " + 
            Arrays.toString(topKFrequentHeap(nums1, k1)));
        
        // Test Case 2
        int[] nums2 = {4, 4, 4, 5, 5, 6, 6, 6, 6, 7};
        int k2 = 3;
        System.out.println("\nTest 2 - Bucket Sort: " + 
            Arrays.toString(topKFrequentBucket(nums2, k2)));
        System.out.println("Test 2 - Min Heap: " + 
            Arrays.toString(topKFrequentHeap(nums2, k2)));
        
        // Test Case 3: Edge case - all same
        int[] nums3 = {5, 5, 5, 5};
        int k3 = 1;
        System.out.println("\nTest 3 - All Same: " + 
            Arrays.toString(topKFrequentBucket(nums3, k3)));
        
        // Test Case 4: Edge case - negative numbers
        int[] nums4 = {-1, -1, -2, -2, -2, 3};
        int k4 = 2;
        System.out.println("\nTest 4 - Negative Numbers: " + 
            Arrays.toString(topKFrequentBucket(nums4, k4)));
    }
}
```

**Output**:
```
Test 1 - Bucket Sort: [1, 2]
Test 1 - Min Heap: [2, 1]

Test 2 - Bucket Sort: [6, 4, 5]
Test 2 - Min Heap: [5, 4, 6]

Test 3 - All Same: [5]

Test 4 - Negative Numbers: [-2, -1]
```

---

## Key Takeaways

1. **Bucket Sort is optimal**: O(n) time, best for interviews
2. **Min Heap is practical**: O(n log k) when k << n
3. **Frequency map is essential**: All solutions start with counting
4. **Order doesn't matter**: Problem allows any order in result
5. **Handle ties carefully**: Multiple elements can have same frequency

---

## Interview Tips

1. **Start with brute force**: Explain sorting approach first
2. **Optimize step-by-step**: Move from O(n log n) to O(n log k) to O(n)
3. **Discuss trade-offs**: Time vs space, simplicity vs performance
4. **Ask clarifying questions**: 
   - Can elements be negative?
   - What if multiple elements have same frequency?
   - Is k always valid (k <= unique elements)?
5. **Test edge cases**: Empty array, k=1, all same frequency
