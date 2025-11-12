# Complete DSA Guide Part 3 - Design Techniques & MCQ Questions

*Advanced algorithmic design techniques and comprehensive MCQ practice for exam preparation*

## Table of Contents
19. [Greedy Algorithms](#greedy)
20. [Dynamic Programming](#dp)
21. [Divide and Conquer](#divide-conquer)
22. [Backtracking](#backtracking)
23. [Advanced Topics](#advanced-topics)
24. [MCQ Practice Questions](#mcq-questions)

---

## 19. Greedy Algorithms {#greedy}

### Understanding Greedy Approach

**Definition**
Greedy algorithms make locally optimal choices at each step, hoping to find a global optimum. They never reconsider their choices.

**Greedy Choice Property**
- A global optimum can be arrived at by making locally optimal choices
- The choice made by a greedy algorithm at each step is safe

**Optimal Substructure**
- An optimal solution contains optimal solutions to subproblems
- Essential for both greedy and dynamic programming

**When Greedy Works**
- Problem exhibits greedy choice property
- Problem has optimal substructure
- Local optimum leads to global optimum

### Classic Greedy Problems

**Activity Selection Problem**
```
function activitySelection(activities):
    // Sort by finish time
    sort(activities) by finish time
    
    selected = [activities[0]]
    lastFinish = activities[0].finish
    
    for i = 1 to length(activities)-1:
        if activities[i].start >= lastFinish:
            selected.append(activities[i])
            lastFinish = activities[i].finish
    
    return selected
```
- **Time**: O(n log n) for sorting
- **Greedy Choice**: Always pick activity that finishes earliest

**Fractional Knapsack**
```
function fractionalKnapsack(items, capacity):
    // Sort by value/weight ratio
    sort(items) by value/weight in descending order
    
    totalValue = 0
    currentWeight = 0
    
    for item in items:
        if currentWeight + item.weight <= capacity:
            // Take whole item
            totalValue += item.value
            currentWeight += item.weight
        else:
            // Take fraction of item
            remainingCapacity = capacity - currentWeight
            fraction = remainingCapacity / item.weight
            totalValue += fraction * item.value
            break
    
    return totalValue
```
- **Time**: O(n log n)
- **Greedy Choice**: Always pick item with highest value/weight ratio

**Huffman Coding**
```
function huffmanCoding(frequencies):
    heap = new MinHeap()
    
    // Create leaf nodes
    for char, freq in frequencies:
        heap.insert(new Node(char, freq))
    
    // Build Huffman tree
    while heap.size() > 1:
        left = heap.extractMin()
        right = heap.extractMin()
        
        merged = new Node(null, left.freq + right.freq)
        merged.left = left
        merged.right = right
        
        heap.insert(merged)
    
    root = heap.extractMin()
    codes = {}
    generateCodes(root, "", codes)
    
    return codes

function generateCodes(node, code, codes):
    if node.isLeaf():
        codes[node.char] = code
        return
    
    generateCodes(node.left, code + "0", codes)
    generateCodes(node.right, code + "1", codes)
```
- **Time**: O(n log n)
- **Greedy Choice**: Always merge two nodes with smallest frequencies

**Job Scheduling with Deadlines**
```
function jobScheduling(jobs):
    // Sort by profit in descending order
    sort(jobs) by profit in descending order
    
    maxDeadline = max(job.deadline for job in jobs)
    schedule = array of size maxDeadline filled with null
    totalProfit = 0
    
    for job in jobs:
        // Find latest available slot before deadline
        for slot = min(job.deadline, maxDeadline) - 1 down to 0:
            if schedule[slot] == null:
                schedule[slot] = job
                totalProfit += job.profit
                break
    
    return schedule, totalProfit
```
- **Time**: O(n²)
- **Greedy Choice**: Always pick job with highest profit that can be scheduled

**Minimum Spanning Tree (Kruskal's)**
```
function kruskalMST(graph):
    edges = getAllEdges(graph)
    sort(edges) by weight
    
    mst = []
    uf = new UnionFind(V)
    
    for edge in edges:
        u, v, weight = edge
        if not uf.connected(u, v):
            uf.union(u, v)
            mst.append(edge)
            
            if length(mst) == V - 1:
                break
    
    return mst
```
- **Time**: O(E log E)
- **Greedy Choice**: Always pick minimum weight edge that doesn't create cycle

**Dijkstra's Shortest Path**
```
function dijkstra(graph, source):
    dist = array filled with ∞
    visited = new Set()
    pq = new PriorityQueue()
    
    dist[source] = 0
    pq.insert((0, source))
    
    while not pq.isEmpty():
        currentDist, u = pq.extractMin()
        
        if u in visited:
            continue
        
        visited.add(u)
        
        for neighbor, weight in graph[u]:
            if neighbor not in visited:
                newDist = dist[u] + weight
                if newDist < dist[neighbor]:
                    dist[neighbor] = newDist
                    pq.insert((newDist, neighbor))
    
    return dist
```
- **Time**: O((V + E) log V)
- **Greedy Choice**: Always visit unvisited vertex with minimum distance

### Coin Change (Greedy vs Optimal)

**Greedy Approach (Works for standard coin systems)**
```
function coinChangeGreedy(coins, amount):
    sort(coins) in descending order
    result = []
    
    for coin in coins:
        while amount >= coin:
            result.append(coin)
            amount -= coin
    
    return result if amount == 0 else null
```
- **Time**: O(n log n + amount/smallest_coin)
- **Note**: Doesn't always give optimal solution for arbitrary coin systems

### Interval Scheduling Problems

**Interval Scheduling Maximization**
```
function maxIntervals(intervals):
    sort(intervals) by end time
    
    count = 1
    lastEnd = intervals[0].end
    
    for i = 1 to length(intervals)-1:
        if intervals[i].start >= lastEnd:
            count++
            lastEnd = intervals[i].end
    
    return count
```

**Minimum Platforms Required**
```
function minPlatforms(arrivals, departures):
    sort(arrivals)
    sort(departures)
    
    platforms = 0
    maxPlatforms = 0
    i = j = 0
    
    while i < length(arrivals):
        if arrivals[i] <= departures[j]:
            platforms++
            maxPlatforms = max(maxPlatforms, platforms)
            i++
        else:
            platforms--
            j++
    
    return maxPlatforms
```

### Greedy Algorithm Design Steps

1. **Cast Problem**: Formulate as making sequence of choices
2. **Prove Greedy Choice**: Show locally optimal choice leads to global optimum
3. **Prove Optimal Substructure**: Show optimal solution contains optimal subproblems
4. **Implement Algorithm**: Make greedy choice and solve remaining subproblem

### Advantages and Disadvantages

**Advantages**
- **Simple**: Easy to understand and implement
- **Efficient**: Usually faster than other approaches
- **Memory Efficient**: No need to store intermediate results

**Disadvantages**
- **Limited Applicability**: Works only for specific problem types
- **No Backtracking**: Cannot undo previous choices
- **May Not Be Optimal**: Doesn't guarantee optimal solution for all problems

---

## 20. Dynamic Programming {#dp}

### Understanding Dynamic Programming

**Definition**
Dynamic Programming solves complex problems by breaking them down into simpler subproblems and storing results to avoid redundant calculations.

**Key Characteristics**
- **Overlapping Subproblems**: Same subproblems solved multiple times
- **Optimal Substructure**: Optimal solution contains optimal subproblems
- **Memoization**: Store results of subproblems

**DP vs Divide and Conquer**
- **DP**: Subproblems overlap, store results
- **D&C**: Subproblems are independent, don't store results

### DP Approaches

**Top-Down (Memoization)**
```
memo = {}

function fibonacci(n):
    if n in memo:
        return memo[n]
    
    if n <= 1:
        return n
    
    memo[n] = fibonacci(n-1) + fibonacci(n-2)
    return memo[n]
```

**Bottom-Up (Tabulation)**
```
function fibonacci(n):
    if n <= 1:
        return n
    
    dp = array of size n+1
    dp[0] = 0
    dp[1] = 1
    
    for i = 2 to n:
        dp[i] = dp[i-1] + dp[i-2]
    
    return dp[n]
```

### Classic DP Problems

**0/1 Knapsack Problem**
```
function knapsack(weights, values, capacity):
    n = length(weights)
    dp = 2D array of size (n+1) x (capacity+1)
    
    // Initialize base cases
    for i = 0 to n:
        dp[i][0] = 0
    for w = 0 to capacity:
        dp[0][w] = 0
    
    // Fill DP table
    for i = 1 to n:
        for w = 1 to capacity:
            if weights[i-1] <= w:
                dp[i][w] = max(
                    values[i-1] + dp[i-1][w - weights[i-1]],  // Include item
                    dp[i-1][w]                                 // Exclude item
                )
            else:
                dp[i][w] = dp[i-1][w]
    
    return dp[n][capacity]
```
- **Time**: O(n × capacity)
- **Space**: O(n × capacity)

**Longest Common Subsequence (LCS)**
```
function LCS(str1, str2):
    m = length(str1)
    n = length(str2)
    dp = 2D array of size (m+1) x (n+1)
    
    // Initialize base cases
    for i = 0 to m:
        dp[i][0] = 0
    for j = 0 to n:
        dp[0][j] = 0
    
    // Fill DP table
    for i = 1 to m:
        for j = 1 to n:
            if str1[i-1] == str2[j-1]:
                dp[i][j] = dp[i-1][j-1] + 1
            else:
                dp[i][j] = max(dp[i-1][j], dp[i][j-1])
    
    return dp[m][n]
```
- **Time**: O(m × n)
- **Space**: O(m × n)

**Edit Distance (Levenshtein Distance)**
```
function editDistance(str1, str2):
    m = length(str1)
    n = length(str2)
    dp = 2D array of size (m+1) x (n+1)
    
    // Initialize base cases
    for i = 0 to m:
        dp[i][0] = i  // Delete all characters
    for j = 0 to n:
        dp[0][j] = j  // Insert all characters
    
    // Fill DP table
    for i = 1 to m:
        for j = 1 to n:
            if str1[i-1] == str2[j-1]:
                dp[i][j] = dp[i-1][j-1]  // No operation needed
            else:
                dp[i][j] = 1 + min(
                    dp[i-1][j],    // Delete
                    dp[i][j-1],    // Insert
                    dp[i-1][j-1]   // Replace
                )
    
    return dp[m][n]
```

**Longest Increasing Subsequence (LIS)**
```
function LIS(arr):
    n = length(arr)
    dp = array of size n filled with 1
    
    for i = 1 to n-1:
        for j = 0 to i-1:
            if arr[j] < arr[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    
    return max(dp)
```
- **Time**: O(n²)
- **Space**: O(n)

**Optimized LIS using Binary Search**
```
function LISOptimized(arr):
    tails = []
    
    for num in arr:
        pos = binarySearch(tails, num)
        if pos == length(tails):
            tails.append(num)
        else:
            tails[pos] = num
    
    return length(tails)
```
- **Time**: O(n log n)

**Coin Change Problem**
```
function coinChange(coins, amount):
    dp = array of size amount+1 filled with ∞
    dp[0] = 0
    
    for i = 1 to amount:
        for coin in coins:
            if coin <= i:
                dp[i] = min(dp[i], dp[i - coin] + 1)
    
    return dp[amount] if dp[amount] != ∞ else -1
```
- **Time**: O(amount × coins)
- **Space**: O(amount)

**Maximum Subarray Sum (Kadane's Algorithm)**
```
function maxSubarraySum(arr):
    maxSoFar = arr[0]
    maxEndingHere = arr[0]
    
    for i = 1 to length(arr)-1:
        maxEndingHere = max(arr[i], maxEndingHere + arr[i])
        maxSoFar = max(maxSoFar, maxEndingHere)
    
    return maxSoFar
```
- **Time**: O(n)
- **Space**: O(1)

**House Robber Problem**
```
function rob(houses):
    if length(houses) == 0:
        return 0
    if length(houses) == 1:
        return houses[0]
    
    dp = array of size length(houses)
    dp[0] = houses[0]
    dp[1] = max(houses[0], houses[1])
    
    for i = 2 to length(houses)-1:
        dp[i] = max(dp[i-1], dp[i-2] + houses[i])
    
    return dp[length(houses)-1]
```

**Palindrome Partitioning**
```
function minPalindromePartitions(str):
    n = length(str)
    
    // Precompute palindrome table
    isPalindrome = 2D boolean array of size n x n
    for i = 0 to n-1:
        for j = i to n-1:
            if i == j:
                isPalindrome[i][j] = true
            elif j == i + 1:
                isPalindrome[i][j] = (str[i] == str[j])
            else:
                isPalindrome[i][j] = (str[i] == str[j]) and isPalindrome[i+1][j-1]
    
    // DP for minimum cuts
    dp = array of size n
    for i = 0 to n-1:
        if isPalindrome[0][i]:
            dp[i] = 0
        else:
            dp[i] = ∞
            for j = 0 to i-1:
                if isPalindrome[j+1][i]:
                    dp[i] = min(dp[i], dp[j] + 1)
    
    return dp[n-1]
```

### Matrix Chain Multiplication

**Problem**: Find optimal way to parenthesize matrix chain multiplication

```
function matrixChainMultiplication(dimensions):
    n = length(dimensions) - 1  // Number of matrices
    dp = 2D array of size n x n filled with 0
    
    // l is chain length
    for l = 2 to n:
        for i = 0 to n-l:
            j = i + l - 1
            dp[i][j] = ∞
            
            for k = i to j-1:
                cost = dp[i][k] + dp[k+1][j] + 
                       dimensions[i] * dimensions[k+1] * dimensions[j+1]
                dp[i][j] = min(dp[i][j], cost)
    
    return dp[0][n-1]
```

### DP on Trees

**Diameter of Binary Tree**
```
function diameter(root):
    maxDiameter = 0
    
    function height(node):
        if node == null:
            return 0
        
        leftHeight = height(node.left)
        rightHeight = height(node.right)
        
        // Update diameter
        maxDiameter = max(maxDiameter, leftHeight + rightHeight)
        
        return 1 + max(leftHeight, rightHeight)
    
    height(root)
    return maxDiameter
```

### Space Optimization Techniques

**1D DP Array**
```
function knapsackOptimized(weights, values, capacity):
    dp = array of size capacity+1 filled with 0
    
    for i = 0 to length(weights)-1:
        for w = capacity down to weights[i]:
            dp[w] = max(dp[w], dp[w - weights[i]] + values[i])
    
    return dp[capacity]
```

**Rolling Array**
```
function LCSOptimized(str1, str2):
    m = length(str1)
    n = length(str2)
    
    // Use only two rows
    prev = array of size n+1 filled with 0
    curr = array of size n+1 filled with 0
    
    for i = 1 to m:
        for j = 1 to n:
            if str1[i-1] == str2[j-1]:
                curr[j] = prev[j-1] + 1
            else:
                curr[j] = max(prev[j], curr[j-1])
        
        // Swap arrays
        prev, curr = curr, prev
    
    return prev[n]
```

### DP Problem Identification

**Signs of DP Problem**
- **Optimal Substructure**: Optimal solution contains optimal subproblems
- **Overlapping Subproblems**: Same subproblems solved multiple times
- **Choices**: At each step, make a choice that affects future choices

**Common DP Patterns**
- **Linear DP**: 1D array, depends on previous elements
- **Grid DP**: 2D array, path problems
- **Interval DP**: Problems on ranges/intervals
- **Tree DP**: Problems on tree structures
- **Bitmask DP**: Use bitmasks to represent states

---

## 21. Divide and Conquer {#divide-conquer}

### Understanding Divide and Conquer

**Definition**
Divide and Conquer breaks a problem into smaller subproblems, solves them recursively, and combines results.

**Three Steps**
1. **Divide**: Break problem into smaller subproblems
2. **Conquer**: Solve subproblems recursively
3. **Combine**: Merge solutions of subproblems

**Recurrence Relations**
Most D&C algorithms follow: T(n) = aT(n/b) + f(n)
- a: number of subproblems
- n/b: size of each subproblem
- f(n): cost of divide and combine steps

### Master Theorem

**For recurrence T(n) = aT(n/b) + f(n)**

**Case 1**: If f(n) = O(n^(log_b(a) - ε)) for some ε > 0
- Then T(n) = Θ(n^log_b(a))

**Case 2**: If f(n) = Θ(n^log_b(a))
- Then T(n) = Θ(n^log_b(a) × log n)

**Case 3**: If f(n) = Ω(n^(log_b(a) + ε)) for some ε > 0, and af(n/b) ≤ cf(n) for some c < 1
- Then T(n) = Θ(f(n))

### Classic Divide and Conquer Algorithms

**Merge Sort**
```
function mergeSort(arr, left, right):
    if left < right:
        mid = (left + right) / 2
        
        // Divide
        mergeSort(arr, left, mid)
        mergeSort(arr, mid + 1, right)
        
        // Combine
        merge(arr, left, mid, right)

function merge(arr, left, mid, right):
    leftArr = arr[left...mid]
    rightArr = arr[mid+1...right]
    
    i = j = 0
    k = left
    
    while i < length(leftArr) and j < length(rightArr):
        if leftArr[i] <= rightArr[j]:
            arr[k] = leftArr[i]
            i++
        else:
            arr[k] = rightArr[j]
            j++
        k++
    
    // Copy remaining elements
    while i < length(leftArr):
        arr[k++] = leftArr[i++]
    while j < length(rightArr):
        arr[k++] = rightArr[j++]
```
- **Recurrence**: T(n) = 2T(n/2) + O(n)
- **Time**: O(n log n)

**Quick Sort**
```
function quickSort(arr, low, high):
    if low < high:
        // Divide
        pivotIndex = partition(arr, low, high)
        
        // Conquer
        quickSort(arr, low, pivotIndex - 1)
        quickSort(arr, pivotIndex + 1, high)

function partition(arr, low, high):
    pivot = arr[high]
    i = low - 1
    
    for j = low to high - 1:
        if arr[j] <= pivot:
            i++
            swap(arr[i], arr[j])
    
    swap(arr[i + 1], arr[high])
    return i + 1
```
- **Average**: T(n) = 2T(n/2) + O(n) = O(n log n)
- **Worst**: T(n) = T(n-1) + O(n) = O(n²)

**Binary Search**
```
function binarySearch(arr, target, left, right):
    if left > right:
        return -1
    
    mid = left + (right - left) / 2
    
    if arr[mid] == target:
        return mid
    elif arr[mid] > target:
        return binarySearch(arr, target, left, mid - 1)
    else:
        return binarySearch(arr, target, mid + 1, right)
```
- **Recurrence**: T(n) = T(n/2) + O(1)
- **Time**: O(log n)

**Maximum Subarray (Divide and Conquer)**
```
function maxSubarray(arr, left, right):
    if left == right:
        return arr[left]
    
    mid = (left + right) / 2
    
    // Conquer
    leftMax = maxSubarray(arr, left, mid)
    rightMax = maxSubarray(arr, mid + 1, right)
    
    // Combine - find max crossing subarray
    leftSum = -∞
    sum = 0
    for i = mid down to left:
        sum += arr[i]
        leftSum = max(leftSum, sum)
    
    rightSum = -∞
    sum = 0
    for i = mid + 1 to right:
        sum += arr[i]
        rightSum = max(rightSum, sum)
    
    crossSum = leftSum + rightSum
    
    return max(leftMax, rightMax, crossSum)
```
- **Recurrence**: T(n) = 2T(n/2) + O(n)
- **Time**: O(n log n)

**Strassen's Matrix Multiplication**
```
function strassenMultiply(A, B):
    n = size of matrices
    
    if n == 1:
        return A[0][0] * B[0][0]
    
    // Divide matrices into quadrants
    A11, A12, A21, A22 = divide(A)
    B11, B12, B21, B22 = divide(B)
    
    // Compute 7 products
    P1 = strassenMultiply(A11 + A22, B11 + B22)
    P2 = strassenMultiply(A21 + A22, B11)
    P3 = strassenMultiply(A11, B12 - B22)
    P4 = strassenMultiply(A22, B21 - B11)
    P5 = strassenMultiply(A11 + A12, B22)
    P6 = strassenMultiply(A21 - A11, B11 + B12)
    P7 = strassenMultiply(A12 - A22, B21 + B22)
    
    // Combine results
    C11 = P1 + P4 - P5 + P7
    C12 = P3 + P5
    C21 = P2 + P4
    C22 = P1 - P2 + P3 + P6
    
    return combine(C11, C12, C21, C22)
```
- **Recurrence**: T(n) = 7T(n/2) + O(n²)
- **Time**: O(n^log₂7) ≈ O(n^2.81)

**Closest Pair of Points**
```
function closestPair(points):
    if length(points) <= 3:
        return bruteForce(points)
    
    // Divide
    mid = length(points) / 2
    midPoint = points[mid]
    
    leftPoints = points[0...mid]
    rightPoints = points[mid+1...end]
    
    // Conquer
    leftMin = closestPair(leftPoints)
    rightMin = closestPair(rightPoints)
    
    minDist = min(leftMin, rightMin)
    
    // Combine - check points near dividing line
    strip = []
    for point in points:
        if abs(point.x - midPoint.x) < minDist:
            strip.append(point)
    
    return min(minDist, stripClosest(strip, minDist))

function stripClosest(strip, minDist):
    sort(strip) by y-coordinate
    
    for i = 0 to length(strip) - 1:
        j = i + 1
        while j < length(strip) and (strip[j].y - strip[i].y) < minDist:
            minDist = min(minDist, distance(strip[i], strip[j]))
            j++
    
    return minDist
```
- **Time**: O(n log n)

### Integer Multiplication

**Karatsuba Algorithm**
```
function karatsuba(x, y):
    if x < 10 or y < 10:
        return x * y
    
    n = max(digits(x), digits(y))
    m = n / 2
    
    // Split numbers
    high1 = x / 10^m
    low1 = x % 10^m
    high2 = y / 10^m
    low2 = y % 10^m
    
    // Three recursive calls
    z0 = karatsuba(low1, low2)
    z1 = karatsuba(low1 + high1, low2 + high2)
    z2 = karatsuba(high1, high2)
    
    return z2 * 10^(2*m) + (z1 - z2 - z0) * 10^m + z0
```
- **Recurrence**: T(n) = 3T(n/2) + O(n)
- **Time**: O(n^log₂3) ≈ O(n^1.59)

### Fast Fourier Transform (FFT)

**Polynomial Multiplication using FFT**
```
function FFT(coefficients, inverse = false):
    n = length(coefficients)
    
    if n == 1:
        return coefficients
    
    // Divide
    even = [coefficients[0], coefficients[2], ...]
    odd = [coefficients[1], coefficients[3], ...]
    
    // Conquer
    evenFFT = FFT(even, inverse)
    oddFFT = FFT(odd, inverse)
    
    // Combine
    result = array of size n
    for k = 0 to n/2 - 1:
        t = exp(-2πi * k / n) * oddFFT[k]  // or +2πi for inverse
        result[k] = evenFFT[k] + t
        result[k + n/2] = evenFFT[k] - t
    
    if inverse:
        for i = 0 to n-1:
            result[i] /= n
    
    return result
```
- **Time**: O(n log n)

### When to Use Divide and Conquer

**Good Candidates**
- Problem can be broken into similar subproblems
- Subproblems are independent
- Combining solutions is efficient
- Base case is simple

**Examples**
- **Sorting**: Merge sort, quick sort
- **Searching**: Binary search
- **Mathematical**: Fast exponentiation, matrix multiplication
- **Geometric**: Closest pair, convex hull

### Advantages and Disadvantages

**Advantages**
- **Parallelizable**: Subproblems can be solved independently
- **Cache Efficient**: Better memory locality
- **Optimal**: Often leads to optimal algorithms

**Disadvantages**
- **Overhead**: Recursive calls have overhead
- **Memory**: May use more memory due to recursion
- **Complex**: Can be harder to implement than iterative solutions

---

## 22. Backtracking {#backtracking}

### Understanding Backtracking

**Definition**
Backtracking is a systematic method for solving problems by trying partial solutions and abandoning them if they cannot lead to a complete solution.

**Key Concepts**
- **State Space Tree**: Tree of all possible states
- **Promising**: A node is promising if it can lead to a solution
- **Pruning**: Abandoning unpromising branches
- **Backtrack**: Return to previous state when current path fails

**General Template**
```
function backtrack(state):
    if isComplete(state):
        if isValid(state):
            addSolution(state)
        return
    
    for each choice in getChoices(state):
        if isPromising(state, choice):
            makeChoice(state, choice)
            backtrack(state)
            undoChoice(state, choice)  // Backtrack
```

### Classic Backtracking Problems

**N-Queens Problem**
```
function solveNQueens(n):
    board = 2D array of size n x n filled with false
    solutions = []
    
    backtrackQueens(board, 0, solutions)
    return solutions

function backtrackQueens(board, row, solutions):
    if row == n:
        solutions.append(copy(board))
        return
    
    for col = 0 to n-1:
        if isSafe(board, row, col):
            board[row][col] = true
            backtrackQueens(board, row + 1, solutions)
            board[row][col] = false  // Backtrack

function isSafe(board, row, col):
    // Check column
    for i = 0 to row-1:
        if board[i][col]:
            return false
    
    // Check diagonal (top-left to bottom-right)
    i = row - 1, j = col - 1
    while i >= 0 and j >= 0:
        if board[i][j]:
            return false
        i--, j--
    
    // Check diagonal (top-right to bottom-left)
    i = row - 1, j = col + 1
    while i >= 0 and j < n:
        if board[i][j]:
            return false
        i--, j++
    
    return true
```
- **Time**: O(N!) in worst case
- **Space**: O(N) for recursion stack

**Sudoku Solver**
```
function solveSudoku(board):
    return backtrackSudoku(board)

function backtrackSudoku(board):
    for row = 0 to 8:
        for col = 0 to 8:
            if board[row][col] == 0:  // Empty cell
                for num = 1 to 9:
                    if isValidSudoku(board, row, col, num):
                        board[row][col] = num
                        
                        if backtrackSudoku(board):
                            return true
                        
                        board[row][col] = 0  // Backtrack
                
                return false  // No valid number found
    
    return true  // All cells filled

function isValidSudoku(board, row, col, num):
    // Check row
    for j = 0 to 8:
        if board[row][j] == num:
            return false
    
    // Check column
    for i = 0 to 8:
        if board[i][col] == num:
            return false
    
    // Check 3x3 box
    startRow = (row / 3) * 3
    startCol = (col / 3) * 3
    for i = startRow to startRow + 2:
        for j = startCol to startCol + 2:
            if board[i][j] == num:
                return false
    
    return true
```

**Generate All Permutations**
```
function permutations(nums):
    result = []
    backtrackPermutations(nums, [], result)
    return result

function backtrackPermutations(nums, current, result):
    if length(current) == length(nums):
        result.append(copy(current))
        return
    
    for i = 0 to length(nums)-1:
        if nums[i] not in current:
            current.append(nums[i])
            backtrackPermutations(nums, current, result)
            current.removeLast()  // Backtrack
```

**Generate All Subsets**
```
function subsets(nums):
    result = []
    backtrackSubsets(nums, 0, [], result)
    return result

function backtrackSubsets(nums, start, current, result):
    result.append(copy(current))
    
    for i = start to length(nums)-1:
        current.append(nums[i])
        backtrackSubsets(nums, i + 1, current, result)
        current.removeLast()  // Backtrack
```

**Combination Sum**
```
function combinationSum(candidates, target):
    result = []
    sort(candidates)
    backtrackCombination(candidates, target, 0, [], result)
    return result

function backtrackCombination(candidates, target, start, current, result):
    if target == 0:
        result.append(copy(current))
        return
    
    for i = start to length(candidates)-1:
        if candidates[i] > target:
            break  // Pruning
        
        current.append(candidates[i])
        backtrackCombination(candidates, target - candidates[i], i, current, result)
        current.removeLast()  // Backtrack
```

**Word Search in Grid**
```
function wordSearch(board, word):
    rows = length(board)
    cols = length(board[0])
    
    for i = 0 to rows-1:
        for j = 0 to cols-1:
            if backtrackWordSearch(board, word, i, j, 0):
                return true
    
    return false

function backtrackWordSearch(board, word, row, col, index):
    if index == length(word):
        return true
    
    if row < 0 or row >= rows or col < 0 or col >= cols or
       board[row][col] != word[index]:
        return false
    
    // Mark as visited
    temp = board[row][col]
    board[row][col] = '#'
    
    // Explore all 4 directions
    found = backtrackWordSearch(board, word, row+1, col, index+1) or
            backtrackWordSearch(board, word, row-1, col, index+1) or
            backtrackWordSearch(board, word, row, col+1, index+1) or
            backtrackWordSearch(board, word, row, col-1, index+1)
    
    // Backtrack
    board[row][col] = temp
    
    return found
```

**Palindrome Partitioning**
```
function palindromePartition(s):
    result = []
    backtrackPalindrome(s, 0, [], result)
    return result

function backtrackPalindrome(s, start, current, result):
    if start == length(s):
        result.append(copy(current))
        return
    
    for end = start to length(s)-1:
        substring = s[start:end+1]
        if isPalindrome(substring):
            current.append(substring)
            backtrackPalindrome(s, end + 1, current, result)
            current.removeLast()  // Backtrack

function isPalindrome(s):
    left = 0, right = length(s) - 1
    while left < right:
        if s[left] != s[right]:
            return false
        left++, right--
    return true
```

**Graph Coloring**
```
function graphColoring(graph, colors):
    n = number of vertices
    coloring = array of size n filled with -1
    
    return backtrackColoring(graph, colors, 0, coloring)

function backtrackColoring(graph, colors, vertex, coloring):
    if vertex == n:
        return true  // All vertices colored
    
    for color = 0 to colors-1:
        if isSafeColor(graph, vertex, color, coloring):
            coloring[vertex] = color
            
            if backtrackColoring(graph, colors, vertex + 1, coloring):
                return true
            
            coloring[vertex] = -1  // Backtrack
    
    return false

function isSafeColor(graph, vertex, color, coloring):
    for neighbor in graph[vertex]:
        if coloring[neighbor] == color:
            return false
    return true
```

### Optimization Techniques

**Pruning**
- **Bound Checking**: Eliminate branches that cannot lead to better solutions
- **Constraint Propagation**: Use constraints to reduce search space
- **Heuristics**: Use domain knowledge to guide search

**Example: Branch and Bound for TSP**
```
function TSP(graph):
    n = number of cities
    visited = array of size n filled with false
    path = []
    minCost = ∞
    bestPath = []
    
    visited[0] = true
    path.append(0)
    
    backtrackTSP(graph, visited, path, 0, 0, minCost, bestPath)
    return bestPath, minCost

function backtrackTSP(graph, visited, path, currentCost, level, minCost, bestPath):
    if level == n - 1:
        totalCost = currentCost + graph[path[level]][0]
        if totalCost < minCost:
            minCost = totalCost
            bestPath = copy(path)
        return
    
    for city = 0 to n-1:
        if not visited[city]:
            newCost = currentCost + graph[path[level]][city]
            
            // Pruning: if current cost already exceeds minimum, skip
            if newCost < minCost:
                visited[city] = true
                path.append(city)
                
                backtrackTSP(graph, visited, path, newCost, level + 1, minCost, bestPath)
                
                // Backtrack
                visited[city] = false
                path.removeLast()
```

### When to Use Backtracking

**Good Candidates**
- **Constraint Satisfaction**: Problems with constraints
- **Combinatorial**: Generate all possible solutions
- **Optimization**: Find best solution among many
- **Decision**: Yes/no problems with complex constraints

**Examples**
- **Puzzle Solving**: Sudoku, N-Queens, crossword
- **Game Playing**: Chess, checkers (with minimax)
- **Scheduling**: Job scheduling with constraints
- **Path Finding**: With complex constraints

### Advantages and Disadvantages

**Advantages**
- **Complete**: Finds all solutions if they exist
- **Optimal**: Can find optimal solution with proper pruning
- **Flexible**: Can handle complex constraints
- **Memory Efficient**: Uses only O(depth) space

**Disadvantages**
- **Exponential Time**: Can be very slow for large problems
- **No Guarantee**: May take very long time
- **Implementation**: Can be complex to implement correctly

---

## 23. Advanced Topics {#advanced-topics}

### Bit Manipulation

**Basic Operations**
```
// Set bit at position i
number |= (1 << i)

// Clear bit at position i
number &= ~(1 << i)

// Toggle bit at position i
number ^= (1 << i)

// Check if bit at position i is set
boolean isSet = (number & (1 << i)) != 0

// Count number of set bits
function countSetBits(n):
    count = 0
    while n:
        count += n & 1
        n >>= 1
    return count

// Brian Kernighan's algorithm
function countSetBitsOptimized(n):
    count = 0
    while n:
        n &= (n - 1)  // Removes rightmost set bit
        count++
    return count
```

**Power of Two Check**
```
function isPowerOfTwo(n):
    return n > 0 and (n & (n - 1)) == 0
```

**Find Single Number**
```
function singleNumber(nums):
    result = 0
    for num in nums:
        result ^= num  // XOR cancels out duplicates
    return result
```

### Trie (Prefix Tree)

**Implementation**
```
class TrieNode:
    children: array of size 26
    isEndOfWord: boolean

class Trie:
    root: TrieNode
    
    constructor():
        root = new TrieNode()
    
    insert(word):
        current = root
        for char in word:
            index = char - 'a'
            if current.children[index] == null:
                current.children[index] = new TrieNode()
            current = current.children[index]
        current.isEndOfWord = true
    
    search(word):
        current = root
        for char in word:
            index = char - 'a'
            if current.children[index] == null:
                return false
            current = current.children[index]
        return current.isEndOfWord
    
    startsWith(prefix):
        current = root
        for char in prefix:
            index = char - 'a'
            if current.children[index] == null:
                return false
            current = current.children[index]
        return true
```

### Segment Tree

**Implementation**
```
class SegmentTree:
    tree: array to store segment tree
    n: size of original array
    
    constructor(arr):
        n = length(arr)
        tree = array of size 4*n
        build(arr, 0, 0, n-1)
    
    build(arr, node, start, end):
        if start == end:
            tree[node] = arr[start]
        else:
            mid = (start + end) / 2
            build(arr, 2*node+1, start, mid)
            build(arr, 2*node+2, mid+1, end)
            tree[node] = tree[2*node+1] + tree[2*node+2]
    
    update(node, start, end, index, value):
        if start == end:
            tree[node] = value
        else:
            mid = (start + end) / 2
            if index <= mid:
                update(2*node+1, start, mid, index, value)
            else:
                update(2*node+2, mid+1, end, index, value)
            tree[node] = tree[2*node+1] + tree[2*node+2]
    
    query(node, start, end, left, right):
        if right < start or end < left:
            return 0
        if left <= start and end <= right:
            return tree[node]
        
        mid = (start + end) / 2
        leftSum = query(2*node+1, start, mid, left, right)
        rightSum = query(2*node+2, mid+1, end, left, right)
        return leftSum + rightSum
```

### Fenwick Tree (Binary Indexed Tree)

**Implementation**
```
class FenwickTree:
    tree: array of size n+1
    n: size
    
    constructor(size):
        n = size
        tree = array of size n+1 filled with 0
    
    update(index, delta):
        while index <= n:
            tree[index] += delta
            index += index & (-index)  // Add LSB
    
    query(index):
        sum = 0
        while index > 0:
            sum += tree[index]
            index -= index & (-index)  // Remove LSB
        return sum
    
    rangeQuery(left, right):
        return query(right) - query(left - 1)
```

### Disjoint Set Union (Union-Find)

**Optimized Implementation**
```
class UnionFind:
    parent: array
    rank: array
    
    constructor(n):
        parent = [0, 1, 2, ..., n-1]
        rank = [0, 0, 0, ..., 0]
    
    find(x):
        if parent[x] != x:
            parent[x] = find(parent[x])  // Path compression
        return parent[x]
    
    union(x, y):
        rootX = find(x)
        rootY = find(y)
        
        if rootX != rootY:
            // Union by rank
            if rank[rootX] < rank[rootY]:
                parent[rootX] = rootY
            elif rank[rootX] > rank[rootY]:
                parent[rootY] = rootX
            else:
                parent[rootY] = rootX
                rank[rootX]++
    
    connected(x, y):
        return find(x) == find(y)
```

---

## 24. MCQ Practice Questions {#mcq-questions}

### Time Complexity Questions

**Q1. What is the time complexity of the following code?**
```
for i = 1 to n:
    for j = 1 to i:
        print i, j
```
a) O(n)  
b) O(n log n)  
c) O(n²)  
d) O(n³)

**Answer: c) O(n²)**
*Explanation: Inner loop runs 1+2+3+...+n = n(n+1)/2 times = O(n²)*

**Q2. Binary search has time complexity:**
a) O(n)  
b) O(log n)  
c) O(n log n)  
d) O(n²)

