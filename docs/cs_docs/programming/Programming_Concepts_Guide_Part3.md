# Programming Concepts Guide - Part 3

*Advanced programming concepts covering inheritance, exception handling, and comprehensive practice*

## Table of Contents (Part 3)
11. [Inheritance](#inheritance)
12. [Polymorphism](#polymorphism)
13. [Exception Handling](#exception-handling)
14. [Advanced Programming Concepts](#advanced-concepts)
15. [MCQ Practice Questions](#mcq-questions)

---

## 11. Inheritance {#inheritance}

### Types of Inheritance

**Single Inheritance**: One base class, one derived class
**Multiple Inheritance**: Multiple base classes (C++ only)
**Multilevel Inheritance**: Chain of inheritance
**Hierarchical Inheritance**: One base class, multiple derived classes
**Hybrid Inheritance**: Combination of multiple types

### C++ Inheritance

**Basic Inheritance**:
```cpp
#include <iostream>
#include <string>
using namespace std;

// Base class
class Vehicle {
protected:
    string brand;
    int year;
    double price;
    
public:
    Vehicle(string b, int y, double p) : brand(b), year(y), price(p) {
        cout << "Vehicle constructor called" << endl;
    }
    
    virtual ~Vehicle() {
        cout << "Vehicle destructor called" << endl;
    }
    
    virtual void start() {
        cout << brand << " vehicle started" << endl;
    }
    
    virtual void displayInfo() {
        cout << "Brand: " << brand << ", Year: " << year << ", Price: $" << price << endl;
    }
    
    // Getters
    string getBrand() const { return brand; }
    int getYear() const { return year; }
    double getPrice() const { return price; }
};

// Derived class
class Car : public Vehicle {
private:
    int doors;
    string fuelType;
    
public:
    Car(string b, int y, double p, int d, string f) 
        : Vehicle(b, y, p), doors(d), fuelType(f) {
        cout << "Car constructor called" << endl;
    }
    
    ~Car() {
        cout << "Car destructor called" << endl;
    }
    
    void start() override {
        cout << brand << " car engine started with " << fuelType << endl;
    }
    
    void displayInfo() override {
        Vehicle::displayInfo(); // Call base class method
        cout << "Doors: " << doors << ", Fuel: " << fuelType << endl;
    }
    
    void openTrunk() {
        cout << "Car trunk opened" << endl;
    }
};

class Motorcycle : public Vehicle {
private:
    bool hasSidecar;
    
public:
    Motorcycle(string b, int y, double p, bool sidecar) 
        : Vehicle(b, y, p), hasSidecar(sidecar) {
        cout << "Motorcycle constructor called" << endl;
    }
    
    ~Motorcycle() {
        cout << "Motorcycle destructor called" << endl;
    }
    
    void start() override {
        cout << brand << " motorcycle engine roared to life" << endl;
    }
    
    void displayInfo() override {
        Vehicle::displayInfo();
        cout << "Sidecar: " << (hasSidecar ? "Yes" : "No") << endl;
    }
    
    void wheelie() {
        cout << "Motorcycle doing a wheelie!" << endl;
    }
};

int main() {
    cout << "=== Creating Objects ===" << endl;
    Car car("Toyota", 2022, 25000, 4, "Gasoline");
    Motorcycle bike("Harley", 2021, 15000, false);
    
    cout << "\n=== Calling Methods ===" << endl;
    car.start();
    car.displayInfo();
    car.openTrunk();
    
    cout << endl;
    bike.start();
    bike.displayInfo();
    bike.wheelie();
    
    cout << "\n=== Polymorphism ===" << endl;
    Vehicle* vehicles[] = {&car, &bike};
    for (int i = 0; i < 2; i++) {
        vehicles[i]->start(); // Virtual function call
        vehicles[i]->displayInfo();
        cout << endl;
    }
    
    return 0;
}
```

**Multiple Inheritance**:
```cpp
#include <iostream>
#include <string>
using namespace std;

class Engine {
protected:
    int horsepower;
    string type;
    
public:
    Engine(int hp, string t) : horsepower(hp), type(t) {
        cout << "Engine constructor: " << hp << "HP " << t << endl;
    }
    
    virtual ~Engine() {
        cout << "Engine destructor" << endl;
    }
    
    void startEngine() {
        cout << type << " engine with " << horsepower << "HP started" << endl;
    }
    
    int getHorsepower() const { return horsepower; }
};

class GPS {
protected:
    string brand;
    bool isActive;
    
public:
    GPS(string b) : brand(b), isActive(false) {
        cout << "GPS constructor: " << b << endl;
    }
    
    virtual ~GPS() {
        cout << "GPS destructor" << endl;
    }
    
    void activate() {
        isActive = true;
        cout << brand << " GPS activated" << endl;
    }
    
    void navigate(string destination) {
        if (isActive) {
            cout << "Navigating to " << destination << " using " << brand << " GPS" << endl;
        } else {
            cout << "GPS not active" << endl;
        }
    }
};

// Multiple inheritance
class SmartCar : public Vehicle, public Engine, public GPS {
private:
    bool autopilot;
    
public:
    SmartCar(string brand, int year, double price, int hp, string engineType, 
             string gpsB, bool auto) 
        : Vehicle(brand, year, price), Engine(hp, engineType), GPS(gpsB), autopilot(auto) {
        cout << "SmartCar constructor called" << endl;
    }
    
    ~SmartCar() {
        cout << "SmartCar destructor called" << endl;
    }
    
    void start() override {
        cout << "Smart car systems initializing..." << endl;
        startEngine();
        activate();
        if (autopilot) {
            cout << "Autopilot ready" << endl;
        }
    }
    
    void displayInfo() override {
        Vehicle::displayInfo();
        cout << "Engine: " << horsepower << "HP " << type << endl;
        cout << "GPS: " << brand << endl;
        cout << "Autopilot: " << (autopilot ? "Yes" : "No") << endl;
    }
    
    void enableAutopilot() {
        if (autopilot) {
            cout << "Autopilot engaged" << endl;
        } else {
            cout << "This car doesn't have autopilot" << endl;
        }
    }
};

int main() {
    SmartCar tesla("Tesla", 2023, 80000, 400, "Electric", "Tesla Nav", true);
    
    cout << "\n=== Smart Car Operations ===" << endl;
    tesla.start();
    tesla.displayInfo();
    tesla.navigate("San Francisco");
    tesla.enableAutopilot();
    
    return 0;
}
```

### Java Inheritance

**Single Inheritance**:
```java
// Base class
class Animal {
    protected String name;
    protected int age;
    protected String species;
    
    public Animal(String name, int age, String species) {
        this.name = name;
        this.age = age;
        this.species = species;
        System.out.println("Animal constructor called for " + name);
    }
    
    public void eat() {
        System.out.println(name + " is eating");
    }
    
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    public void makeSound() {
        System.out.println(name + " makes a sound");
    }
    
    public void displayInfo() {
        System.out.printf("Name: %s, Age: %d, Species: %s%n", name, age, species);
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getSpecies() { return species; }
}

// Derived class
class Dog extends Animal {
    private String breed;
    private boolean isTrained;
    
    public Dog(String name, int age, String breed, boolean isTrained) {
        super(name, age, "Canine"); // Call parent constructor
        this.breed = breed;
        this.isTrained = isTrained;
        System.out.println("Dog constructor called for " + name);
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " barks: Woof! Woof!");
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo(); // Call parent method
        System.out.printf("Breed: %s, Trained: %s%n", breed, isTrained ? "Yes" : "No");
    }
    
    // Dog-specific methods
    public void fetch() {
        System.out.println(name + " is fetching the ball");
    }
    
    public void wagTail() {
        System.out.println(name + " is wagging tail happily");
    }
    
    public void train() {
        if (!isTrained) {
            isTrained = true;
            System.out.println(name + " has been trained");
        } else {
            System.out.println(name + " is already trained");
        }
    }
    
    public String getBreed() { return breed; }
    public boolean isTrained() { return isTrained; }
}

class Cat extends Animal {
    private boolean isIndoor;
    private int livesLeft;
    
    public Cat(String name, int age, boolean isIndoor) {
        super(name, age, "Feline");
        this.isIndoor = isIndoor;
        this.livesLeft = 9; // Cats have 9 lives!
        System.out.println("Cat constructor called for " + name);
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " meows: Meow! Meow!");
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.printf("Indoor: %s, Lives left: %d%n", 
                         isIndoor ? "Yes" : "No", livesLeft);
    }
    
    public void purr() {
        System.out.println(name + " is purring contentedly");
    }
    
    public void climb() {
        System.out.println(name + " is climbing");
    }
    
    public void useLive() {
        if (livesLeft > 0) {
            livesLeft--;
            System.out.println(name + " used a life. Lives left: " + livesLeft);
        }
    }
}

public class InheritanceDemo {
    public static void main(String[] args) {
        System.out.println("=== Creating Animals ===");
        Dog dog = new Dog("Buddy", 3, "Golden Retriever", false);
        Cat cat = new Cat("Whiskers", 2, true);
        
        System.out.println("\n=== Animal Information ===");
        dog.displayInfo();
        cat.displayInfo();
        
        System.out.println("\n=== Animal Behaviors ===");
        dog.eat();
        dog.makeSound();
        dog.fetch();
        dog.wagTail();
        dog.train();
        
        System.out.println();
        cat.eat();
        cat.makeSound();
        cat.purr();
        cat.climb();
        cat.useLive();
        
        System.out.println("\n=== Polymorphism ===");
        Animal[] animals = {dog, cat};
        for (Animal animal : animals) {
            animal.makeSound(); // Calls overridden method
            animal.sleep();
            System.out.println();
        }
    }
}
```

**Interface Implementation (Multiple Inheritance of Type)**:
```java
interface Flyable {
    void fly();
    void land();
    
    default void displayFlightInfo() {
        System.out.println("This object can fly");
    }
}

interface Swimmable {
    void swim();
    void dive();
    
    default void displaySwimInfo() {
        System.out.println("This object can swim");
    }
}

class Duck extends Animal implements Flyable, Swimmable {
    private boolean canFly;
    
    public Duck(String name, int age, boolean canFly) {
        super(name, age, "Waterfowl");
        this.canFly = canFly;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " quacks: Quack! Quack!");
    }
    
    @Override
    public void fly() {
        if (canFly) {
            System.out.println(name + " is flying gracefully");
        } else {
            System.out.println(name + " cannot fly");
        }
    }
    
    @Override
    public void land() {
        System.out.println(name + " has landed on water");
    }
    
    @Override
    public void swim() {
        System.out.println(name + " is swimming in the pond");
    }
    
    @Override
    public void dive() {
        System.out.println(name + " dives underwater for food");
    }
    
    public void waddle() {
        System.out.println(name + " is waddling on land");
    }
}

class Penguin extends Animal implements Swimmable {
    private String habitat;
    
    public Penguin(String name, int age, String habitat) {
        super(name, age, "Bird");
        this.habitat = habitat;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " makes penguin sounds");
    }
    
    @Override
    public void swim() {
        System.out.println(name + " swims expertly in cold water");
    }
    
    @Override
    public void dive() {
        System.out.println(name + " dives deep for fish");
    }
    
    public void slide() {
        System.out.println(name + " slides on ice");
    }
}
```

---

## 12. Polymorphism {#polymorphism}

### Runtime Polymorphism

**C++ Virtual Functions**:
```cpp
#include <iostream>
#include <vector>
#include <memory>
using namespace std;

class Shape {
protected:
    string color;
    
public:
    Shape(string c) : color(c) {}
    virtual ~Shape() = default;
    
    // Pure virtual functions
    virtual double area() = 0;
    virtual double perimeter() = 0;
    virtual void draw() = 0;
    
    // Virtual function with implementation
    virtual void displayInfo() {
        cout << "Shape - Color: " << color 
             << ", Area: " << area() 
             << ", Perimeter: " << perimeter() << endl;
    }
    
    string getColor() const { return color; }
};

class Rectangle : public Shape {
private:
    double width, height;
    
public:
    Rectangle(string color, double w, double h) : Shape(color), width(w), height(h) {}
    
    double area() override {
        return width * height;
    }
    
    double perimeter() override {
        return 2 * (width + height);
    }
    
    void draw() override {
        cout << "Drawing a " << color << " rectangle (" << width << "x" << height << ")" << endl;
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
    
    double area() override {
        return PI * radius * radius;
    }
    
    double perimeter() override {
        return 2 * PI * radius;
    }
    
    void draw() override {
        cout << "Drawing a " << color << " circle (radius: " << radius << ")" << endl;
    }
    
    void displayInfo() override {
        cout << "Circle - ";
        Shape::displayInfo();
    }
};

class Triangle : public Shape {
private:
    double side1, side2, side3;
    
public:
    Triangle(string color, double s1, double s2, double s3) 
        : Shape(color), side1(s1), side2(s2), side3(s3) {}
    
    double area() override {
        // Using Heron's formula
        double s = (side1 + side2 + side3) / 2;
        return sqrt(s * (s - side1) * (s - side2) * (s - side3));
    }
    
    double perimeter() override {
        return side1 + side2 + side3;
    }
    
    void draw() override {
        cout << "Drawing a " << color << " triangle (" 
             << side1 << ", " << side2 << ", " << side3 << ")" << endl;
    }
};

void processShapes(vector<unique_ptr<Shape>>& shapes) {
    cout << "Processing shapes polymorphically:" << endl;
    
    for (auto& shape : shapes) {
        shape->draw();           // Polymorphic call
        shape->displayInfo();    // Polymorphic call
        cout << "---" << endl;
    }
}

int main() {
    vector<unique_ptr<Shape>> shapes;
    
    shapes.push_back(make_unique<Rectangle>("Red", 5.0, 3.0));
    shapes.push_back(make_unique<Circle>("Blue", 4.0));
    shapes.push_back(make_unique<Triangle>("Green", 3.0, 4.0, 5.0));
    shapes.push_back(make_unique<Rectangle>("Yellow", 2.0, 8.0));
    
    processShapes(shapes);
    
    return 0;
}
```

**Java Polymorphism**:
```java
abstract class Employee {
    protected String name;
    protected int id;
    protected double baseSalary;
    
    public Employee(String name, int id, double baseSalary) {
        this.name = name;
        this.id = id;
        this.baseSalary = baseSalary;
    }
    
    // Abstract methods
    public abstract double calculateSalary();
    public abstract void displayRole();
    
    // Concrete methods
    public void displayInfo() {
        System.out.printf("ID: %d, Name: %s, Base Salary: $%.2f%n", 
                         id, name, baseSalary);
        displayRole();
        System.out.printf("Total Salary: $%.2f%n", calculateSalary());
    }
    
    public String getName() { return name; }
    public int getId() { return id; }
    public double getBaseSalary() { return baseSalary; }
}

class Manager extends Employee {
    private double bonus;
    private int teamSize;
    
    public Manager(String name, int id, double baseSalary, double bonus, int teamSize) {
        super(name, id, baseSalary);
        this.bonus = bonus;
        this.teamSize = teamSize;
    }
    
    @Override
    public double calculateSalary() {
        return baseSalary + bonus + (teamSize * 1000); // Team bonus
    }
    
    @Override
    public void displayRole() {
        System.out.println("Role: Manager (Team size: " + teamSize + ")");
    }
    
    public void conductMeeting() {
        System.out.println(name + " is conducting a team meeting");
    }
}

class Developer extends Employee {
    private String programmingLanguage;
    private int projectsCompleted;
    
    public Developer(String name, int id, double baseSalary, 
                    String language, int projects) {
        super(name, id, baseSalary);
        this.programmingLanguage = language;
        this.projectsCompleted = projects;
    }
    
    @Override
    public double calculateSalary() {
        return baseSalary + (projectsCompleted * 500); // Project bonus
    }
    
    @Override
    public void displayRole() {
        System.out.println("Role: Developer (" + programmingLanguage + 
                          ", Projects: " + projectsCompleted + ")");
    }
    
    public void writeCode() {
        System.out.println(name + " is writing " + programmingLanguage + " code");
    }
}

class Salesperson extends Employee {
    private double commissionRate;
    private double salesAmount;
    
    public Salesperson(String name, int id, double baseSalary, 
                      double commissionRate, double salesAmount) {
        super(name, id, baseSalary);
        this.commissionRate = commissionRate;
        this.salesAmount = salesAmount;
    }
    
    @Override
    public double calculateSalary() {
        return baseSalary + (salesAmount * commissionRate);
    }
    
    @Override
    public void displayRole() {
        System.out.printf("Role: Salesperson (Commission: %.1f%%, Sales: $%.2f)%n",
                         commissionRate * 100, salesAmount);
    }
    
    public void makeSale(double amount) {
        salesAmount += amount;
        System.out.printf("%s made a sale of $%.2f%n", name, amount);
    }
}

public class PolymorphismDemo {
    public static void processEmployees(Employee[] employees) {
        System.out.println("Processing employees polymorphically:");
        System.out.println("=".repeat(50));
        
        double totalPayroll = 0;
        
        for (Employee emp : employees) {
            emp.displayInfo(); // Polymorphic method call
            totalPayroll += emp.calculateSalary(); // Polymorphic method call
            
            // Type checking and casting
            if (emp instanceof Manager) {
                ((Manager) emp).conductMeeting();
            } else if (emp instanceof Developer) {
                ((Developer) emp).writeCode();
            } else if (emp instanceof Salesperson) {
                ((Salesperson) emp).makeSale(1000);
            }
            
            System.out.println("-".repeat(30));
        }
        
        System.out.printf("Total Payroll: $%.2f%n", totalPayroll);
    }
    
    public static void main(String[] args) {
        Employee[] employees = {
            new Manager("Alice Johnson", 101, 80000, 15000, 5),
            new Developer("Bob Smith", 102, 70000, "Java", 8),
            new Salesperson("Carol Davis", 103, 50000, 0.05, 100000),
            new Developer("David Wilson", 104, 75000, "Python", 12),
            new Manager("Eve Brown", 105, 85000, 20000, 8)
        };
        
        processEmployees(employees);
    }
}
```

---

## 13. Exception Handling {#exception-handling}

### C++ Exception Handling

**Basic Exception Handling**:
```cpp
#include <iostream>
#include <stdexcept>
#include <string>
using namespace std;

class DivisionByZeroException : public exception {
public:
    const char* what() const noexcept override {
        return "Division by zero error";
    }
};

class NegativeNumberException : public exception {
private:
    string message;
public:
    NegativeNumberException(const string& msg) : message(msg) {}
    
    const char* what() const noexcept override {
        return message.c_str();
    }
};

class Calculator {
public:
    static double divide(double a, double b) {
        if (b == 0) {
            throw DivisionByZeroException();
        }
        return a / b;
    }
    
    static double squareRoot(double x) {
        if (x < 0) {
            throw NegativeNumberException("Cannot calculate square root of negative number: " + to_string(x));
        }
        return sqrt(x);
    }
    
    static int factorial(int n) {
        if (n < 0) {
            throw invalid_argument("Factorial not defined for negative numbers");
        }
        if (n > 20) {
            throw overflow_error("Factorial too large for int type");
        }
        
        int result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
};

void demonstrateExceptionHandling() {
    cout << "=== Exception Handling Demo ===" << endl;
    
    // Division by zero
    try {
        double result = Calculator::divide(10, 0);
        cout << "Result: " << result << endl;
    }
    catch (const DivisionByZeroException& e) {
        cout << "Caught custom exception: " << e.what() << endl;
    }
    
    // Negative square root
    try {
        double result = Calculator::squareRoot(-25);
        cout << "Square root: " << result << endl;
    }
    catch (const NegativeNumberException& e) {
        cout << "Caught custom exception: " << e.what() << endl;
    }
    
    // Multiple exception types
    try {
        int result = Calculator::factorial(-5);
        cout << "Factorial: " << result << endl;
    }
    catch (const invalid_argument& e) {
        cout << "Invalid argument: " << e.what() << endl;
    }
    catch (const overflow_error& e) {
        cout << "Overflow error: " << e.what() << endl;
    }
    catch (const exception& e) {
        cout << "General exception: " << e.what() << endl;
    }
    
    // Exception in constructor
    try {
        vector<int> vec(-1); // Invalid size
    }
    catch (const exception& e) {
        cout << "Vector construction failed: " << e.what() << endl;
    }
}

// RAII and exception safety
class Resource {
private:
    string name;
    
public:
    Resource(const string& n) : name(n) {
        cout << "Resource " << name << " acquired" << endl;
        if (name == "bad") {
            throw runtime_error("Bad resource");
        }
    }
    
    ~Resource() {
        cout << "Resource " << name << " released" << endl;
    }
    
    void use() {
        cout << "Using resource " << name << endl;
    }
};

void demonstrateRAII() {
    cout << "\n=== RAII Demo ===" << endl;
    
    try {
        Resource r1("good");
        r1.use();
        
        Resource r2("bad"); // This will throw
        r2.use(); // Never reached
    }
    catch (const exception& e) {
        cout << "Exception caught: " << e.what() << endl;
        // r1 is automatically destroyed here
    }
}

int main() {
    demonstrateExceptionHandling();
    demonstrateRAII();
    
    return 0;
}
```

### Java Exception Handling

**Comprehensive Exception Handling**:
```java
import java.io.*;
import java.util.*;

// Custom checked exception
class InsufficientFundsException extends Exception {
    private double balance;
    private double amount;
    
    public InsufficientFundsException(double balance, double amount) {
        super("Insufficient funds: Balance=" + balance + ", Requested=" + amount);
        this.balance = balance;
        this.amount = amount;
    }
    
    public double getBalance() { return balance; }
    public double getAmount() { return amount; }
}

// Custom unchecked exception
class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }
}

class BankAccount {
    private String accountNumber;
    private double balance;
    private boolean isActive;
    
    public BankAccount(String accountNumber, double initialBalance) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidAccountException("Account number cannot be null or empty");
        }
        if (initialBalance < 0) {
            throw new InvalidAccountException("Initial balance cannot be negative");
        }
        
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.isActive = true;
    }
    
    public void deposit(double amount) {
        if (!isActive) {
            throw new InvalidAccountException("Account is not active");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        
        balance += amount;
        System.out.printf("Deposited $%.2f. New balance: $%.2f%n", amount, balance);
    }
    
    public void withdraw(double amount) throws InsufficientFundsException {
        if (!isActive) {
            throw new InvalidAccountException("Account is not active");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (amount > balance) {
            throw new InsufficientFundsException(balance, amount);
        }
        
        balance -= amount;
        System.out.printf("Withdrew $%.2f. New balance: $%.2f%n", amount, balance);
    }
    
    public double getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    public boolean isActive() { return isActive; }
    
    public void closeAccount() {
        isActive = false;
        System.out.println("Account " + accountNumber + " has been closed");
    }
}

class FileProcessor {
    public static void processFile(String filename) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                System.out.println("Line " + lineNumber + ": " + line);
                lineNumber++;
            }
        } finally {
            // Always executed, even if exception occurs
            if (reader != null) {
                try {
                    reader.close();
                    System.out.println("File closed successfully");
                } catch (IOException e) {
                    System.err.println("Error closing file: " + e.getMessage());
                }
            }
        }
    }
    
    // Try-with-resources (Java 7+)
    public static void processFileWithTryWithResources(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                System.out.println("Line " + lineNumber + ": " + line);
                lineNumber++;
            }
        } // reader.close() called automatically
    }
}

public class ExceptionHandlingDemo {
    
    public static void demonstrateBankOperations() {
        System.out.println("=== Bank Operations Demo ===");
        
        try {
            BankAccount account = new BankAccount("12345", 1000.0);
            
            account.deposit(500.0);
            account.withdraw(200.0);
            account.withdraw(2000.0); // This will throw InsufficientFundsException
            
        } catch (InsufficientFundsException e) {
            System.err.println("Transaction failed: " + e.getMessage());
            System.err.printf("Available balance: $%.2f%n", e.getBalance());
        } catch (InvalidAccountException e) {
            System.err.println("Account error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument: " + e.getMessage());
        }
    }
    
    public static void demonstrateMultipleCatch() {
        System.out.println("\n=== Multiple Catch Demo ===");
        
        String[] testInputs = {"123", "abc", null, "456"};
        
        for (String input : testInputs) {
            try {
                int number = Integer.parseInt(input);
                int result = 100 / number;
                System.out.println("Result: " + result);
                
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format: " + input);
            } catch (ArithmeticException e) {
                System.err.println("Arithmetic error: " + e.getMessage());
            } catch (NullPointerException e) {
                System.err.println("Null input provided");
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
            } finally {
                System.out.println("Processing completed for input: " + input);
            }
        }
    }
    
    public static void demonstrateFileProcessing() {
        System.out.println("\n=== File Processing Demo ===");
        
        // Create a test file
        try (PrintWriter writer = new PrintWriter("test.txt")) {
            writer.println("Line 1: Hello World");
            writer.println("Line 2: Java Exception Handling");
            writer.println("Line 3: Try-Catch-Finally");
        } catch (IOException e) {
            System.err.println("Error creating test file: " + e.getMessage());
            return;
        }
        
        // Process existing file
        try {
            System.out.println("Processing existing file:");
            FileProcessor.processFileWithTryWithResources("test.txt");
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
        
        // Process non-existing file
        try {
            System.out.println("Processing non-existing file:");
            FileProcessor.processFile("nonexistent.txt");
        } catch (IOException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }
    
    public static void demonstrateChainedExceptions() {
        System.out.println("\n=== Chained Exceptions Demo ===");
        
        try {
            processData();
        } catch (Exception e) {
            System.err.println("Top-level exception: " + e.getMessage());
            
            Throwable cause = e.getCause();
            while (cause != null) {
                System.err.println("Caused by: " + cause.getMessage());
                cause = cause.getCause();
            }
            
            System.err.println("Stack trace:");
            e.printStackTrace();
        }
    }
    
    private static void processData() throws Exception {
        try {
            parseData();
        } catch (NumberFormatException e) {
            throw new Exception("Data processing failed", e);
        }
    }
    
    private static void parseData() {
        try {
            Integer.parseInt("invalid");
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid data format in parseData()");
        }
    }
    
    public static void main(String[] args) {
        demonstrateBankOperations();
        demonstrateMultipleCatch();
        demonstrateFileProcessing();
        demonstrateChainedExceptions();
        
        // Clean up test file
        new File("test.txt").delete();
    }
}
```

---

## 14. Advanced Programming Concepts {#advanced-concepts}

### Generic Programming

**C++ Templates**:
```cpp
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

// Function template
template<typename T>
T maximum(T a, T b) {
    return (a > b) ? a : b;
}

// Class template
template<typename T>
class Stack {
private:
    vector<T> elements;
    
public:
    void push(const T& element) {
        elements.push_back(element);
    }
    
    T pop() {
        if (elements.empty()) {
            throw runtime_error("Stack is empty");
        }
        T top = elements.back();
        elements.pop_back();
        return top;
    }
    
    bool empty() const {
        return elements.empty();
    }
    
    size_t size() const {
        return elements.size();
    }
    
    const T& top() const {
        if (elements.empty()) {
            throw runtime_error("Stack is empty");
        }
        return elements.back();
    }
};

// Template specialization
template<>
class Stack<bool> {
private:
    vector<bool> elements;
    
public:
    void push(bool element) {
        elements.push_back(element);
        cout << "Specialized bool stack: pushed " << (element ? "true" : "false") << endl;
    }
    
    bool pop() {
        if (elements.empty()) {
            throw runtime_error("Bool stack is empty");
        }
        bool top = elements.back();
        elements.pop_back();
        return top;
    }
    
    bool empty() const { return elements.empty(); }
    size_t size() const { return elements.size(); }
};

int main() {
    // Function template usage
    cout << "Max of 10 and 20: " << maximum(10, 20) << endl;
    cout << "Max of 3.14 and 2.71: " << maximum(3.14, 2.71) << endl;
    cout << "Max of 'a' and 'z': " << maximum('a', 'z') << endl;
    
    // Class template usage
    Stack<int> intStack;
    intStack.push(10);
    intStack.push(20);
    intStack.push(30);
    
    cout << "Int stack size: " << intStack.size() << endl;
    cout << "Popping: " << intStack.pop() << endl;
    cout << "Top: " << intStack.top() << endl;
    
    // Template specialization
    Stack<bool> boolStack;
    boolStack.push(true);
    boolStack.push(false);
    
    return 0;
}
```

**Java Generics**:
```java
import java.util.*;

// Generic class
class Pair<T, U> {
    private T first;
    private U second;
    
    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
    
    public T getFirst() { return first; }
    public U getSecond() { return second; }
    
    public void setFirst(T first) { this.first = first; }
    public void setSecond(U second) { this.second = second; }
    
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}

// Generic method
class Utility {
    public static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    public static <T extends Comparable<T>> T findMax(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) {
                max = item;
            }
        }
        return max;
    }
    
    // Wildcards
    public static void printList(List<?> list) {
        for (Object item : list) {
            System.out.print(item + " ");
        }
        System.out.println();
    }
    
    public static double sumNumbers(List<? extends Number> numbers) {
        double sum = 0.0;
        for (Number num : numbers) {
            sum += num.doubleValue();
        }
        return sum;
    }
}

public class GenericsDemo {
    public static void main(String[] args) {
        // Generic class usage
        Pair<String, Integer> nameAge = new Pair<>("Alice", 25);
        Pair<Double, Boolean> scorePass = new Pair<>(85.5, true);
        
        System.out.println("Name-Age pair: " + nameAge);
        System.out.println("Score-Pass pair: " + scorePass);
        
        // Generic method usage
        String[] names = {"Alice", "Bob", "Charlie"};
        System.out.println("Before swap: " + Arrays.toString(names));
        Utility.swap(names, 0, 2);
        System.out.println("After swap: " + Arrays.toString(names));
        
        // Bounded type parameters
        List<Integer> numbers = Arrays.asList(3, 7, 2, 9, 1);
        System.out.println("Max number: " + Utility.findMax(numbers));
        
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        System.out.println("Max word: " + Utility.findMax(words));
        
        // Wildcards
        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);
        List<Double> doubleList = Arrays.asList(1.1, 2.2, 3.3);
        
        System.out.print("Integer list: ");
        Utility.printList(intList);
        
        System.out.print("Double list: ");
        Utility.printList(doubleList);
        
        System.out.println("Sum of integers: " + Utility.sumNumbers(intList));
        System.out.println("Sum of doubles: " + Utility.sumNumbers(doubleList));
    }
}
```

---

## 15. MCQ Practice Questions {#mcq-questions}

### Questions 1-10: Basic Programming Concepts

**1. Which parameter passing mechanism allows the called function to modify the original variable?**
a) Pass by value
b) Pass by reference
c) Pass by name
d) Pass by result

**Answer: b) Pass by reference**
**Explanation**: Pass by reference passes the address of the variable, allowing modifications to affect the original variable.

