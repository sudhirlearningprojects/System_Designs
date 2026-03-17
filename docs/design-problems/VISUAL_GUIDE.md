# Design Problems - Visual Guide

## 🎨 Data Structure Visualizations

### LRU Cache - HashMap + Doubly Linked List

```
Initial State (capacity = 3):
┌─────────────────────────────────────┐
│         HashMap (key → node)        │
├─────────────────────────────────────┤
│  Empty                              │
└─────────────────────────────────────┘

┌──────┐      ┌──────┐
│ HEAD │◄────►│ TAIL │  (Dummy nodes)
└──────┘      └──────┘

After put(1, 10):
┌─────────────────────────────────────┐
│         HashMap                     │
├─────────────────────────────────────┤
│  1 → Node(1,10)                     │
└─────────────────────────────────────┘

┌──────┐      ┌──────────┐      ┌──────┐
│ HEAD │◄────►│ 1:10     │◄────►│ TAIL │
└──────┘      └──────────┘      └──────┘
              (Most Recent)

After put(2, 20), put(3, 30):
┌─────────────────────────────────────┐
│         HashMap                     │
├─────────────────────────────────────┤
│  1 → Node(1,10)                     │
│  2 → Node(2,20)                     │
│  3 → Node(3,30)                     │
└─────────────────────────────────────┘

┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐
│ HEAD │◄─►│ 3:30 │◄─►│ 2:20 │◄─►│ 1:10 │◄─►│ TAIL │
└──────┘   └──────┘   └──────┘   └──────┘   └──────┘
           (Most Recent)          (Least Recent)

After get(1):  // Move 1 to head
┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐
│ HEAD │◄─►│ 1:10 │◄─►│ 3:30 │◄─►│ 2:20 │◄─►│ TAIL │
└──────┘   └──────┘   └──────┘   └──────┘   └──────┘

After put(4, 40):  // Evict 2 (LRU)
┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐   ┌──────┐
│ HEAD │◄─►│ 4:40 │◄─►│ 1:10 │◄─►│ 3:30 │◄─►│ TAIL │
└──────┘   └──────┘   └──────┘   └──────┘   └──────┘
```

### LFU Cache - Multi-level Structure

```
Data Structures:
1. cache: HashMap<Key, Node>
2. freqMap: HashMap<Frequency, DoublyLinkedList>
3. minFreq: int

Example State:
┌─────────────────────────────────────┐
│         cache                       │
├─────────────────────────────────────┤
│  1 → Node(1, 10, freq=3)            │
│  2 → Node(2, 20, freq=2)            │
│  3 → Node(3, 30, freq=2)            │
│  4 → Node(4, 40, freq=1)            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│         freqMap                     │
├─────────────────────────────────────┤
│  freq=1: [4:40]                     │
│  freq=2: [3:30] ◄─► [2:20]          │
│  freq=3: [1:10]                     │
└─────────────────────────────────────┘
           ↑
        minFreq = 1

When capacity exceeded:
1. Find minFreq list (freq=1)
2. Remove LRU from that list (4:40)
3. Update minFreq if needed
```

### Insert Delete GetRandom - HashMap + ArrayList

```
Data Structures:
1. valToIndex: HashMap<Value, Index>
2. values: ArrayList<Value>

Example: [10, 20, 30, 40]

┌─────────────────────────────────────┐
│         valToIndex                  │
├─────────────────────────────────────┤
│  10 → 0                             │
│  20 → 1                             │
│  30 → 2                             │
│  40 → 3                             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│         values                      │
├─────────────────────────────────────┤
│  [0]  [1]  [2]  [3]                 │
│  10   20   30   40                  │
└─────────────────────────────────────┘

Remove 20 (Swap with Last):

Step 1: Get index of 20 → 1
Step 2: Get last element → 40
Step 3: Swap
┌─────────────────────────────────────┐
│  [0]  [1]  [2]  [3]                 │
│  10   40   30   40                  │
└─────────────────────────────────────┘

Step 4: Update map: 40 → 1
Step 5: Remove last
┌─────────────────────────────────────┐
│  [0]  [1]  [2]                      │
│  10   40   30                       │
└─────────────────────────────────────┘

Step 6: Remove 20 from map
```

### Min Stack - Two Stacks

