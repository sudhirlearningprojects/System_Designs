# Fail-Fast vs Fail-Safe in Java

## Core Concept

| | Fail-Fast | Fail-Safe |
|---|---|---|
| Works on | **Original** collection | **Copy** of collection |
| Modification during iteration | Throws `ConcurrentModificationException` | Allowed |
| Memory | No extra memory | Extra memory for copy |
| Data consistency | Always consistent | May reflect stale data |
| Package | `java.util` | `java.util.concurrent` |

---

## How Fail-Fast Works

Uses an internal `modCount` counter. Every structural modification increments it. Iterator captures `expectedModCount` at creation. On every `next()` call, it checks `modCount == expectedModCount`.

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));
Iterator<String> it = list.iterator(); // expectedModCount = 3

list.add("d");  // modCount becomes 4

it.next();  // modCount(4) != expectedModCount(3) → ConcurrentModificationException
```

```
ArrayList internals:
  modCount = 3  ←── incremented on add/remove/clear

Iterator internals:
  expectedModCount = 3  ←── captured at iterator creation

  next() → if (modCount != expectedModCount) throw CME
```

### Fail-Fast Collections
- `ArrayList`, `LinkedList`, `HashMap`, `HashSet`, `TreeMap`, `TreeSet`

---

## How Fail-Safe Works

Iterator operates on a **snapshot** (copy) taken at iterator creation time. Modifications go to the original, iterator sees the old copy.

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("a", "b", "c"));
Iterator<String> it = list.iterator(); // snapshot: ["a", "b", "c"]

list.add("d");  // original becomes ["a", "b", "c", "d"]

while (it.hasNext()) {
    System.out.println(it.next()); // prints: a, b, c — NOT "d"
}
```

### Fail-Safe Collections
- `CopyOnWriteArrayList`, `CopyOnWriteArraySet`
- `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`

---

## ConcurrentHashMap — Special Case

`ConcurrentHashMap` is fail-safe but uses **weakly consistent** iteration (not a full copy). It reflects some but not necessarily all modifications made after iterator creation.

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("a", 1); map.put("b", 2);

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    map.put("c", 3);  // NO exception — weakly consistent
    System.out.println(entry.getKey()); // may or may not see "c"
}
```

---

## Safe Removal During Iteration

### Option 1: Iterator.remove() — works on fail-fast
```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove(); // safe — updates modCount
}
```

### Option 2: removeIf() — cleanest approach
```java
list.removeIf(s -> s.equals("b"));
```

### Option 3: Collect then remove
```java
list.stream()
    .filter(s -> !s.equals("b"))
    .collect(Collectors.toList());
```

### Option 4: CopyOnWriteArrayList — iterate and modify freely
```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(original);
for (String s : list) {
    if (s.equals("b")) list.remove(s); // no exception
}
```

---

## Common Pitfalls

**Pitfall 1: Enhanced for-loop is still fail-fast**
```java
for (String s : arrayList) {
    arrayList.remove(s);  // ConcurrentModificationException!
}
```

**Pitfall 2: Single-threaded modification also triggers CME**
```java
// Even without multiple threads!
for (String s : list) {
    if (s.equals("b")) list.remove(s);  // CME — modCount changed
}
```

**Pitfall 3: CopyOnWriteArrayList is expensive for write-heavy use**
```java
// Every write creates a full array copy — O(n) per write
// Only use when reads >> writes
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("x"); // copies entire array!
```

---

## Decision Guide

```
Iterating + modifying in single thread?
  └─► Use Iterator.remove() or removeIf()

Multi-threaded, read-heavy?
  └─► CopyOnWriteArrayList / CopyOnWriteArraySet

Multi-threaded, balanced read/write?
  └─► ConcurrentHashMap / ConcurrentSkipListMap

Need strong consistency during iteration?
  └─► Lock the collection manually with synchronized or ReentrantLock

Just need no CME, don't care about stale data?
  └─► Any java.util.concurrent collection
```

---

## Internal modCount Flow (Fail-Fast)

```
list.add()    ──► modCount++
list.remove() ──► modCount++
list.clear()  ──► modCount++
list.set()    ──► modCount NOT incremented (not structural)

Iterator.next():
  if (modCount != expectedModCount)
      throw new ConcurrentModificationException()
```

`set()` does **not** increment `modCount` — replacing a value is not a structural change, so it won't trigger CME.