**Answer: b) O(log n)**

**Q3. What is the worst-case time complexity of Quick Sort?**
a) O(n log n)  
b) O(n²)  
c) O(n)  
d) O(log n)

**Answer: b) O(n²)**

### Data Structure Questions

**Q4. Which data structure is used for BFS traversal?**
a) Stack  
b) Queue  
c) Priority Queue  
d) Array

**Answer: b) Queue**

**Q5. In a max heap, the parent node is:**
a) Smaller than children  
b) Greater than or equal to children  
c) Equal to children  
d) No specific relationship

**Answer: b) Greater than or equal to children**

**Q6. Hash table collision resolution using chaining has average search time:**
a) O(1)  
b) O(log n)  
c) O(n)  
d) O(n log n)

**Answer: a) O(1)**

### Tree Questions

**Q7. In a binary search tree, inorder traversal gives:**
a) Random order  
b) Sorted order  
c) Reverse sorted order  
d) Level order

**Answer: b) Sorted order**

**Q8. Maximum number of nodes in a binary tree of height h is:**
a) 2^h  
b) 2^h - 1  
c) 2^(h+1) - 1  
d) 2^(h-1)

**Answer: c) 2^(h+1) - 1**

**Q9. A complete binary tree with n nodes has height:**
a) log n  
b) ⌊log₂ n⌋  
c) ⌈log₂(n+1)⌉ - 1  
d) n