**2. What is the output of this C++ code?**
```cpp
int x = 10;
{
    int x = 20;
    cout << x;
}
cout << x;
```
a) 1010
b) 2020
c) 2010
d) 1020

**Answer: c) 2010**
**Explanation**: The inner block variable x shadows the outer x, so it prints 20 then 10.

**3. In Java, which statement about constructors is FALSE?**
a) Constructors can be overloaded
b) Constructors can call other constructors using this()
c) Constructors can be inherited
d) Constructors have no return type

**Answer: c) Constructors can be inherited**
**Explanation**: Constructors are not inherited in Java; each class must define its own constructors.

**4. What is the difference between function overloading and function overriding?**
a) Overloading is compile-time, overriding is runtime
b) Overriding is compile-time, overloading is runtime
c) Both are compile-time
d) Both are runtime

**Answer: a) Overloading is compile-time, overriding is runtime**
**Explanation**: Function overloading is resolved at compile time, while overriding is resolved at runtime through virtual function calls.

**5. Which OOP principle is violated if private data members are accessed directly?**
a) Inheritance
b) Polymorphism
c) Encapsulation
d) Abstraction

**Answer: c) Encapsulation**
**Explanation**: Encapsulation requires data hiding through private access modifiers and controlled access via public methods.

**6. In C++, what happens when a destructor is not virtual in a base class?**
a) Compilation error
b) Runtime error
c) Undefined behavior during polymorphic deletion
d) Nothing, it works fine

