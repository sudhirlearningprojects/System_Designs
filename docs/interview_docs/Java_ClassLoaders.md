# Java ClassLoaders - Interview Questions

## For 5-6 Years Experienced Software Engineers

---

## Table of Contents
1. [ClassLoader Basics](#classloader-basics)
2. [ClassLoader Hierarchy](#classloader-hierarchy)
3. [Parent Delegation Model](#parent-delegation-model)
4. [Custom ClassLoaders](#custom-classloaders)
5. [Class Loading Methods](#class-loading-methods)
6. [ClassLoader Isolation](#classloader-isolation)
7. [Context ClassLoader](#context-classloader)
8. [Advanced Scenarios](#advanced-scenarios)

---

## ClassLoader Basics

### Q1: Get ClassLoader Information

```java
public class Test {
    public static void main(String[] args) {
        // Application ClassLoader
        ClassLoader appLoader = Test.class.getClassLoader();
        System.out.println("App: " + appLoader.getClass().getSimpleName());
        
        // Extension ClassLoader
        ClassLoader extLoader = appLoader.getParent();
        System.out.println("Ext: " + extLoader.getClass().getSimpleName());
        
        // Bootstrap ClassLoader
        ClassLoader bootLoader = extLoader.getParent();
        System.out.println("Boot: " + bootLoader);
    }
}
```

**Output:**
```
App: AppClassLoader
Ext: ExtClassLoader
Boot: null
```

**Explanation:**
- Application ClassLoader loads user classes
- Extension ClassLoader loads extension classes
- Bootstrap ClassLoader is native (C/C++) → returns null
- Hierarchy: Bootstrap → Extension → Application

---

### Q2: Core Java Class ClassLoader

```java
public class Test {
    public static void main(String[] args) {
        System.out.println("String: " + String.class.getClassLoader());
        System.out.println("ArrayList: " + java.util.ArrayList.class.getClassLoader());
        System.out.println("HashMap: " + java.util.HashMap.class.getClassLoader());
        System.out.println("Test: " + Test.class.getClassLoader());
    }
}
```

**Output:**
```
String: null
ArrayList: null
HashMap: null
Test: sun.misc.Launcher$AppClassLoader@18b4aac2
```

**Explanation:**
- Core Java classes (java.lang.*, java.util.*) loaded by Bootstrap
- Bootstrap ClassLoader represented as null
- User classes loaded by Application ClassLoader
- Bootstrap is written in native code

---

### Q3: ClassLoader Name and Type

```java
public class Test {
    public static void main(String[] args) {
        ClassLoader loader = Test.class.getClassLoader();
        
        System.out.println("Name: " + loader.getName());
        System.out.println("Class: " + loader.getClass().getName());
        System.out.println("String: " + loader.toString());
        
        // Check type
        if (loader instanceof java.net.URLClassLoader) {
            System.out.println("Is URLClassLoader");
        }
    }
}
```

**Output:**
```
Name: app
Class: jdk.internal.loader.ClassLoaders$AppClassLoader
String: jdk.internal.loader.ClassLoaders$AppClassLoader@<hashcode>
Is URLClassLoader
```

**Explanation:**
- Application ClassLoader name is "app"
- Extends URLClassLoader (loads from URLs)
- Different implementations in Java 8 vs Java 9+
- Java 9+ uses module system

---

## ClassLoader Hierarchy

### Q4: ClassLoader Chain

```java
public class Test {
    public static void main(String[] args) {
        ClassLoader loader = Test.class.getClassLoader();
        int level = 1;
        
        while (loader != null) {
            System.out.println("Level " + level + ": " + loader.getClass().getSimpleName());
            loader = loader.getParent();
            level++;
        }
        
        System.out.println("Level " + level + ": Bootstrap (null)");
    }
}
```

**Output:**
```
Level 1: AppClassLoader
Level 2: PlatformClassLoader
Level 3: Bootstrap (null)
```

**Explanation:**
- Traverses ClassLoader hierarchy using getParent()
- Chain ends at Bootstrap (null)
- Java 9+ has PlatformClassLoader instead of ExtClassLoader
- Each level delegates to parent first

---

### Q5: Different Classes, Different Loaders

```java
public class Test {
    public static void main(String[] args) {
        // Core class
        ClassLoader stringLoader = String.class.getClassLoader();
        
        // Extension class (Java 8)
        ClassLoader cryptoLoader = javax.crypto.Cipher.class.getClassLoader();
        
        // Application class
        ClassLoader testLoader = Test.class.getClassLoader();
        
        System.out.println("String: " + stringLoader);
        System.out.println("Cipher: " + cryptoLoader);
        System.out.println("Test: " + testLoader);
        
        System.out.println("Same? " + (testLoader == cryptoLoader));
    }
}
```

**Output:**
```
String: null
Cipher: sun.misc.Launcher$ExtClassLoader@<hashcode>
Test: sun.misc.Launcher$AppClassLoader@<hashcode>
Same? false
```

**Explanation:**
- Different classes loaded by different ClassLoaders
- Bootstrap loads core Java classes
- Extension loads javax.* packages
- Application loads user classes
- Each ClassLoader has specific responsibility

---

## Parent Delegation Model

### Q6: Delegation in Action

```java
class MyClass {
    static {
        System.out.println("MyClass static block");
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("Loading MyClass...");
        
        ClassLoader loader = Test.class.getClassLoader();
        Class<?> clazz = loader.loadClass("MyClass");
        
        System.out.println("Loaded by: " + clazz.getClassLoader());
        System.out.println("Creating instance...");
        
        Object obj = clazz.getDeclaredConstructor().newInstance();
    }
}
```

**Output:**
```
Loading MyClass...
Loaded by: sun.misc.Launcher$AppClassLoader@<hashcode>
Creating instance...
MyClass static block
```

**Explanation:**
- loadClass() doesn't initialize class (no static block)
- Static block runs only when instance created
- Parent delegation: Bootstrap → Extension → Application
- Application ClassLoader finds and loads MyClass

---

### Q7: Class Already Loaded

```java
public class Test {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = Test.class.getClassLoader();
        
        // Load String class
        Class<?> stringClass1 = loader.loadClass("java.lang.String");
        System.out.println("First load: " + stringClass1.getClassLoader());
        
        // Load again
        Class<?> stringClass2 = loader.loadClass("java.lang.String");
        System.out.println("Second load: " + stringClass2.getClassLoader());
        
        // Same instance?
        System.out.println("Same class? " + (stringClass1 == stringClass2));
    }
}
```

**Output:**
```
First load: null
Second load: null
Same class? true
```

**Explanation:**
- String loaded by Bootstrap ClassLoader
- ClassLoader caches loaded classes
- Second load returns cached instance
- Same Class object reference
- Improves performance, prevents duplicate loading

---

### Q8: Prevent Core Class Override

```java
// Attempt to create fake String class (won't work)
package java.lang;

public class String {
    static {
        System.out.println("Fake String loaded!");
    }
}

// Test class
public class Test {
    public static void main(String[] args) {
        String str = "Hello";
        System.out.println(str.getClass().getClassLoader());
    }
}
```

**Output:**
```
null
(Fake String never loaded)
```

**Explanation:**
- Bootstrap ClassLoader loads java.lang.String first
- Parent delegation prevents fake String from loading
- Security feature: core classes can't be replaced
- Application ClassLoader never gets chance to load fake String
- Protects JVM integrity

---

## Custom ClassLoaders

### Q9: Simple Custom ClassLoader

```java
class CustomLoader extends ClassLoader {
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("CustomLoader finding: " + name);
        return super.findClass(name);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        System.out.println("CustomLoader loading: " + name);
        return super.loadClass(name);
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        CustomLoader loader = new CustomLoader();
        Class<?> clazz = loader.loadClass("java.lang.String");
        System.out.println("Loaded: " + clazz.getName());
    }
}
```

**Output:**
```
CustomLoader loading: java.lang.String
Loaded: java.lang.String
```

**Explanation:**
- Custom ClassLoader extends ClassLoader
- loadClass() called first (delegation)
- findClass() not called (parent found it)
- Bootstrap ClassLoader loads String
- Custom loader respects parent delegation

---

### Q10: Custom ClassLoader with findClass

```java
class CustomLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println("findClass called for: " + name);
        
        if (name.equals("MyCustomClass")) {
            byte[] classData = new byte[]{/* bytecode */};
            return defineClass(name, classData, 0, classData.length);
        }
        
        throw new ClassNotFoundException(name);
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        CustomLoader loader = new CustomLoader();
        
        try {
            Class<?> clazz = loader.loadClass("MyCustomClass");
            System.out.println("Loaded: " + clazz.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("Not found: " + e.getMessage());
        }
    }
}
```

**Output:**
```
findClass called for: MyCustomClass
Not found: MyCustomClass
```

**Explanation:**
- findClass() called when parent can't find class
- defineClass() converts byte[] to Class object
- In this example, no actual bytecode provided
- Real implementation would read .class file
- Used for loading from custom sources

---

## Class Loading Methods

### Q11: Class.forName() vs loadClass()

```java
class InitTest {
    static {
        System.out.println("InitTest static block executed");
    }
    
    static int value = 100;
}

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Using Class.forName() ===");
        Class<?> clazz1 = Class.forName("InitTest");
        System.out.println("Class loaded");
        
        System.out.println("\n=== Using loadClass() ===");
        ClassLoader loader = Test.class.getClassLoader();
        Class<?> clazz2 = loader.loadClass("InitTest");
        System.out.println("Class loaded");
        
        System.out.println("\n=== Accessing static field ===");
        System.out.println("Value: " + InitTest.value);
    }
}
```

**Output:**
```
=== Using Class.forName() ===
InitTest static block executed
Class loaded

=== Using loadClass() ===
Class loaded

=== Accessing static field ===
Value: 100
```

**Explanation:**
- Class.forName() initializes class (runs static blocks)
- loadClass() only loads, doesn't initialize
- Static block runs on first access after loadClass()
- forName() used for JDBC drivers
- loadClass() used for lazy initialization

---

### Q12: Class.forName() with Initialize Parameter

```java
class TestClass {
    static {
        System.out.println("TestClass initialized");
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Initialize = true ===");
        Class<?> clazz1 = Class.forName("TestClass", true, 
                                        Test.class.getClassLoader());
        
        System.out.println("\n=== Initialize = false ===");
        Class<?> clazz2 = Class.forName("TestClass", false, 
                                        Test.class.getClassLoader());
        System.out.println("Loaded without initialization");
    }
}
```

**Output:**
```
=== Initialize = true ===
TestClass initialized

=== Initialize = false ===
Loaded without initialization
```

**Explanation:**
- Class.forName(name, initialize, loader) controls initialization
- initialize=true runs static blocks
- initialize=false defers initialization
- Useful for reflection without side effects
- Second parameter gives fine-grained control

---

## ClassLoader Isolation

### Q13: Same Class, Different ClassLoaders

```java
class MyClass {
    static int counter = 0;
    
    public MyClass() {
        counter++;
        System.out.println("Instance created, counter: " + counter);
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        // Load with default ClassLoader
        MyClass obj1 = new MyClass();
        MyClass obj2 = new MyClass();
        
        System.out.println("Total instances: " + MyClass.counter);
        
        // Note: In real scenario, different ClassLoaders would show isolation
        System.out.println("ClassLoader: " + MyClass.class.getClassLoader());
    }
}
```

**Output:**
```
Instance created, counter: 1
Instance created, counter: 2
Total instances: 2
ClassLoader: sun.misc.Launcher$AppClassLoader@<hashcode>
```

**Explanation:**
- Same ClassLoader = shared static variables
- Different ClassLoaders = isolated static variables
- Each ClassLoader has its own namespace
- Used in web servers for application isolation
- Prevents interference between applications

---

### Q14: ClassCastException with Different Loaders

```java
public class Test {
    public static void main(String[] args) throws Exception {
        ClassLoader loader1 = Test.class.getClassLoader();
        ClassLoader loader2 = Test.class.getClassLoader();
        
        Class<?> class1 = loader1.loadClass("java.lang.String");
        Class<?> class2 = loader2.loadClass("java.lang.String");
        
        System.out.println("Same class? " + (class1 == class2));
        System.out.println("Class1 loader: " + class1.getClassLoader());
        System.out.println("Class2 loader: " + class2.getClassLoader());
        
        // Both loaded by Bootstrap, so same class
        String str = "test";
        Object obj = class1.cast(str);
        System.out.println("Cast successful: " + obj);
    }
}
```

**Output:**
```
Same class? true
Class1 loader: null
Class2 loader: null
Cast successful: test
```

**Explanation:**
- Same ClassLoader returns same Class object
- Both delegate to Bootstrap for String
- Bootstrap ensures single String class
- Cast succeeds because same class
- Different loaders for user classes would fail

---

## Context ClassLoader

### Q15: Thread Context ClassLoader

```java
public class Test {
    public static void main(String[] args) {
        Thread currentThread = Thread.currentThread();
        
        // Get context ClassLoader
        ClassLoader contextLoader = currentThread.getContextClassLoader();
        System.out.println("Context: " + contextLoader.getClass().getSimpleName());
        
        // Get class ClassLoader
        ClassLoader classLoader = Test.class.getClassLoader();
        System.out.println("Class: " + classLoader.getClass().getSimpleName());
        
        // Usually same
        System.out.println("Same? " + (contextLoader == classLoader));
        
        // Set custom context ClassLoader
        ClassLoader customLoader = new URLClassLoader(new URL[]{});
        currentThread.setContextClassLoader(customLoader);
        
        System.out.println("New context: " + 
            currentThread.getContextClassLoader().getClass().getSimpleName());
    }
}
```

**Output:**
```
Context: AppClassLoader
Class: AppClassLoader
Same? true
New context: URLClassLoader
```

**Explanation:**
- Context ClassLoader set per thread
- Default is Application ClassLoader
- Frameworks (Spring, Hibernate) use context loader
- Can be changed with setContextClassLoader()
- Useful for plugin architectures

---

## Advanced Scenarios

### Q16: Resource Loading

```java
public class Test {
    public static void main(String[] args) {
        ClassLoader loader = Test.class.getClassLoader();
        
        // Load resource
        URL resource = loader.getResource("config.properties");
        System.out.println("Resource URL: " + resource);
        
        // Load as stream
        InputStream stream = loader.getResourceAsStream("config.properties");
        System.out.println("Stream: " + (stream != null ? "Found" : "Not found"));
        
        // Get all resources with same name
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/MANIFEST.MF");
            int count = 0;
            while (resources.hasMoreElements()) {
                resources.nextElement();
                count++;
            }
            System.out.println("Found " + count + " manifests");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Output:**
```
Resource URL: null
Stream: Not found
Found 0 manifests
```

**Explanation:**
- ClassLoader can load resources (not just classes)
- getResource() returns URL
- getResourceAsStream() returns InputStream
- getResources() finds all matching resources
- Used for configuration files, properties

---

### Q17: ClassLoader Memory Leak

```java
class LeakyClass {
    private static byte[] memory = new byte[1024 * 1024]; // 1MB
    
    static {
        System.out.println("LeakyClass loaded");
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = new URLClassLoader(new URL[]{});
        
        Class<?> clazz = loader.loadClass("LeakyClass");
        System.out.println("Class loaded by: " + clazz.getClassLoader());
        
        // Even if we null the reference, ClassLoader holds class
        clazz = null;
        
        // ClassLoader not GC'd if any class reference exists
        System.out.println("Loader still referenced");
        
        // To allow GC: remove all references to classes and ClassLoader
        loader = null;
        System.gc();
        System.out.println("GC suggested");
    }
}
```

**Output:**
```
LeakyClass loaded
Class loaded by: java.net.URLClassLoader@<hashcode>
Loader still referenced
GC suggested
```

**Explanation:**
- ClassLoader holds references to all loaded classes
- Classes hold reference to ClassLoader
- Circular reference prevents GC
- Common in web servers (hot deployment)
- Must remove all references for GC
- Memory leaks if ClassLoader not released

---

### Q18: Parallel Class Loading (Java 7+)

```java
class ParallelLoader extends ClassLoader {
    static {
        registerAsParallelCapable();
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println(Thread.currentThread().getName() + " loading: " + name);
        return super.findClass(name);
    }
}

public class Test {
    public static void main(String[] args) throws Exception {
        ParallelLoader loader = new ParallelLoader();
        
        Thread t1 = new Thread(() -> {
            try {
                loader.loadClass("java.util.ArrayList");
            } catch (Exception e) {}
        });
        
        Thread t2 = new Thread(() -> {
            try {
                loader.loadClass("java.util.HashMap");
            } catch (Exception e) {}
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        System.out.println("Parallel loading completed");
    }
}
```

**Output:**
```
Thread-0 loading: java.util.ArrayList
Thread-1 loading: java.util.HashMap
Parallel loading completed
```

**Explanation:**
- registerAsParallelCapable() enables parallel loading
- Multiple threads can load different classes simultaneously
- Improves performance in multi-threaded environments
- Default in Java 7+ for built-in ClassLoaders
- Custom loaders must explicitly register

---

## Summary

### ClassLoader Hierarchy

```
Bootstrap ClassLoader (null)
    ↓ parent
Extension/Platform ClassLoader
    ↓ parent
Application/System ClassLoader
    ↓ parent
Custom ClassLoader (optional)
```

### Key Methods

| Method | Purpose | Initialization |
|--------|---------|----------------|
| `Class.forName(name)` | Load and initialize | ✅ Yes |
| `loader.loadClass(name)` | Load only | ❌ No |
| `Class.forName(name, false, loader)` | Load without init | ❌ No |
| `findClass(name)` | Find class (override) | - |
| `defineClass(...)` | Convert bytes to class | - |

### Parent Delegation Flow

```
1. Check if class already loaded
2. Delegate to parent ClassLoader
3. If parent can't find, call findClass()
4. If still not found, throw ClassNotFoundException
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| ClassNotFoundException | Class not in classpath | Check classpath |
| NoClassDefFoundError | Class found at compile, not runtime | Check dependencies |
| ClassCastException | Same class, different loaders | Use same ClassLoader |
| Memory Leak | ClassLoader not GC'd | Remove all references |

### Real-World Use Cases

1. **Web Servers**: Isolate web applications
2. **Plugin Systems**: Load plugins dynamically
3. **Hot Deployment**: Reload classes without restart
4. **OSGi**: Modular system with complex hierarchy
5. **Application Servers**: EJB container isolation

### Best Practices

1. ✅ Respect parent delegation model
2. ✅ Override findClass(), not loadClass()
3. ✅ Use Class.forName() for initialization
4. ✅ Use loadClass() for lazy loading
5. ✅ Handle ClassNotFoundException properly
6. ✅ Be aware of ClassLoader memory leaks
7. ✅ Use context ClassLoader in frameworks
8. ✅ Register parallel capability for performance

---

## Interview Tips

1. **Understand hierarchy**: Bootstrap → Extension → Application
2. **Know delegation**: Parent-first model
3. **Explain null**: Bootstrap is native, returns null
4. **Differentiate methods**: forName() vs loadClass()
5. **Recognize isolation**: Different loaders = different classes
6. **Identify leaks**: ClassLoader holds class references
7. **Context loader**: Used by frameworks
8. **Real-world examples**: Tomcat, OSGi, plugins