**Answer: c) ⌈log₂(n+1)⌉ - 1**

### Graph Questions

**Q10. DFS uses which data structure?**
a) Queue  
b) Stack  
c) Priority Queue  
d) Array

**Answer: b) Stack**

**Q11. Dijkstra's algorithm finds:**
a) Minimum spanning tree  
b) Single source shortest path  
c) All pairs shortest path  
d) Maximum flow

**Answer: b) Single source shortest path**

**Q12. Kruskal's algorithm is used for:**
a) Shortest path  
b) Topological sorting  
c) Minimum spanning tree  
d) Strongly connected components

**Answer: c) Minimum spanning tree**

### Sorting Questions

**Q13. Which sorting algorithm is stable?**
a) Quick Sort  
b) Heap Sort  
c) Merge Sort  
d) Selection Sort

**Answer: c) Merge Sort**

**Q14. Best case time complexity of Bubble Sort is:**
a) O(n)  
b) O(n log n)  
c) O(n²)  
d) O(log n)

**Answer: a) O(n)**

**Q15. Counting sort works best when:**
a) Array is already sorted  
b) Range of input is small  
c) Array size is large  
d) Elements are floating point

**Answer: b) Range of input is small**

### Dynamic Programming Questions

**Q16. Dynamic programming is applicable when problem has:**
a) Optimal substructure only  
b) Overlapping subproblems only  
c) Both optimal substructure and overlapping subproblems  
d) Neither