**Answer: c) Undefined behavior during polymorphic deletion**
**Explanation**: Non-virtual destructors can cause undefined behavior when deleting derived objects through base class pointers.

**7. Which Java keyword prevents method overriding?**
a) static
b) final
c) private
d) abstract

**Answer: b) final**
**Explanation**: The final keyword prevents methods from being overridden in subclasses.

**8. What is the purpose of the 'this' pointer in C++?**
a) Points to the current object
b) Points to the base class
c) Points to static members
d) Points to global variables

**Answer: a) Points to the current object**
**Explanation**: The 'this' pointer refers to the current object instance within member functions.

**9. In Java, which exception type must be either caught or declared in the method signature?**
a) RuntimeException
b) Error
c) Checked exceptions
d) Unchecked exceptions

**Answer: c) Checked exceptions**
**Explanation**: Checked exceptions must be either caught with try-catch or declared in the method signature with throws.

**10. What is the result of multiple inheritance in C++?**
a) Always causes compilation errors
b) Can lead to diamond problem
c) Is not supported
d) Only works with interfaces

**Answer: b) Can lead to diamond problem**
**Explanation**: Multiple inheritance can cause the diamond problem when a class inherits from two classes that share a common base class.

### Questions 11-20: Advanced Concepts

**11. Which binding type is used for virtual functions in C++?**
a) Static binding
b) Dynamic binding
c) Early binding
d) Compile-time binding

