# Java 8 - Filter Fruits by Input String Characters

## Problem Statement

Given:
- An input string (e.g., "abc")
- A Map where key = character, value = List of fruits starting with that character
- Print all fruits that start with characters present in the input string

---

## Solution 1: Simple Stream Approach (Recommended)

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        // Input string
        String input = "abc";
        
        // Map: Character -> List of fruits
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Solution: Filter and print fruits
        input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .forEach(System.out::println);
    }
}
```

**Output**:
```
Apple
Apricot
Avocado
Banana
Blueberry
Blackberry
Cherry
Coconut
Cranberry
```

---

## Solution 2: Collect to List

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Collect all matching fruits to a list
        List<String> matchingFruits = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        // Print results
        System.out.println("Matching Fruits: " + matchingFruits);
        System.out.println("Total Count: " + matchingFruits.size());
    }
}
```

**Output**:
```
Matching Fruits: [Apple, Apricot, Avocado, Banana, Blueberry, Blackberry, Cherry, Coconut, Cranberry]
Total Count: 9
```

---

## Solution 3: Grouped by Character

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Group fruits by character
        Map<Character, List<String>> result = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .filter(fruitMap::containsKey)
            .collect(Collectors.toMap(
                c -> c,
                fruitMap::get
            ));
        
        // Print results grouped by character
        result.forEach((character, fruits) -> {
            System.out.println(character + " -> " + fruits);
        });
    }
}
```

**Output**:
```
a -> [Apple, Apricot, Avocado]
b -> [Banana, Blueberry, Blackberry]
c -> [Cherry, Coconut, Cranberry]
```

---

## Solution 4: With Sorting

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "cba"; // Unsorted input
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Sort fruits alphabetically
        List<String> sortedFruits = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .sorted()
            .collect(Collectors.toList());
        
        sortedFruits.forEach(System.out::println);
    }
}
```

**Output**:
```
Apple
Apricot
Avocado
Banana
Blackberry
Blueberry
Cherry
Coconut
Cranberry
```

---

## Solution 5: Case-Insensitive

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "AbC"; // Mixed case
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Case-insensitive filtering
        input.toLowerCase()
            .chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .forEach(System.out::println);
    }
}
```

**Output**:
```
Apple
Apricot
Avocado
Banana
Blueberry
Blackberry
Cherry
Coconut
Cranberry
```

---

## Solution 6: With Count per Character

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Print with count
        input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .filter(fruitMap::containsKey)
            .forEach(c -> {
                List<String> fruits = fruitMap.get(c);
                System.out.println(c + " (" + fruits.size() + " fruits):");
                fruits.forEach(fruit -> System.out.println("  - " + fruit));
            });
    }
}
```

**Output**:
```
a (3 fruits):
  - Apple
  - Apricot
  - Avocado
b (3 fruits):
  - Banana
  - Blueberry
  - Blackberry
c (3 fruits):
  - Cherry
  - Coconut
  - Cranberry
```

---

## Solution 7: Filter with Predicate

```java
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Additional filter: Only fruits with length > 6
        Predicate<String> lengthFilter = fruit -> fruit.length() > 6;
        
        List<String> filteredFruits = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(lengthFilter)
            .collect(Collectors.toList());
        
        System.out.println("Fruits with length > 6:");
        filteredFruits.forEach(System.out::println);
    }
}
```

**Output**:
```
Fruits with length > 6:
Apricot
Blueberry
Blackberry
Coconut
Cranberry
```

---

## Solution 8: Using Optional for Safety

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abcxyz"; // Contains characters not in map
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Safe handling with Optional
        List<String> fruits = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(c -> Optional.ofNullable(fruitMap.get(c)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        System.out.println("Found " + fruits.size() + " fruits:");
        fruits.forEach(System.out::println);
    }
}
```

**Output**:
```
Found 9 fruits:
Apple
Apricot
Avocado
Banana
Blueberry
Blackberry
Cherry
Coconut
Cranberry
```

---

## Solution 9: Parallel Stream for Large Data

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = new HashMap<>();
        fruitMap.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        fruitMap.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        fruitMap.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        fruitMap.put('d', Arrays.asList("Dragon Fruit", "Date"));
        fruitMap.put('m', Arrays.asList("Mango", "Melon"));
        
        // Parallel processing for better performance
        List<String> fruits = input.chars()
            .parallel()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        fruits.forEach(System.out::println);
    }
}
```

---

