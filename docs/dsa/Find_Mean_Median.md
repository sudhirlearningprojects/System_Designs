# Find Mean and Median of Array

## Problem Statement

Given an unsorted array of n integers, find its mean (average) and median (middle value).

**Definitions:**
- **Mean:** Sum of all elements divided by count
- **Median:** Middle value when sorted (or average of two middle values for even length)

**Input:** Unsorted array of integers  
**Output:** Mean and Median

**Examples:**
```
Input:  [5, 3, 1, 4, 2]
Output: Mean = 3.0, Median = 3.0
Explanation: Sum = 15, Count = 5, Mean = 15/5 = 3.0
             Sorted: [1, 2, 3, 4, 5], Median = 3 (middle)

Input:  [1, 2, 3, 4]
Output: Mean = 2.5, Median = 2.5
Explanation: Sum = 10, Count = 4, Mean = 10/4 = 2.5
             Sorted: [1, 2, 3, 4], Median = (2+3)/2 = 2.5

Input:  [10, 20, 30]
Output: Mean = 20.0, Median = 20.0
Explanation: Sum = 60, Count = 3, Mean = 60/3 = 20.0
             Sorted: [10, 20, 30], Median = 20

Input:  [7]
Output: Mean = 7.0, Median = 7.0
Explanation: Single element

Input:  [5, 5, 5, 5]
Output: Mean = 5.0, Median = 5.0
Explanation: All same values
```

---

## Solution Approaches

### Approach 1: Sort for Median (Standard)

**Time Complexity:** O(n log n)  
**Space Complexity:** O(1) or O(n) depending on sort

```java
public static double[] findMeanAndMedian(int[] arr) {
    if (arr == null || arr.length == 0) return new double[]{0, 0};
    
    // Calculate mean
    long sum = 0;
    for (int num : arr) {
        sum += num;
    }
    double mean = (double) sum / arr.length;
    
    // Calculate median
    Arrays.sort(arr);
    double median;
    int n = arr.length;
    if (n % 2 == 0) {
        median = (arr[n/2 - 1] + arr[n/2]) / 2.0;
    } else {
        median = arr[n/2];
    }
    
    return new double[]{mean, median};
}
```

---

### Approach 2: QuickSelect for Median (Optimal)

**Time Complexity:** O(n) average for median  
**Space Complexity:** O(1)

```java
public static double[] findMeanAndMedianQuickSelect(int[] arr) {
    if (arr == null || arr.length == 0) return new double[]{0, 0};
    
    // Calculate mean
    long sum = 0;
    for (int num : arr) {
        sum += num;
    }
    double mean = (double) sum / arr.length;
    
    // Calculate median using QuickSelect
    int n = arr.length;
    double median;
    if (n % 2 == 0) {
        int left = quickSelect(arr.clone(), 0, n - 1, n/2 - 1);
        int right = quickSelect(arr.clone(), 0, n - 1, n/2);
        median = (left + right) / 2.0;
    } else {
        median = quickSelect(arr.clone(), 0, n - 1, n/2);
    }
    
    return new double[]{mean, median};
}

private static int quickSelect(int[] arr, int left, int right, int k) {
    if (left == right) return arr[left];
    
    int pivotIndex = partition(arr, left, right);
    
    if (k == pivotIndex) {
        return arr[k];
    } else if (k < pivotIndex) {
        return quickSelect(arr, left, pivotIndex - 1, k);
    } else {
        return quickSelect(arr, pivotIndex + 1, right, k);
    }
}

private static int partition(int[] arr, int left, int right) {
    int pivot = arr[right];
    int i = left;
    
    for (int j = left; j < right; j++) {
        if (arr[j] <= pivot) {
            swap(arr, i, j);
            i++;
        }
    }
    
    swap(arr, i, right);
    return i;
}

private static void swap(int[] arr, int i, int j) {
    int temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
}
```

---

### Approach 3: Separate Methods

**Time Complexity:** O(n) for mean, O(n log n) for median  
**Space Complexity:** O(1)

```java
public static double findMean(int[] arr) {
    if (arr == null || arr.length == 0) return 0;
    
    long sum = 0;
    for (int num : arr) {
        sum += num;
    }
    
    return (double) sum / arr.length;
}

public static double findMedian(int[] arr) {
    if (arr == null || arr.length == 0) return 0;
    
    Arrays.sort(arr);
    int n = arr.length;
    
    if (n % 2 == 0) {
        return (arr[n/2 - 1] + arr[n/2]) / 2.0;
    } else {
        return arr[n/2];
    }
}
```

---

## Algorithm Walkthrough

### Example: [5, 3, 1, 4, 2]

**Step-by-Step Execution:**