**Answer: b) Dynamic binding**
**Explanation**: Virtual functions use dynamic binding, where the actual function to call is determined at runtime.

**12. In Java, what is the difference between abstract classes and interfaces?**
a) No difference
b) Abstract classes can have constructors, interfaces cannot
c) Interfaces can have constructors, abstract classes cannot
d) Both can have constructors

**Answer: b) Abstract classes can have constructors, interfaces cannot**
**Explanation**: Abstract classes can have constructors and concrete methods, while interfaces cannot have constructors.

**13. What is the purpose of the finally block in exception handling?**
a) Handle specific exceptions
b) Execute code regardless of exception occurrence
c) Throw new exceptions
d) Catch all exceptions

**Answer: b) Execute code regardless of exception occurrence**
**Explanation**: The finally block executes whether an exception occurs or not, typically used for cleanup.

**14. In C++, what is RAII?**
a) Resource Acquisition Is Initialization
b) Runtime Application Interface Implementation
c) Recursive Algorithm Implementation Interface
d) Reference Assignment and Initialization

**Answer: a) Resource Acquisition Is Initialization**
**Explanation**: RAII is a programming idiom where resource acquisition is tied to object initialization and release to destruction.

**15. Which Java feature allows a class to implement multiple interfaces?**
a) Multiple inheritance
b) Interface inheritance
c) Polymorphism
d) All of the above

