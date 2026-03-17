# Insert Delete GetRandom O(1) (LeetCode 380)

## Problem Statement

Implement the `RandomizedSet` class:
- `RandomizedSet()` Initializes the RandomizedSet object.
- `bool insert(int val)` Inserts an item val into the set if not present. Returns true if the item was not present, false otherwise.
- `bool remove(int val)` Removes an item val from the set if present. Returns true if the item was present, false otherwise.
- `int getRandom()` Returns a random element from the current set of elements (it's guaranteed that at least one element exists when this method is called). Each element must have the same probability of being returned.

**Time Complexity Requirement**: O(1) average time for each operation.

**Constraints:**
- -2^31 <= val <= 2^31 - 1
- At most 2 * 10^5 calls will be made to insert, remove, and getRandom
- There will be at least one element in the data structure when getRandom is called

## Approach 1: HashMap + ArrayList (Optimal)

### Intuition
- ArrayList: O(1) random access by index
- HashMap: O(1) lookup to check existence and get index
- Remove trick: Swap with last element, then remove last (O(1))

### Implementation

```java
class RandomizedSet {
    private List<Integer> nums;
    private Map<Integer, Integer> valToIndex;
    private Random random;
    
    public RandomizedSet() {
        nums = new ArrayList<>();
        valToIndex = new HashMap<>();
        random = new Random();
    }
    
    public boolean insert(int val) {
        if (valToIndex.containsKey(val)) {
            return false;
        }
        
        valToIndex.put(val, nums.size());
        nums.add(val);
        return true;
    }
    
    public boolean remove(int val) {
        if (!valToIndex.containsKey(val)) {
            return false;
        }
        
        // Get index of element to remove
        int index = valToIndex.get(val);
        int lastElement = nums.get(nums.size() - 1);
        
        // Swap with last element
        nums.set(index, lastElement);
        valToIndex.put(lastElement, index);
        
        // Remove last element
        nums.remove(nums.size() - 1);
        valToIndex.remove(val);
        
        return true;
    }
    
    public int getRandom() {
        int randomIndex = random.nextInt(nums.size());
        return nums.get(randomIndex);
    }
}
```

**Time Complexity**: O(1) average for all operations
**Space Complexity**: O(n)

### Pros
- Optimal O(1) time complexity
- True uniform random distribution
- Memory efficient

### Cons
- Slightly complex remove logic
- Need to maintain two data structures

---

## Approach 2: HashSet Only (Incorrect for getRandom)

### Intuition
HashSet provides O(1) insert/remove but no O(1) random access.

### Implementation

```java
class RandomizedSet {
    private Set<Integer> set;
    private Random random;
    
    public RandomizedSet() {
        set = new HashSet<>();
        random = new Random();
    }
    
    public boolean insert(int val) {
        return set.add(val);
    }
    
    public boolean remove(int val) {
        return set.remove(val);
    }
    
    public int getRandom() {
        // Convert to array - O(n) - BAD!
        Integer[] arr = set.toArray(new Integer[0]);
        return arr[random.nextInt(arr.length)];
    }
}
```

**Time Complexity**: O(1) for insert/remove, O(n) for getRandom
**Space Complexity**: O(n)

### Pros
- Simple insert/remove

### Cons
- **Does NOT meet O(1) requirement for getRandom**
- Converting to array is O(n)
- Not acceptable in interviews

---

## Approach 3: ArrayList Only (Incorrect for Remove)

### Intuition
ArrayList provides O(1) random access but O(n) remove.

### Implementation

```java
class RandomizedSet {
    private List<Integer> nums;
    private Random random;
    
    public RandomizedSet() {
        nums = new ArrayList<>();
        random = new Random();
    }
    
    public boolean insert(int val) {
        if (nums.contains(val)) { // O(n) - BAD!
            return false;
        }
        nums.add(val);
        return true;
    }
    
    public boolean remove(int val) {
        return nums.remove(Integer.valueOf(val)); // O(n) - BAD!
    }
    
    public int getRandom() {
        return nums.get(random.nextInt(nums.size()));
    }
}
```

**Time Complexity**: O(n) for insert/remove, O(1) for getRandom
**Space Complexity**: O(n)

### Pros
- Simple getRandom

### Cons
- **Does NOT meet O(1) requirement**
- contains() and remove() are O(n)
- Not acceptable in interviews

---

## Approach 4: LinkedHashSet (Incorrect for getRandom)

### Intuition
LinkedHashSet maintains insertion order but still no O(1) random access.

### Implementation

```java
class RandomizedSet {
    private LinkedHashSet<Integer> set;
    private Random random;
    
    public RandomizedSet() {
        set = new LinkedHashSet<>();
        random = new Random();
    }
    
    public boolean insert(int val) {
        return set.add(val);
    }
    
    public boolean remove(int val) {
        return set.remove(val);
    }
    
    public int getRandom() {
        int index = random.nextInt(set.size());
        Iterator<Integer> it = set.iterator();
        for (int i = 0; i < index; i++) {
            it.next(); // O(n) - BAD!
        }
        return it.next();
    }
}
```

**Time Complexity**: O(1) for insert/remove, O(n) for getRandom
**Space Complexity**: O(n)

### Pros
- Maintains insertion order
- O(1) insert/remove

### Cons
- **Does NOT meet O(1) requirement for getRandom**
- Iterator traversal is O(n)
- Not acceptable in interviews

---

## Key Insight: Why HashMap + ArrayList?

The challenge is combining three requirements:
1. **O(1) insert**: HashMap ✅, ArrayList ✅
2. **O(1) remove**: HashMap ✅, ArrayList ❌ (unless we swap with last)
3. **O(1) random access**: HashMap ❌, ArrayList ✅

**Solution**: Use BOTH!
- HashMap: Track value -> index mapping
- ArrayList: Store actual values for random access
- Remove trick: Swap with last element to avoid O(n) shift

---

## Detailed Remove Operation Walkthrough

```
Initial state:
nums = [10, 20, 30, 40]
valToIndex = {10:0, 20:1, 30:2, 40:3}

Remove 20:
1. Get index of 20: index = 1
2. Get last element: lastElement = 40
3. Swap: nums[1] = 40
   nums = [10, 40, 30, 40]
4. Update map: valToIndex[40] = 1
   valToIndex = {10:0, 40:1, 30:2, 40:3}
5. Remove last: nums.remove(3)
   nums = [10, 40, 30]
6. Remove from map: valToIndex.remove(20)
   valToIndex = {10:0, 40:1, 30:2}

Final state:
nums = [10, 40, 30]
valToIndex = {10:0, 40:1, 30:2}
```

---

## Edge Cases

### Edge Case 1: Remove Last Element
```java
// nums = [10, 20, 30], remove 30
// No swap needed, just remove last
if (index == nums.size() - 1) {
    nums.remove(nums.size() - 1);
    valToIndex.remove(val);
    return true;
}
```

### Edge Case 2: Single Element
```java
// nums = [10], remove 10
// Works correctly with swap logic
```

### Edge Case 3: Duplicate Insert
```java
// insert(10), insert(10)
// Second insert returns false
```

---

## Test Cases

```java
public class RandomizedSetTest {
    public static void main(String[] args) {
        // Test Case 1: Basic operations
        RandomizedSet set = new RandomizedSet();
        assert set.insert(1) == true;
        assert set.remove(2) == false;
        assert set.insert(2) == true;
        int random = set.getRandom();
        assert random == 1 || random == 2;
        assert set.remove(1) == true;
        assert set.insert(2) == false;
        assert set.getRandom() == 2;
        
        // Test Case 2: Multiple operations
        RandomizedSet set2 = new RandomizedSet();
        set2.insert(0);
        set2.insert(1);
        set2.remove(0);
        set2.insert(2);
        set2.remove(1);
        assert set2.getRandom() == 2;
        
        // Test Case 3: Random distribution
        RandomizedSet set3 = new RandomizedSet();
        set3.insert(1);
        set3.insert(2);
        set3.insert(3);
        
        Map<Integer, Integer> freq = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            int val = set3.getRandom();
            freq.put(val, freq.getOrDefault(val, 0) + 1);
        }
        
        // Each should appear ~3333 times (within 10% tolerance)
        for (int count : freq.values()) {
            assert Math.abs(count - 3333) < 333;
        }
        
        System.out.println("All tests passed!");
    }
}
```

---

## Follow-up Questions

1. **Q: How to make it thread-safe?**
   ```java
   class RandomizedSet {
       private final ReadWriteLock lock = new ReentrantReadWriteLock();
       
       public boolean insert(int val) {
           lock.writeLock().lock();
           try {
               // ... insert logic
           } finally {
               lock.writeLock().unlock();
           }
       }
       
       public int getRandom() {
           lock.readLock().lock();
           try {
               // ... getRandom logic
           } finally {
               lock.readLock().unlock();
           }
       }
   }
   ```

2. **Q: How to support weighted random (some elements more likely)?**
   ```java
   class WeightedRandomizedSet {
       private List<Integer> nums;
       private List<Integer> weights;
       private int totalWeight;
       
       public int getRandom() {
           int rand = random.nextInt(totalWeight);
           int sum = 0;
           for (int i = 0; i < nums.size(); i++) {
               sum += weights.get(i);
               if (rand < sum) return nums.get(i);
           }
           return nums.get(nums.size() - 1);
       }
   }
   ```

3. **Q: How to support duplicates? (LC 381)**
   - Use `Map<Integer, Set<Integer>>` to store all indices
   - Remove any one index from the set
   - See [Insert Delete GetRandom O(1) - Duplicates](./insert-delete-getrandom-duplicates.md)

4. **Q: How to implement in a distributed system?**
   - Shard data across multiple nodes
   - Use consistent hashing for distribution
   - getRandom: Pick random shard, then random element
   - Trade-off: Not perfectly uniform distribution

5. **Q: How to optimize for read-heavy workload?**
   - Use CopyOnWriteArrayList for nums
   - Reads don't need locks
   - Writes are slower but reads are faster

---

## Common Mistakes

1. ❌ Using ArrayList.remove(index) without swap - O(n)
2. ❌ Using HashSet.toArray() for getRandom - O(n)
3. ❌ Forgetting to update valToIndex after swap
4. ❌ Not handling edge case when removing last element
5. ❌ Using contains() on ArrayList - O(n)

---

## Complexity Analysis

### Why is remove() O(1)?

```
Operation breakdown:
1. valToIndex.get(val)           - O(1) HashMap lookup
2. nums.get(nums.size() - 1)     - O(1) ArrayList access
3. nums.set(index, lastElement)  - O(1) ArrayList set
4. valToIndex.put(lastElement, index) - O(1) HashMap put
5. nums.remove(nums.size() - 1)  - O(1) ArrayList remove last
6. valToIndex.remove(val)        - O(1) HashMap remove

Total: O(1)
```

### Why is getRandom() truly random?

- `random.nextInt(n)` generates uniform random in [0, n)
- ArrayList provides O(1) access by index
- Each element has exactly 1/n probability

---

## Related Problems

- [Insert Delete GetRandom O(1) - Duplicates (LC 381)](./insert-delete-getrandom-duplicates.md)
- [Random Pick Index (LC 398)](./random-pick-index.md)
- [Random Pick with Weight (LC 528)](./random-pick-weight.md)
- [Linked List Random Node (LC 382)](./linked-list-random-node.md)

---

## Real-world Applications

1. **Load Balancer**: Random server selection
2. **A/B Testing**: Random user assignment to experiments
3. **Shuffle Playlist**: Random song selection without replacement
4. **Game Development**: Random item drops
5. **Sampling**: Random sampling from large datasets
