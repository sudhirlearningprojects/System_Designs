# Merge Sort

## Overview
Merge Sort is a divide-and-conquer algorithm that divides the input array into two halves, recursively sorts them, and then merges the two sorted halves.

## Algorithm

1. **Divide**: Split the array into two halves
2. **Conquer**: Recursively sort both halves
3. **Combine**: Merge the two sorted halves into a single sorted array

## Implementation

```java
public class MergeSort {
    
    public static void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            int mid = left + (right - left) / 2;
            
            // Sort first and second halves
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);
            
            // Merge the sorted halves
            merge(arr, left, mid, right);
        }
    }
    
    private static void merge(int[] arr, int left, int mid, int right) {
        // Calculate sizes of two subarrays
        int n1 = mid - left + 1;
        int n2 = right - mid;
        
        // Create temp arrays
        int[] L = new int[n1];
        int[] R = new int[n2];
        
        // Copy data to temp arrays
        for (int i = 0; i < n1; i++)
            L[i] = arr[left + i];
        for (int j = 0; j < n2; j++)
            R[j] = arr[mid + 1 + j];
        
        // Merge the temp arrays back
        int i = 0, j = 0, k = left;
        
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }
        
        // Copy remaining elements of L[]
        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }
        
        // Copy remaining elements of R[]
        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }
    
    public static void main(String[] args) {
        int[] arr = {38, 27, 43, 3, 9, 82, 10};
        System.out.println("Original: " + Arrays.toString(arr));
        
        mergeSort(arr, 0, arr.length - 1);
        System.out.println("Sorted: " + Arrays.toString(arr));
    }
}
```

## Dry Run Example

**Input**: `[38, 27, 43, 3, 9, 82, 10]`

### Step-by-Step Execution:

```
Level 0: [38, 27, 43, 3, 9, 82, 10]
         Split at mid = 3
         ↓
Level 1: [38, 27, 43, 3]          [9, 82, 10]
         Split at mid=1            Split at mid=1
         ↓                         ↓
Level 2: [38, 27]    [43, 3]      [9, 82]    [10]
         ↓           ↓             ↓
Level 3: [38] [27]   [43] [3]     [9] [82]   [10]

--- Now Merge Back Up ---

Level 3→2: [27, 38]  [3, 43]      [9, 82]    [10]
           Merge     Merge         Merge
           ↓         ↓             ↓
Level 2→1: [3, 27, 38, 43]        [9, 10, 82]
           Merge these two
           ↓
Level 1→0: [3, 9, 10, 27, 38, 43, 82]
```

### Detailed Merge Example (Level 2→1):
Merging `[27, 38]` and `[3, 43]`:

```
L = [27, 38]    R = [3, 43]
i=0, j=0, k=0

Step 1: L[0]=27 vs R[0]=3  → 3 < 27  → arr[0]=3,  j=1
Step 2: L[0]=27 vs R[1]=43 → 27 < 43 → arr[1]=27, i=1
Step 3: L[1]=38 vs R[1]=43 → 38 < 43 → arr[2]=38, i=2
Step 4: i=2 (done), copy R[1]=43    → arr[3]=43

Result: [3, 27, 38, 43]
```

## Edge Test Cases

```java
// Test Case 1: Empty array
int[] arr1 = {};
mergeSort(arr1, 0, arr1.length - 1);
// Expected: []

// Test Case 2: Single element
int[] arr2 = {5};
mergeSort(arr2, 0, arr2.length - 1);
// Expected: [5]

// Test Case 3: Two elements (sorted)
int[] arr3 = {1, 2};
mergeSort(arr3, 0, arr3.length - 1);
// Expected: [1, 2]

// Test Case 4: Two elements (unsorted)
int[] arr4 = {2, 1};
mergeSort(arr4, 0, arr4.length - 1);
// Expected: [1, 2]

// Test Case 5: Already sorted
int[] arr5 = {1, 2, 3, 4, 5};
mergeSort(arr5, 0, arr5.length - 1);
// Expected: [1, 2, 3, 4, 5]

// Test Case 6: Reverse sorted
int[] arr6 = {5, 4, 3, 2, 1};
mergeSort(arr6, 0, arr6.length - 1);
// Expected: [1, 2, 3, 4, 5]

// Test Case 7: All duplicates
int[] arr7 = {3, 3, 3, 3};
mergeSort(arr7, 0, arr7.length - 1);
// Expected: [3, 3, 3, 3]

// Test Case 8: Negative numbers
int[] arr8 = {-5, 3, -1, 0, 8, -3};
mergeSort(arr8, 0, arr8.length - 1);
// Expected: [-5, -3, -1, 0, 3, 8]

// Test Case 9: Large array with duplicates
int[] arr9 = {10, 5, 2, 5, 8, 2, 10, 1};
mergeSort(arr9, 0, arr9.length - 1);
// Expected: [1, 2, 2, 5, 5, 8, 10, 10]
```

## Time Complexity

### Best Case: O(n log n)
- Even if the array is already sorted, merge sort still divides and merges
- No optimization for sorted arrays

### Average Case: O(n log n)
- Array is divided into log n levels
- At each level, merging takes O(n) time
- Total: O(n) × O(log n) = O(n log n)

### Worst Case: O(n log n)
- Same as average case
- Merge sort always performs the same number of operations regardless of input

### Mathematical Proof:
```
T(n) = 2T(n/2) + O(n)

Using Master Theorem:
a = 2, b = 2, f(n) = n
log_b(a) = log_2(2) = 1
f(n) = n^1

Since f(n) = Θ(n^log_b(a)), we have Case 2:
T(n) = Θ(n^1 × log n) = Θ(n log n)
```

## Space Complexity

### Auxiliary Space: O(n)
- Temporary arrays L[] and R[] are created during merge
- At any point, the total size of temp arrays = n
- Not in-place sorting

### Call Stack Space: O(log n)
- Recursion depth = log n (height of recursion tree)
- Each recursive call stores constant space on stack

### Total Space: O(n) + O(log n) = O(n)
- Auxiliary space dominates

## Advantages

1. **Stable Sort**: Maintains relative order of equal elements
2. **Predictable Performance**: Always O(n log n)
3. **Good for Linked Lists**: No random access needed
4. **External Sorting**: Efficient for sorting large files
5. **Parallelizable**: Can be easily parallelized

## Disadvantages

1. **Space Complexity**: Requires O(n) extra space
2. **Not In-Place**: Cannot sort in-place
3. **Slower for Small Arrays**: Overhead of recursion
4. **Not Adaptive**: Doesn't benefit from partially sorted data

## When to Use

- When stable sorting is required
- When predictable O(n log n) performance is needed
- When sorting linked lists
- For external sorting (large datasets)
- When parallelization is possible

## Comparison with Other Algorithms

| Algorithm    | Time (Avg) | Time (Worst) | Space | Stable |
|-------------|-----------|-------------|-------|--------|
| Merge Sort  | O(n log n)| O(n log n)  | O(n)  | Yes    |
| Quick Sort  | O(n log n)| O(n²)       | O(log n)| No   |
| Heap Sort   | O(n log n)| O(n log n)  | O(1)  | No     |
| Bubble Sort | O(n²)     | O(n²)       | O(1)  | Yes    |
| Insertion Sort| O(n²)   | O(n²)       | O(1)  | Yes    |

## Optimizations

1. **Hybrid Approach**: Use insertion sort for small subarrays (< 10 elements)
2. **In-Place Merge**: Reduce space complexity (complex implementation)
3. **Natural Merge Sort**: Take advantage of existing sorted runs
4. **Parallel Merge Sort**: Use multiple threads for divide and merge steps