```
Input: [5, 3, 1, 4, 2]

Step 1: Calculate Mean
  sum = 5 + 3 + 1 + 4 + 2 = 15
  count = 5
  mean = 15 / 5 = 3.0

Step 2: Calculate Median
  Sort array: [1, 2, 3, 4, 5]
  n = 5 (odd)
  median index = n/2 = 5/2 = 2
  median = arr[2] = 3

Result: Mean = 3.0, Median = 3.0
```

### Example: [1, 2, 3, 4] (Even Length)

```
Input: [1, 2, 3, 4]

Step 1: Calculate Mean
  sum = 1 + 2 + 3 + 4 = 10
  count = 4
  mean = 10 / 4 = 2.5

Step 2: Calculate Median
  Already sorted: [1, 2, 3, 4]
  n = 4 (even)
  median = (arr[n/2 - 1] + arr[n/2]) / 2
         = (arr[1] + arr[2]) / 2
         = (2 + 3) / 2
         = 2.5

Result: Mean = 2.5, Median = 2.5
```

### Example: [10, 20, 30]

```
Input: [10, 20, 30]

Step 1: Calculate Mean
  sum = 10 + 20 + 30 = 60
  count = 3
  mean = 60 / 3 = 20.0

Step 2: Calculate Median
  Already sorted: [10, 20, 30]
  n = 3 (odd)
  median = arr[3/2] = arr[1] = 20

Result: Mean = 20.0, Median = 20.0
```

---

## Complete Implementation

```java
import java.util.*;

public class Solution {
    
    // Approach 1: Sort for Median (Standard)
    public static double[] findMeanAndMedian(int[] arr) {
        if (arr == null || arr.length == 0) return new double[]{0, 0};
        
        // Calculate mean
        long sum = 0;
        for (int num : arr) {
            sum += num;
        }
        double mean = (double) sum / arr.length;
        
        // Calculate median
        Arrays.sort(arr);
        double median;
        int n = arr.length;
        if (n % 2 == 0) {
            median = (arr[n/2 - 1] + arr[n/2]) / 2.0;
        } else {
            median = arr[n/2];
        }
        
        return new double[]{mean, median};
    }
    
    // Separate methods
    public static double findMean(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        
        long sum = 0;
        for (int num : arr) {
            sum += num;
        }
        
        return (double) sum / arr.length;
    }
    
    public static double findMedian(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        
        int[] sorted = arr.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        
        if (n % 2 == 0) {
            return (sorted[n/2 - 1] + sorted[n/2]) / 2.0;
        } else {
            return sorted[n/2];
        }
    }
    
    // Helper: Find mode (most frequent element)
    public static int findMode(int[] arr) {
        Map<Integer, Integer> freq = new HashMap<>();
        int maxFreq = 0;
        int mode = arr[0];
        
        for (int num : arr) {
            int count = freq.getOrDefault(num, 0) + 1;
            freq.put(num, count);
            
            if (count > maxFreq) {
                maxFreq = count;
                mode = num;
            }
        }
        
        return mode;
    }
    
    // Helper: Find range (max - min)
    public static int findRange(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        
        int min = arr[0], max = arr[0];
        for (int num : arr) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        
        return max - min;
    }
    
    // Helper: Find variance
    public static double findVariance(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        
        double mean = findMean(arr);
        double sumSquaredDiff = 0;
        
        for (int num : arr) {
            double diff = num - mean;
            sumSquaredDiff += diff * diff;
        }
        
        return sumSquaredDiff / arr.length;
    }
    
    // Helper: Find standard deviation
    public static double findStdDev(int[] arr) {
        return Math.sqrt(findVariance(arr));
    }
    
    // Helper: Get statistics summary
    public static Map<String, Double> getStatistics(int[] arr) {
        Map<String, Double> stats = new HashMap<>();
        
        double[] meanMedian = findMeanAndMedian(arr.clone());
        stats.put("mean", meanMedian[0]);
        stats.put("median", meanMedian[1]);
        stats.put("mode", (double) findMode(arr));
        stats.put("range", (double) findRange(arr));
        stats.put("variance", findVariance(arr));
        stats.put("stdDev", findStdDev(arr));
        
        return stats;
    }
    
    public static boolean doTestsPass() {
        // Test 1: Odd length
        double[] result1 = findMeanAndMedian(new int[]{5, 3, 1, 4, 2});
        if (Math.abs(result1[0] - 3.0) > 0.001) return false;
        if (Math.abs(result1[1] - 3.0) > 0.001) return false;
        
        // Test 2: Even length
        double[] result2 = findMeanAndMedian(new int[]{1, 2, 3, 4});
        if (Math.abs(result2[0] - 2.5) > 0.001) return false;
        if (Math.abs(result2[1] - 2.5) > 0.001) return false;
        
        // Test 3: Single element
        double[] result3 = findMeanAndMedian(new int[]{7});
        if (Math.abs(result3[0] - 7.0) > 0.001) return false;
        if (Math.abs(result3[1] - 7.0) > 0.001) return false;
        
        return true;
    }
    
    public static void main(String[] args) {
        if (doTestsPass()) {
            System.out.println("All tests pass\n");
        } else {
            System.out.println("Tests fail\n");
        }
        
        // Demo
        int[][] testCases = {
            {5, 3, 1, 4, 2},
            {1, 2, 3, 4},
            {10, 20, 30},
            {7},
            {5, 5, 5, 5}
        };
        
        for (int[] arr : testCases) {
            double[] result = findMeanAndMedian(arr.clone());
            System.out.println("Input:  " + Arrays.toString(arr));
            System.out.println("Mean:   " + result[0]);
            System.out.println("Median: " + result[1]);
            System.out.println();
        }
        
        // Statistics summary
        int[] data = {5, 3, 1, 4, 2, 3, 5};
        System.out.println("Statistics for: " + Arrays.toString(data));
        Map<String, Double> stats = getStatistics(data);
        stats.forEach((key, value) -> 
            System.out.printf("%s: %.2f%n", key, value));
    }
}
```