```
Main Stack:  [5, 2, 8, 1, 3]
Min Stack:   [5, 2, 2, 1, 1]
             ↑  ↑  ↑  ↑  ↑
             │  │  │  │  └─ min(3, 1) = 1
             │  │  │  └──── min(1, 2) = 1
             │  │  └─────── min(8, 2) = 2
             │  └────────── min(2, 5) = 2
             └───────────── min(5) = 5

getMin() → minStack.peek() → 1  (O(1))

After pop():
Main Stack:  [5, 2, 8, 1]
Min Stack:   [5, 2, 2, 1]
getMin() → 1
```

### Design Hit Counter - Circular Array

```
Circular Array (size = 300):
┌───┬───┬───┬───┬───┬───┬───┬───┐
│ 0 │ 1 │ 2 │...│298│299│ 0 │...│
├───┼───┼───┼───┼───┼───┼───┼───┤
│time│time│time│...│time│time│time│...│
├───┼───┼───┼───┼───┼───┼───┼───┤
│hits│hits│hits│...│hits│hits│hits│...│
└───┴───┴───┴───┴───┴───┴───┴───┘

hit(timestamp):
  index = timestamp % 300
  if (times[index] != timestamp) {
    times[index] = timestamp
    hits[index] = 1
  } else {
    hits[index]++
  }

getHits(timestamp):
  total = 0
  for i in 0..299:
    if (timestamp - times[i] < 300):
      total += hits[i]
  return total
```

### Design Skiplist - Multi-level Linked Lists

```
Level 3:  1 ─────────────────────────► 9
          │                            │
Level 2:  1 ─────────► 4 ─────────────► 9
          │            │               │
Level 1:  1 ──► 3 ──► 4 ──► 7 ────────► 9
          │     │     │     │          │
Level 0:  1 ──► 3 ──► 4 ──► 7 ──► 8 ──► 9 ──► 12
          ↑                                    ↑
        HEAD                                 TAIL

Search for 7:
1. Start at Level 3, HEAD
2. 1 → 9 (too far), drop to Level 2
3. 1 → 4 → 9 (too far), drop to Level 1
4. 4 → 7 (found!)

Expected path length: O(log n)
```

## 🔄 Algorithm Flowcharts

### LRU Cache - get() Operation

```
┌─────────────┐
│   get(key)  │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ key in cache?   │
└────┬────────┬───┘
     │ No     │ Yes
     │        │
     ▼        ▼
┌─────────┐  ┌──────────────┐
│return -1│  │ Get node     │
└─────────┘  └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │ Remove node  │
             │ from current │
             │ position     │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │ Add node to  │
             │ head (MRU)   │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │ Return value │
             └──────────────┘
```

### Insert Delete GetRandom - remove() Operation

```
┌─────────────┐
│ remove(val) │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ val in map?     │
└────┬────────┬───┘
     │ No     │ Yes
     │        │
     ▼        ▼
┌─────────┐  ┌──────────────────┐
│return   │  │ index = map[val] │
│ false   │  └────────┬─────────┘
└─────────┘           │
                      ▼
              ┌──────────────────┐
              │ lastVal =        │
              │ list[size-1]     │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ list[index] =    │
              │ lastVal          │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ map[lastVal] =   │
              │ index            │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ list.removeLast()│
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ map.remove(val)  │
              └────────┬─────────┘
                       │
                       ▼
              ┌──────────────────┐
              │ return true      │
              └──────────────────┘
```

### Design Hit Counter - getHits() with Cleanup

```
┌──────────────────┐
│ getHits(time)    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ queue empty?     │
└────┬─────────┬───┘
     │ Yes     │ No
     │         │
     ▼         ▼
┌─────────┐  ┌──────────────────┐
│return 0 │  │ peek = queue.peek│
└─────────┘  └────────┬─────────┘
                      │
                      ▼
             ┌──────────────────┐
             │ time - peek      │
             │ >= 300?          │
             └────┬─────────┬───┘
                  │ No      │ Yes
                  │         │
                  ▼         ▼
          ┌──────────┐  ┌──────────┐
          │ Break    │  │queue.poll│
          │ loop     │  └────┬─────┘
          └────┬─────┘       │
               │             │
               │◄────────────┘
               │ (Loop back)
               │
               ▼
          ┌──────────────┐
          │ return       │
          │ queue.size() │
          └──────────────┘
```

## 📊 Complexity Comparison Charts

### Time Complexity Comparison

