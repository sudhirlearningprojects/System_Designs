# Programming Concepts Guide - Part 2

*Advanced programming concepts covering parameter passing, paradigms, and object-oriented programming*

## Table of Contents (Part 2)
6. [Parameter Passing](#parameter-passing)
7. [Programming Paradigms](#programming-paradigms)
8. [Object-Oriented Programming Concepts](#oop-concepts)
9. [Classes and Objects](#classes-objects)
10. [Constructors and Destructors](#constructors-destructors)

---

## 6. Parameter Passing {#parameter-passing}

### Parameter Passing Mechanisms

**Pass by Value**:
- **Concept**: Copy of actual parameter is passed
- **Changes**: Don't affect original variable
- **Memory**: More memory usage for large objects
- **Performance**: Slower for large objects

**Pass by Reference**:
- **Concept**: Reference/address of actual parameter is passed
- **Changes**: Affect original variable
- **Memory**: Less memory usage
- **Performance**: Faster for large objects

**Pass by Pointer**:
- **Concept**: Address of variable is passed
- **Changes**: Can affect original variable through dereferencing
- **Flexibility**: Can be reassigned to point to different objects

### C Parameter Passing

**Pass by Value**:
```c
#include <stdio.h>

void modifyValue(int x) {
    x = 100; // Only modifies local copy
    printf("Inside function: x = %d\n", x);
}

void modifyArray(int arr[], int size) {
    // Arrays are always passed by reference in C
    arr[0] = 999; // Modifies original array
    printf("Inside function: arr[0] = %d\n", arr[0]);
}

int main() {
    int num = 50;
    printf("Before function call: num = %d\n", num);
    
    modifyValue(num);
    printf("After function call: num = %d\n", num); // Still 50
    
    int array[] = {1, 2, 3, 4, 5};
    printf("Before function call: array[0] = %d\n", array[0]);
    
    modifyArray(array, 5);
    printf("After function call: array[0] = %d\n", array[0]); // Changed to 999
    
    return 0;
}
```

**Pass by Pointer**:
```c
#include <stdio.h>

void modifyByPointer(int* ptr) {
    *ptr = 200; // Modifies value at the address
    printf("Inside function: *ptr = %d\n", *ptr);
}

void swapValues(int* a, int* b) {
    int temp = *a;
    *a = *b;
    *b = temp;
}

// Simulating pass by reference using pointers
void modifyByReference(int* x) {
    (*x)++; // Increment the value at address
}

int main() {
    int num = 100;
    printf("Before: num = %d\n", num);
    
    modifyByPointer(&num);
    printf("After: num = %d\n", num); // Changed to 200
    
    int a = 10, b = 20;
    printf("Before swap: a = %d, b = %d\n", a, b);
    
    swapValues(&a, &b);
    printf("After swap: a = %d, b = %d\n", a, b);
    
    int count = 5;
    printf("Before increment: count = %d\n", count);
    
    modifyByReference(&count);
    printf("After increment: count = %d\n", count);
    
    return 0;
}
```

### C++ Parameter Passing

**Pass by Value, Reference, and Pointer**:
```cpp
#include <iostream>
#include <string>
using namespace std;

// Pass by value
void passByValue(int x) {
    x = 100;
    cout << "Inside passByValue: x = " << x << endl;
}

// Pass by reference
void passByReference(int& x) {
    x = 200;
    cout << "Inside passByReference: x = " << x << endl;
}

// Pass by pointer
void passByPointer(int* x) {
    *x = 300;
    cout << "Inside passByPointer: *x = " << *x << endl;
}

// Pass by const reference (efficient for large objects)
void displayString(const string& str) {
    cout << "String: " << str << endl;
    // str = "modified"; // Error: cannot modify const reference
}

// Pass large objects efficiently
class LargeObject {
private:
    int data[1000];
public:
    LargeObject() {
        for (int i = 0; i < 1000; i++) {
            data[i] = i;
        }
    }
    
    void display() const {
        cout << "LargeObject with data[0] = " << data[0] << endl;
    }
};

// Inefficient - pass by value (copies entire object)
void processObjectByValue(LargeObject obj) {
    obj.display();
}

// Efficient - pass by const reference
void processObjectByReference(const LargeObject& obj) {
    obj.display();
}

int main() {
    int num = 50;
    
    cout << "Original: num = " << num << endl;
    
    passByValue(num);
    cout << "After passByValue: num = " << num << endl; // Still 50
    
    passByReference(num);
    cout << "After passByReference: num = " << num << endl; // Changed to 200
    
    passByPointer(&num);
    cout << "After passByPointer: num = " << num << endl; // Changed to 300
    
    string message = "Hello, World!";
    displayString(message); // Efficient for large strings
    
    LargeObject obj;
    cout << "\nProcessing large object:" << endl;
    processObjectByReference(obj); // Efficient
    
    return 0;
}
```

**Function Overloading with Different Parameter Types**:
```cpp
#include <iostream>
#include <vector>
using namespace std;

class Calculator {
public:
    // Overloaded functions with different parameter passing
    int add(int a, int b) { // Pass by value
        return a + b;
    }
    
    void add(int a, int b, int& result) { // Pass by reference
        result = a + b;
    }
    
    void add(const int* a, const int* b, int* result) { // Pass by pointer
        *result = *a + *b;
    }
    
    // Template function with universal reference
    template<typename T>
    T add(T&& a, T&& b) {
        return a + b;
    }
};

int main() {
    Calculator calc;
    
    // Different ways to call add function
    int result1 = calc.add(5, 3); // Pass by value
    cout << "Result1: " << result1 << endl;
    
    int result2;
    calc.add(10, 20, result2); // Pass by reference
    cout << "Result2: " << result2 << endl;
    
    int a = 15, b = 25, result3;
    calc.add(&a, &b, &result3); // Pass by pointer
    cout << "Result3: " << result3 << endl;
    
    return 0;
}
```

### Java Parameter Passing

**Java uses Pass by Value for primitives and Pass by Value of References for objects**:

```java
public class ParameterPassing {
    
    // Pass by value for primitives
    public static void modifyPrimitive(int x) {
        x = 100; // Only modifies local copy
        System.out.println("Inside method: x = " + x);
    }
    
    // Pass by value of reference for objects
    public static void modifyObject(StringBuilder sb) {
        sb.append(" World"); // Modifies the object
        System.out.println("Inside method: sb = " + sb);
    }
    
    // Reassigning reference doesn't affect original
    public static void reassignReference(StringBuilder sb) {
        sb = new StringBuilder("New String"); // Only changes local reference
        System.out.println("Inside method after reassign: sb = " + sb);
    }
    
    // Array modification
    public static void modifyArray(int[] arr) {
        if (arr.length > 0) {
            arr[0] = 999; // Modifies original array
        }
        System.out.println("Inside method: arr[0] = " + arr[0]);
    }
    
    // Wrapper classes are immutable
    public static void modifyWrapper(Integer num) {
        num = 200; // Only changes local reference
        System.out.println("Inside method: num = " + num);
    }
    
    public static void main(String[] args) {
        // Primitive parameter passing
        int number = 50;
        System.out.println("Before: number = " + number);
        modifyPrimitive(number);
        System.out.println("After: number = " + number); // Still 50
        
        // Object parameter passing
        StringBuilder sb = new StringBuilder("Hello");
        System.out.println("Before: sb = " + sb);
        modifyObject(sb);
        System.out.println("After: sb = " + sb); // Modified to "Hello World"
        
        // Reference reassignment
        StringBuilder sb2 = new StringBuilder("Original");
        System.out.println("Before reassign: sb2 = " + sb2);
        reassignReference(sb2);
        System.out.println("After reassign: sb2 = " + sb2); // Still "Original"
        
        // Array parameter passing
        int[] array = {1, 2, 3, 4, 5};
        System.out.println("Before: array[0] = " + array[0]);
        modifyArray(array);
        System.out.println("After: array[0] = " + array[0]); // Changed to 999
        
        // Wrapper class parameter passing
        Integer wrapperNum = 100;
        System.out.println("Before: wrapperNum = " + wrapperNum);
        modifyWrapper(wrapperNum);
        System.out.println("After: wrapperNum = " + wrapperNum); // Still 100
    }
}
```

**Varargs (Variable Arguments)**:
```java
public class VarargsExample {
    
    // Method with varargs
    public static int sum(int... numbers) {
        int total = 0;
        for (int num : numbers) {
            total += num;
        }
        return total;
    }
    
    // Method with regular parameter and varargs
    public static void printInfo(String prefix, Object... objects) {
        System.out.print(prefix + ": ");
        for (Object obj : objects) {
            System.out.print(obj + " ");
        }
        System.out.println();
    }
    
    // Generic varargs method
    public static <T> void printArray(T... elements) {
        for (T element : elements) {
            System.out.print(element + " ");
        }
        System.out.println();
    }
    
    public static void main(String[] args) {
        // Calling varargs methods
        System.out.println("Sum of no arguments: " + sum());
        System.out.println("Sum of 1,2,3: " + sum(1, 2, 3));
        System.out.println("Sum of 1,2,3,4,5: " + sum(1, 2, 3, 4, 5));
        
        // Passing array to varargs
        int[] numbers = {10, 20, 30};
        System.out.println("Sum of array: " + sum(numbers));
        
        // Mixed parameters
        printInfo("Numbers", 1, 2, 3, 4, 5);
        printInfo("Strings", "Hello", "World", "Java");
        
        // Generic varargs
        printArray(1, 2, 3, 4, 5);
        printArray("A", "B", "C", "D");
        printArray(1.1, 2.2, 3.3);
    }
}
```

---

## 7. Programming Paradigms {#programming-paradigms}

### Functional Programming

**Functional Programming Concepts**:
- **Pure Functions**: No side effects, same input always produces same output
- **Immutability**: Data doesn't change after creation
- **Higher-Order Functions**: Functions that take or return other functions
- **Recursion**: Preferred over iteration

**C++ Functional Programming**:
```cpp
#include <iostream>
#include <vector>
#include <algorithm>
#include <functional>
#include <numeric>
using namespace std;

// Pure function - no side effects
int add(int a, int b) {
    return a + b; // Always returns same result for same inputs
}

// Higher-order function - takes function as parameter
int applyOperation(int a, int b, function<int(int, int)> operation) {
    return operation(a, b);
}

// Function that returns a function
function<int(int)> createMultiplier(int factor) {
    return [factor](int x) { return x * factor; };
}

// Recursive function (functional style)
int factorial(int n) {
    return (n <= 1) ? 1 : n * factorial(n - 1);
}

// Functional operations on collections
void demonstrateFunctionalOperations() {
    vector<int> numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    
    // Filter even numbers
    vector<int> evenNumbers;
    copy_if(numbers.begin(), numbers.end(), back_inserter(evenNumbers),
            [](int n) { return n % 2 == 0; });
    
    cout << "Even numbers: ";
    for (int n : evenNumbers) cout << n << " ";
    cout << endl;
    
    // Transform (map) - square each number
    vector<int> squares;
    transform(numbers.begin(), numbers.end(), back_inserter(squares),
              [](int n) { return n * n; });
    
    cout << "Squares: ";
    for (int n : squares) cout << n << " ";
    cout << endl;
    
    // Reduce (fold) - sum all numbers
    int sum = accumulate(numbers.begin(), numbers.end(), 0);
    cout << "Sum: " << sum << endl;
    
    // Reduce with custom operation
    int product = accumulate(numbers.begin(), numbers.end(), 1,
                           [](int a, int b) { return a * b; });
    cout << "Product: " << product << endl;
}

int main() {
    // Using pure functions
    cout << "Add(5, 3): " << add(5, 3) << endl;
    
    // Higher-order functions
    cout << "Apply add: " << applyOperation(10, 20, add) << endl;
    cout << "Apply lambda: " << applyOperation(10, 20, [](int a, int b) { return a * b; }) << endl;
    
    // Function factory
    auto multiplyBy5 = createMultiplier(5);
    cout << "Multiply 7 by 5: " << multiplyBy5(7) << endl;
    
    // Recursion
    cout << "Factorial of 5: " << factorial(5) << endl;
    
    // Functional operations
    demonstrateFunctionalOperations();
    
    return 0;
}
```

**Java Functional Programming (Java 8+)**:
```java
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class FunctionalProgramming {
    
    // Pure function
    public static int add(int a, int b) {
        return a + b; // No side effects
    }
    
    // Higher-order function
    public static int applyOperation(int a, int b, BinaryOperator<Integer> operation) {
        return operation.apply(a, b);
    }
    
    // Function that returns a function
    public static Function<Integer, Integer> createMultiplier(int factor) {
        return x -> x * factor;
    }
    
    // Recursive function
    public static int factorial(int n) {
        return (n <= 1) ? 1 : n * factorial(n - 1);
    }
    
    public static void demonstrateStreams() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Filter and collect
        List<Integer> evenNumbers = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        System.out.println("Even numbers: " + evenNumbers);
        
        // Map (transform)
        List<Integer> squares = numbers.stream()
            .map(n -> n * n)
            .collect(Collectors.toList());
        System.out.println("Squares: " + squares);
        
        // Reduce
        int sum = numbers.stream()
            .reduce(0, Integer::sum);
        System.out.println("Sum: " + sum);
        
        // Complex stream operations
        List<String> result = numbers.stream()
            .filter(n -> n > 5)
            .map(n -> "Number: " + n)
            .sorted()
            .collect(Collectors.toList());
        System.out.println("Filtered and mapped: " + result);
        
        // Parallel processing
        long count = numbers.parallelStream()
            .filter(n -> n % 2 == 0)
            .count();
        System.out.println("Count of even numbers: " + count);
    }
    
    public static void main(String[] args) {
        // Pure functions
        System.out.println("Add(5, 3): " + add(5, 3));
        
        // Higher-order functions
        System.out.println("Apply add: " + applyOperation(10, 20, Integer::sum));
        System.out.println("Apply multiply: " + applyOperation(10, 20, (a, b) -> a * b));
        
        // Function factory
        Function<Integer, Integer> multiplyBy5 = createMultiplier(5);
        System.out.println("Multiply 7 by 5: " + multiplyBy5.apply(7));
        
        // Method references
        List<String> words = Arrays.asList("hello", "world", "java", "functional");
        words.stream()
            .map(String::toUpperCase)
            .forEach(System.out::println);
        
        // Recursion
        System.out.println("Factorial of 5: " + factorial(5));
        
        // Stream operations
        demonstrateStreams();
    }
}
```

### Logic Programming Concepts

**Logic Programming Principles**:
- **Declarative**: Describe what you want, not how to get it
- **Facts**: Basic statements about the world
- **Rules**: Logical implications
- **Queries**: Questions about the facts and rules

**Simulated Logic Programming in Java**:
```java
import java.util.*;

// Simple rule-based system simulation
class LogicProgramming {
    
    // Facts storage
    private Set<String> facts = new HashSet<>();
    private Map<String, List<String>> rules = new HashMap<>();
    
    // Add a fact
    public void addFact(String fact) {
        facts.add(fact);
    }
    
    // Add a rule: if condition then conclusion
    public void addRule(String condition, String conclusion) {
        rules.computeIfAbsent(condition, k -> new ArrayList<>()).add(conclusion);
    }
    
    // Query if a fact is true (with simple inference)
    public boolean query(String fact) {
        // Direct fact
        if (facts.contains(fact)) {
            return true;
        }
        
        // Check rules
        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            String condition = entry.getKey();
            List<String> conclusions = entry.getValue();
            
            if (conclusions.contains(fact) && query(condition)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Get all derivable facts
    public Set<String> getAllFacts() {
        Set<String> allFacts = new HashSet<>(facts);
        boolean changed = true;
        
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
                String condition = entry.getKey();
                List<String> conclusions = entry.getValue();
                
                if (allFacts.contains(condition)) {
                    for (String conclusion : conclusions) {
                        if (!allFacts.contains(conclusion)) {
                            allFacts.add(conclusion);
                            changed = true;
                        }
                    }
                }
            }
        }
        
        return allFacts;
    }
    
    public static void main(String[] args) {
        LogicProgramming lp = new LogicProgramming();
        
        // Add facts
        lp.addFact("Socrates is human");
        lp.addFact("Plato is human");
        
        // Add rules
        lp.addRule("Socrates is human", "Socrates is mortal");
        lp.addRule("Plato is human", "Plato is mortal");
        lp.addRule("X is human", "X is mortal"); // General rule
        
        // Queries
        System.out.println("Is Socrates human? " + lp.query("Socrates is human"));
        System.out.println("Is Socrates mortal? " + lp.query("Socrates is mortal"));
        System.out.println("Is Aristotle mortal? " + lp.query("Aristotle is mortal"));
        
        // Get all derivable facts
        System.out.println("All facts: " + lp.getAllFacts());
    }
}
```

---

## 8. Object-Oriented Programming Concepts {#oop-concepts}

### Four Pillars of OOP

**1. Encapsulation**:
- **Definition**: Bundling data and methods that operate on that data
- **Access Control**: Private, protected, public modifiers
- **Benefits**: Data hiding, modularity, maintainability

**2. Inheritance**:
- **Definition**: Creating new classes based on existing classes
- **Benefits**: Code reuse, hierarchical organization
- **Types**: Single, multiple, multilevel, hierarchical

**3. Polymorphism**:
- **Definition**: Same interface, different implementations
- **Types**: Compile-time (overloading), runtime (overriding)
- **Benefits**: Flexibility, extensibility

**4. Abstraction**:
- **Definition**: Hiding implementation details, showing only essential features
- **Implementation**: Abstract classes, interfaces
- **Benefits**: Simplicity, focus on what rather than how

### Encapsulation Examples

**C++ Encapsulation**:
```cpp
#include <iostream>
#include <string>
using namespace std;

class BankAccount {
private:
    string accountNumber;
    double balance;
    string ownerName;
    
    // Private helper method
    bool isValidAmount(double amount) {
        return amount > 0;
    }
    
public:
    // Constructor
    BankAccount(string accNum, string owner, double initialBalance = 0.0) {
        accountNumber = accNum;
        ownerName = owner;
        balance = (initialBalance >= 0) ? initialBalance : 0.0;
    }
    
    // Public interface methods
    bool deposit(double amount) {
        if (isValidAmount(amount)) {
            balance += amount;
            return true;
        }
        return false;
    }
    
    bool withdraw(double amount) {
        if (isValidAmount(amount) && amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }
    
    // Getter methods (accessors)
    double getBalance() const {
        return balance;
    }
    
    string getOwnerName() const {
        return ownerName;
    }
    
    string getAccountNumber() const {
        return accountNumber;
    }
    
    // Display account information
    void displayInfo() const {
        cout << "Account: " << accountNumber 
             << ", Owner: " << ownerName 
             << ", Balance: $" << balance << endl;
    }
};

int main() {
    BankAccount account("12345", "John Doe", 1000.0);
    
    account.displayInfo();
    
    account.deposit(500.0);
    cout << "After deposit: ";
    account.displayInfo();
    
    account.withdraw(200.0);
    cout << "After withdrawal: ";
    account.displayInfo();
    
    // Cannot access private members directly
    // account.balance = 5000; // Error: private member
    
    return 0;
}
```

**Java Encapsulation**:
```java
public class Student {
    // Private fields (data hiding)
    private int studentId;
    private String name;
    private double gpa;
    private int age;
    
    // Constructor
    public Student(int studentId, String name, int age) {
        this.studentId = studentId;
        this.name = name;
        this.age = (age > 0) ? age : 18; // Validation
        this.gpa = 0.0;
    }
    
    // Getter methods (accessors)
    public int getStudentId() {
        return studentId;
    }
    
    public String getName() {
        return name;
    }
    
    public double getGpa() {
        return gpa;
    }
    
    public int getAge() {
        return age;
    }
    
    // Setter methods (mutators) with validation
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }
    
    public void setAge(int age) {
        if (age > 0 && age < 120) {
            this.age = age;
        }
    }
    
    public void setGpa(double gpa) {
        if (gpa >= 0.0 && gpa <= 4.0) {
            this.gpa = gpa;
        }
    }
    
    // Business logic methods
    public String getGradeLevel() {
        if (gpa >= 3.5) return "Excellent";
        else if (gpa >= 3.0) return "Good";
        else if (gpa >= 2.0) return "Average";
        else return "Below Average";
    }
    
    public boolean isEligibleForHonors() {
        return gpa >= 3.5;
    }
    
    // Display method
    public void displayInfo() {
        System.out.printf("Student ID: %d, Name: %s, Age: %d, GPA: %.2f (%s)%n",
                         studentId, name, age, gpa, getGradeLevel());
    }
    
    // Override toString for better object representation
    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', age=%d, gpa=%.2f}",
                           studentId, name, age, gpa);
    }
    
    public static void main(String[] args) {
        Student student = new Student(1001, "Alice Johnson", 20);
        
        student.displayInfo();
        
        student.setGpa(3.7);
        student.displayInfo();
        
        System.out.println("Eligible for honors: " + student.isEligibleForHonors());
        System.out.println("Student object: " + student);
    }
}
```

### Abstraction Examples

**C++ Abstract Classes**:
```cpp
#include <iostream>
#include <vector>
#include <memory>
using namespace std;

// Abstract base class
class Shape {
protected:
    string color;
    
public:
    Shape(string c) : color(c) {}
    
    // Pure virtual function (makes class abstract)
    virtual double calculateArea() = 0;
    virtual double calculatePerimeter() = 0;
    
    // Concrete method
    virtual void displayInfo() {
        cout << "Color: " << color << ", Area: " << calculateArea() 
             << ", Perimeter: " << calculatePerimeter() << endl;
    }
    
    // Virtual destructor
    virtual ~Shape() {}
    
    // Getter
    string getColor() const { return color; }
};

// Concrete derived class
class Rectangle : public Shape {
private:
    double width, height;
    
public:
    Rectangle(string color, double w, double h) : Shape(color), width(w), height(h) {}
    
    double calculateArea() override {
        return width * height;
    }
    
    double calculatePerimeter() override {
        return 2 * (width + height);
    }
    
    void displayInfo() override {
        cout << "Rectangle - ";
        Shape::displayInfo();
    }
};

class Circle : public Shape {
private:
    double radius;
    static constexpr double PI = 3.14159;
    
public:
    Circle(string color, double r) : Shape(color), radius(r) {}
    
    double calculateArea() override {
        return PI * radius * radius;
    }
    
    double calculatePerimeter() override {
        return 2 * PI * radius;
    }
    
    void displayInfo() override {
        cout << "Circle - ";
        Shape::displayInfo();
    }
};

int main() {
    // Cannot instantiate abstract class
    // Shape shape("red"); // Error
    
    vector<unique_ptr<Shape>> shapes;
    shapes.push_back(make_unique<Rectangle>("Red", 5.0, 3.0));
    shapes.push_back(make_unique<Circle>("Blue", 4.0));
    shapes.push_back(make_unique<Rectangle>("Green", 2.0, 8.0));
    
    cout << "Shape Information:" << endl;
    for (const auto& shape : shapes) {
        shape->displayInfo(); // Polymorphic call
    }
    
    return 0;
}
```

**Java Interfaces and Abstract Classes**:
```java
// Interface for drawable objects
interface Drawable {
    void draw(); // Abstract method (implicitly public abstract)
    
    // Default method (Java 8+)
    default void display() {
        System.out.println("Displaying drawable object");
        draw();
    }
    
    // Static method (Java 8+)
    static void printInfo() {
        System.out.println("This is a drawable interface");
    }
}

// Abstract class
abstract class Animal {
    protected String name;
    protected int age;
    
    // Constructor
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Abstract methods
    public abstract void makeSound();
    public abstract void move();
    
    // Concrete method
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    public void displayInfo() {
        System.out.println("Name: " + name + ", Age: " + age);
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
}

// Concrete class implementing interface and extending abstract class
class Dog extends Animal implements Drawable {
    private String breed;
    
    public Dog(String name, int age, String breed) {
        super(name, age);
        this.breed = breed;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " barks: Woof! Woof!");
    }
    
    @Override
    public void move() {
        System.out.println(name + " runs on four legs");
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing a " + breed + " dog named " + name);
    }
    
    public void wagTail() {
        System.out.println(name + " is wagging tail happily");
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("Breed: " + breed);
    }
}

class Cat extends Animal implements Drawable {
    private boolean isIndoor;
    
    public Cat(String name, int age, boolean isIndoor) {
        super(name, age);
        this.isIndoor = isIndoor;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " meows: Meow! Meow!");
    }
    
    @Override
    public void move() {
        System.out.println(name + " walks gracefully");
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing a " + (isIndoor ? "indoor" : "outdoor") + " cat named " + name);
    }
    
    public void purr() {
        System.out.println(name + " is purring contentedly");
    }
}

public class AbstractionDemo {
    public static void main(String[] args) {
        // Cannot instantiate abstract class
        // Animal animal = new Animal("Generic", 5); // Error
        
        Dog dog = new Dog("Buddy", 3, "Golden Retriever");
        Cat cat = new Cat("Whiskers", 2, true);
        
        // Using abstract class methods
        dog.displayInfo();
        dog.makeSound();
        dog.move();
        dog.sleep();
        dog.wagTail();
        
        System.out.println();
        
        cat.displayInfo();
        cat.makeSound();
        cat.move();
        cat.purr();
        
        System.out.println();
        
        // Using interface methods
        Drawable[] drawables = {dog, cat};
        for (Drawable drawable : drawables) {
            drawable.display(); // Uses default method
        }
        
        Drawable.printInfo(); // Static method call
    }
}
```

---

## 9. Classes and Objects {#classes-objects}

### Class Definition and Object Creation

**C++ Classes**:
```cpp
#include <iostream>
#include <string>
#include <vector>
using namespace std;

class Car {
private:
    string make;
    string model;
    int year;
    double mileage;
    bool isRunning;
    
public:
    // Default constructor
    Car() : make("Unknown"), model("Unknown"), year(2000), mileage(0.0), isRunning(false) {
        cout << "Default constructor called" << endl;
    }
    
    // Parameterized constructor
    Car(string make, string model, int year) 
        : make(make), model(model), year(year), mileage(0.0), isRunning(false) {
        cout << "Parameterized constructor called for " << make << " " << model << endl;
    }
    
    // Copy constructor
    Car(const Car& other) 
        : make(other.make), model(other.model), year(other.year), 
          mileage(other.mileage), isRunning(other.isRunning) {
        cout << "Copy constructor called" << endl;
    }
    
    // Assignment operator
    Car& operator=(const Car& other) {
        if (this != &other) {
            make = other.make;
            model = other.model;
            year = other.year;
            mileage = other.mileage;
            isRunning = other.isRunning;
            cout << "Assignment operator called" << endl;
        }
        return *this;
    }
    
    // Destructor
    ~Car() {
        cout << "Destructor called for " << make << " " << model << endl;
    }
    
    // Member functions
    void start() {
        if (!isRunning) {
            isRunning = true;
            cout << make << " " << model << " started" << endl;
        } else {
            cout << make << " " << model << " is already running" << endl;
        }
    }
    
    void stop() {
        if (isRunning) {
            isRunning = false;
            cout << make << " " << model << " stopped" << endl;
        } else {
            cout << make << " " << model << " is already stopped" << endl;
        }
    }
    
    void drive(double miles) {
        if (isRunning && miles > 0) {
            mileage += miles;
            cout << "Drove " << miles << " miles. Total mileage: " << mileage << endl;
        } else {
            cout << "Cannot drive. Car must be running and miles must be positive." << endl;
        }
    }
    
    // Getters
    string getMake() const { return make; }
    string getModel() const { return model; }
    int getYear() const { return year; }
    double getMileage() const { return mileage; }
    bool getIsRunning() const { return isRunning; }
    
    // Display information
    void displayInfo() const {
        cout << year << " " << make << " " << model 
             << " - Mileage: " << mileage 
             << " - Status: " << (isRunning ? "Running" : "Stopped") << endl;
    }
};

int main() {
    // Object creation using different constructors
    Car car1; // Default constructor
    Car car2("Toyota", "Camry", 2022); // Parameterized constructor
    Car car3 = car2; // Copy constructor
    
    cout << "\nCar Information:" << endl;
    car1.displayInfo();
    car2.displayInfo();
    car3.displayInfo();
    
    cout << "\nOperating cars:" << endl;
    car2.start();
    car2.drive(100.5);
    car2.displayInfo();
    car2.stop();
    
    // Assignment
    car1 = car2; // Assignment operator
    car1.displayInfo();
    
    return 0;
} // Destructors called automatically
```

**Java Classes**:
```java
import java.util.ArrayList;
import java.util.List;

public class Book {
    // Instance variables
    private String title;
    private String author;
    private String isbn;
    private double price;
    private int pages;
    private boolean isAvailable;
    
    // Static variable (class variable)
    private static int totalBooks = 0;
    
    // Static block - executed when class is first loaded
    static {
        System.out.println("Book class loaded");
    }
    
    // Default constructor
    public Book() {
        this("Unknown Title", "Unknown Author", "000-0000000000", 0.0, 0);
    }
    
    // Parameterized constructor
    public Book(String title, String author, String isbn, double price, int pages) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
        this.pages = pages;
        this.isAvailable = true;
        totalBooks++;
        System.out.println("Book created: " + title);
    }
    
    // Copy constructor (Java doesn't have built-in copy constructor)
    public Book(Book other) {
        this(other.title, other.author, other.isbn, other.price, other.pages);
        this.isAvailable = other.isAvailable;
    }
    
    // Instance methods
    public void borrowBook() {
        if (isAvailable) {
            isAvailable = false;
            System.out.println("Book '" + title + "' has been borrowed");
        } else {
            System.out.println("Book '" + title + "' is not available");
        }
    }
    
    public void returnBook() {
        if (!isAvailable) {
            isAvailable = true;
            System.out.println("Book '" + title + "' has been returned");
        } else {
            System.out.println("Book '" + title + "' was not borrowed");
        }
    }
    
    public double calculateDiscountPrice(double discountPercent) {
        return price * (1 - discountPercent / 100);
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public double getPrice() { return price; }
    public int getPages() { return pages; }
    public boolean isAvailable() { return isAvailable; }
    
    // Setters with validation
    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
    }
    
    public void setPrice(double price) {
        if (price >= 0) {
            this.price = price;
        }
    }
    
    // Static methods
    public static int getTotalBooks() {
        return totalBooks;
    }
    
    public static void printLibraryInfo() {
        System.out.println("Total books in library: " + totalBooks);
    }
    
    // Override toString method
    @Override
    public String toString() {
        return String.format("Book{title='%s', author='%s', isbn='%s', price=%.2f, pages=%d, available=%s}",
                           title, author, isbn, price, pages, isAvailable);
    }
    
    // Override equals method
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Book book = (Book) obj;
        return isbn.equals(book.isbn); // Books are equal if ISBN is same
    }
    
    // Override hashCode method
    @Override
    public int hashCode() {
        return isbn.hashCode();
    }
    
    // Display method
    public void displayInfo() {
        System.out.printf("Title: %s%nAuthor: %s%nISBN: %s%nPrice: $%.2f%nPages: %d%nAvailable: %s%n%n",
                         title, author, isbn, price, pages, isAvailable ? "Yes" : "No");
    }
    
    public static void main(String[] args) {
        // Creating objects
        Book book1 = new Book(); // Default constructor
        Book book2 = new Book("Java Programming", "John Smith", "978-1234567890", 49.99, 500);
        Book book3 = new Book("Python Basics", "Jane Doe", "978-0987654321", 39.99, 350);
        Book book4 = new Book(book2); // Copy constructor
        
        System.out.println("\nBook Information:");
        book1.displayInfo();
        book2.displayInfo();
        book3.displayInfo();
        
        System.out.println("Book operations:");
        book2.borrowBook();
        book2.borrowBook(); // Try to borrow again
        book2.returnBook();
        
        System.out.println("Discount price for book2: $" + book2.calculateDiscountPrice(20));
        
        // Static method calls
        Book.printLibraryInfo();
        System.out.println("Total books: " + Book.getTotalBooks());
        
        // Using toString
        System.out.println("Book2 details: " + book2);
        
        // Testing equality
        System.out.println("book2 equals book4: " + book2.equals(book4));
    }
}
```

---

## 10. Constructors and Destructors {#constructors-destructors}

### Constructor Types and Usage

**C++ Constructors**:
```cpp
#include <iostream>
#include <string>
#include <cstring>
using namespace std;

class Person {
private:
    char* name;
    int age;
    string email;
    
public:
    // Default constructor
    Person() {
        cout << "Default constructor called" << endl;
        name = new char[1];
        name[0] = '\0';
        age = 0;
        email = "";
    }
    
    // Parameterized constructor
    Person(const char* n, int a, const string& e) {
        cout << "Parameterized constructor called" << endl;
        name = new char[strlen(n) + 1];
        strcpy(name, n);
        age = a;
        email = e;
    }
    
    // Copy constructor (deep copy)
    Person(const Person& other) {
        cout << "Copy constructor called" << endl;
        name = new char[strlen(other.name) + 1];
        strcpy(name, other.name);
        age = other.age;
        email = other.email;
    }
    
    // Move constructor (C++11)
    Person(Person&& other) noexcept {
        cout << "Move constructor called" << endl;
        name = other.name;
        age = other.age;
        email = move(other.email);
        
        // Reset other object
        other.name = nullptr;
        other.age = 0;
    }
    
    // Assignment operator
    Person& operator=(const Person& other) {
        cout << "Assignment operator called" << endl;
        if (this != &other) {
            // Clean up existing resources
            delete[] name;
            
            // Copy new data
            name = new char[strlen(other.name) + 1];
            strcpy(name, other.name);
            age = other.age;
            email = other.email;
        }
        return *this;
    }
    
    // Move assignment operator (C++11)
    Person& operator=(Person&& other) noexcept {
        cout << "Move assignment operator called" << endl;
        if (this != &other) {
            // Clean up existing resources
            delete[] name;
            
            // Move data
            name = other.name;
            age = other.age;
            email = move(other.email);
            
            // Reset other object
            other.name = nullptr;
            other.age = 0;
        }
        return *this;
    }
    
    // Destructor
    ~Person() {
        cout << "Destructor called for " << (name ? name : "null") << endl;
        delete[] name;
    }
    
    // Member functions
    void displayInfo() const {
        cout << "Name: " << (name ? name : "null") 
             << ", Age: " << age 
             << ", Email: " << email << endl;
    }
    
    // Getters
    const char* getName() const { return name; }
    int getAge() const { return age; }
    const string& getEmail() const { return email; }
};

// Function that returns Person object (triggers move constructor)
Person createPerson(const char* name, int age, const string& email) {
    return Person(name, age, email);
}

int main() {
    cout << "=== Constructor Examples ===" << endl;
    
    // Default constructor
    Person p1;
    p1.displayInfo();
    
    // Parameterized constructor
    Person p2("Alice", 25, "alice@email.com");
    p2.displayInfo();
    
    // Copy constructor
    Person p3 = p2; // or Person p3(p2);
    p3.displayInfo();
    
    // Assignment operator
    p1 = p2;
    p1.displayInfo();
    
    // Move constructor (C++11)
    Person p4 = createPerson("Bob", 30, "bob@email.com");
    p4.displayInfo();
    
    cout << "\n=== End of main function ===" << endl;
    return 0;
} // Destructors called automatically in reverse order
```

**Java Constructors**:
```java
import java.util.ArrayList;
import java.util.List;

public class Employee {
    // Instance variables
    private int employeeId;
    private String firstName;
    private String lastName;
    private String department;
    private double salary;
    private List<String> skills;
    
    // Static variable for generating unique IDs
    private static int nextId = 1000;
    
    // Default constructor
    public Employee() {
        this("Unknown", "Unknown", "General", 30000.0);
        System.out.println("Default constructor called");
    }
    
    // Constructor with basic information
    public Employee(String firstName, String lastName) {
        this(firstName, lastName, "General", 30000.0);
        System.out.println("Two-parameter constructor called");
    }
    
    // Constructor with department
    public Employee(String firstName, String lastName, String department) {
        this(firstName, lastName, department, 30000.0);
        System.out.println("Three-parameter constructor called");
    }
    
    // Full constructor (other constructors chain to this one)
    public Employee(String firstName, String lastName, String department, double salary) {
        this.employeeId = nextId++;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.salary = salary;
        this.skills = new ArrayList<>();
        System.out.println("Full constructor called for " + firstName + " " + lastName);
    }
    
    // Copy constructor (Java doesn't have built-in copy constructor)
    public Employee(Employee other) {
        this.employeeId = nextId++; // New employee gets new ID
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.department = other.department;
        this.salary = other.salary;
        this.skills = new ArrayList<>(other.skills); // Deep copy of list
        System.out.println("Copy constructor called");
    }
    
    // Instance initializer block (runs before constructor)
    {
        System.out.println("Instance initializer block executed");
    }
    
    // Static initializer block (runs when class is first loaded)
    static {
        System.out.println("Static initializer block executed - Employee class loaded");
    }
    
    // Methods
    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty() && !skills.contains(skill)) {
            skills.add(skill);
        }
    }
    
    public void removeSkill(String skill) {
        skills.remove(skill);
    }
    
    public void giveRaise(double percentage) {
        if (percentage > 0) {
            salary *= (1 + percentage / 100);
            System.out.printf("Salary increased by %.1f%% to $%.2f%n", percentage, salary);
        }
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Getters
    public int getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartment() { return department; }
    public double getSalary() { return salary; }
    public List<String> getSkills() { return new ArrayList<>(skills); } // Return copy
    
    // Setters with validation
    public void setFirstName(String firstName) {
        if (firstName != null && !firstName.trim().isEmpty()) {
            this.firstName = firstName;
        }
    }
    
    public void setLastName(String lastName) {
        if (lastName != null && !lastName.trim().isEmpty()) {
            this.lastName = lastName;
        }
    }
    
    public void setDepartment(String department) {
        if (department != null && !department.trim().isEmpty()) {
            this.department = department;
        }
    }
    
    public void setSalary(double salary) {
        if (salary > 0) {
            this.salary = salary;
        }
    }
    
    // Display method
    public void displayInfo() {
        System.out.printf("Employee ID: %d%n", employeeId);
        System.out.printf("Name: %s%n", getFullName());
        System.out.printf("Department: %s%n", department);
        System.out.printf("Salary: $%.2f%n", salary);
        System.out.printf("Skills: %s%n", skills);
        System.out.println();
    }
    
    // Override toString
    @Override
    public String toString() {
        return String.format("Employee{id=%d, name='%s', dept='%s', salary=%.2f, skills=%s}",
                           employeeId, getFullName(), department, salary, skills);
    }
    
    // Override equals
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        return employeeId == employee.employeeId;
    }
    
    // Override hashCode
    @Override
    public int hashCode() {
        return Integer.hashCode(employeeId);
    }
    
    // Finalize method (called by garbage collector - deprecated in Java 9+)
    @Override
    protected void finalize() throws Throwable {
        System.out.println("Finalize called for employee: " + getFullName());
        super.finalize();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Creating Employee Objects ===");
        
        // Different constructor calls
        Employee emp1 = new Employee(); // Default constructor
        Employee emp2 = new Employee("John", "Doe"); // Two parameters
        Employee emp3 = new Employee("Jane", "Smith", "IT"); // Three parameters
        Employee emp4 = new Employee("Bob", "Johnson", "HR", 55000.0); // Full constructor
        Employee emp5 = new Employee(emp4); // Copy constructor
        
        System.out.println("\n=== Employee Information ===");
        emp1.displayInfo();
        emp2.displayInfo();
        emp3.displayInfo();
        emp4.displayInfo();
        emp5.displayInfo();
        
        System.out.println("=== Adding Skills and Operations ===");
        emp4.addSkill("Java");
        emp4.addSkill("Python");
        emp4.addSkill("SQL");
        emp4.giveRaise(10.0);
        emp4.displayInfo();
        
        System.out.println("=== Object Comparison ===");
        System.out.println("emp4 equals emp5: " + emp4.equals(emp5));
        System.out.println("emp4 toString: " + emp4);
        
        // Suggest garbage collection (finalize methods may be called)
        emp1 = null;
        emp2 = null;
        System.gc();
        
        System.out.println("\n=== End of main method ===");
    }
}
```

---

*Continue to Part 3 for Inheritance, Exception Handling, and Comprehensive MCQ Practice*