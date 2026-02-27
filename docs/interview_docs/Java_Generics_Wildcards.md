# Java Generics & Wildcards - Interview Questions

## For 5-6 Years Experienced Software Engineers

---

## Table of Contents
1. [Basic Generics](#basic-generics)
2. [Wildcards - Unbounded](#wildcards---unbounded)
3. [Upper Bounded Wildcards](#upper-bounded-wildcards)
4. [Lower Bounded Wildcards](#lower-bounded-wildcards)
5. [Type Erasure](#type-erasure)
6. [Generic Methods](#generic-methods)
7. [PECS Principle](#pecs-principle)
8. [Advanced Scenarios](#advanced-scenarios)

---

## Basic Generics

### Q1: Generic Class Type Safety

```java
class Box<T> {
    private T item;
    
    public void set(T item) { this.item = item; }
    public T get() { return item; }
}

public class Test {
    public static void main(String[] args) {
        Box<String> stringBox = new Box<>();
        stringBox.set("Hello");
        String str = stringBox.get();
        System.out.println(str);
        
        Box<Integer> intBox = new Box<>();
        intBox.set(100);
        Integer num = intBox.get();
        System.out.println(num);
    }
}
```

**Output:**
```
Hello
100
```

**Explanation:**
- Generic type `T` is replaced with actual type at compile time
- Type safety enforced - no casting needed
- Each Box instance has its own type parameter

---

### Q2: Raw Type vs Generic Type

```java
public class Test {
    public static void main(String[] args) {
        List list = new ArrayList();  // Raw type
        list.add("String");
        list.add(100);
        list.add(true);
        
        for (Object obj : list) {
            System.out.println(obj.getClass().getSimpleName() + ": " + obj);
        }
    }
}
```

**Output:**
```
String: String
Integer: 100
Boolean: true
```

**Explanation:**
- Raw type allows any object type
- No compile-time type checking
- Requires casting when retrieving
- Avoid raw types - use generics for type safety

---

### Q3: Generic Type Mismatch

```java
public class Test {
    public static void main(String[] args) {
        List<Object> objList = new ArrayList<Object>();
        objList.add("String");
        objList.add(100);
        
        // List<Object> list2 = new ArrayList<String>();  // Compile error
        // List<String> list3 = new ArrayList<Object>();  // Compile error
        
        System.out.println(objList);
    }
}
```

**Output:**
```
[String, 100]
```

**Explanation:**
- `List<Object>` is NOT a supertype of `List<String>`
- Generics are invariant (not covariant)
- Prevents heap pollution
- Use wildcards for flexibility

---

## Wildcards - Unbounded

### Q4: Unbounded Wildcard Read-Only

```java
public class Test {
    public static void printList(List<?> list) {
        for (Object obj : list) {
            System.out.println(obj);
        }
        // list.add("test");  // Compile error
        // list.add(null);    // OK - null is allowed
    }
    
    public static void main(String[] args) {
        List<String> strings = Arrays.asList("A", "B", "C");
        List<Integer> integers = Arrays.asList(1, 2, 3);
        
        printList(strings);
        printList(integers);
    }
}
```

**Output:**
```
A
B
C
1
2
3
```

**Explanation:**
- `List<?>` accepts any type of list
- Can only read as `Object`
- Cannot add elements (except null)
- Maximum flexibility for read-only operations

---

### Q5: Wildcard vs Object

```java
public class Test {
    public static void processWildcard(List<?> list) {
        System.out.println("Wildcard size: " + list.size());
        // list.add("test");  // Compile error
    }
    
    public static void processObject(List<Object> list) {
        System.out.println("Object size: " + list.size());
        list.add("test");  // OK
    }
    
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>(Arrays.asList("A", "B"));
        
        processWildcard(strings);  // OK
        // processObject(strings);  // Compile error
        
        List<Object> objects = new ArrayList<>();
        processObject(objects);
        System.out.println(objects);
    }
}
```

**Output:**
```
Wildcard size: 2
Object size: 0
[test]
```

**Explanation:**
- `List<?>` accepts `List<String>`
- `List<Object>` does NOT accept `List<String>`
- Wildcards provide flexibility
- Object requires exact type match

---

## Upper Bounded Wildcards

### Q6: Extends Wildcard - Producer

```java
public class Test {
    public static double sumNumbers(List<? extends Number> list) {
        double sum = 0;
        for (Number num : list) {
            sum += num.doubleValue();
        }
        // list.add(10);      // Compile error
        // list.add(10.5);    // Compile error
        return sum;
    }
    
    public static void main(String[] args) {
        List<Integer> integers = Arrays.asList(1, 2, 3, 4, 5);
        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);
        
        System.out.println("Integer sum: " + sumNumbers(integers));
        System.out.println("Double sum: " + sumNumbers(doubles));
    }
}
```

**Output:**
```
Integer sum: 15.0
Double sum: 6.6
```

**Explanation:**
- `<? extends Number>` accepts any Number subtype
- Can read as Number or its supertypes
- Cannot add elements (type safety)
- Producer - provides data to be read

---

### Q7: Extends Wildcard Hierarchy

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }

public class Test {
    public static void processAnimals(List<? extends Animal> animals) {
        for (Animal animal : animals) {
            System.out.println(animal.getClass().getSimpleName());
        }
        // animals.add(new Dog());  // Compile error
    }
    
    public static void main(String[] args) {
        List<Dog> dogs = Arrays.asList(new Dog(), new Dog());
        List<Cat> cats = Arrays.asList(new Cat(), new Cat());
        
        processAnimals(dogs);
        processAnimals(cats);
    }
}
```

**Output:**
```
Dog
Dog
Cat
Cat
```

**Explanation:**
- Accepts List of Animal or any subtype
- Can read as Animal
- Cannot add - compiler doesn't know exact subtype
- Prevents heap pollution

---

### Q8: Multiple Bounds

```java
interface Flyable {
    default String fly() { return "Flying"; }
}

class Bird implements Comparable<Bird>, Flyable {
    private String name;
    
    Bird(String name) { this.name = name; }
    
    public int compareTo(Bird other) {
        return this.name.compareTo(other.name);
    }
    
    public String toString() { return name; }
}

public class Test {
    public static <T extends Comparable<T> & Flyable> void process(T item) {
        System.out.println(item + " can " + item.fly());
    }
    
    public static void main(String[] args) {
        Bird bird = new Bird("Eagle");
        process(bird);
    }
}
```

**Output:**
```
Eagle can Flying
```

**Explanation:**
- Multiple bounds with `&` operator
- Type must implement all interfaces
- Class bound must come first (if any)
- Enables multiple capabilities

---

## Lower Bounded Wildcards

### Q9: Super Wildcard - Consumer

```java
public class Test {
    public static void addIntegers(List<? super Integer> list) {
        list.add(10);
        list.add(20);
        list.add(30);
        
        // Integer num = list.get(0);  // Compile error
        Object obj = list.get(0);      // OK
        System.out.println("Added integers, first element: " + obj);
    }
    
    public static void main(String[] args) {
        List<Integer> integers = new ArrayList<>();
        List<Number> numbers = new ArrayList<>();
        List<Object> objects = new ArrayList<>();
        
        addIntegers(integers);
        addIntegers(numbers);
        addIntegers(objects);
        
        System.out.println("Integers: " + integers);
        System.out.println("Numbers: " + numbers);
        System.out.println("Objects: " + objects);
    }
}
```

**Output:**
```
Added integers, first element: 10
Added integers, first element: 10
Added integers, first element: 10
Integers: [10, 20, 30]
Numbers: [10, 20, 30]
Objects: [10, 20, 30]
```

**Explanation:**
- `<? super Integer>` accepts Integer or its supertypes
- Can add Integer or its subtypes
- Can only read as Object
- Consumer - accepts data to be written

---

### Q10: Super Wildcard Hierarchy

```java
public class Test {
    public static void addNumbers(List<? super Integer> list) {
        list.add(100);
        list.add(200);
        // list.add(10.5);  // Compile error - Double not subtype of Integer
    }
    
    public static void main(String[] args) {
        List<Number> numbers = new ArrayList<>();
        addNumbers(numbers);
        
        for (Number num : numbers) {
            System.out.println(num.getClass().getSimpleName() + ": " + num);
        }
    }
}
```

**Output:**
```
Integer: 100
Integer: 200
```

**Explanation:**
- Can add Integer and its subtypes only
- List stores as declared type (Number)
- Type safety maintained
- Flexible for polymorphic collections

---

## Type Erasure

### Q11: Type Erasure at Runtime

```java
public class Test {
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();
        
        System.out.println(strings.getClass() == integers.getClass());
        System.out.println(strings.getClass().getName());
        
        // if (strings instanceof List<String>) { }  // Compile error
        if (strings instanceof List<?>) {
            System.out.println("Is a List");
        }
    }
}
```

**Output:**
```
true
java.util.ArrayList
Is a List
```

**Explanation:**
- Generic type information erased at runtime
- Both lists have same class type
- Cannot use instanceof with specific generic type
- Use unbounded wildcard for instanceof

---

### Q12: Type Erasure with Bounds

```java
class Container<T extends Number> {
    private T value;
    
    public void set(T value) { this.value = value; }
    
    public void printType() {
        System.out.println("Runtime type: " + value.getClass().getName());
        // System.out.println("Generic type: " + T.class);  // Compile error
    }
}

public class Test {
    public static void main(String[] args) {
        Container<Integer> intContainer = new Container<>();
        intContainer.set(100);
        intContainer.printType();
        
        Container<Double> doubleContainer = new Container<>();
        doubleContainer.set(99.9);
        doubleContainer.printType();
    }
}
```

**Output:**
```
Runtime type: java.lang.Integer
Runtime type: java.lang.Double
```

**Explanation:**
- Generic type T erased to bound (Number)
- Actual object type preserved
- Cannot access T.class at runtime
- Use getClass() on instance

---

## Generic Methods

### Q13: Generic Method Return Type

```java
public class Test {
    public static <T> T getFirst(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }
    
    public static Object getFirstWildcard(List<?> list) {
        return list.isEmpty() ? null : list.get(0);
    }
    
    public static void main(String[] args) {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        
        String name1 = getFirst(names);           // No cast needed
        Object name2 = getFirstWildcard(names);   // Returns Object
        
        System.out.println("Generic method: " + name1);
        System.out.println("Wildcard method: " + name2);
    }
}
```

**Output:**
```
Generic method: Alice
Wildcard method: Alice
```

**Explanation:**
- Generic method preserves type information
- Wildcard method returns Object
- Generic method provides better type safety
- No casting required with generics

---

### Q14: Generic Method with Multiple Type Parameters

```java
public class Test {
    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
    
    public static void main(String[] args) {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        
        Map<Integer, String> names = new HashMap<>();
        names.put(1, "First");
        names.put(2, "Second");
        
        printMap(scores);
        System.out.println("---");
        printMap(names);
    }
}
```

**Output:**
```
Alice -> 95
Bob -> 87
---
1 -> First
2 -> Second
```

**Explanation:**
- Multiple type parameters K and V
- Works with different key-value types
- Type inference from arguments
- Reusable for any Map type

---

## PECS Principle

### Q15: Producer Extends, Consumer Super

```java
public class Test {
    // Producer - use extends (read from)
    public static void processProducer(List<? extends Number> producer) {
        for (Number num : producer) {
            System.out.println("Read: " + num);
        }
        // producer.add(10);  // Compile error
    }
    
    // Consumer - use super (write to)
    public static void processConsumer(List<? super Integer> consumer) {
        consumer.add(100);
        consumer.add(200);
        System.out.println("Written to consumer");
    }
    
    public static void main(String[] args) {
        List<Integer> integers = Arrays.asList(1, 2, 3);
        processProducer(integers);
        
        List<Number> numbers = new ArrayList<>();
        processConsumer(numbers);
        System.out.println("Consumer contents: " + numbers);
    }
}
```

**Output:**
```
Read: 1
Read: 2
Read: 3
Written to consumer
Consumer contents: [100, 200]
```

**Explanation:**
- Producer (extends): Read from collection
- Consumer (super): Write to collection
- PECS: Producer Extends, Consumer Super
- Maximizes API flexibility

---

### Q16: Copy Method with PECS

```java
public class Test {
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (T item : src) {
            dest.add(item);
        }
    }
    
    public static void main(String[] args) {
        List<Integer> integers = Arrays.asList(1, 2, 3, 4, 5);
        List<Number> numbers = new ArrayList<>();
        
        copy(numbers, integers);
        System.out.println("Copied: " + numbers);
        
        List<Object> objects = new ArrayList<>();
        copy(objects, integers);
        System.out.println("Copied to objects: " + objects);
    }
}
```

**Output:**
```
Copied: [1, 2, 3, 4, 5]
Copied to objects: [1, 2, 3, 4, 5]
```

**Explanation:**
- src is producer (extends) - read from
- dest is consumer (super) - write to
- Flexible: dest can be supertype of src
- Mimics Collections.copy() design

---

## Advanced Scenarios

### Q17: Recursive Type Bound

```java
class Person implements Comparable<Person> {
    private String name;
    private int age;
    
    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public int compareTo(Person other) {
        return Integer.compare(this.age, other.age);
    }
    
    public String toString() {
        return name + "(" + age + ")";
    }
}

public class Test {
    public static <T extends Comparable<T>> T max(List<T> list) {
        if (list.isEmpty()) return null;
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) {
                max = item;
            }
        }
        return max;
    }
    
    public static void main(String[] args) {
        List<Person> people = Arrays.asList(
            new Person("Alice", 30),
            new Person("Bob", 25),
            new Person("Charlie", 35)
        );
        
        Person oldest = max(people);
        System.out.println("Oldest: " + oldest);
    }
}
```

**Output:**
```
Oldest: Charlie(35)
```

**Explanation:**
- Recursive bound: `T extends Comparable<T>`
- Ensures type can compare with itself
- Common pattern in Java Collections
- Type-safe comparison

---

### Q18: Wildcard Capture

```java
public class Test {
    public static void reverse(List<?> list) {
        reverseHelper(list);
    }
    
    private static <T> void reverseHelper(List<T> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            T temp = list.get(i);
            list.set(i, list.get(size - 1 - i));
            list.set(size - 1 - i, temp);
        }
    }
    
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
        System.out.println("Before: " + strings);
        
        reverse(strings);
        System.out.println("After: " + strings);
    }
}
```

**Output:**
```
Before: [A, B, C, D]
After: [D, C, B, A]
```

**Explanation:**
- Wildcard capture helper pattern
- Public method accepts wildcard
- Private helper captures specific type
- Enables modification of wildcard list

---

### Q19: Bridge Methods

```java
class Node<T> {
    private T data;
    
    public void setData(T data) {
        this.data = data;
        System.out.println("Set: " + data);
    }
    
    public T getData() {
        return data;
    }
}

class IntNode extends Node<Integer> {
    public void setData(Integer data) {
        System.out.println("IntNode setData called");
        super.setData(data);
    }
}

public class Test {
    public static void main(String[] args) {
        IntNode node = new IntNode();
        node.setData(100);
        
        Node<Integer> genericNode = node;
        genericNode.setData(200);
    }
}
```

**Output:**
```
IntNode setData called
Set: 100
IntNode setData called
Set: 200
```

**Explanation:**
- Compiler generates bridge method for type erasure
- Bridge method delegates to actual implementation
- Maintains polymorphism after erasure
- Transparent to developer

---

### Q20: Generic Array Creation Workaround

```java
public class Test {
    // Cannot create generic array directly
    // private T[] array = new T[10];  // Compile error
    
    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(int size) {
        return (T[]) new Object[size];
    }
    
    public static void main(String[] args) {
        String[] strings = createArray(5);
        strings[0] = "Hello";
        strings[1] = "World";
        
        // This will cause ClassCastException at runtime
        try {
            String[] result = (String[]) (Object) strings;
            System.out.println("Array created");
            for (String s : result) {
                if (s != null) System.out.println(s);
            }
        } catch (ClassCastException e) {
            System.out.println("ClassCastException: " + e.getMessage());
        }
    }
}
```

**Output:**
```
Array created
Hello
World
```

**Explanation:**
- Cannot create generic arrays directly
- Workaround: Create Object[] and cast
- Type safety not guaranteed at runtime
- Use ArrayList<T> instead when possible

---

## Summary

### Key Differences

| Feature | Generics `<T>` | Wildcards `<?>` |
|---------|---------------|-----------------|
| **Purpose** | Type parameterization | Flexibility in parameters |
| **Read** | ✅ Type-safe | ✅ As Object/bound |
| **Write** | ✅ Type-safe | ❌ Restricted |
| **Return Type** | ✅ Preserves type | ❌ Returns Object |
| **Use Case** | Class/method definition | Method parameters |

### Wildcard Rules

| Wildcard | Read | Write | Use Case |
|----------|------|-------|----------|
| `<?>` | Object only | ❌ No | Maximum flexibility |
| `<? extends T>` | As T | ❌ No | Producer (read) |
| `<? super T>` | Object only | ✅ T and subtypes | Consumer (write) |

### Best Practices

1. **Use generics** for class/method definitions
2. **Use wildcards** for flexible method parameters
3. **PECS**: Producer Extends, Consumer Super
4. **Avoid raw types** - always use generics
5. **Prefer List<T>** over generic arrays
6. **Use bounded wildcards** for API flexibility
7. **Type erasure** - no runtime generic type info

---

## Interview Tips

1. Understand **invariance** of generics
2. Know **PECS principle** by heart
3. Explain **type erasure** implications
4. Recognize **wildcard capture** pattern
5. Understand **bridge methods** concept
6. Know when to use **extends vs super**
7. Explain **generic array limitations**
8. Understand **recursive type bounds**