**Answer: c) Both optimal substructure and overlapping subproblems**

**Q17. 0/1 Knapsack problem can be solved using:**
a) Greedy approach optimally  
b) Dynamic programming optimally  
c) Divide and conquer optimally  
d) All of the above

**Answer: b) Dynamic programming optimally**

**Q18. Time complexity of LCS using dynamic programming is:**
a) O(m + n)  
b) O(m × n)  
c) O(m² × n²)  
d) O(2^(m+n))

**Answer: b) O(m × n)**

### Greedy Algorithm Questions

**Q19. Fractional knapsack can be solved optimally using:**
a) Dynamic programming  
b) Greedy approach  
c) Backtracking  
d) Divide and conquer

**Answer: b) Greedy approach**

**Q20. Activity selection problem uses greedy choice:**
a) Select activity with earliest start time  
b) Select activity with latest start time  
c) Select activity with earliest finish time  
d) Select activity with maximum duration

**Answer: c) Select activity with earliest finish time**

### Divide and Conquer Questions

**Q21. Master theorem is used to solve:**
a) Dynamic programming recurrences  
b) Divide and conquer recurrences  
c) Greedy algorithm complexity  
d) Graph algorithm complexity

**Answer: b) Divide and conquer recurrences**

**Q22. Merge sort follows which approach?**
a) Greedy  
b) Dynamic programming  
c) Divide and conquer  
d) Backtracking

