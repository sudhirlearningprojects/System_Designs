# First Missing Positive (LeetCode 41)

## Problem

Given an unsorted integer array, find the smallest missing positive integer.

**Examples:**
```
Input:  [3, 4, -1, 1]  → Output: 2
Input:  [1, 2, 0]      → Output: 3
Input:  [7, 8, 9, 11]  → Output: 1
```

**Constraints:**
- O(n) time, O(1) space required
- Array can contain negatives, zeros, and duplicates

---

## Key Insight

The answer must be in range `[1, n+1]` where `n = array length`. Why? If all positions `1..n` are filled, answer is `n+1`. Otherwise it's some value in `[1, n]`.

**Strategy:** Use the array itself as a hash map — place each number `x` at index `x-1`.

---

## Solution: Cyclic Sort

```java
int firstMissingPositive(int[] nums) {
    int n = nums.length;
    for (int i = 0; i < n; i++)
        while (nums[i] > 0 && nums[i] <= n && nums[nums[i] - 1] != nums[i])
            swap(nums, i, nums[i] - 1);
    for (int i = 0; i < n; i++)
        if (nums[i] != i + 1) return i + 1;
    return n + 1;
}

void swap(int[] nums, int i, int j) {
    int tmp = nums[i]; nums[i] = nums[j]; nums[j] = tmp;
}
```

---

## Trace

**Input:** `[3, 4, -1, 1]`

| Step | i | Action | Array |
|------|---|--------|-------|
| 1 | 0 | swap(0,2): place 3 at index 2 | [-1, 4, 3, 1] |
| 2 | 0 | -1 not in [1,4], skip | [-1, 4, 3, 1] |
| 3 | 1 | swap(1,3): place 4 at index 3 | [-1, 1, 3, 4] |
| 4 | 1 | swap(1,0): place 1 at index 0 | [1, -1, 3, 4] |
| 5 | 1 | -1 not in [1,4], skip | [1, -1, 3, 4] |
| 6 | 2 | nums[2]=3, nums[2]==3, skip | [1, -1, 3, 4] |
| 7 | 3 | nums[3]=4, nums[3]==4, skip | [1, -1, 3, 4] |

**Scan:** index 1 has `-1` ≠ 2 → **answer is 2** ✅

---

## Why the While Loop Conditions?

```java
while (nums[i] > 0              // ignore negatives/zero
    && nums[i] <= n             // ignore values > n (can't place them)
    && nums[nums[i] - 1] != nums[i])  // not already in correct position (handles duplicates)
```

The third condition prevents infinite loops with duplicates like `[1, 1]`.

---

## Complexity

| | Time | Space |
|--|------|-------|
| Cyclic sort | O(n) | O(1) |

Each element is swapped at most once to its correct position → total swaps ≤ n.

---

## Edge Cases

| Input | Output | Reason |
|-------|--------|--------|
| `[1]` | 2 | All positions filled |
| `[2]` | 1 | 1 is missing |
| `[-1, -2]` | 1 | No positives |
| `[1, 1]` | 2 | Duplicates |
| `[1, 2, 3]` | 4 | All present, answer is n+1 |

---

## Common Mistakes

1. **Forgetting duplicate check** → infinite loop on `[1, 1]`
2. **Using `if` instead of `while`** → single swap doesn't fully place elements
3. **Off-by-one** → number `x` goes to index `x-1`

---

## Real-Life Software Development Use Cases

### 1. Auto-Increment ID Gap Recovery (Databases)

When rows are deleted from a database table with auto-increment IDs, gaps appear. Finding the first missing positive helps reclaim IDs:

```java
// After bulk deletes, find first reusable ID
// existing_ids = [1, 3, 5, 7] → first gap = 2
int nextReusableId = firstMissingPositive(existingIds);
```

**Used in:** PostgreSQL sequence reset, custom ID generators, ticket numbering systems.

---

### 2. Sequential Invoice/Order Number Generation

E-commerce and billing systems require sequential numbering with no gaps for audit compliance:

```java
// Invoices: [INV-001, INV-002, INV-004, INV-005]
// Missing: INV-003 → flag for audit or reassign
int missingInvoice = firstMissingPositive(invoiceNumbers);
```