```
Operation: get/search
┌─────────────────────────────────────┐
│ O(1)    ████████████████████████    │ HashMap, LRU, LFU, Min Stack
│ O(log n)████████                    │ TreeMap, Skiplist
│ O(n)    ████                        │ ArrayList.contains()
└─────────────────────────────────────┘

Operation: insert/add
┌─────────────────────────────────────┐
│ O(1)    ████████████████████████    │ HashMap, LRU, LFU, Min Stack
│ O(log n)████████                    │ TreeMap, Skiplist
│ O(n)    ████                        │ ArrayList.add(index)
└─────────────────────────────────────┘

Operation: delete/remove
┌─────────────────────────────────────┐
│ O(1)    ████████████████████████    │ HashMap, LRU, LFU (with swap)
│ O(log n)████████                    │ TreeMap, Skiplist
│ O(n)    ████                        │ ArrayList.remove(index)
└─────────────────────────────────────┘
```

### Space Complexity Comparison

```
┌─────────────────────────────────────┐
│ O(1)    ████                        │ Circular Array (fixed)
│ O(n)    ████████████████████        │ HashMap, ArrayList
│ O(n log n) ████████                 │ Skiplist
│ O(N*L)  ████                        │ Trie (N=words, L=length)
└─────────────────────────────────────┘
```

## 🎯 Pattern Decision Tree

```
Need O(1) operations?
├─ Yes
│  ├─ Need random access?
│  │  ├─ Yes → HashMap + ArrayList
│  │  └─ No → HashMap + LinkedList
│  │
│  └─ Need to maintain order?
│     ├─ Yes → HashMap + DLL
│     └─ No → HashMap only
│
└─ No
   ├─ Need sorted data?
   │  ├─ Yes → TreeMap
   │  └─ No → ArrayList
   │
   └─ Need prefix matching?
      └─ Yes → Trie

Need time-based operations?
├─ Fixed window → Circular Array
├─ Sliding window → Queue with cleanup
└─ Range queries → TreeMap

Need probabilistic structure?
└─ Skiplist (O(log n) expected)
```

## 🔍 Problem Selection Guide

```
Interview Question Type:
│
├─ "Design a cache"
│  ├─ LRU eviction → LRU Cache
│  ├─ LFU eviction → LFU Cache
│  └─ No eviction → HashMap
│
├─ "Design with O(1) operations"
│  ├─ Random access needed → HashMap + ArrayList
│  ├─ Order matters → HashMap + DLL
│  └─ Min/Max tracking → Two Stacks
│
├─ "Design for strings"
│  ├─ Prefix matching → Trie
│  ├─ Wildcard search → Trie + DFS
│  └─ Autocomplete → Trie + PriorityQueue
│
├─ "Design time-based system"
│  ├─ Fixed window → Circular Array
│  ├─ Sliding window → Queue
│  └─ Range queries → TreeMap
│
└─ "Design social feature"
   ├─ News feed → HashMap + PriorityQueue
   ├─ Follow system → HashMap<User, Set<User>>
   └─ Timeline → TreeMap (timestamp → post)
```

## 📈 Learning Path Visualization

```
Week 1: Foundations
┌─────────────────────────────────────┐
│ Day 1-2: LRU Cache                  │
│   ├─ HashMap + DLL pattern          │
│   └─ Swap-with-last technique       │
├─────────────────────────────────────┤
│ Day 3: Min Stack                    │
│   └─ Auxiliary data structure       │
├─────────────────────────────────────┤
│ Day 4: Insert Delete GetRandom      │
│   └─ HashMap + ArrayList            │
├─────────────────────────────────────┤
│ Day 5: Design HashMap               │
│   └─ Array + chaining               │
└─────────────────────────────────────┘
         ↓
Week 2: Advanced
┌─────────────────────────────────────┐
│ Day 1-2: LFU Cache                  │
│   └─ Multi-level indexing           │
├─────────────────────────────────────┤
│ Day 3: Design Hit Counter           │
│   └─ Sliding window                 │
├─────────────────────────────────────┤
│ Day 4: Design Twitter               │
│   └─ K-way merge                    │
└─────────────────────────────────────┘
         ↓
Week 3: Mastery
┌─────────────────────────────────────┐
│ Day 1: Trie problems                │
│ Day 2: Skiplist                     │
│ Day 3-4: System design              │
│ Day 5-7: Mock interviews            │
└─────────────────────────────────────┘
```

---

**Tip**: Print these visualizations and keep them handy while studying!