## Solution 10: Method Reference Approach

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilter {
    
    public static void main(String[] args) {
        String input = "abc";
        
        Map<Character, List<String>> fruitMap = createFruitMap();
        
        // Clean method reference approach
        List<String> fruits = getFruitsForInput(input, fruitMap);
        
        fruits.forEach(System.out::println);
    }
    
    private static Map<Character, List<String>> createFruitMap() {
        Map<Character, List<String>> map = new HashMap<>();
        map.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        map.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        map.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        map.put('d', Arrays.asList("Dragon Fruit", "Date"));
        map.put('m', Arrays.asList("Mango", "Melon"));
        return map;
    }
    
    private static List<String> getFruitsForInput(String input, 
                                                   Map<Character, List<String>> fruitMap) {
        return input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
```

---

## Complete Working Example

```java
import java.util.*;
import java.util.stream.Collectors;

public class FruitFilterDemo {
    
    public static void main(String[] args) {
        // Test with different inputs
        String[] testInputs = {"abc", "dm", "xyz", "abcdefghijklmnopqrstuvwxyz"};
        
        Map<Character, List<String>> fruitMap = initializeFruitMap();
        
        for (String input : testInputs) {
            System.out.println("\n=== Input: \"" + input + "\" ===");
            printFruits(input, fruitMap);
        }
    }
    
    private static Map<Character, List<String>> initializeFruitMap() {
        Map<Character, List<String>> map = new HashMap<>();
        map.put('a', Arrays.asList("Apple", "Apricot", "Avocado"));
        map.put('b', Arrays.asList("Banana", "Blueberry", "Blackberry"));
        map.put('c', Arrays.asList("Cherry", "Coconut", "Cranberry"));
        map.put('d', Arrays.asList("Dragon Fruit", "Date"));
        map.put('e', Arrays.asList("Elderberry"));
        map.put('f', Arrays.asList("Fig"));
        map.put('g', Arrays.asList("Grape", "Guava", "Grapefruit"));
        map.put('k', Arrays.asList("Kiwi"));
        map.put('l', Arrays.asList("Lemon", "Lime", "Lychee"));
        map.put('m', Arrays.asList("Mango", "Melon"));
        map.put('o', Arrays.asList("Orange"));
        map.put('p', Arrays.asList("Papaya", "Peach", "Pear", "Pineapple", "Plum"));
        map.put('s', Arrays.asList("Strawberry"));
        map.put('w', Arrays.asList("Watermelon"));
        return map;
    }
    
    private static void printFruits(String input, Map<Character, List<String>> fruitMap) {
        List<String> fruits = input.chars()
            .mapToObj(c -> (char) c)
            .distinct()
            .map(fruitMap::get)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        if (fruits.isEmpty()) {
            System.out.println("No fruits found!");
        } else {
            System.out.println("Found " + fruits.size() + " fruits:");
            fruits.forEach(fruit -> System.out.println("  - " + fruit));
        }
    }
}
```

**Output**:
```
=== Input: "abc" ===
Found 9 fruits:
  - Apple
  - Apricot
  - Avocado
  - Banana
  - Blueberry
  - Blackberry
  - Cherry
  - Coconut
  - Cranberry

=== Input: "dm" ===
Found 4 fruits:
  - Dragon Fruit
  - Date
  - Mango
  - Melon

=== Input: "xyz" ===
No fruits found!

=== Input: "abcdefghijklmnopqrstuvwxyz" ===
Found 30 fruits:
  - Apple
  - Apricot
  - Avocado
  - Banana
  - Blueberry
  - Blackberry
  - Cherry
  - Coconut
  - Cranberry
  - Dragon Fruit
  - Date
  - Elderberry
  - Fig
  - Grape
  - Guava
  - Grapefruit
  - Kiwi
  - Lemon
  - Lime
  - Lychee
  - Mango
  - Melon
  - Orange
  - Papaya
  - Peach
  - Pear
  - Pineapple
  - Plum
  - Strawberry
  - Watermelon
```

---

## Step-by-Step Explanation

### Breaking Down the Stream Pipeline

```java
input.chars()                          // IntStream of character codes
    .mapToObj(c -> (char) c)          // Convert int to Character
    .distinct()                        // Remove duplicate characters
    .map(fruitMap::get)               // Get List<String> for each character
    .filter(Objects::nonNull)         // Remove null values (missing keys)
    .flatMap(List::stream)            // Flatten List<List<String>> to List<String>
    .forEach(System.out::println);    // Print each fruit
```

**Example Flow**:
```
Input: "abc"

Step 1: input.chars()
→ IntStream: [97, 98, 99]

Step 2: mapToObj(c -> (char) c)
→ Stream<Character>: ['a', 'b', 'c']

Step 3: distinct()
→ Stream<Character>: ['a', 'b', 'c'] (no duplicates)

Step 4: map(fruitMap::get)
→ Stream<List<String>>: [
    [Apple, Apricot, Avocado],
    [Banana, Blueberry, Blackberry],
    [Cherry, Coconut, Cranberry]
  ]

Step 5: filter(Objects::nonNull)
→ Stream<List<String>>: (same, no nulls)

Step 6: flatMap(List::stream)
→ Stream<String>: [Apple, Apricot, Avocado, Banana, Blueberry, 
                   Blackberry, Cherry, Coconut, Cranberry]

Step 7: forEach(System.out::println)
→ Print each fruit
```

---

## Performance Comparison

| Approach | Time Complexity | Space Complexity | Best For |
|----------|----------------|------------------|----------|
| Simple Stream | O(n + m) | O(m) | Small to medium data |
| Parallel Stream | O(n + m) | O(m) | Large datasets |
| Grouped Result | O(n + m) | O(m) | Need grouping |
| Sorted Result | O(n + m log m) | O(m) | Need sorted output |

Where:
- n = length of input string
- m = total number of fruits matching input characters

---

## Key Java 8 Features Used

1. ✅ **Stream API** - `stream()`, `flatMap()`, `filter()`, `map()`
2. ✅ **Method References** - `fruitMap::get`, `Objects::nonNull`, `System.out::println`
3. ✅ **Lambda Expressions** - `c -> (char) c`
4. ✅ **Collectors** - `Collectors.toList()`, `Collectors.toMap()`
5. ✅ **Optional** - `Optional.ofNullable()`
6. ✅ **Functional Interfaces** - `Predicate`, `Function`

---

## Summary

**Simplest Solution**:
```java
input.chars()
    .mapToObj(c -> (char) c)
    .distinct()
    .map(fruitMap::get)
    .filter(Objects::nonNull)
    .flatMap(List::stream)
    .forEach(System.out::println);
```

This solution is:
- ✅ Concise and readable
- ✅ Uses pure Java 8 features
- ✅ Handles null values safely
- ✅ Removes duplicate characters
- ✅ Efficient for most use cases