**Answer: c) Divide and conquer**

### Backtracking Questions

**Q23. N-Queens problem is solved using:**
a) Greedy approach  
b) Dynamic programming  
c) Backtracking  
d) Divide and conquer

**Answer: c) Backtracking**

**Q24. Backtracking is used when:**
a) Problem has optimal substructure  
b) Problem requires exploring all possibilities  
c) Problem has overlapping subproblems  
d) Problem can be divided into subproblems

**Answer: b) Problem requires exploring all possibilities**

### Advanced Topics Questions

**Q25. Trie is used for:**
a) Sorting strings  
b) String searching and prefix matching  
c) Graph traversal  
d) Numerical computations

**Answer: b) String searching and prefix matching**

**Q26. Union-Find data structure is used in:**
a) Dijkstra's algorithm  
b) Kruskal's MST algorithm  
c) Topological sorting  
d) Binary search

**Answer: b) Kruskal's MST algorithm**

**Q27. Segment tree is used for:**
a) Sorting  
b) Range queries and updates  
c) Graph traversal  
d) String matching

**Answer: b) Range queries and updates**

**Q28. Time complexity of finding if a number is power of 2 using bit manipulation:**
a) O(log n)  
b) O(n)  
c) O(1)  
d) O(n log n)

**Answer: c) O(1)**