**Answer: b) Interface inheritance**
**Explanation**: Java supports multiple interface inheritance, allowing a class to implement multiple interfaces.

**16. What is the difference between shallow copy and deep copy?**
a) No difference
b) Shallow copy copies references, deep copy copies objects
c) Deep copy copies references, shallow copy copies objects
d) Both copy references only

**Answer: b) Shallow copy copies references, deep copy copies objects**
**Explanation**: Shallow copy copies references to objects, while deep copy creates new copies of the objects themselves.

**17. In functional programming, what is a pure function?**
a) Function with no parameters
b) Function with no side effects and same output for same input
c) Function that returns void
d) Function with only primitive parameters

**Answer: b) Function with no side effects and same output for same input**
**Explanation**: Pure functions have no side effects and always return the same result for the same input parameters.

**18. What is method overloading resolution based on in Java?**
a) Return type only
b) Method name only
c) Parameter list (number, type, order)
d) Access modifiers

**Answer: c) Parameter list (number, type, order)**
**Explanation**: Method overloading is resolved based on the method signature: parameter count, types, and order.

**19. Which C++ feature allows compile-time polymorphism?**
a) Virtual functions
b) Function templates
c) Inheritance
d) Dynamic casting

**Answer: b) Function templates**
**Explanation**: Function templates provide compile-time polymorphism by generating different function versions for different types.