---

## Test Cases

```java
@Test
public void testMeanAndMedian() {
    // Test 1: Odd length
    double[] result1 = findMeanAndMedian(new int[]{5, 3, 1, 4, 2});
    assertEquals(3.0, result1[0], 0.001);
    assertEquals(3.0, result1[1], 0.001);
    
    // Test 2: Even length
    double[] result2 = findMeanAndMedian(new int[]{1, 2, 3, 4});
    assertEquals(2.5, result2[0], 0.001);
    assertEquals(2.5, result2[1], 0.001);
    
    // Test 3: Three elements
    double[] result3 = findMeanAndMedian(new int[]{10, 20, 30});
    assertEquals(20.0, result3[0], 0.001);
    assertEquals(20.0, result3[1], 0.001);
    
    // Test 4: Single element
    double[] result4 = findMeanAndMedian(new int[]{7});
    assertEquals(7.0, result4[0], 0.001);
    assertEquals(7.0, result4[1], 0.001);
    
    // Test 5: All same
    double[] result5 = findMeanAndMedian(new int[]{5, 5, 5, 5});
    assertEquals(5.0, result5[0], 0.001);
    assertEquals(5.0, result5[1], 0.001);
    
    // Test 6: Negative numbers
    double[] result6 = findMeanAndMedian(new int[]{-5, -3, -1, -4, -2});
    assertEquals(-3.0, result6[0], 0.001);
    assertEquals(-3.0, result6[1], 0.001);
    
    // Test 7: Mixed positive/negative
    double[] result7 = findMeanAndMedian(new int[]{-2, -1, 0, 1, 2});
    assertEquals(0.0, result7[0], 0.001);
    assertEquals(0.0, result7[1], 0.001);
}
```

---

## Visual Representation

### Mean Calculation

```
Array: [5, 3, 1, 4, 2]

Sum Calculation:
┌───┬───┬───┬───┬───┐
│ 5 │ 3 │ 1 │ 4 │ 2 │
└───┴───┴───┴───┴───┘
  ↓   ↓   ↓   ↓   ↓
  5 + 3 + 1 + 4 + 2 = 15

Mean = Sum / Count
     = 15 / 5
     = 3.0
```

### Median Calculation (Odd Length)

```
Array: [5, 3, 1, 4, 2]

Step 1: Sort
[1, 2, 3, 4, 5]
 0  1  2  3  4  (indices)

Step 2: Find middle
n = 5 (odd)
middle index = n/2 = 2

Median = arr[2] = 3
```

### Median Calculation (Even Length)

```
Array: [1, 2, 3, 4]

Already sorted:
[1, 2, 3, 4]
 0  1  2  3  (indices)

n = 4 (even)
middle indices = n/2-1 and n/2
               = 1 and 2

Median = (arr[1] + arr[2]) / 2
       = (2 + 3) / 2
       = 2.5
```

---

## Edge Cases

1. **Empty array:** Return 0 or handle error
2. **Null array:** Return 0 or handle error
3. **Single element:** Mean = Median = that element
4. **Two elements:** Mean = average, Median = average
5. **All same elements:** Mean = Median = that value
6. **Negative numbers:** Handle correctly
7. **Large numbers:** Use long for sum to avoid overflow
8. **Floating point precision:** Use double for results

---

## Complexity Analysis

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Mean | O(n) | O(1) | Single pass sum |
| Median (Sort) | O(n log n) | O(1) | Sorting required |
| Median (QuickSelect) | O(n) avg | O(1) | Optimal but complex |
| Both | O(n log n) | O(1) | **Standard approach** |

**Where n = array length**

**Time Complexity Breakdown:**
- Mean: O(n) to sum all elements
- Median: O(n log n) for sorting
- Total: O(n log n) dominated by sorting