**Used in:** Stripe invoice numbering, SAP billing, tax compliance systems.

---

### 3. Port/Resource Allocation in Cloud Infrastructure

When allocating ports, container IDs, or worker slots, find the first available slot:

```java
// Allocated ports: [8080, 8081, 8083, 8084]
// Offset by base: [1, 2, 4, 5] → first available = 3 → port 8082
int nextPort = BASE_PORT + firstMissingPositive(allocatedOffsets) - 1;
```

**Used in:** Kubernetes pod scheduling, Docker port mapping, AWS ECS task placement.

---

### 4. Memory/Storage Block Allocation

File systems and memory allocators track used blocks and need to find the first free block:

```java
// Used blocks: [1, 2, 4, 5, 6] → first free = 3
int freeBlock = firstMissingPositive(usedBlocks);
```

**Used in:** ext4 block allocation, Redis memory management, custom allocators.

---

### 5. Player/Session Slot Assignment (Gaming/Chat)

Multiplayer games and chat rooms assign slot numbers. When players leave, find the lowest available slot:

```java
// Active slots: [1, 2, 4, 5] → Player 3 left → assign next joiner slot 3
int nextSlot = firstMissingPositive(activeSlots);
```

**Used in:** Discord voice channels, game lobbies, Zoom participant numbering.

---

### 6. Test Case ID Management (CI/CD)

Test suites with numbered test cases need gap detection after test removals:

```java
// Test IDs: [1, 2, 5, 6, 7] → missing = 3
// Alert: "Test cases 3, 4 missing — possible regression gap"
int firstGap = firstMissingPositive(testCaseIds);
```

**Used in:** JUnit test auditing, Selenium test management, coverage gap detection.

---

### 7. Sequence Number Validation (Distributed Systems)

In message queues and event streaming, detect the first missing sequence number to identify dropped messages:

```java
// Received packets: [1, 2, 3, 5, 6, 8]
// First missing = 4 → request retransmission from offset 4
int missingSeq = firstMissingPositive(receivedSequences);
```

**Used in:** Kafka consumer lag detection, TCP packet reassembly, event sourcing gap detection.

---

### 8. Parking Spot / Locker Assignment

Physical resource management where numbered spots are occupied/released dynamically:

```java
// Occupied lockers: [1, 3, 4, 5] → assign locker 2 to next customer
int nextLocker = firstMissingPositive(occupiedLockers);
```

**Used in:** Amazon Locker assignment, parking lot systems, gym locker management.

---

### 9. Version Number Assignment (Git/Releases)

When release versions are reverted or deleted, find the next version to assign:

```java
// Existing tags: [v1, v2, v4, v5] → next patch = v3 (backfill) or v6 (append)
int missingVersion = firstMissingPositive(versionNumbers);
```

**Used in:** GitHub release management, npm package versioning, API version tracking.

---

### 10. Shard/Partition ID Assignment (Distributed Databases)

When shards are decommissioned, find the first available shard ID for new partitions:

```java
// Active shards: [1, 2, 4, 5, 6] → shard-3 was removed → reuse ID 3
int nextShardId = firstMissingPositive(activeShardIds);
```

**Used in:** Cassandra ring management, Kafka partition assignment, DynamoDB shard splitting.

---

## Summary Table

| Domain | Use Case | Why O(1) Space Matters |
|--------|----------|------------------------|
| Databases | ID gap recovery | Millions of rows, can't allocate extra array |
| Cloud Infra | Port/slot allocation | Hot path, called per container spawn |
| Networking | Sequence gap detection | Real-time packet processing |
| Gaming | Player slot assignment | Low-latency matchmaking |
| File Systems | Block allocation | Kernel-level, memory constrained |
| Distributed Systems | Shard ID reuse | Coordination-free local decision |

---

## Related Problems

- [Missing Number](https://leetcode.com/problems/missing-number/) (LC 268) — easier, range [0, n]
- [Find All Numbers Disappeared](https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/) (LC 448) — same technique
- [Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/) (LC 287) — cycle detection variant