**20. In Java generics, what does the wildcard '?' represent?**
a) Any specific type
b) Unknown type
c) Primitive types only
d) Reference types only

**Answer: b) Unknown type**
**Explanation**: The wildcard '?' represents an unknown type in Java generics, used for type flexibility.

### Questions 21-30: Exception Handling and Advanced Topics

**21. What happens if an exception is thrown in a constructor?**
a) Object is created normally
b) Object creation fails, destructor not called
c) Compilation error
d) Runtime warning only

**Answer: b) Object creation fails, destructor not called**
**Explanation**: If constructor throws an exception, object creation fails and destructor is not called since object wasn't fully constructed.

**22. Which Java keyword is used to manually throw an exception?**
a) throws
b) throw
c) try
d) catch

**Answer: b) throw**
**Explanation**: The 'throw' keyword is used to manually throw an exception, while 'throws' declares exceptions in method signature.

**23. What is the diamond problem in multiple inheritance?**
a) Memory allocation issue
b) Ambiguity when inheriting from classes with common base
c) Compilation speed issue
d) Runtime performance issue

**Answer: b) Ambiguity when inheriting from classes with common base**
**Explanation**: Diamond problem occurs when a class inherits from two classes that share a common base class, causing ambiguity.

**24. In C++, what is the purpose of virtual destructors?**
a) Faster destruction
b) Proper cleanup in polymorphic hierarchies
c) Memory optimization
d) Compilation optimization