### Complexity Analysis Questions

**Q29. Space complexity of merge sort is:**
a) O(1)  
b) O(log n)  
c) O(n)  
d) O(n log n)

**Answer: c) O(n)**

**Q30. Which has better average case performance?**
a) Quick Sort  
b) Merge Sort  
c) Heap Sort  
d) All have same average case

**Answer: a) Quick Sort**

---

## Study Tips for MCQ Exams

### 1. Focus on Fundamentals
- **Time Complexity**: Master Big O analysis
- **Space Complexity**: Understand memory usage
- **Algorithm Properties**: Stable vs unstable, in-place vs not

### 2. Practice Pattern Recognition
- **Sorting**: Know when to use which algorithm
- **Searching**: Binary search variations
- **Graph**: BFS vs DFS applications
- **DP**: Identify optimal substructure

### 3. Memorize Key Facts
- **Sorting Complexities**: All algorithms' best/average/worst cases
- **Tree Properties**: Height, nodes, traversals
- **Graph Algorithms**: Applications and complexities
- **Data Structure Operations**: Insert/delete/search complexities

### 4. Common Mistakes to Avoid
- **Off-by-one errors** in complexity analysis
- **Confusing stable vs unstable** sorting
- **Mixing up BFS vs DFS** applications
- **Forgetting space complexity** of recursive algorithms

### 5. Quick Reference Formulas
- **Tree height**: ⌊log₂ n⌋ for complete binary tree
- **Heap operations**: O(log n) for insert/delete
- **Hash table**: O(1) average, O(n) worst case
- **Graph traversal**: O(V + E) for adjacency list

This completes the comprehensive DSA guide covering all essential topics for freshers and MCQ exam preparation. The guide provides theoretical foundations, practical implementations, and extensive practice questions to ensure thorough understanding of data structures and algorithms.