**Space Complexity:**
- Mean: O(1) only sum variable
- Median: O(1) if in-place sort, O(n) if cloning array

---

## Related Problems

1. **Find Mode** - Most frequent element
2. **Find Range** - Max - Min
3. **Find Variance** - Measure of spread
4. **Find Standard Deviation** - Square root of variance
5. **Find Quartiles** - Q1, Q2 (median), Q3
6. **Find Percentile** - Value at given percentile

---

## Interview Tips

### Clarification Questions
1. Can array be empty? (Handle edge case)
2. Can array have negative numbers? (Yes)
3. Should we modify original array? (Clone if needed)
4. What precision for results? (Use double)
5. Handle overflow? (Use long for sum)

### Approach Explanation
1. "For mean, I'll sum all elements and divide by count"
2. "For median, I'll sort the array first"
3. "If odd length, median is middle element"
4. "If even length, median is average of two middle elements"
5. "Time O(n log n) for sorting, Space O(1)"

### Common Mistakes
1. **Integer division** - Use double for mean
2. **Overflow** - Use long for sum
3. **Wrong median index** - n/2 for odd, (n/2-1, n/2) for even
4. **Modifying original** - Clone array before sorting
5. **Empty array** - Check for null/empty

### Follow-up Questions
1. "Can you find median in O(n)?" - Yes, QuickSelect algorithm
2. "What about mode?" - Use HashMap for frequency
3. "Find variance?" - Sum of squared differences from mean
4. "Find quartiles?" - Similar to median, find 25th, 50th, 75th percentiles
5. "Streaming data?" - Use running mean, heap for median

---

## Real-World Applications

1. **Statistics** - Data analysis and reporting
2. **Machine Learning** - Feature normalization
3. **Finance** - Portfolio analysis
4. **Healthcare** - Patient data analysis
5. **Education** - Grade analysis
6. **Quality Control** - Manufacturing metrics
7. **Performance Monitoring** - System metrics

---

## Key Takeaways

1. **Mean is simple:** O(n) sum divided by count
2. **Median requires sorting:** O(n log n) standard approach
3. **Odd vs even length:** Different median calculation
4. **Use double for precision:** Avoid integer division
5. **Use long for sum:** Prevent overflow
6. **Clone array if needed:** Don't modify original
7. **QuickSelect for O(n) median:** Advanced optimization

---

## Statistical Formulas

### Mean (Average)
```
Mean = (Σxᵢ) / n

Where:
- xᵢ = each element
- n = number of elements
```

### Median
```
Odd length:  Median = x[(n+1)/2]
Even length: Median = (x[n/2] + x[n/2+1]) / 2

Where array is sorted
```

### Variance
```
Variance = Σ(xᵢ - mean)² / n
```

### Standard Deviation
```
StdDev = √Variance
```

---

## Optimization Notes

### Mean Calculation
```java
// Avoid overflow with large numbers
long sum = 0;  // Use long, not int
for (int num : arr) {
    sum += num;
}
double mean = (double) sum / arr.length;  // Cast to double
```

### Median Without Modifying Original
```java
// Clone array before sorting
int[] sorted = arr.clone();
Arrays.sort(sorted);
// Now original array is unchanged
```

### Best Practice
```java
public static double[] findMeanAndMedian(int[] arr) {
    if (arr == null || arr.length == 0) {
        return new double[]{0, 0};
    }
    
    // Mean: O(n)
    long sum = 0;
    for (int num : arr) {
        sum += num;
    }
    double mean = (double) sum / arr.length;
    
    // Median: O(n log n)
    int[] sorted = arr.clone();  // Don't modify original
    Arrays.sort(sorted);
    
    int n = sorted.length;
    double median;
    if (n % 2 == 0) {
        median = (sorted[n/2 - 1] + sorted[n/2]) / 2.0;
    } else {
        median = sorted[n/2];
    }
    
    return new double[]{mean, median};
}
```

---

## Advanced: QuickSelect for O(n) Median

```java
// Find kth smallest element in O(n) average time
public static int quickSelect(int[] arr, int k) {
    return quickSelectHelper(arr, 0, arr.length - 1, k);
}

private static int quickSelectHelper(int[] arr, int left, int right, int k) {
    if (left == right) return arr[left];
    
    int pivotIndex = partition(arr, left, right);
    
    if (k == pivotIndex) {
        return arr[k];
    } else if (k < pivotIndex) {
        return quickSelectHelper(arr, left, pivotIndex - 1, k);
    } else {
        return quickSelectHelper(arr, pivotIndex + 1, right, k);
    }
}

// For median:
// Odd:  median = quickSelect(arr, n/2)
// Even: median = (quickSelect(arr, n/2-1) + quickSelect(arr, n/2)) / 2.0
```
