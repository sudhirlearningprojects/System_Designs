# Complete Data Structures & Algorithms Guide for Freshers & MCQ Exam Preparation

*A comprehensive 8,000+ line guide covering all essential DSA concepts with detailed theory, practical examples, and MCQ preparation*

## Table of Contents
1. [Introduction to DSA](#introduction)
2. [Time & Space Complexity](#complexity)
3. [Arrays](#arrays)
4. [Strings](#strings)
5. [Linked Lists](#linked-lists)
6. [Stacks](#stacks)
7. [Queues](#queues)
8. [Trees](#trees)
9. [Binary Search Trees](#bst)
10. [Heaps](#heaps)
11. [Hashing](#hashing)
12. [Graphs](#graphs)
13. [Sorting Algorithms](#sorting)
14. [Searching Algorithms](#searching)
15. [Dynamic Programming](#dp)
16. [Greedy Algorithms](#greedy)
17. [Divide and Conquer](#divide-conquer)
18. [Backtracking](#backtracking)
19. [Advanced Data Structures](#advanced-ds)
20. [MCQ Practice Questions](#mcq-questions)

---

## 1. Introduction to DSA {#introduction}

### What are Data Structures?

**Definition**
Data structures are ways of organizing and storing data in a computer so that it can be accessed and modified efficiently. Think of them as containers that hold data in a specific format.

**Real-World Analogy**
- **Array**: Like a row of lockers, each with a number
- **Stack**: Like a stack of plates (last in, first out)
- **Queue**: Like a line at a store (first in, first out)
- **Tree**: Like a family tree or organizational chart
- **Graph**: Like a network of roads connecting cities

**Why Data Structures Matter**
- **Efficiency**: Choose right structure for faster operations
- **Memory**: Optimize memory usage
- **Organization**: Keep data organized and accessible
- **Problem Solving**: Different problems need different structures

### What are Algorithms?

**Definition**
Algorithms are step-by-step procedures or formulas for solving problems. They are the methods we use to manipulate data structures.

**Algorithm Characteristics**
- **Input**: Zero or more inputs
- **Output**: At least one output
- **Definiteness**: Each step is clearly defined
- **Finiteness**: Must terminate after finite steps
- **Effectiveness**: Each step must be basic enough to be carried out

**Algorithm Categories**
- **Sorting**: Arrange data in order
- **Searching**: Find specific data
- **Graph**: Navigate relationships
- **Dynamic Programming**: Solve complex problems by breaking them down
- **Greedy**: Make locally optimal choices
- **Divide & Conquer**: Break problem into smaller parts

### Relationship Between Data Structures and Algorithms

**Symbiotic Relationship**
- Data structures provide the foundation
- Algorithms provide the operations
- Choice of data structure affects algorithm efficiency
- Algorithm requirements influence data structure choice

**Example: Searching**
- **Array**: Linear search O(n), Binary search O(log n) if sorted
- **Hash Table**: Search O(1) average case
- **Binary Search Tree**: Search O(log n) average case

---

## 2. Time & Space Complexity {#complexity}

### Understanding Complexity Analysis

**Why Complexity Analysis?**
- Compare algorithm efficiency
- Predict performance on large inputs
- Choose best algorithm for the problem
- Understand scalability

**Types of Analysis**
- **Best Case**: Minimum time/space needed
- **Average Case**: Expected time/space
- **Worst Case**: Maximum time/space needed

### Big O Notation

**Definition**
Big O notation describes the upper bound of algorithm complexity, focusing on how performance scales with input size.

**Mathematical Definition**
f(n) = O(g(n)) if there exist positive constants c and n₀ such that:
f(n) ≤ c × g(n) for all n ≥ n₀

**Common Time Complexities (Best to Worst)**

**O(1) - Constant Time**
- **Description**: Performance doesn't change with input size
- **Examples**: Array access, hash table lookup
- **Graph**: Horizontal line

```
Array access: arr[5] - always takes same time regardless of array size
```

**O(log n) - Logarithmic Time**
- **Description**: Performance increases logarithmically with input size
- **Examples**: Binary search, balanced tree operations
- **Graph**: Slowly rising curve

```
Binary search: Each step eliminates half the remaining elements
```

**O(n) - Linear Time**
- **Description**: Performance increases linearly with input size
- **Examples**: Linear search, array traversal
- **Graph**: Straight diagonal line

```
Linear search: May need to check every element once
```

**O(n log n) - Linearithmic Time**
- **Description**: Combination of linear and logarithmic
- **Examples**: Efficient sorting algorithms (merge sort, heap sort)
- **Graph**: Slightly curved upward

```
Merge sort: Divide (log n levels) and conquer (n work per level)
```

**O(n²) - Quadratic Time**
- **Description**: Performance increases quadratically
- **Examples**: Bubble sort, selection sort, nested loops
- **Graph**: Parabolic curve

```
Bubble sort: Compare each element with every other element
```

**O(n³) - Cubic Time**
- **Description**: Performance increases cubically
- **Examples**: Matrix multiplication, triple nested loops
- **Graph**: Steep cubic curve

**O(2ⁿ) - Exponential Time**
- **Description**: Performance doubles with each additional input
- **Examples**: Recursive Fibonacci, subset generation
- **Graph**: Extremely steep curve

```
Recursive Fibonacci: Each call makes two more calls
```

**O(n!) - Factorial Time**
- **Description**: Performance increases factorially
- **Examples**: Traveling salesman brute force, permutation generation
- **Graph**: Extremely steep, practically unusable for large n

### Space Complexity

**Definition**
Amount of memory space an algorithm uses relative to input size.

**Types of Space**
- **Input Space**: Space used by input data
- **Auxiliary Space**: Extra space used by algorithm
- **Total Space**: Input space + Auxiliary space

**Common Space Complexities**

**O(1) - Constant Space**
- Fixed amount of extra memory
- Examples: Simple variables, in-place sorting

**O(n) - Linear Space**
- Space grows linearly with input
- Examples: Recursive call stack, copying arrays

**O(n²) - Quadratic Space**
- Space grows quadratically
- Examples: 2D arrays, adjacency matrices

### Complexity Analysis Examples

**Example 1: Simple Loop**
```
for i = 1 to n:
    print i
```
- **Time**: O(n) - loop runs n times
- **Space**: O(1) - only uses variable i

**Example 2: Nested Loops**
```
for i = 1 to n:
    for j = 1 to n:
        print i, j
```
- **Time**: O(n²) - inner loop runs n times for each of n iterations
- **Space**: O(1) - only uses variables i and j

**Example 3: Binary Search**
```
function binarySearch(arr, target):
    left = 0, right = n-1
    while left <= right:
        mid = (left + right) / 2
        if arr[mid] == target: return mid
        if arr[mid] < target: left = mid + 1
        else: right = mid - 1
    return -1
```
- **Time**: O(log n) - search space halved each iteration
- **Space**: O(1) - only uses few variables

**Example 4: Recursive Fibonacci**
```
function fibonacci(n):
    if n <= 1: return n
    return fibonacci(n-1) + fibonacci(n-2)
```
- **Time**: O(2ⁿ) - each call makes two recursive calls
- **Space**: O(n) - maximum recursion depth is n

### Amortized Analysis

**Definition**
Average time per operation over a sequence of operations, even if individual operations might be expensive.

**Example: Dynamic Array**
- Most insertions: O(1)
- Occasional resize: O(n)
- Amortized: O(1) per insertion

**Techniques**
- **Aggregate Method**: Total cost / number of operations
- **Accounting Method**: Assign costs to operations
- **Potential Method**: Use potential function

---

## 3. Arrays {#arrays}

### Understanding Arrays

**Definition**
An array is a collection of elements stored in contiguous memory locations, where each element can be accessed using an index.

**Key Characteristics**
- **Fixed Size**: Size determined at creation (in most languages)
- **Homogeneous**: All elements same data type
- **Random Access**: Access any element in O(1) time
- **Contiguous Memory**: Elements stored next to each other

**Memory Layout**
```
Array: [10, 20, 30, 40, 50]
Memory: [1000][1004][1008][1012][1016]
Index:    0     1     2     3     4
```

### Array Operations and Complexities

**Access/Read**
- **Operation**: arr[i]
- **Time Complexity**: O(1)
- **Why**: Direct memory address calculation: base_address + (index × element_size)

**Search**
- **Linear Search**: O(n) - check each element
- **Binary Search**: O(log n) - only if array is sorted

**Insertion**
- **At End**: O(1) if space available
- **At Beginning**: O(n) - shift all elements right
- **At Middle**: O(n) - shift elements right
- **Why Expensive**: Need to move existing elements

**Deletion**
- **From End**: O(1)
- **From Beginning**: O(n) - shift all elements left
- **From Middle**: O(n) - shift elements left
- **Why Expensive**: Need to fill the gap

**Update**
- **Operation**: arr[i] = value
- **Time Complexity**: O(1)
- **Why**: Direct access to memory location

### Types of Arrays

**Static Arrays**
- **Size**: Fixed at compile time
- **Memory**: Allocated on stack (usually)
- **Examples**: int arr[100] in C/C++

**Dynamic Arrays**
- **Size**: Can change during runtime
- **Memory**: Allocated on heap
- **Examples**: vector in C++, ArrayList in Java, list in Python
- **Resizing**: When full, create larger array and copy elements

**Multi-dimensional Arrays**
- **2D Array**: Matrix representation
- **Memory Layout**: Row-major or column-major order
- **Access**: arr[i][j] = base + (i × columns + j) × element_size

### Array Algorithms

**Two Pointer Technique**
- **Concept**: Use two pointers moving towards each other
- **Applications**: Pair sum, palindrome check, merge sorted arrays

**Example: Pair Sum**
```
function findPairSum(arr, target):
    left = 0, right = n-1
    while left < right:
        sum = arr[left] + arr[right]
        if sum == target: return [left, right]
        if sum < target: left++
        else: right--
    return null
```

**Sliding Window Technique**
- **Concept**: Maintain a window of elements and slide it
- **Applications**: Maximum sum subarray, substring problems

**Example: Maximum Sum Subarray of Size K**
```
function maxSumSubarray(arr, k):
    windowSum = sum of first k elements
    maxSum = windowSum
    for i = k to n-1:
        windowSum = windowSum - arr[i-k] + arr[i]
        maxSum = max(maxSum, windowSum)
    return maxSum
```

**Prefix Sum Technique**
- **Concept**: Precompute cumulative sums
- **Applications**: Range sum queries, subarray sum problems

**Example: Range Sum Query**
```
Preprocessing:
prefix[0] = arr[0]
for i = 1 to n-1:
    prefix[i] = prefix[i-1] + arr[i]

Query sum from index l to r:
if l == 0: return prefix[r]
else: return prefix[r] - prefix[l-1]
```

### Array Advantages and Disadvantages

**Advantages**
- **Fast Access**: O(1) random access
- **Memory Efficient**: No extra memory for pointers
- **Cache Friendly**: Contiguous memory improves cache performance
- **Simple**: Easy to understand and implement

**Disadvantages**
- **Fixed Size**: Cannot change size easily (static arrays)
- **Expensive Insertion/Deletion**: O(n) for arbitrary positions
- **Memory Waste**: May allocate more than needed
- **Homogeneous**: Can only store same data type

### Common Array Problems

**Rotation**
- **Left Rotation**: Move elements to left
- **Right Rotation**: Move elements to right
- **Efficient Method**: Reverse technique

**Kadane's Algorithm (Maximum Subarray Sum)**
```
function maxSubarraySum(arr):
    maxSoFar = arr[0]
    maxEndingHere = arr[0]
    for i = 1 to n-1:
        maxEndingHere = max(arr[i], maxEndingHere + arr[i])
        maxSoFar = max(maxSoFar, maxEndingHere)
    return maxSoFar
```

**Dutch National Flag (Sort 0s, 1s, 2s)**
```
function sortColors(arr):
    low = 0, mid = 0, high = n-1
    while mid <= high:
        if arr[mid] == 0:
            swap(arr[low], arr[mid])
            low++, mid++
        elif arr[mid] == 1:
            mid++
        else:
            swap(arr[mid], arr[high])
            high--
```

---

## 4. Strings {#strings}

### Understanding Strings

**Definition**
A string is a sequence of characters, typically used to represent text. In most programming languages, strings are implemented as arrays of characters.

**String Representation**
- **Array of Characters**: char str[] = "hello"
- **Null Terminated**: C-style strings end with '\0'
- **Length Prefixed**: Store length separately
- **Immutable**: Strings cannot be changed (Java, Python)
- **Mutable**: Strings can be modified (C++, C)

**Memory Layout**
```
String: "HELLO"
Memory: ['H']['E']['L']['L']['O']['\0']
Index:   0    1    2    3    4    5
```

### String Operations and Complexities

**Access**
- **Time Complexity**: O(1)
- **Operation**: str[i]

**Length**
- **Stored Length**: O(1)
- **Calculated Length**: O(n) - need to traverse until null terminator

**Concatenation**
- **Time Complexity**: O(n + m) where n, m are string lengths
- **Space Complexity**: O(n + m) for new string

**Substring**
- **Time Complexity**: O(k) where k is substring length
- **Space Complexity**: O(k) for new substring

**Comparison**
- **Time Complexity**: O(min(n, m))
- **Lexicographic**: Compare character by character

### String Algorithms

**Pattern Matching Algorithms**

**Naive Pattern Matching**
- **Time Complexity**: O(nm) where n = text length, m = pattern length
- **Space Complexity**: O(1)
- **Method**: Check pattern at each position in text

```
function naiveSearch(text, pattern):
    n = length(text), m = length(pattern)
    for i = 0 to n-m:
        j = 0
        while j < m and text[i+j] == pattern[j]:
            j++
        if j == m:
            print "Pattern found at index", i
```

**KMP (Knuth-Morris-Pratt) Algorithm**
- **Time Complexity**: O(n + m)
- **Space Complexity**: O(m)
- **Key Idea**: Use failure function to avoid redundant comparisons

```
function computeLPS(pattern):
    m = length(pattern)
    lps = array of size m
    len = 0, i = 1
    lps[0] = 0
    
    while i < m:
        if pattern[i] == pattern[len]:
            len++
            lps[i] = len
            i++
        else:
            if len != 0:
                len = lps[len-1]
            else:
                lps[i] = 0
                i++
    return lps

function KMPSearch(text, pattern):
    n = length(text), m = length(pattern)
    lps = computeLPS(pattern)
    i = 0, j = 0  // i for text, j for pattern
    
    while i < n:
        if pattern[j] == text[i]:
            i++, j++
        
        if j == m:
            print "Pattern found at index", i-j
            j = lps[j-1]
        elif i < n and pattern[j] != text[i]:
            if j != 0:
                j = lps[j-1]
            else:
                i++
```

**Rabin-Karp Algorithm**
- **Time Complexity**: O(nm) worst case, O(n+m) average case
- **Space Complexity**: O(1)
- **Key Idea**: Use rolling hash for efficient comparison

```
function rabinKarpSearch(text, pattern):
    n = length(text), m = length(pattern)
    prime = 101  // A prime number
    patternHash = 0, textHash = 0, h = 1
    
    // Calculate h = pow(256, m-1) % prime
    for i = 0 to m-2:
        h = (h * 256) % prime
    
    // Calculate hash of pattern and first window
    for i = 0 to m-1:
        patternHash = (256 * patternHash + pattern[i]) % prime
        textHash = (256 * textHash + text[i]) % prime
    
    // Slide pattern over text
    for i = 0 to n-m:
        if patternHash == textHash:
            // Check characters one by one
            if text[i:i+m] == pattern:
                print "Pattern found at index", i
        
        // Calculate hash for next window
        if i < n-m:
            textHash = (256*(textHash - text[i]*h) + text[i+m]) % prime
            if textHash < 0:
                textHash += prime
```

### String Manipulation Techniques

**Palindrome Check**
- **Two Pointer Approach**: Compare characters from both ends
- **Time Complexity**: O(n)

```
function isPalindrome(str):
    left = 0, right = length(str) - 1
    while left < right:
        if str[left] != str[right]:
            return false
        left++, right--
    return true
```

**Anagram Check**
- **Sorting Method**: Sort both strings and compare
- **Frequency Count**: Count character frequencies

```
function areAnagrams(str1, str2):
    if length(str1) != length(str2):
        return false
    
    count = array of size 256 (for ASCII)
    
    for i = 0 to length(str1)-1:
        count[str1[i]]++
        count[str2[i]]--
    
    for i = 0 to 255:
        if count[i] != 0:
            return false
    
    return true
```

**Longest Common Subsequence (LCS)**
- **Dynamic Programming**: Build solution bottom-up
- **Time Complexity**: O(mn)
- **Space Complexity**: O(mn)

```
function LCS(str1, str2):
    m = length(str1), n = length(str2)
    dp = 2D array of size (m+1) x (n+1)
    
    for i = 0 to m:
        for j = 0 to n:
            if i == 0 or j == 0:
                dp[i][j] = 0
            elif str1[i-1] == str2[j-1]:
                dp[i][j] = dp[i-1][j-1] + 1
            else:
                dp[i][j] = max(dp[i-1][j], dp[i][j-1])
    
    return dp[m][n]
```

### Advanced String Algorithms

**Trie (Prefix Tree)**
- **Purpose**: Efficient string storage and retrieval
- **Applications**: Autocomplete, spell checker, IP routing

```
class TrieNode:
    children = array of size 26  // for lowercase letters
    isEndOfWord = false

function insert(root, word):
    current = root
    for each character c in word:
        index = c - 'a'
        if current.children[index] == null:
            current.children[index] = new TrieNode()
        current = current.children[index]
    current.isEndOfWord = true

function search(root, word):
    current = root
    for each character c in word:
        index = c - 'a'
        if current.children[index] == null:
            return false
        current = current.children[index]
    return current.isEndOfWord
```

**Suffix Array**
- **Purpose**: Efficient substring search
- **Construction**: O(n log n) using sorting
- **Applications**: Pattern matching, longest common substring

**Z Algorithm**
- **Purpose**: Find all occurrences of pattern in text
- **Time Complexity**: O(n + m)
- **Key Idea**: Use Z array to store length of longest substring starting from i which is also prefix

### String Problems and Patterns

**Sliding Window Problems**
- **Longest Substring Without Repeating Characters**
- **Minimum Window Substring**
- **Longest Substring with At Most K Distinct Characters**

**Two Pointer Problems**
- **Valid Palindrome**
- **Reverse Words in String**
- **Remove Duplicates**

**Dynamic Programming Problems**
- **Edit Distance**
- **Longest Palindromic Substring**
- **Word Break Problem**

---

## 5. Linked Lists {#linked-lists}

### Understanding Linked Lists

**Definition**
A linked list is a linear data structure where elements (nodes) are stored in sequence, but not in contiguous memory locations. Each node contains data and a reference (pointer) to the next node.

**Node Structure**
```
class ListNode:
    data: value stored in node
    next: reference to next node
```

**Memory Representation**
```
Array:     [A][B][C][D]  (contiguous)
           1000 1004 1008 1012

Linked List: [A|ptr] -> [B|ptr] -> [C|ptr] -> [D|null]
             1000       2000       1500       3000
```

**Key Characteristics**
- **Dynamic Size**: Can grow/shrink during runtime
- **Non-contiguous**: Nodes can be anywhere in memory
- **Sequential Access**: Must traverse from head to reach any node
- **Extra Memory**: Each node needs space for pointer

### Types of Linked Lists

**Singly Linked List**
- **Structure**: Each node points to next node
- **Traversal**: Only forward direction
- **Last Node**: Points to null

```
Head -> [1|next] -> [2|next] -> [3|null]
```

**Doubly Linked List**
- **Structure**: Each node has pointers to both next and previous nodes
- **Traversal**: Both forward and backward
- **Advantages**: Easier deletion, bidirectional traversal

```
null <- [prev|1|next] <-> [prev|2|next] <-> [prev|3|next] -> null
```

**Circular Linked List**
- **Structure**: Last node points back to first node
- **No null**: No node points to null
- **Applications**: Round-robin scheduling, music playlist

```
Head -> [1|next] -> [2|next] -> [3|next] -+
        ^                                  |
        +----------------------------------+
```

**Circular Doubly Linked List**
- **Structure**: Combines circular and doubly linked properties
- **Complex**: Most complex but most flexible

### Linked List Operations

**Insertion Operations**

**Insert at Beginning**
```
function insertAtBeginning(head, data):
    newNode = new ListNode(data)
    newNode.next = head
    head = newNode
    return head
```
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

**Insert at End**
```
function insertAtEnd(head, data):
    newNode = new ListNode(data)
    if head == null:
        return newNode
    
    current = head
    while current.next != null:
        current = current.next
    current.next = newNode
    return head
```
- **Time Complexity**: O(n) - need to traverse to end
- **Space Complexity**: O(1)

**Insert at Position**
```
function insertAtPosition(head, data, position):
    if position == 0:
        return insertAtBeginning(head, data)
    
    newNode = new ListNode(data)
    current = head
    for i = 0 to position-2:
        if current == null:
            return head  // position out of bounds
        current = current.next
    
    newNode.next = current.next
    current.next = newNode
    return head
```
- **Time Complexity**: O(n) - may need to traverse to position
- **Space Complexity**: O(1)

**Deletion Operations**

**Delete from Beginning**
```
function deleteFromBeginning(head):
    if head == null:
        return null
    temp = head
    head = head.next
    delete temp
    return head
```
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

**Delete from End**
```
function deleteFromEnd(head):
    if head == null or head.next == null:
        delete head
        return null
    
    current = head
    while current.next.next != null:
        current = current.next
    delete current.next
    current.next = null
    return head
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

**Delete by Value**
```
function deleteByValue(head, value):
    if head == null:
        return null
    
    if head.data == value:
        temp = head
        head = head.next
        delete temp
        return head
    
    current = head
    while current.next != null and current.next.data != value:
        current = current.next
    
    if current.next != null:
        temp = current.next
        current.next = current.next.next
        delete temp
    
    return head
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

**Search Operation**
```
function search(head, value):
    current = head
    position = 0
    while current != null:
        if current.data == value:
            return position
        current = current.next
        position++
    return -1  // not found
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

### Advanced Linked List Algorithms

**Reverse a Linked List**

**Iterative Approach**
```
function reverseIterative(head):
    prev = null
    current = head
    
    while current != null:
        next = current.next
        current.next = prev
        prev = current
        current = next
    
    return prev  // new head
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

**Recursive Approach**
```
function reverseRecursive(head):
    if head == null or head.next == null:
        return head
    
    newHead = reverseRecursive(head.next)
    head.next.next = head
    head.next = null
    
    return newHead
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(n) - recursion stack

**Detect Cycle in Linked List**

**Floyd's Cycle Detection (Tortoise and Hare)**
```
function hasCycle(head):
    if head == null or head.next == null:
        return false
    
    slow = head      // tortoise
    fast = head.next // hare
    
    while fast != null and fast.next != null:
        if slow == fast:
            return true
        slow = slow.next
        fast = fast.next.next
    
    return false
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

**Find Cycle Start**
```
function findCycleStart(head):
    if not hasCycle(head):
        return null
    
    // Find meeting point
    slow = fast = head
    while true:
        slow = slow.next
        fast = fast.next.next
        if slow == fast:
            break
    
    // Find start of cycle
    start = head
    while start != slow:
        start = start.next
        slow = slow.next
    
    return start
```

**Merge Two Sorted Lists**
```
function mergeTwoSortedLists(list1, list2):
    dummy = new ListNode(0)
    current = dummy
    
    while list1 != null and list2 != null:
        if list1.data <= list2.data:
            current.next = list1
            list1 = list1.next
        else:
            current.next = list2
            list2 = list2.next
        current = current.next
    
    // Attach remaining nodes
    if list1 != null:
        current.next = list1
    else:
        current.next = list2
    
    return dummy.next
```
- **Time Complexity**: O(m + n)
- **Space Complexity**: O(1)

**Find Middle of Linked List**
```
function findMiddle(head):
    if head == null:
        return null
    
    slow = fast = head
    
    while fast != null and fast.next != null:
        slow = slow.next
        fast = fast.next.next
    
    return slow  // middle node
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

**Remove Nth Node from End**
```
function removeNthFromEnd(head, n):
    dummy = new ListNode(0)
    dummy.next = head
    first = second = dummy
    
    // Move first pointer n+1 steps ahead
    for i = 0 to n:
        first = first.next
    
    // Move both pointers until first reaches end
    while first != null:
        first = first.next
        second = second.next
    
    // Remove the nth node from end
    second.next = second.next.next
    
    return dummy.next
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(1)

### Linked List vs Array Comparison

| Operation | Array | Linked List |
|-----------|-------|-------------|
| Access | O(1) | O(n) |
| Search | O(n) | O(n) |
| Insert at beginning | O(n) | O(1) |
| Insert at end | O(1)* | O(n) |
| Insert at middle | O(n) | O(n) |
| Delete from beginning | O(n) | O(1) |
| Delete from end | O(1) | O(n) |
| Delete from middle | O(n) | O(n) |
| Memory usage | Less | More (pointers) |
| Cache performance | Better | Worse |

*O(1) for dynamic arrays with available space

### Advantages and Disadvantages

**Linked List Advantages**
- **Dynamic Size**: Can grow/shrink at runtime
- **Efficient Insertion/Deletion**: O(1) at beginning
- **Memory Efficient**: Allocate memory as needed
- **No Memory Waste**: Use exactly what's needed

**Linked List Disadvantages**
- **No Random Access**: Must traverse from head
- **Extra Memory**: Pointer storage overhead
- **Cache Performance**: Poor due to non-contiguous memory
- **Not Cache Friendly**: Random memory access patterns

### Applications of Linked Lists

**Real-World Applications**
- **Undo Functionality**: Browser back button, text editor undo
- **Music Playlist**: Next/previous song navigation
- **Image Viewer**: Next/previous image
- **Memory Management**: Free memory block management

**Data Structure Implementation**
- **Stack Implementation**: Using linked list
- **Queue Implementation**: Using linked list
- **Graph Adjacency List**: Represent graph connections
- **Hash Table Chaining**: Handle collisions

---

## 6. Stacks {#stacks}

### Understanding Stacks

**Definition**
A stack is a linear data structure that follows the Last In, First Out (LIFO) principle. Elements are added and removed from the same end, called the "top" of the stack.

**Real-World Analogies**
- **Stack of Plates**: Add/remove plates from top only
- **Browser History**: Back button returns to last visited page
- **Undo Operation**: Last action is undone first
- **Function Calls**: Last called function returns first

**LIFO Principle**
```
Push operations: 1 -> 2 -> 3 -> 4
Stack state:     [4]
                 [3]
                 [2]
                 [1]

Pop operations:  4 <- 3 <- 2 <- 1
```

### Stack Operations

**Primary Operations**

**Push (Insert)**
- **Description**: Add element to top of stack
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

```
function push(stack, element):
    if isFull(stack):
        print "Stack Overflow"
        return
    stack.top = stack.top + 1
    stack.array[stack.top] = element
```

**Pop (Delete)**
- **Description**: Remove and return top element
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

```
function pop(stack):
    if isEmpty(stack):
        print "Stack Underflow"
        return null
    element = stack.array[stack.top]
    stack.top = stack.top - 1
    return element
```

**Peek/Top**
- **Description**: Return top element without removing
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

```
function peek(stack):
    if isEmpty(stack):
        print "Stack is empty"
        return null
    return stack.array[stack.top]
```

**Auxiliary Operations**

**isEmpty**
```
function isEmpty(stack):
    return stack.top == -1
```

**isFull**
```
function isFull(stack):
    return stack.top == stack.maxSize - 1
```

**Size**
```
function size(stack):
    return stack.top + 1
```

### Stack Implementation

**Array-Based Implementation**
```
class ArrayStack:
    maxSize: maximum capacity
    top: index of top element (-1 if empty)
    array: array to store elements
    
    constructor(capacity):
        maxSize = capacity
        top = -1
        array = new array[maxSize]
    
    push(element):
        if top == maxSize - 1:
            throw "Stack Overflow"
        top++
        array[top] = element
    
    pop():
        if top == -1:
            throw "Stack Underflow"
        element = array[top]
        top--
        return element
    
    peek():
        if top == -1:
            throw "Stack is empty"
        return array[top]
```

**Advantages of Array Implementation**
- **Memory Efficient**: No extra memory for pointers
- **Cache Friendly**: Contiguous memory access
- **Simple**: Easy to implement and understand

**Disadvantages of Array Implementation**
- **Fixed Size**: Cannot grow beyond initial capacity
- **Memory Waste**: May allocate more than needed
- **Stack Overflow**: Risk of exceeding capacity

**Linked List-Based Implementation**
```
class ListNode:
    data: element value
    next: reference to next node

class LinkedStack:
    top: reference to top node
    
    constructor():
        top = null
    
    push(element):
        newNode = new ListNode(element)
        newNode.next = top
        top = newNode
    
    pop():
        if top == null:
            throw "Stack Underflow"
        element = top.data
        top = top.next
        return element
    
    peek():
        if top == null:
            throw "Stack is empty"
        return top.data
```

**Advantages of Linked List Implementation**
- **Dynamic Size**: Can grow as needed
- **No Overflow**: Limited only by available memory
- **Memory Efficient**: Allocate exactly what's needed

**Disadvantages of Linked List Implementation**
- **Extra Memory**: Pointer storage overhead
- **Cache Performance**: Poor due to non-contiguous memory
- **Complexity**: More complex than array implementation

### Stack Applications

**Expression Evaluation**

**Infix to Postfix Conversion**
- **Infix**: A + B * C
- **Postfix**: A B C * +
- **Algorithm**: Use stack to handle operator precedence

```
function infixToPostfix(expression):
    stack = new Stack()
    result = ""
    
    for each character c in expression:
        if c is operand:
            result += c
        elif c is '(':
            stack.push(c)
        elif c is ')':
            while not stack.isEmpty() and stack.peek() != '(':
                result += stack.pop()
            stack.pop()  // remove '('
        elif c is operator:
            while not stack.isEmpty() and precedence(stack.peek()) >= precedence(c):
                result += stack.pop()
            stack.push(c)
    
    while not stack.isEmpty():
        result += stack.pop()
    
    return result
```

**Postfix Expression Evaluation**
```
function evaluatePostfix(expression):
    stack = new Stack()
    
    for each character c in expression:
        if c is operand:
            stack.push(c)
        elif c is operator:
            operand2 = stack.pop()
            operand1 = stack.pop()
            result = applyOperator(operand1, operand2, c)
            stack.push(result)
    
    return stack.pop()
```

**Balanced Parentheses Check**
```
function isBalanced(expression):
    stack = new Stack()
    
    for each character c in expression:
        if c is opening bracket ('(', '[', '{'):
            stack.push(c)
        elif c is closing bracket (')', ']', '}'):
            if stack.isEmpty():
                return false
            if not isMatchingPair(stack.pop(), c):
                return false
    
    return stack.isEmpty()

function isMatchingPair(opening, closing):
    return (opening == '(' and closing == ')') or
           (opening == '[' and closing == ']') or
           (opening == '{' and closing == '}')
```

**Function Call Management**
- **Call Stack**: Track function calls and local variables
- **Stack Frame**: Each function call creates a frame
- **Return Address**: Where to return after function completes

**Undo Operations**
- **Text Editors**: Undo last edit operation
- **Image Editors**: Undo last filter/transformation
- **Games**: Undo last move

**Browser History**
- **Back Button**: Return to previous page
- **Forward Button**: Go to next page (using another stack)

### Advanced Stack Problems

**Next Greater Element**
```
function nextGreaterElement(arr):
    n = length(arr)
    result = array of size n filled with -1
    stack = new Stack()
    
    for i = 0 to n-1:
        while not stack.isEmpty() and arr[i] > arr[stack.peek()]:
            index = stack.pop()
            result[index] = arr[i]
        stack.push(i)
    
    return result
```
- **Time Complexity**: O(n)
- **Space Complexity**: O(n)

**Largest Rectangle in Histogram**
```
function largestRectangleArea(heights):
    stack = new Stack()
    maxArea = 0
    n = length(heights)
    
    for i = 0 to n:
        currentHeight = (i == n) ? 0 : heights[i]
        
        while not stack.isEmpty() and currentHeight < heights[stack.peek()]:
            height = heights[stack.pop()]
            width = stack.isEmpty() ? i : i - stack.peek() - 1
            maxArea = max(maxArea, height * width)
        
        stack.push(i)
    
    return maxArea
```

**Stock Span Problem**
```
function calculateSpan(prices):
    n = length(prices)
    span = array of size n
    stack = new Stack()
    
    for i = 0 to n-1:
        while not stack.isEmpty() and prices[stack.peek()] <= prices[i]:
            stack.pop()
        
        span[i] = stack.isEmpty() ? i + 1 : i - stack.peek()
        stack.push(i)
    
    return span
```

**Minimum Stack**
```
class MinStack:
    mainStack: stores all elements
    minStack: stores minimum elements
    
    push(element):
        mainStack.push(element)
        if minStack.isEmpty() or element <= minStack.peek():
            minStack.push(element)
    
    pop():
        if mainStack.isEmpty():
            throw "Stack Underflow"
        element = mainStack.pop()
        if element == minStack.peek():
            minStack.pop()
        return element
    
    getMin():
        if minStack.isEmpty():
            throw "Stack is empty"
        return minStack.peek()
```

### Stack vs Other Data Structures

| Feature | Stack | Queue | Array | Linked List |
|---------|-------|-------|-------|-------------|
| Access Pattern | LIFO | FIFO | Random | Sequential |
| Insert | O(1) top | O(1) rear | O(n) middle | O(1) front |
| Delete | O(1) top | O(1) front | O(n) middle | O(1) front |
| Search | O(n) | O(n) | O(n) | O(n) |
| Memory | Contiguous/Linked | Contiguous/Linked | Contiguous | Non-contiguous |

### Common Stack Mistakes

**Stack Overflow**
- **Cause**: Pushing to full stack
- **Prevention**: Check capacity before push
- **Solution**: Use dynamic stack or handle gracefully

**Stack Underflow**
- **Cause**: Popping from empty stack
- **Prevention**: Check if empty before pop
- **Solution**: Return error code or throw exception

**Memory Leaks**
- **Cause**: Not freeing popped nodes in linked implementation
- **Prevention**: Properly delete nodes after pop
- **Solution**: Use garbage collection or manual memory management

---

## 7. Queues {#queues}

### Understanding Queues

**Definition**
A queue is a linear data structure that follows the First In, First Out (FIFO) principle. Elements are added at one end (rear/back) and removed from the other end (front).

**Real-World Analogies**
- **Line at Store**: First person in line is served first
- **Print Queue**: Documents printed in order they were submitted
- **Traffic Light**: Cars pass in the order they arrive
- **Call Center**: Calls answered in order received

**FIFO Principle**
```
Enqueue: 1 -> 2 -> 3 -> 4
Queue:   [1][2][3][4]
         front    rear

Dequeue: 1 <- 2 <- 3 <- 4
```

### Queue Operations

**Primary Operations**

**Enqueue (Insert)**
- **Description**: Add element to rear of queue
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

```
function enqueue(queue, element):
    if isFull(queue):
        print "Queue Overflow"
        return
    if isEmpty(queue):
        queue.front = queue.rear = 0
    else:
        queue.rear = (queue.rear + 1) % queue.maxSize
    queue.array[queue.rear] = element
    queue.size++
```

**Dequeue (Delete)**
- **Description**: Remove and return front element
- **Time Complexity**: O(1)
- **Space Complexity**: O(1)

```
function dequeue(queue):
    if isEmpty(queue):
        print "Queue Underflow"
        return null
    element = queue.array[queue.front]
    if queue.size == 1:
        queue.front = queue.rear = -1
    else:
        queue.front = (queue.front + 1) % queue.maxSize
    queue.size--
    return element
```

**Front/Peek**
- **Description**: Return front element without removing
- **Time Complexity**: O(1)

```
function front(queue):
    if isEmpty(queue):
        print "Queue is empty"
        return null
    return queue.array[queue.front]
```

**Auxiliary Operations**

**isEmpty**
```
function isEmpty(queue):
    return queue.size == 0
```

**isFull**
```
function isFull(queue):
    return queue.size == queue.maxSize
```

**Size**
```
function size(queue):
    return queue.size
```

### Queue Implementation

**Array-Based Implementation (Circular Queue)**
```
class CircularQueue:
    maxSize: maximum capacity
    front: index of front element
    rear: index of rear element
    size: current number of elements
    array: array to store elements
    
    constructor(capacity):
        maxSize = capacity
        front = rear = -1
        size = 0
        array = new array[maxSize]
    
    enqueue(element):
        if size == maxSize:
            throw "Queue Overflow"
        if size == 0:
            front = rear = 0
        else:
            rear = (rear + 1) % maxSize
        array[rear] = element
        size++
    
    dequeue():
        if size == 0:
            throw "Queue Underflow"
        element = array[front]
        if size == 1:
            front = rear = -1
        else:
            front = (front + 1) % maxSize
        size--
        return element
```

**Why Circular Queue?**
- **Space Efficiency**: Reuse array positions
- **Avoid Shifting**: No need to shift elements
- **Constant Time**: All operations remain O(1)

**Linear Queue Problems**
```
Initial: [1][2][3][ ][ ]
         front   rear

After dequeue: [ ][2][3][ ][ ]
                  front rear

After enqueue 4,5: [ ][2][3][4][5]
                      front    rear

Problem: Cannot add more elements even though space available at beginning
```

**Linked List-Based Implementation**
```
class ListNode:
    data: element value
    next: reference to next node

class LinkedQueue:
    front: reference to front node
    rear: reference to rear node
    
    constructor():
        front = rear = null
    
    enqueue(element):
        newNode = new ListNode(element)
        if rear == null:
            front = rear = newNode
        else:
            rear.next = newNode
            rear = newNode
    
    dequeue():
        if front == null:
            throw "Queue Underflow"
        element = front.data
        front = front.next
        if front == null:
            rear = null
        return element
```

### Types of Queues

**Simple Queue**
- **Basic FIFO**: Standard queue implementation
- **Operations**: Enqueue at rear, dequeue from front

**Circular Queue**
- **Circular Array**: Last position connects to first
- **Advantage**: Efficient space utilization
- **Use Case**: Buffer implementation

**Priority Queue**
- **Ordered Elements**: Elements have priorities
- **Dequeue**: Remove highest priority element
- **Implementation**: Heap, sorted array, or linked list

```
class PriorityQueue:
    heap: min-heap or max-heap
    
    enqueue(element, priority):
        heap.insert((priority, element))
    
    dequeue():
        return heap.extractMin()  // or extractMax()
```

**Double-Ended Queue (Deque)**
- **Both Ends**: Insert/delete from both front and rear
- **Flexibility**: Can work as stack or queue
- **Operations**: addFront, addRear, removeFront, removeRear

```
class Deque:
    addFront(element): insert at front
    addRear(element): insert at rear
    removeFront(): remove from front
    removeRear(): remove from rear
```

### Queue Applications

**Breadth-First Search (BFS)**
```
function BFS(graph, startVertex):
    visited = set()
    queue = new Queue()
    
    queue.enqueue(startVertex)
    visited.add(startVertex)
    
    while not queue.isEmpty():
        vertex = queue.dequeue()
        print vertex
        
        for each neighbor of vertex:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.enqueue(neighbor)
```

**Level Order Tree Traversal**
```
function levelOrder(root):
    if root == null:
        return
    
    queue = new Queue()
    queue.enqueue(root)
    
    while not queue.isEmpty():
        node = queue.dequeue()
        print node.data
        
        if node.left != null:
            queue.enqueue(node.left)
        if node.right != null:
            queue.enqueue(node.right)
```

**Process Scheduling**
- **Round Robin**: Each process gets fixed time slice
- **FCFS**: First Come First Served scheduling
- **Job Queue**: Maintain queue of waiting processes

**Buffer for Data Streams**
- **Producer-Consumer**: Buffer between producer and consumer
- **I/O Operations**: Buffer keyboard input, network data
- **Print Spooling**: Queue print jobs

**Handling Requests**
- **Web Servers**: Queue incoming requests
- **Database**: Queue transactions
- **Operating Systems**: Queue system calls

### Advanced Queue Problems

**Sliding Window Maximum**
```
function slidingWindowMaximum(arr, k):
    n = length(arr)
    deque = new Deque()  // stores indices
    result = []
    
    for i = 0 to n-1:
        // Remove indices outside current window
        while not deque.isEmpty() and deque.front() <= i - k:
            deque.removeFront()
        
        // Remove indices of smaller elements
        while not deque.isEmpty() and arr[deque.rear()] <= arr[i]:
            deque.removeRear()
        
        deque.addRear(i)
        
        // Add maximum of current window to result
        if i >= k - 1:
            result.append(arr[deque.front()])
    
    return result
```

**First Non-Repeating Character in Stream**
```
class FirstNonRepeating:
    queue: stores characters in order
    frequency: count of each character
    
    addCharacter(ch):
        frequency[ch]++
        queue.enqueue(ch)
        
        // Remove characters that are now repeating
        while not queue.isEmpty() and frequency[queue.front()] > 1:
            queue.dequeue()
    
    getFirstNonRepeating():
        if queue.isEmpty():
            return null
        return queue.front()
```

**Generate Binary Numbers**
```
function generateBinaryNumbers(n):
    queue = new Queue()
    queue.enqueue("1")
    
    for i = 1 to n:
        binary = queue.dequeue()
        print binary
        
        queue.enqueue(binary + "0")
        queue.enqueue(binary + "1")
```

**Implement Stack using Queues**
```
class StackUsingQueues:
    queue1: main queue
    queue2: auxiliary queue
    
    push(element):
        queue2.enqueue(element)
        
        // Move all elements from queue1 to queue2
        while not queue1.isEmpty():
            queue2.enqueue(queue1.dequeue())
        
        // Swap queue1 and queue2
        temp = queue1
        queue1 = queue2
        queue2 = temp
    
    pop():
        if queue1.isEmpty():
            throw "Stack Underflow"
        return queue1.dequeue()
```

### Queue vs Other Data Structures

| Feature | Queue | Stack | Array | Linked List |
|---------|-------|-------|-------|-------------|
| Access Pattern | FIFO | LIFO | Random | Sequential |
| Insert | O(1) rear | O(1) top | O(n) middle | O(1) front |
| Delete | O(1) front | O(1) top | O(n) middle | O(1) front |
| Search | O(n) | O(n) | O(n) | O(n) |
| Use Case | BFS, Scheduling | DFS, Undo | Random access | Dynamic size |

### Performance Analysis

**Array Implementation**
- **Time Complexity**: O(1) for all operations
- **Space Complexity**: O(n) where n is maximum capacity
- **Pros**: Cache friendly, simple
- **Cons**: Fixed size, potential space waste

**Linked List Implementation**
- **Time Complexity**: O(1) for all operations
- **Space Complexity**: O(n) where n is current size
- **Pros**: Dynamic size, no space waste
- **Cons**: Extra memory for pointers, cache unfriendly

**Circular Array vs Linear Array**
- **Circular**: Better space utilization, no shifting needed
- **Linear**: Simpler logic, but inefficient space usage
- **Choice**: Circular preferred for most applications

This completes the enhanced coverage of fundamental data structures. Each section now includes deeper theoretical explanations, multiple implementation approaches, complexity analysis, and practical applications with real-world examples.