**Answer: b) Proper cleanup in polymorphic hierarchies**
**Explanation**: Virtual destructors ensure proper cleanup when deleting derived objects through base class pointers.

**25. Which Java collection allows duplicate elements?**
a) Set
b) Map
c) List
d) Both b and c

**Answer: c) List**
**Explanation**: List interface allows duplicate elements, while Set does not allow duplicates.

**26. What is the difference between checked and unchecked exceptions in Java?**
a) Checked exceptions are caught at compile time
b) Checked exceptions must be handled or declared
c) Unchecked exceptions must be handled or declared
d) No difference

**Answer: b) Checked exceptions must be handled or declared**
**Explanation**: Checked exceptions must be either caught or declared in the method signature, while unchecked exceptions don't have this requirement.

**27. In C++, what is the purpose of the 'const' keyword in member functions?**
a) Makes the function faster
b) Prevents the function from modifying object state
c) Makes the function static
d) Prevents function overriding

**Answer: b) Prevents the function from modifying object state**
**Explanation**: Const member functions promise not to modify the object's state and can be called on const objects.

**28. What is polymorphism?**
a) Having multiple constructors
b) Same interface, different implementations
c) Multiple inheritance
d) Function overloading only

**Answer: b) Same interface, different implementations**
**Explanation**: Polymorphism allows objects of different types to be treated uniformly through a common interface.

