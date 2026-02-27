# Java Output-Based Interview Questions

## For 5-6 Years Experienced Software Engineers

---

## Table of Contents
1. [String & Immutability](#string--immutability)
2. [Collections Framework](#collections-framework)
3. [Multithreading & Concurrency](#multithreading--concurrency)
4. [Exception Handling](#exception-handling)
5. [Inheritance & Polymorphism](#inheritance--polymorphism)
6. [Static & Final](#static--final)
7. [Autoboxing & Unboxing](#autoboxing--unboxing)
8. [Generics & Type Erasure](#generics--type-erasure)
9. [Lambda & Streams](#lambda--streams)
10. [Tricky Scenarios](#tricky-scenarios)

---

## String & Immutability

### Q1: String Pool Behavior

```java
public class Test {
    public static void main(String[] args) {
        String s1 = "Hello";
        String s2 = "Hello";
        String s3 = new String("Hello");
        String s4 = s3.intern();
        
        System.out.println(s1 == s2);
        System.out.println(s1 == s3);
        System.out.println(s1 == s4);
        System.out.println(s3 == s4);
    }
}
```

**Output:**
```
true
false
true
false
```

**Explanation:**
- `s1 == s2`: Both point to same string literal in pool → `true`
- `s1 == s3`: s3 is heap object, s1 is pool → `false`
- `s1 == s4`: intern() returns pool reference → `true`
- `s3 == s4`: s3 is heap, s4 is pool → `false`

---

### Q2: String Concatenation

```java
public class Test {
    public static void main(String[] args) {
        String s1 = "Java" + "Programming";
        String s2 = "JavaProgramming";
        String s3 = "Java";
        String s4 = s3 + "Programming";
        
        System.out.println(s1 == s2);
        System.out.println(s2 == s4);
        System.out.println(s1.equals(s4));
    }
}
```

**Output:**
```
true
false
true
```

**Explanation:**
- `s1 == s2`: Compile-time constant folding → same pool reference → `true`
- `s2 == s4`: s4 uses StringBuilder at runtime → heap object → `false`
- `s1.equals(s4)`: Content comparison → `true`

---

### Q3: String Modification

```java
public class Test {
    public static void main(String[] args) {
        String str = "Hello";
        str.concat(" World");
        str.toUpperCase();
        str = str + "!";
        
        System.out.println(str);
    }
}
```

**Output:**
```
Hello!
```

**Explanation:**
- `concat()` and `toUpperCase()` return new strings but not assigned
- Only `str = str + "!"` modifies the reference
- Strings are immutable

---

## Collections Framework

### Q4: HashMap Key Modification

```java
public class Test {
    public static void main(String[] args) {
        Map<StringBuilder, Integer> map = new HashMap<>();
        StringBuilder key = new StringBuilder("Key");
        
        map.put(key, 100);
        System.out.println(map.get(key));
        
        key.append("Modified");
        System.out.println(map.get(key));
        System.out.println(map.containsKey(key));
    }
}
```

**Output:**
```
100
null
false
```

**Explanation:**
- After modification, hashCode changes
- HashMap can't find the entry in new bucket
- Never use mutable objects as HashMap keys

---

### Q5: ArrayList vs LinkedList

```java
public class Test {
    public static void main(String[] args) {
        List<String> list1 = new ArrayList<>();
        list1.add("A");
        list1.add("B");
        list1.add(1, "C");
        
        List<String> list2 = new LinkedList<>(list1);
        list2.remove(1);
        
        System.out.println(list1);
        System.out.println(list2);
    }
}
```

**Output:**
```
[A, C, B]
[A, B]
```

**Explanation:**
- list1: Insert "C" at index 1 → [A, C, B]
- list2: Copy of list1, then remove index 1 → [A, B]
- Collections are independent

---

### Q6: ConcurrentModificationException

```java
public class Test {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        
        for (Integer num : list) {
            if (num == 3) {
                list.remove(num);
            }
        }
        
        System.out.println(list);
    }
}
```

**Output:**
```
Exception in thread "main" java.util.ConcurrentModificationException
```

**Explanation:**
- Enhanced for-loop uses Iterator
- Modifying collection during iteration throws exception
- Use `Iterator.remove()` or `removeIf()` instead

---

### Q7: TreeSet Custom Ordering

```java
public class Test {
    public static void main(String[] args) {
        Set<Integer> set = new TreeSet<>((a, b) -> b - a);
        set.add(5);
        set.add(2);
        set.add(8);
        set.add(2);
        
        System.out.println(set);
    }
}
```

**Output:**
```
[8, 5, 2]
```

**Explanation:**
- TreeSet with reverse comparator
- Duplicate 2 ignored (Set property)
- Sorted in descending order

---

## Multithreading & Concurrency

### Q8: Thread Execution Order

```java
public class Test {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> System.out.print("A"));
        Thread t2 = new Thread(() -> System.out.print("B"));
        
        t1.start();
        t2.start();
        System.out.print("C");
    }
}
```

**Output:**
```
CAB or CBA or ACB or ABC or BAC or BCA (any order)
```

**Explanation:**
- Thread execution order is non-deterministic
- Main thread prints "C" immediately
- t1 and t2 execute concurrently
- Output varies each run

---

### Q9: Volatile Variable

```java
public class Test {
    private static volatile int counter = 0;
    
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter++;
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) counter++;
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        System.out.println(counter);
    }
}
```

**Output:**
```
<2000 (likely less, e.g., 1847, 1923, etc.)
```

**Explanation:**
- `volatile` ensures visibility, NOT atomicity
- `counter++` is not atomic (read-modify-write)
- Race condition causes lost updates
- Use `AtomicInteger` for thread-safe increment

---

### Q10: Deadlock Scenario

```java
public class Test {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("T1: Holding lock1");
                try { Thread.sleep(10); } catch (InterruptedException e) {}
                synchronized (lock2) {
                    System.out.println("T1: Holding lock1 & lock2");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("T2: Holding lock2");
                synchronized (lock1) {
                    System.out.println("T2: Holding lock1 & lock2");
                }
            }
        });
        
        t1.start();
        t2.start();
    }
}
```

**Output:**
```
T1: Holding lock1
T2: Holding lock2
(Program hangs - deadlock)
```

**Explanation:**
- T1 holds lock1, waits for lock2
- T2 holds lock2, waits for lock1
- Circular dependency → deadlock
- Fix: Acquire locks in same order

---

## Exception Handling

### Q11: Try-Catch-Finally

```java
public class Test {
    public static void main(String[] args) {
        System.out.println(test());
    }
    
    static int test() {
        try {
            return 1;
        } catch (Exception e) {
            return 2;
        } finally {
            return 3;
        }
    }
}
```

**Output:**
```
3
```

**Explanation:**
- `finally` always executes
- `return` in finally overrides try/catch return
- Bad practice: avoid return in finally

---

### Q12: Exception Propagation

```java
public class Test {
    public static void main(String[] args) {
        try {
            method1();
        } catch (Exception e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
    
    static void method1() {
        try {
            method2();
        } finally {
            System.out.println("Finally in method1");
        }
    }
    
    static void method2() {
        throw new RuntimeException("Error in method2");
    }
}
```

**Output:**
```
Finally in method1
Caught: Error in method2
```

**Explanation:**
- Exception propagates from method2 → method1 → main
- Finally block executes during unwinding
- Exception caught in main

---

### Q13: Try-With-Resources

```java
class Resource implements AutoCloseable {
    private String name;
    
    Resource(String name) {
        this.name = name;
        System.out.println(name + " opened");
    }
    
    public void close() {
        System.out.println(name + " closed");
    }
}

public class Test {
    public static void main(String[] args) {
        try (Resource r1 = new Resource("R1");
             Resource r2 = new Resource("R2")) {
            System.out.println("Using resources");
            throw new RuntimeException("Error");
        } catch (Exception e) {
            System.out.println("Exception caught");
        }
    }
}
```

**Output:**
```
R1 opened
R2 opened
Using resources
R2 closed
R1 closed
Exception caught
```

**Explanation:**
- Resources opened in order
- Resources closed in reverse order (LIFO)
- close() called even when exception thrown

---

## Inheritance & Polymorphism

### Q14: Method Overriding

```java
class Parent {
    void display() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    void display() {
        System.out.println("Child");
    }
}

public class Test {
    public static void main(String[] args) {
        Parent p = new Child();
        p.display();
        
        Parent p2 = new Parent();
        p2.display();
    }
}
```

**Output:**
```
Child
Parent
```

**Explanation:**
- Runtime polymorphism (dynamic dispatch)
- Method resolved based on actual object type
- p references Child object → calls Child.display()

---

### Q15: Constructor Chaining

```java
class Parent {
    Parent() {
        System.out.println("Parent constructor");
        display();
    }
    
    void display() {
        System.out.println("Parent display");
    }
}

class Child extends Parent {
    private int value = 10;
    
    Child() {
        System.out.println("Child constructor");
    }
    
    void display() {
        System.out.println("Child display: " + value);
    }
}

public class Test {
    public static void main(String[] args) {
        new Child();
    }
}
```

**Output:**
```
Parent constructor
Child display: 0
Child constructor
```

**Explanation:**
- Parent constructor called first
- display() is overridden → calls Child.display()
- But Child's instance variables not initialized yet → value = 0
- Then Child constructor executes

---

### Q16: Static Method Hiding

```java
class Parent {
    static void display() {
        System.out.println("Parent static");
    }
}

class Child extends Parent {
    static void display() {
        System.out.println("Child static");
    }
}

public class Test {
    public static void main(String[] args) {
        Parent p = new Child();
        p.display();
        
        Child c = new Child();
        c.display();
    }
}
```

**Output:**
```
Parent static
Child static
```

**Explanation:**
- Static methods are NOT overridden (method hiding)
- Resolved at compile-time based on reference type
- p is Parent reference → calls Parent.display()
- c is Child reference → calls Child.display()

---

## Static & Final

### Q17: Static Block Execution

```java
public class Test {
    static {
        System.out.println("Static block 1");
    }
    
    static int value = initialize();
    
    static {
        System.out.println("Static block 2");
    }
    
    static int initialize() {
        System.out.println("Initialize method");
        return 100;
    }
    
    public static void main(String[] args) {
        System.out.println("Main method");
        System.out.println(value);
    }
}
```

**Output:**
```
Static block 1
Initialize method
Static block 2
Main method
100
```

**Explanation:**
- Static blocks execute in order during class loading
- Static variable initialization happens in sequence
- All static initialization before main() executes

---

### Q18: Final Variable Modification

```java
public class Test {
    public static void main(String[] args) {
        final StringBuilder sb = new StringBuilder("Hello");
        sb.append(" World");
        System.out.println(sb);
        
        final int[] arr = {1, 2, 3};
        arr[0] = 10;
        System.out.println(Arrays.toString(arr));
        
        // arr = new int[]{4, 5, 6}; // Compilation error
    }
}
```

**Output:**
```
Hello World
[10, 2, 3]
```

**Explanation:**
- `final` means reference can't change
- Object content CAN be modified
- sb reference is final, but StringBuilder is mutable
- arr reference is final, but array elements can change

---

## Autoboxing & Unboxing

### Q19: Integer Cache

```java
public class Test {
    public static void main(String[] args) {
        Integer a = 127;
        Integer b = 127;
        System.out.println(a == b);
        
        Integer c = 128;
        Integer d = 128;
        System.out.println(c == d);
        
        Integer e = new Integer(127);
        Integer f = new Integer(127);
        System.out.println(e == f);
    }
}
```

**Output:**
```
true
false
false
```

**Explanation:**
- Integer cache: -128 to 127
- a, b use cached objects → same reference → `true`
- c, d outside cache → different objects → `false`
- new Integer() always creates new object → `false`

---

### Q20: Autoboxing in Collections

```java
public class Test {
    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        
        list.remove(1);
        System.out.println(list);
        
        list.remove(Integer.valueOf(1));
        System.out.println(list);
    }
}
```

**Output:**
```
[1, 3]
[3]
```

**Explanation:**
- `remove(1)`: Removes at index 1 → removes 2
- `remove(Integer.valueOf(1))`: Removes object 1
- Overloaded methods: remove(int index) vs remove(Object o)

---

## Generics & Type Erasure

### Q21: Generic Type Erasure

```java
public class Test {
    public static void main(String[] args) {
        List<String> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        
        System.out.println(list1.getClass() == list2.getClass());
        System.out.println(list1.getClass().getName());
    }
}
```

**Output:**
```
true
java.util.ArrayList
```

**Explanation:**
- Type erasure: Generic types removed at runtime
- Both lists are ArrayList at runtime
- Type information only at compile-time

---

### Q22: Wildcard Generics

```java
public class Test {
    static void print(List<?> list) {
        for (Object obj : list) {
            System.out.print(obj + " ");
        }
        System.out.println();
    }
    
    public static void main(String[] args) {
        List<Integer> intList = Arrays.asList(1, 2, 3);
        List<String> strList = Arrays.asList("A", "B", "C");
        
        print(intList);
        print(strList);
    }
}
```

**Output:**
```
1 2 3 
A B C 
```

**Explanation:**
- `List<?>` accepts any type of List
- Can only read as Object
- Cannot add elements (except null)

---

## Lambda & Streams

### Q23: Stream Operations

```java
public class Test {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        int sum = numbers.stream()
            .filter(n -> {
                System.out.println("Filter: " + n);
                return n % 2 == 0;
            })
            .map(n -> {
                System.out.println("Map: " + n);
                return n * 2;
            })
            .reduce(0, Integer::sum);
        
        System.out.println("Sum: " + sum);
    }
}
```

**Output:**
```
Filter: 1
Filter: 2
Map: 2
Filter: 3
Filter: 4
Map: 4
Filter: 5
Sum: 12
```

**Explanation:**
- Streams are lazy: operations execute only when terminal operation called
- Pipeline processes elements one-by-one
- Filter 2 → Map to 4, Filter 4 → Map to 8
- Sum: 4 + 8 = 12

---

### Q24: Parallel Stream

```java
public class Test {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        numbers.parallelStream()
            .forEach(n -> System.out.print(n + " "));
    }
}
```

**Output:**
```
3 5 4 1 2  (or any other order)
```

**Explanation:**
- Parallel streams use ForkJoinPool
- Order not guaranteed with forEach
- Use forEachOrdered() for ordered processing

---

### Q25: Method Reference

```java
class Printer {
    static void print(String s) {
        System.out.println("Static: " + s);
    }
    
    void display(String s) {
        System.out.println("Instance: " + s);
    }
}

public class Test {
    public static void main(String[] args) {
        List<String> list = Arrays.asList("A", "B", "C");
        
        list.forEach(Printer::print);
        
        Printer p = new Printer();
        list.forEach(p::display);
    }
}
```

**Output:**
```
Static: A
Static: B
Static: C
Instance: A
Instance: B
Instance: C
```

**Explanation:**
- `Printer::print` → static method reference
- `p::display` → instance method reference
- Method references are syntactic sugar for lambdas

---

## Tricky Scenarios

### Q26: Operator Precedence

```java
public class Test {
    public static void main(String[] args) {
        int a = 5;
        int b = 10;
        
        System.out.println(a + b + " = Sum");
        System.out.println("Sum = " + a + b);
        System.out.println("Sum = " + (a + b));
    }
}
```

**Output:**
```
15 = Sum
Sum = 510
Sum = 15
```

**Explanation:**
- Line 1: (5 + 10) + " = Sum" → "15 = Sum"
- Line 2: ("Sum = " + 5) + 10 → "Sum = 5" + 10 → "Sum = 510"
- Line 3: "Sum = " + (15) → "Sum = 15"
- String concatenation is left-associative

---

### Q27: Short-Circuit Evaluation

```java
public class Test {
    static boolean check1() {
        System.out.println("check1");
        return true;
    }
    
    static boolean check2() {
        System.out.println("check2");
        return false;
    }
    
    public static void main(String[] args) {
        if (check1() || check2()) {
            System.out.println("Result: true");
        }
        
        System.out.println("---");
        
        if (check2() && check1()) {
            System.out.println("Result: true");
        } else {
            System.out.println("Result: false");
        }
    }
}
```

**Output:**
```
check1
Result: true
---
check2
Result: false
```

**Explanation:**
- `||` short-circuits: check1() is true, check2() not evaluated
- `&&` short-circuits: check2() is false, check1() not evaluated

---

### Q28: Post vs Pre Increment

```java
public class Test {
    public static void main(String[] args) {
        int x = 5;
        int y = x++ + ++x + x++;
        
        System.out.println("x = " + x);
        System.out.println("y = " + y);
    }
}
```

**Output:**
```
x = 8
y = 19
```

**Explanation:**
- `x++`: Use 5, then x becomes 6
- `++x`: x becomes 7, use 7
- `x++`: Use 7, then x becomes 8
- y = 5 + 7 + 7 = 19

---

### Q29: Array vs ArrayList

```java
public class Test {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3};
        List<Integer> list = Arrays.asList(arr);
        
        System.out.println(list.size());
        System.out.println(list.get(0).getClass().getName());
    }
}
```

**Output:**
```
1
[I
```

**Explanation:**
- `Arrays.asList(arr)` treats int[] as single object
- List contains one element: the array itself
- list.get(0) returns int[] (primitive array)
- Fix: Use `Arrays.stream(arr).boxed().collect(Collectors.toList())`

---

### Q30: Enum Comparison

```java
enum Day {
    MONDAY, TUESDAY, WEDNESDAY
}

public class Test {
    public static void main(String[] args) {
        Day d1 = Day.MONDAY;
        Day d2 = Day.MONDAY;
        
        System.out.println(d1 == d2);
        System.out.println(d1.equals(d2));
        System.out.println(d1.compareTo(d2));
        System.out.println(Day.MONDAY.compareTo(Day.WEDNESDAY));
    }
}
```

**Output:**
```
true
true
0
-2
```

**Explanation:**
- Enums are singletons: `==` works
- equals() also works (overridden)
- compareTo() compares ordinal positions
- MONDAY (0) vs WEDNESDAY (2) → -2

---

## Advanced Scenarios

### Q31: ClassCastException

```java
public class Test {
    public static void main(String[] args) {
        Object obj = "Hello";
        
        if (obj instanceof String) {
            String str = (String) obj;
            System.out.println(str.toUpperCase());
        }
        
        Object num = 123;
        String str2 = (String) num;  // Runtime error
    }
}
```

**Output:**
```
HELLO
Exception in thread "main" java.lang.ClassCastException: 
java.lang.Integer cannot be cast to java.lang.String
```

**Explanation:**
- First cast succeeds: obj is String
- Second cast fails: Integer cannot be cast to String
- Always use instanceof before casting

---

### Q32: NullPointerException

```java
public class Test {
    public static void main(String[] args) {
        String str = null;
        
        System.out.println(str + " World");
        System.out.println(str.length());  // NPE
    }
}
```

**Output:**
```
null World
Exception in thread "main" java.lang.NullPointerException
```

**Explanation:**
- String concatenation with null works (converts to "null")
- Method call on null throws NPE
- Use Optional or null checks

---

### Q33: Varargs Ambiguity

```java
public class Test {
    static void print(int... nums) {
        System.out.println("int varargs: " + nums.length);
    }
    
    static void print(Integer... nums) {
        System.out.println("Integer varargs: " + nums.length);
    }
    
    public static void main(String[] args) {
        print(1, 2, 3);
        print(new Integer[]{1, 2, 3});
    }
}
```

**Output:**
```
int varargs: 3
Integer varargs: 3
```

**Explanation:**
- First call: primitive int → matches int varargs
- Second call: Integer array → matches Integer varargs
- Compiler chooses most specific method

---

### Q34: Double Brace Initialization

```java
public class Test {
    public static void main(String[] args) {
        List<String> list = new ArrayList<String>() {{
            add("A");
            add("B");
            add("C");
        }};
        
        System.out.println(list);
        System.out.println(list.getClass().getName());
        System.out.println(list.getClass().getSuperclass().getName());
    }
}
```

**Output:**
```
[A, B, C]
Test$1
java.util.ArrayList
```

**Explanation:**
- Double brace creates anonymous inner class
- First brace: anonymous class
- Second brace: instance initializer block
- Creates extra class file (Test$1.class)

---

### Q35: Ternary Operator Type

```java
public class Test {
    public static void main(String[] args) {
        char x = 'X';
        int i = 0;
        
        System.out.println(true ? x : 0);
        System.out.println(false ? i : x);
        System.out.println(true ? 'A' : 0);
        System.out.println(false ? 0 : 'A');
    }
}
```

**Output:**
```
X
88
A
65
```

**Explanation:**
- Line 1: Returns char 'X'
- Line 2: Type promotion → int 88 (ASCII of 'X')
- Line 3: Returns char 'A'
- Line 4: Type promotion → int 65 (ASCII of 'A')
- Ternary operator promotes to common type

---

## Summary

These questions cover:
- ✅ String internment and immutability
- ✅ Collection behavior and pitfalls
- ✅ Concurrency and thread safety
- ✅ Exception handling nuances
- ✅ Inheritance and polymorphism
- ✅ Static and final semantics
- ✅ Autoboxing and caching
- ✅ Generics and type erasure
- ✅ Lambda and stream operations
- ✅ Tricky edge cases

**Interview Tips:**
1. Explain the "why" behind the output
2. Mention best practices and alternatives
3. Discuss performance implications
4. Reference Java version differences
5. Connect to real-world scenarios

---

**Next Steps:**
- Practice writing code without IDE
- Understand JVM internals
- Study Java Memory Model
- Learn design patterns
- Review concurrency utilities
