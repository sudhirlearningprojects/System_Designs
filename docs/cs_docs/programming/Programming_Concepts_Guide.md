# Programming Concepts Guide - Part 1

*Comprehensive guide covering programming fundamentals in Java, C, and C++*

## Table of Contents (Part 1)
1. [Introduction to Programming Languages](#introduction)
2. [Program Control Structures](#program-control)
3. [Functions and Methods](#functions)
4. [Scope of Variables](#scope)
5. [Variable and Function Binding](#binding)

---

## 1. Introduction to Programming Languages {#introduction}

### Language Paradigms

**Procedural Programming (C)**:
- **Structure**: Functions and procedures
- **Data**: Global and local variables
- **Approach**: Top-down problem solving
- **Example**: C language

**Object-Oriented Programming (Java/C++)**:
- **Structure**: Classes and objects
- **Data**: Encapsulated within objects
- **Approach**: Model real-world entities
- **Examples**: Java, C++

**Key Differences**:

| Aspect | C | C++ | Java |
|--------|---|-----|------|
| Paradigm | Procedural | Multi-paradigm | Object-oriented |
| Memory Management | Manual | Manual | Automatic (GC) |
| Compilation | Native code | Native code | Bytecode |
| Platform | Platform-specific | Platform-specific | Platform-independent |
| Pointers | Yes | Yes | No (references) |

### Basic Program Structure

**C Program Structure**:
```c
#include <stdio.h>

// Global variables
int globalVar = 10;

// Function declaration
int add(int a, int b);

int main() {
    // Local variables
    int result = add(5, 3);
    printf("Result: %d\n", result);
    return 0;
}

// Function definition
int add(int a, int b) {
    return a + b;
}
```

**C++ Program Structure**:
```cpp
#include <iostream>
using namespace std;

// Class declaration
class Calculator {
private:
    int value;
public:
    Calculator(int v = 0) : value(v) {}
    int add(int a, int b) { return a + b; }
};

int main() {
    Calculator calc;
    cout << "Result: " << calc.add(5, 3) << endl;
    return 0;
}
```

**Java Program Structure**:
```java
public class Calculator {
    // Instance variable
    private int value;
    
    // Constructor
    public Calculator(int value) {
        this.value = value;
    }
    
    // Method
    public int add(int a, int b) {
        return a + b;
    }
    
    // Main method
    public static void main(String[] args) {
        Calculator calc = new Calculator(0);
        System.out.println("Result: " + calc.add(5, 3));
    }
}
```

---

## 2. Program Control Structures {#program-control}

### Iteration (Loops)

**For Loop**:

**C/C++**:
```c
// Basic for loop
for (int i = 0; i < 10; i++) {
    printf("%d ", i);
}

// Nested loops
for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
        printf("(%d,%d) ", i, j);
    }
}

// Infinite loop
for (;;) {
    // Loop body
    if (condition) break;
}
```

**Java**:
```java
// Basic for loop
for (int i = 0; i < 10; i++) {
    System.out.print(i + " ");
}

// Enhanced for loop (for-each)
int[] array = {1, 2, 3, 4, 5};
for (int element : array) {
    System.out.print(element + " ");
}

// Nested loops
for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
        System.out.printf("(%d,%d) ", i, j);
    }
}
```

**While Loop**:

**C/C++**:
```c
int i = 0;
while (i < 10) {
    printf("%d ", i);
    i++;
}

// While with complex condition
while (input != 'q' && count < MAX_COUNT) {
    // Process input
    input = getchar();
    count++;
}
```

**Java**:
```java
int i = 0;
while (i < 10) {
    System.out.print(i + " ");
    i++;
}

// While with scanner
Scanner scanner = new Scanner(System.in);
while (scanner.hasNextInt()) {
    int num = scanner.nextInt();
    System.out.println("Read: " + num);
}
```

**Do-While Loop**:

**C/C++**:
```c
int choice;
do {
    printf("Enter choice (1-3, 0 to exit): ");
    scanf("%d", &choice);
    
    switch (choice) {
        case 1: printf("Option 1 selected\n"); break;
        case 2: printf("Option 2 selected\n"); break;
        case 3: printf("Option 3 selected\n"); break;
        case 0: printf("Exiting...\n"); break;
        default: printf("Invalid choice\n");
    }
} while (choice != 0);
```

**Java**:
```java
Scanner scanner = new Scanner(System.in);
int choice;
do {
    System.out.print("Enter choice (1-3, 0 to exit): ");
    choice = scanner.nextInt();
    
    switch (choice) {
        case 1: System.out.println("Option 1 selected"); break;
        case 2: System.out.println("Option 2 selected"); break;
        case 3: System.out.println("Option 3 selected"); break;
        case 0: System.out.println("Exiting..."); break;
        default: System.out.println("Invalid choice");
    }
} while (choice != 0);
```

### Recursion

**Basic Recursion Concepts**:
- **Base Case**: Condition to stop recursion
- **Recursive Case**: Function calls itself
- **Stack**: Each call creates new stack frame

**Factorial Example**:

**C**:
```c
int factorial(int n) {
    // Base case
    if (n <= 1) {
        return 1;
    }
    // Recursive case
    return n * factorial(n - 1);
}

// Usage
int main() {
    int result = factorial(5); // 120
    printf("5! = %d\n", result);
    return 0;
}
```

**Java**:
```java
public static int factorial(int n) {
    // Base case
    if (n <= 1) {
        return 1;
    }
    // Recursive case
    return n * factorial(n - 1);
}

// Usage
public static void main(String[] args) {
    int result = factorial(5); // 120
    System.out.println("5! = " + result);
}
```

**Fibonacci Sequence**:

**C++**:
```cpp
// Recursive approach (inefficient)
int fibonacci(int n) {
    if (n <= 1) return n;
    return fibonacci(n-1) + fibonacci(n-2);
}

// Iterative approach (efficient)
int fibonacciIterative(int n) {
    if (n <= 1) return n;
    
    int prev = 0, curr = 1;
    for (int i = 2; i <= n; i++) {
        int temp = curr;
        curr = prev + curr;
        prev = temp;
    }
    return curr;
}
```

**Tree Traversal (Recursion)**:

**C++**:
```cpp
struct TreeNode {
    int data;
    TreeNode* left;
    TreeNode* right;
    
    TreeNode(int val) : data(val), left(nullptr), right(nullptr) {}
};

// Inorder traversal
void inorderTraversal(TreeNode* root) {
    if (root == nullptr) return; // Base case
    
    inorderTraversal(root->left);   // Left subtree
    cout << root->data << " ";      // Process node
    inorderTraversal(root->right);  // Right subtree
}
```

**Java**:
```java
class TreeNode {
    int data;
    TreeNode left, right;
    
    TreeNode(int data) {
        this.data = data;
        left = right = null;
    }
}

public static void inorderTraversal(TreeNode root) {
    if (root == null) return; // Base case
    
    inorderTraversal(root.left);   // Left subtree
    System.out.print(root.data + " "); // Process node
    inorderTraversal(root.right);  // Right subtree
}
```

**Tail Recursion**:

**C**:
```c
// Non-tail recursive (less efficient)
int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1); // Operation after recursive call
}

// Tail recursive (more efficient)
int factorialTail(int n, int accumulator) {
    if (n <= 1) return accumulator;
    return factorialTail(n - 1, n * accumulator); // No operation after call
}

// Wrapper function
int factorial_optimized(int n) {
    return factorialTail(n, 1);
}
```

### Control Flow Statements

**Conditional Statements**:

**C/C++**:
```c
// If-else
if (score >= 90) {
    grade = 'A';
} else if (score >= 80) {
    grade = 'B';
} else if (score >= 70) {
    grade = 'C';
} else {
    grade = 'F';
}

// Switch statement
switch (day) {
    case 1: printf("Monday"); break;
    case 2: printf("Tuesday"); break;
    case 3: printf("Wednesday"); break;
    default: printf("Invalid day");
}

// Ternary operator
int max = (a > b) ? a : b;
```

**Java**:
```java
// If-else
if (score >= 90) {
    grade = 'A';
} else if (score >= 80) {
    grade = 'B';
} else if (score >= 70) {
    grade = 'C';
} else {
    grade = 'F';
}

// Switch statement (Java 14+ enhanced)
String dayName = switch (day) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 3 -> "Wednesday";
    default -> "Invalid day";
};

// Traditional switch
switch (day) {
    case 1: System.out.println("Monday"); break;
    case 2: System.out.println("Tuesday"); break;
    default: System.out.println("Invalid day");
}
```

**Jump Statements**:

**C/C++**:
```c
// Break and continue
for (int i = 0; i < 10; i++) {
    if (i == 3) continue; // Skip iteration
    if (i == 7) break;    // Exit loop
    printf("%d ", i);
}

// Goto (not recommended)
int i = 0;
loop_start:
    printf("%d ", i);
    i++;
    if (i < 5) goto loop_start;
```

**Java**:
```java
// Break and continue
for (int i = 0; i < 10; i++) {
    if (i == 3) continue; // Skip iteration
    if (i == 7) break;    // Exit loop
    System.out.print(i + " ");
}

// Labeled break/continue
outer: for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
        if (i == 1 && j == 1) break outer; // Break outer loop
        System.out.printf("(%d,%d) ", i, j);
    }
}
```

---

## 3. Functions and Methods {#functions}

### Function Basics

**C Functions**:
```c
// Function declaration (prototype)
int add(int a, int b);
void printArray(int arr[], int size);
double calculateAverage(int numbers[], int count);

// Function definition
int add(int a, int b) {
    return a + b;
}

void printArray(int arr[], int size) {
    for (int i = 0; i < size; i++) {
        printf("%d ", arr[i]);
    }
    printf("\n");
}

double calculateAverage(int numbers[], int count) {
    if (count == 0) return 0.0;
    
    int sum = 0;
    for (int i = 0; i < count; i++) {
        sum += numbers[i];
    }
    return (double)sum / count;
}
```

**C++ Functions**:
```cpp
#include <iostream>
#include <vector>
using namespace std;

// Function overloading
int add(int a, int b) {
    return a + b;
}

double add(double a, double b) {
    return a + b;
}

string add(string a, string b) {
    return a + b;
}

// Default parameters
void printMessage(string message, int times = 1) {
    for (int i = 0; i < times; i++) {
        cout << message << endl;
    }
}

// Template functions
template<typename T>
T maximum(T a, T b) {
    return (a > b) ? a : b;
}

// Usage
int main() {
    cout << add(5, 3) << endl;        // Calls int version
    cout << add(5.5, 3.2) << endl;   // Calls double version
    cout << add("Hello", "World") << endl; // Calls string version
    
    printMessage("Hello");            // Uses default times = 1
    printMessage("Hi", 3);            // Prints 3 times
    
    cout << maximum(10, 20) << endl;  // Template with int
    cout << maximum(5.5, 3.2) << endl; // Template with double
    
    return 0;
}
```

**Java Methods**:
```java
public class MathUtils {
    // Static method
    public static int add(int a, int b) {
        return a + b;
    }
    
    // Instance method
    public double calculateAverage(int[] numbers) {
        if (numbers.length == 0) return 0.0;
        
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return (double) sum / numbers.length;
    }
    
    // Method overloading
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public double multiply(double a, double b) {
        return a * b;
    }
    
    // Varargs method
    public int sum(int... numbers) {
        int total = 0;
        for (int num : numbers) {
            total += num;
        }
        return total;
    }
    
    // Usage
    public static void main(String[] args) {
        MathUtils utils = new MathUtils();
        
        System.out.println(add(5, 3)); // Static method call
        
        int[] array = {1, 2, 3, 4, 5};
        System.out.println(utils.calculateAverage(array)); // Instance method
        
        System.out.println(utils.multiply(5, 3));    // int version
        System.out.println(utils.multiply(5.5, 3.2)); // double version
        
        System.out.println(utils.sum(1, 2, 3, 4, 5)); // Varargs
    }
}
```

### Advanced Function Concepts

**Function Pointers (C/C++)**:

**C**:
```c
#include <stdio.h>

// Functions to be pointed to
int add(int a, int b) { return a + b; }
int subtract(int a, int b) { return a - b; }
int multiply(int a, int b) { return a * b; }

// Function that takes function pointer as parameter
int calculate(int a, int b, int (*operation)(int, int)) {
    return operation(a, b);
}

int main() {
    // Function pointer declaration and assignment
    int (*mathOp)(int, int);
    
    mathOp = add;
    printf("Add: %d\n", mathOp(5, 3));
    
    mathOp = subtract;
    printf("Subtract: %d\n", mathOp(5, 3));
    
    // Array of function pointers
    int (*operations[])(int, int) = {add, subtract, multiply};
    
    for (int i = 0; i < 3; i++) {
        printf("Operation %d: %d\n", i, operations[i](6, 4));
    }
    
    // Using function pointer as parameter
    printf("Calculate: %d\n", calculate(8, 2, multiply));
    
    return 0;
}
```

**C++**:
```cpp
#include <iostream>
#include <functional>
using namespace std;

// Lambda expressions
int main() {
    // Simple lambda
    auto add = [](int a, int b) { return a + b; };
    cout << "Lambda add: " << add(5, 3) << endl;
    
    // Lambda with capture
    int multiplier = 10;
    auto multiply = [multiplier](int x) { return x * multiplier; };
    cout << "Lambda multiply: " << multiply(5) << endl;
    
    // Lambda with mutable capture
    auto counter = [count = 0]() mutable { return ++count; };
    cout << "Counter: " << counter() << ", " << counter() << endl;
    
    // std::function
    function<int(int, int)> operation = add;
    cout << "Function wrapper: " << operation(7, 3) << endl;
    
    return 0;
}
```

**Recursive Functions - Advanced Examples**:

**Binary Search (C++)**:
```cpp
int binarySearch(int arr[], int left, int right, int target) {
    // Base case
    if (left > right) {
        return -1; // Not found
    }
    
    int mid = left + (right - left) / 2;
    
    if (arr[mid] == target) {
        return mid; // Found
    } else if (arr[mid] > target) {
        return binarySearch(arr, left, mid - 1, target); // Search left half
    } else {
        return binarySearch(arr, mid + 1, right, target); // Search right half
    }
}
```

**Tower of Hanoi (Java)**:
```java
public static void towerOfHanoi(int n, char source, char destination, char auxiliary) {
    // Base case
    if (n == 1) {
        System.out.println("Move disk 1 from " + source + " to " + destination);
        return;
    }
    
    // Move n-1 disks from source to auxiliary
    towerOfHanoi(n - 1, source, auxiliary, destination);
    
    // Move the largest disk from source to destination
    System.out.println("Move disk " + n + " from " + source + " to " + destination);
    
    // Move n-1 disks from auxiliary to destination
    towerOfHanoi(n - 1, auxiliary, destination, source);
}
```

---

## 4. Scope of Variables {#scope}

### Variable Scope Types

**Global Scope**:

**C**:
```c
#include <stdio.h>

// Global variables
int globalVar = 100;
static int fileStaticVar = 200; // File scope only

void function1() {
    printf("Global var in function1: %d\n", globalVar);
    globalVar = 150; // Modify global variable
}

void function2() {
    printf("Global var in function2: %d\n", globalVar);
}

int main() {
    printf("Global var in main: %d\n", globalVar);
    function1();
    function2();
    return 0;
}
```

**Java**:
```java
public class ScopeExample {
    // Class-level variables (similar to global)
    static int classVar = 100;        // Static variable
    private int instanceVar = 200;    // Instance variable
    
    public void method1() {
        System.out.println("Class var in method1: " + classVar);
        classVar = 150; // Modify class variable
    }
    
    public void method2() {
        System.out.println("Class var in method2: " + classVar);
        System.out.println("Instance var: " + instanceVar);
    }
    
    public static void main(String[] args) {
        System.out.println("Class var in main: " + classVar);
        
        ScopeExample obj = new ScopeExample();
        obj.method1();
        obj.method2();
    }
}
```

**Local Scope**:

**C**:
```c
#include <stdio.h>

int globalVar = 10;

void demonstrateScope() {
    int localVar = 20;        // Local to function
    static int staticVar = 30; // Retains value between calls
    
    printf("Local var: %d\n", localVar);
    printf("Static var: %d\n", staticVar);
    
    localVar++;
    staticVar++;
    
    // Block scope
    {
        int blockVar = 40;    // Local to this block
        printf("Block var: %d\n", blockVar);
        
        // Can access outer scope variables
        printf("Local var from block: %d\n", localVar);
    }
    // blockVar is not accessible here
}

int main() {
    demonstrateScope();
    demonstrateScope(); // Static variable retains value
    return 0;
}
```

**C++**:
```cpp
#include <iostream>
using namespace std;

int globalVar = 10;

void demonstrateScope() {
    int localVar = 20;
    static int staticVar = 30;
    
    cout << "Local var: " << localVar << endl;
    cout << "Static var: " << staticVar << endl;
    
    localVar++;
    staticVar++;
    
    // Block scope with initialization
    for (int i = 0; i < 3; i++) {
        int loopVar = i * 10;
        cout << "Loop var: " << loopVar << endl;
    }
    // i and loopVar not accessible here
    
    // Range-based for loop
    int array[] = {1, 2, 3, 4, 5};
    for (int element : array) {
        cout << "Element: " << element << endl;
    }
    // element not accessible here
}
```

**Java**:
```java
public class JavaScope {
    private int instanceVar = 10; // Instance scope
    static int classVar = 20;     // Class scope
    
    public void demonstrateScope() {
        int localVar = 30; // Method scope
        
        // Block scope
        {
            int blockVar = 40;
            System.out.println("Block var: " + blockVar);
            System.out.println("Local var from block: " + localVar);
        }
        // blockVar not accessible here
        
        // Loop scope
        for (int i = 0; i < 3; i++) {
            int loopVar = i * 10;
            System.out.println("Loop var: " + loopVar);
        }
        // i and loopVar not accessible here
        
        // Enhanced for loop
        int[] array = {1, 2, 3, 4, 5};
        for (int element : array) {
            System.out.println("Element: " + element);
        }
        // element not accessible here
    }
}
```

### Variable Shadowing

**C/C++**:
```c
#include <stdio.h>

int x = 10; // Global variable

void demonstrateShadowing() {
    int x = 20; // Local variable shadows global
    
    printf("Local x: %d\n", x); // Prints 20
    
    // Access global variable using scope resolution (C++)
    // printf("Global x: %d\n", ::x); // C++ only
    
    {
        int x = 30; // Block variable shadows local
        printf("Block x: %d\n", x); // Prints 30
    }
    
    printf("Local x again: %d\n", x); // Prints 20
}

int main() {
    printf("Global x: %d\n", x); // Prints 10
    demonstrateShadowing();
    printf("Global x after function: %d\n", x); // Still 10
    return 0;
}
```

**Java**:
```java
public class ShadowingExample {
    private int x = 10; // Instance variable
    
    public void demonstrateShadowing(int x) { // Parameter shadows instance variable
        System.out.println("Parameter x: " + x);
        System.out.println("Instance x: " + this.x); // Access instance variable
        
        {
            // int x = 30; // Error: Cannot redeclare parameter in same scope
            int y = 30;
            System.out.println("Block y: " + y);
        }
        
        // Loop variable
        for (int i = 0; i < 3; i++) {
            System.out.println("Loop i: " + i);
        }
    }
    
    public static void main(String[] args) {
        ShadowingExample obj = new ShadowingExample();
        obj.demonstrateShadowing(20);
    }
}
```

---

## 5. Variable and Function Binding {#binding}

### Static vs Dynamic Binding

**Static Binding (Early Binding)**:
- **Resolved**: At compile time
- **Examples**: Function overloading, static methods
- **Performance**: Faster execution

**Dynamic Binding (Late Binding)**:
- **Resolved**: At runtime
- **Examples**: Virtual functions, method overriding
- **Performance**: Slight overhead, more flexible

### Function Binding Examples

**C++ Virtual Functions**:
```cpp
#include <iostream>
using namespace std;

class Shape {
public:
    // Virtual function - dynamic binding
    virtual void draw() {
        cout << "Drawing a shape" << endl;
    }
    
    // Non-virtual function - static binding
    void info() {
        cout << "This is a shape" << endl;
    }
    
    virtual ~Shape() {} // Virtual destructor
};

class Circle : public Shape {
public:
    void draw() override { // Override virtual function
        cout << "Drawing a circle" << endl;
    }
    
    void info() { // Hides base class function
        cout << "This is a circle" << endl;
    }
};

class Rectangle : public Shape {
public:
    void draw() override {
        cout << "Drawing a rectangle" << endl;
    }
};

void demonstrateBinding() {
    Shape* shapes[] = {
        new Circle(),
        new Rectangle(),
        new Shape()
    };
    
    for (int i = 0; i < 3; i++) {
        shapes[i]->draw(); // Dynamic binding - calls appropriate version
        shapes[i]->info(); // Static binding - always calls Shape::info()
        cout << "---" << endl;
    }
    
    // Cleanup
    for (int i = 0; i < 3; i++) {
        delete shapes[i];
    }
}

int main() {
    demonstrateBinding();
    return 0;
}
```

**Java Method Overriding**:
```java
abstract class Animal {
    // Abstract method - must be overridden
    public abstract void makeSound();
    
    // Concrete method - can be overridden
    public void sleep() {
        System.out.println("Animal is sleeping");
    }
    
    // Final method - cannot be overridden
    public final void breathe() {
        System.out.println("Animal is breathing");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Dog barks: Woof!");
    }
    
    @Override
    public void sleep() {
        System.out.println("Dog is sleeping on the floor");
    }
    
    // Method specific to Dog
    public void wagTail() {
        System.out.println("Dog is wagging tail");
    }
}

class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Cat meows: Meow!");
    }
}

public class BindingDemo {
    public static void demonstrateBinding() {
        Animal[] animals = {
            new Dog(),
            new Cat()
        };
        
        for (Animal animal : animals) {
            animal.makeSound(); // Dynamic binding
            animal.sleep();     // Dynamic binding
            animal.breathe();   // Static binding (final method)
            
            // animal.wagTail(); // Error: not available in Animal reference
            
            // Type checking and casting
            if (animal instanceof Dog) {
                Dog dog = (Dog) animal;
                dog.wagTail(); // Now accessible
            }
            
            System.out.println("---");
        }
    }
    
    public static void main(String[] args) {
        demonstrateBinding();
    }
}
```

### Variable Binding

**Static Variables (C++)**:
```cpp
#include <iostream>
using namespace std;

class Counter {
private:
    static int count; // Static variable declaration
    int id;
    
public:
    Counter() {
        id = ++count; // Each object gets unique ID
    }
    
    void display() {
        cout << "Object ID: " << id << ", Total objects: " << count << endl;
    }
    
    static int getCount() { // Static method
        return count;
    }
};

// Static variable definition
int Counter::count = 0;

int main() {
    cout << "Initial count: " << Counter::getCount() << endl;
    
    Counter c1, c2, c3;
    c1.display();
    c2.display();
    c3.display();
    
    cout << "Final count: " << Counter::getCount() << endl;
    return 0;
}
```

**Java Static Variables**:
```java
public class Student {
    private static int totalStudents = 0; // Static variable
    private int studentId;
    private String name;
    
    // Static block - executed once when class is loaded
    static {
        System.out.println("Student class loaded");
        totalStudents = 0;
    }
    
    public Student(String name) {
        this.studentId = ++totalStudents;
        this.name = name;
    }
    
    public void displayInfo() {
        System.out.println("Student ID: " + studentId + 
                          ", Name: " + name + 
                          ", Total Students: " + totalStudents);
    }
    
    public static int getTotalStudents() { // Static method
        return totalStudents;
    }
    
    public static void main(String[] args) {
        System.out.println("Initial count: " + Student.getTotalStudents());
        
        Student s1 = new Student("Alice");
        Student s2 = new Student("Bob");
        Student s3 = new Student("Charlie");
        
        s1.displayInfo();
        s2.displayInfo();
        s3.displayInfo();
        
        System.out.println("Final count: " + Student.getTotalStudents());
    }
}
```

### Binding Time Examples

**Compile-Time Binding**:
```cpp
// Function overloading - resolved at compile time
class MathUtils {
public:
    int add(int a, int b) {
        return a + b;
    }
    
    double add(double a, double b) {
        return a + b;
    }
    
    string add(string a, string b) {
        return a + b;
    }
};

// Template instantiation - compile time
template<typename T>
T maximum(T a, T b) {
    return (a > b) ? a : b;
}
```

**Runtime Binding**:
```java
// Interface implementation - runtime polymorphism
interface Drawable {
    void draw();
}

class Circle implements Drawable {
    public void draw() {
        System.out.println("Drawing circle");
    }
}

class Square implements Drawable {
    public void draw() {
        System.out.println("Drawing square");
    }
}

// Runtime binding example
public void drawShape(Drawable shape) {
    shape.draw(); // Actual method determined at runtime
}
```

---

*Continue to Part 2 for Parameter Passing, Programming Paradigms, and OOP Concepts*