**29. In Java, what is the difference between String and StringBuilder?**
a) No difference
b) String is mutable, StringBuilder is immutable
c) String is immutable, StringBuilder is mutable
d) Both are mutable

**Answer: c) String is immutable, StringBuilder is mutable**
**Explanation**: String objects are immutable in Java, while StringBuilder provides a mutable sequence of characters.

**30. What is the purpose of generic programming?**
a) Faster execution
b) Type safety and code reusability
c) Memory optimization
d) Easier debugging

**Answer: b) Type safety and code reusability**
**Explanation**: Generic programming provides type safety at compile time and allows writing reusable code for different types.

---

## Study Tips for Programming Concepts

### Key Areas to Focus On

**1. Object-Oriented Programming**
- Master the four pillars: Encapsulation, Inheritance, Polymorphism, Abstraction
- Understand constructor/destructor lifecycle
- Practice inheritance hierarchies and virtual functions

**2. Exception Handling**
- Learn try-catch-finally patterns
- Understand checked vs unchecked exceptions
- Practice resource management with RAII/try-with-resources

**3. Memory Management**
- Understand stack vs heap allocation
- Learn about constructors, destructors, and object lifecycle
- Practice with pointers, references, and smart pointers

**4. Generic Programming**
- Master templates in C++ and generics in Java
- Understand type constraints and wildcards
- Practice writing reusable, type-safe code

### Common Mistakes to Avoid

**1. Constructor/Destructor Issues**
- Forgetting virtual destructors in base classes
- Not handling exceptions in constructors
- Incorrect constructor chaining

**2. Inheritance Problems**
- Confusing overloading with overriding
- Not understanding virtual function behavior
- Improper access specifier usage

**3. Exception Handling Errors**
- Catching exceptions too broadly
- Not cleaning up resources properly
- Throwing exceptions from destructors

### Best Practices

**1. Code Organization**
- Use proper encapsulation with private data
- Follow single responsibility principle
- Write clear, self-documenting code

**2. Resource Management**
- Use RAII in C++ and try-with-resources in Java
- Properly handle object lifecycle
- Avoid memory leaks and resource leaks

**3. Error Handling**
- Use exceptions for exceptional conditions
- Provide meaningful error messages
- Handle errors at appropriate levels

---

**End of Programming Concepts Guide**

This comprehensive guide covers all essential programming concepts in Java, C, and C++ from basic control structures to advanced topics like generic programming and exception handling. Practice implementing these concepts and focus on understanding the underlying principles rather than just memorizing syntax.