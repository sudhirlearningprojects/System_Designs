# Interface vs Abstract Class - Complete Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Quick Comparison](#quick-comparison)
3. [Interface](#interface)
4. [Abstract Class](#abstract-class)
5. [Default Methods (Java 8+)](#default-methods-java-8)
6. [Static Methods (Java 8+)](#static-methods-java-8)
7. [Private Methods (Java 9+)](#private-methods-java-9)
8. [Key Differences](#key-differences)
9. [When to Use What](#when-to-use-what)
10. [Real-World Examples](#real-world-examples)

---

## Introduction

### What is an Interface?

A contract that defines **what** a class must do, not **how** it does it.

```java
public interface Flyable {
    void fly();  // Abstract method
}
```

### What is an Abstract Class?

A class that cannot be instantiated and may contain both abstract and concrete methods.

```java
public abstract class Animal {
    abstract void makeSound();  // Abstract method
    
    void sleep() {  // Concrete method
        System.out.println("Sleeping...");
    }
}
```

---

## Quick Comparison

| Feature | Interface | Abstract Class |
|---------|-----------|----------------|
| **Keyword** | `interface` | `abstract class` |
| **Methods** | Abstract, default, static, private | Abstract and concrete |
| **Variables** | public static final (constants) | Any access modifier |
| **Constructor** | ❌ No | ✅ Yes |
| **Multiple Inheritance** | ✅ Yes | ❌ No |
| **Access Modifiers** | public (default) | Any |
| **When to use** | "Can do" relationship | "Is a" relationship |
| **Since** | Java 1.0 | Java 1.0 |
| **Default methods** | ✅ Yes (Java 8+) | N/A |
| **Static methods** | ✅ Yes (Java 8+) | ✅ Yes |

---

## Interface

### Basic Interface (Before Java 8)

```java
public interface Vehicle {
    // All methods are public abstract by default
    void start();
    void stop();
    
    // All variables are public static final by default
    int MAX_SPEED = 200;
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }
    
    @Override
    public void stop() {
        System.out.println("Car stopped");
    }
}
```

### Multiple Interface Implementation

```java
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

class Duck implements Flyable, Swimmable {
    @Override
    public void fly() {
        System.out.println("Duck flying");
    }
    
    @Override
    public void swim() {
        System.out.println("Duck swimming");
    }
}
```

### Interface Extending Interface

```java
interface Animal {
    void eat();
}

interface Mammal extends Animal {
    void breathe();
}

class Dog implements Mammal {
    @Override
    public void eat() {
        System.out.println("Dog eating");
    }
    
    @Override
    public void breathe() {
        System.out.println("Dog breathing");
    }
}
```

---

## Abstract Class

### Basic Abstract Class

```java
public abstract class Shape {
    // Instance variable
    protected String color;
    
    // Constructor
    public Shape(String color) {
        this.color = color;
    }
    
    // Abstract method (must be implemented by subclass)
    public abstract double calculateArea();
    
    // Concrete method (inherited by subclass)
    public void displayColor() {
        System.out.println("Color: " + color);
    }
}

class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}
```

### Abstract Class with Concrete Methods

```java
public abstract class Employee {
    private String name;
    private double baseSalary;
    
    public Employee(String name, double baseSalary) {
        this.name = name;
        this.baseSalary = baseSalary;
    }
    
    // Abstract method
    public abstract double calculateBonus();
    
    // Concrete method
    public double calculateTotalSalary() {
        return baseSalary + calculateBonus();
    }
    
    public String getName() {
        return name;
    }
}

class Manager extends Employee {
    public Manager(String name, double baseSalary) {
        super(name, baseSalary);
    }
    
    @Override
    public double calculateBonus() {
        return 5000;  // Fixed bonus for managers
    }
}

class Developer extends Employee {
    public Developer(String name, double baseSalary) {
        super(name, baseSalary);
    }
    
    @Override
    public double calculateBonus() {
        return 3000;  // Fixed bonus for developers
    }
}
```

---

## Default Methods (Java 8+)

### What are Default Methods?

Methods with implementation in interfaces. Allows adding new methods without breaking existing implementations.

### Basic Default Method

```java
public interface Vehicle {
    // Abstract method
    void start();
    
    // Default method (has implementation)
    default void honk() {
        System.out.println("Beep beep!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }
    
    // Can use default honk() or override it
}

// Usage
Car car = new Car();
car.start();  // Car started
car.honk();   // Beep beep! (uses default implementation)
```

### Overriding Default Method

```java
interface Vehicle {
    default void honk() {
        System.out.println("Generic honk");
    }
}

class Truck implements Vehicle {
    @Override
    public void honk() {
        System.out.println("Loud truck horn!");
    }
}

// Usage
Truck truck = new Truck();
truck.honk();  // Loud truck horn! (overridden)
```

### Default Method Calling Another Method

```java
interface Logger {
    void log(String message);
    
    default void logInfo(String message) {
        log("INFO: " + message);
    }
    
    default void logError(String message) {
        log("ERROR: " + message);
    }
}

class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println(message);
    }
}

// Usage
ConsoleLogger logger = new ConsoleLogger();
logger.logInfo("Application started");   // INFO: Application started
logger.logError("Something went wrong"); // ERROR: Something went wrong
```

### Multiple Inheritance with Default Methods

```java
interface A {
    default void hello() {
        System.out.println("Hello from A");
    }
}

interface B {
    default void hello() {
        System.out.println("Hello from B");
    }
}

class C implements A, B {
    // Must override to resolve conflict
    @Override
    public void hello() {
        A.super.hello();  // Call A's implementation
        // Or B.super.hello();
        // Or custom implementation
    }
}
```

### Real-World Example: Collection Interface

```java
// Java 8 added default methods to Collection interface
interface Collection<E> {
    // Existing abstract methods
    boolean add(E e);
    boolean remove(Object o);
    
    // New default method (Java 8) - doesn't break existing implementations
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
    
    // Another default method
    default void forEach(Consumer<? super E> action) {
        for (E element : this) {
            action.accept(element);
        }
    }
}
```

---

## Static Methods (Java 8+)

### What are Static Methods?

Utility methods that belong to the interface, not to instances.

### Basic Static Method

```java
public interface MathUtils {
    // Static method
    static int add(int a, int b) {
        return a + b;
    }
    
    static int multiply(int a, int b) {
        return a * b;
    }
}

// Usage - called on interface, not instance
int sum = MathUtils.add(5, 3);        // 8
int product = MathUtils.multiply(5, 3); // 15
```

### Static Method with Default Method

```java
interface StringUtils {
    // Static method
    static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    // Default method using static method
    default boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}

// Usage
boolean empty = StringUtils.isEmpty("");  // true (static method)

class MyClass implements StringUtils {
    public void process(String str) {
        if (isNotEmpty(str)) {  // default method
            System.out.println(str);
        }
    }
}
```

### Real-World Example: Comparator Interface

```java
interface Comparator<T> {
    int compare(T o1, T o2);
    
    // Static factory method
    static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
        return (c1, c2) -> c1.compareTo(c2);
    }
    
    // Another static factory method
    static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
        return Collections.reverseOrder();
    }
}

// Usage
List<String> names = Arrays.asList("John", "Alice", "Bob");
names.sort(Comparator.naturalOrder());  // Static method
```

### Static vs Default Methods

```java
interface Calculator {
    // Static method - utility function
    static int add(int a, int b) {
        return a + b;
    }
    
    // Default method - instance behavior
    default void printResult(int result) {
        System.out.println("Result: " + result);
    }
}

// Static method - called on interface
int sum = Calculator.add(5, 3);

// Default method - called on instance
class MyCalculator implements Calculator {}
MyCalculator calc = new MyCalculator();
calc.printResult(sum);
```

---

## Private Methods (Java 9+)

### What are Private Methods?

Helper methods in interfaces to avoid code duplication in default methods.

### Private Instance Method

```java
interface Logger {
    default void logInfo(String message) {
        log("INFO", message);
    }
    
    default void logError(String message) {
        log("ERROR", message);
    }
    
    default void logWarning(String message) {
        log("WARNING", message);
    }
    
    // Private method - code reuse
    private void log(String level, String message) {
        System.out.println(level + ": " + message);
    }
}
```

### Private Static Method

```java
interface MathUtils {
    static int sumOfSquares(int a, int b) {
        return square(a) + square(b);
    }
    
    static int sumOfCubes(int a, int b) {
        return cube(a) + cube(b);
    }
    
    // Private static helper method
    private static int square(int n) {
        return n * n;
    }
    
    private static int cube(int n) {
        return n * n * n;
    }
}
```

### Complete Example

```java
interface PaymentProcessor {
    // Abstract method
    void processPayment(double amount);
    
    // Default methods
    default void processWithFee(double amount) {
        double total = calculateTotal(amount, 0.03);
        processPayment(total);
    }
    
    default void processWithDiscount(double amount, double discount) {
        double total = calculateTotal(amount, -discount);
        processPayment(total);
    }
    
    // Private helper method
    private double calculateTotal(double amount, double adjustment) {
        return amount + (amount * adjustment);
    }
    
    // Static method
    static boolean isValidAmount(double amount) {
        return amount > 0;
    }
    
    // Private static helper
    private static void logTransaction(String message) {
        System.out.println("Transaction: " + message);
    }
}
```

---

## Key Differences

### 1. Multiple Inheritance

**Interface:**
```java
interface A { void methodA(); }
interface B { void methodB(); }

class C implements A, B {  // ✅ Multiple inheritance allowed
    public void methodA() {}
    public void methodB() {}
}
```

**Abstract Class:**
```java
abstract class A { abstract void methodA(); }
abstract class B { abstract void methodB(); }

class C extends A, B {  // ❌ Compile error - multiple inheritance not allowed
}
```

---

### 2. Constructor

**Interface:**
```java
interface Vehicle {
    // ❌ Cannot have constructor
}
```

**Abstract Class:**
```java
abstract class Vehicle {
    private String brand;
    
    // ✅ Can have constructor
    public Vehicle(String brand) {
        this.brand = brand;
    }
}
```

---

### 3. Variables

**Interface:**
```java
interface Constants {
    // All variables are public static final (constants)
    int MAX_VALUE = 100;
    String NAME = "Default";
    
    // ❌ Cannot have instance variables
}
```

**Abstract Class:**
```java
abstract class Vehicle {
    // ✅ Can have instance variables
    private String brand;
    protected int speed;
    public String color;
    
    // ✅ Can have static variables
    private static int count = 0;
}
```

---

### 4. Method Implementation

**Interface (Java 8+):**
```java
interface Calculator {
    // Abstract method (no implementation)
    int calculate(int a, int b);
    
    // Default method (has implementation)
    default void printResult(int result) {
        System.out.println("Result: " + result);
    }
    
    // Static method (has implementation)
    static int add(int a, int b) {
        return a + b;
    }
}
```

**Abstract Class:**
```java
abstract class Calculator {
    // Abstract method (no implementation)
    abstract int calculate(int a, int b);
    
    // Concrete method (has implementation)
    void printResult(int result) {
        System.out.println("Result: " + result);
    }
    
    // Static method (has implementation)
    static int add(int a, int b) {
        return a + b;
    }
}
```

---

### 5. Access Modifiers

**Interface:**
```java
interface Example {
    // All methods are public by default
    void method1();  // public abstract
    
    default void method2() {}  // public
    
    static void method3() {}   // public
    
    // ❌ Cannot use protected or private for abstract methods
}
```

**Abstract Class:**
```java
abstract class Example {
    // ✅ Can use any access modifier
    public abstract void method1();
    protected abstract void method2();
    abstract void method3();  // package-private
    
    private void method4() {}  // private concrete method
}
```

---

## When to Use What

### Use Interface When:

✅ **Defining a contract** - "Can do" relationship
```java
interface Flyable {
    void fly();
}

class Bird implements Flyable {
    public void fly() { /* implementation */ }
}

class Airplane implements Flyable {
    public void fly() { /* implementation */ }
}
```

✅ **Multiple inheritance needed**
```java
class Duck implements Flyable, Swimmable, Walkable {
    // Implements all three behaviors
}
```

✅ **Unrelated classes share behavior**
```java
interface Comparable<T> {
    int compareTo(T other);
}

class String implements Comparable<String> { }
class Integer implements Comparable<Integer> { }
class Date implements Comparable<Date> { }
```

---

### Use Abstract Class When:

✅ **"Is a" relationship** - Common base class
```java
abstract class Animal {
    protected String name;
    
    public Animal(String name) {
        this.name = name;
    }
    
    abstract void makeSound();
    
    void sleep() {
        System.out.println(name + " is sleeping");
    }
}

class Dog extends Animal {
    public Dog(String name) {
        super(name);
    }
    
    void makeSound() {
        System.out.println("Woof!");
    }
}
```

✅ **Need instance variables**
```java
abstract class Vehicle {
    protected String brand;
    protected int speed;
    
    public Vehicle(String brand) {
        this.brand = brand;
    }
}
```

✅ **Need constructor logic**
```java
abstract class DatabaseConnection {
    protected Connection connection;
    
    public DatabaseConnection(String url) {
        // Constructor logic
        this.connection = DriverManager.getConnection(url);
    }
}
```

✅ **Need protected/private methods**
```java
abstract class Template {
    // Template method pattern
    public final void execute() {
        step1();
        step2();
        step3();
    }
    
    protected abstract void step1();
    protected abstract void step2();
    
    private void step3() {
        // Common implementation
    }
}
```

---

## Real-World Examples

### Example 1: Payment System

**Using Interface:**
```java
interface PaymentMethod {
    boolean processPayment(double amount);
    
    default void printReceipt(double amount) {
        System.out.println("Payment of $" + amount + " processed");
    }
    
    static boolean isValidAmount(double amount) {
        return amount > 0;
    }
}

class CreditCard implements PaymentMethod {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing credit card payment: $" + amount);
        return true;
    }
}

class PayPal implements PaymentMethod {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing PayPal payment: $" + amount);
        return true;
    }
}

class Bitcoin implements PaymentMethod {
    @Override
    public boolean processPayment(double amount) {
        System.out.println("Processing Bitcoin payment: $" + amount);
        return true;
    }
}
```

---

### Example 2: Shape Hierarchy

**Using Abstract Class:**
```java
abstract class Shape {
    protected String color;
    protected boolean filled;
    
    public Shape(String color, boolean filled) {
        this.color = color;
        this.filled = filled;
    }
    
    // Abstract methods
    public abstract double getArea();
    public abstract double getPerimeter();
    
    // Concrete methods
    public String getColor() {
        return color;
    }
    
    public void displayInfo() {
        System.out.println("Color: " + color);
        System.out.println("Area: " + getArea());
        System.out.println("Perimeter: " + getPerimeter());
    }
}

class Circle extends Shape {
    private double radius;
    
    public Circle(String color, boolean filled, double radius) {
        super(color, filled);
        this.radius = radius;
    }
    
    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public double getPerimeter() {
        return 2 * Math.PI * radius;
    }
}

class Rectangle extends Shape {
    private double width;
    private double height;
    
    public Rectangle(String color, boolean filled, double width, double height) {
        super(color, filled);
        this.width = width;
        this.height = height;
    }
    
    @Override
    public double getArea() {
        return width * height;
    }
    
    @Override
    public double getPerimeter() {
        return 2 * (width + height);
    }
}
```

---

### Example 3: Combining Both

```java
// Interface for behavior
interface Drawable {
    void draw();
    
    default void display() {
        System.out.println("Displaying...");
        draw();
    }
}

// Abstract class for common state and behavior
abstract class Shape {
    protected String color;
    
    public Shape(String color) {
        this.color = color;
    }
    
    public abstract double getArea();
    
    public String getColor() {
        return color;
    }
}

// Concrete class implementing both
class Circle extends Shape implements Drawable {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing " + color + " circle");
    }
}

// Usage
Circle circle = new Circle("Red", 5.0);
circle.draw();           // From Drawable interface
circle.display();        // From Drawable default method
double area = circle.getArea();  // From Shape abstract class
String color = circle.getColor(); // From Shape concrete method
```

---

### Example 4: Collection Framework Pattern

```java
// Interface defining contract
interface Collection<E> {
    boolean add(E element);
    boolean remove(E element);
    int size();
    
    // Default method (Java 8)
    default boolean isEmpty() {
        return size() == 0;
    }
    
    // Static factory method (Java 8)
    static <E> Collection<E> empty() {
        return new ArrayList<>();
    }
}

// Abstract class providing partial implementation
abstract class AbstractCollection<E> implements Collection<E> {
    // Concrete implementation of common methods
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        // Common toString logic
        return sb.append("]").toString();
    }
    
    // Abstract methods to be implemented by subclasses
    public abstract boolean add(E element);
    public abstract boolean remove(E element);
    public abstract int size();
}

// Concrete implementation
class ArrayList<E> extends AbstractCollection<E> {
    private Object[] elements;
    private int size;
    
    public ArrayList() {
        elements = new Object[10];
    }
    
    @Override
    public boolean add(E element) {
        elements[size++] = element;
        return true;
    }
    
    @Override
    public boolean remove(E element) {
        // Implementation
        return false;
    }
    
    @Override
    public int size() {
        return size;
    }
}
```

---

## Summary

### Quick Decision Guide

**Choose Interface if:**
- Multiple inheritance needed
- Defining a contract/capability
- No state (instance variables) needed
- Unrelated classes share behavior

**Choose Abstract Class if:**
- Single inheritance is enough
- Need instance variables
- Need constructor logic
- "Is a" relationship
- Need protected/private methods

### Evolution of Interfaces

| Java Version | Feature |
|--------------|---------|
| Java 1.0 | Abstract methods only |
| Java 8 | Default methods, Static methods |
| Java 9 | Private methods |

### Key Points

1. **Interface** = Contract (what to do)
2. **Abstract Class** = Partial implementation (what and how)
3. **Default methods** = Add methods without breaking implementations
4. **Static methods** = Utility methods in interfaces
5. **Private methods** = Code reuse in default methods
6. **Use both** when needed for flexibility

---

**Last Updated**: 2024  
**Java Versions**: 8, 9, 11, 17, 21
