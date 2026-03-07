# Spring Boot Startup Internals - Quick Reference

> **The Interview Question**: "What actually happens when you run a Spring Boot application?"
> 
> Most developers say: *"Spring Boot automatically configures everything."*  
> That's marketing, not an explanation.

---

## The 8-Step Startup Flow

### 1️⃣ **main() Method Executes**
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
**Nothing magical yet.** Just a plain Java method call.

---

### 2️⃣ **SpringApplication Prepares Environment**
Spring Boot internally:
- Determines application type (Servlet/Reactive/None)
- Creates ApplicationContext
- Loads configuration
- Applies auto-configuration
- Starts embedded server (if web app)

---

### 3️⃣ **Classpath Detection - Decides App Type**
Spring Boot scans your classpath:

| Found | Result |
|-------|--------|
| `spring-webmvc` | Servlet-based app (Tomcat) |
| `spring-webflux` | Reactive app (Netty) |
| Neither | Non-web app |

**Key Point**: This is why adding/removing dependencies changes behavior.

---

### 4️⃣ **ApplicationContext Created**
The heart of Spring:
- Manages beans
- Handles dependency injection
- Controls lifecycle
- Publishes events

**Implementation**: `AnnotationConfigServletWebServerApplicationContext` (for MVC apps)

---

### 5️⃣ **Auto-Configuration Kicks In**

**How it works**:
```java
@ConditionalOnClass(DataSource.class)
@ConditionalOnMissingBean(DataSource.class)
public class DataSourceAutoConfiguration {
    // Creates default datasource
}
```

**Translation**:
- IF `DataSource` exists on classpath
- AND you didn't define your own bean
- THEN Boot creates one for you

**Not magic. Conditional logic.**

**Mechanism**:
- `@EnableAutoConfiguration`
- `spring.factories` (older) / `AutoConfiguration.imports` (newer)
- Conditional annotations: `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`

---

### 6️⃣ **Component Scanning**

`@SpringBootApplication` = 3 annotations:
```java
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan  // Scans current package + subpackages
```

**Common Mistake**: Moving main class to random package breaks component scanning.

---

### 7️⃣ **Beans Created and Wired**
Spring now:
1. Instantiates beans
2. Resolves dependencies (follows dependency graph)
3. Applies proxies (AOP, transactions)
4. Executes `@PostConstruct`

**Watch out**: Circular dependencies cause startup failure.

---

### 8️⃣ **Embedded Server Starts**
For web apps, Boot starts:
- **Tomcat** (default)
- Jetty
- Undertow

**Why `java -jar app.jar` works**: Server is embedded, no external WAR needed.

---

## Why Developers Fail This Question

They use Spring Boot like:
```
Add dependency → Add annotation → It works → Ship it
```

They never:
- ❌ Read auto-configuration classes
- ❌ Debug startup logs deeply
- ❌ Explore `ConditionEvaluationReport`
- ❌ Understand lifecycle events

**Framework comfort becomes framework blindness.**

---

## How to Master This (Actionable)

### 1. **Turn On Debug Logs**
```bash
java -jar app.jar --debug
```
Spring Boot prints full auto-configuration report:
- What got applied
- What got skipped
- **Why**

### 2. **Read Auto-Configuration Classes**
Open: `org.springframework.boot.autoconfigure`

Pick any class. Read the conditions. You'll see Boot is just structured logic.

### 3. **Break It On Purpose**
```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```
Remove dependencies. See what fails. **Controlled failure teaches more than tutorials.**

### 4. **Explain It Out Loud**
Practice explaining in 2-3 minutes:
1. Classpath detection
2. Context creation
3. Auto-configuration
4. Bean lifecycle
5. Embedded server startup

**If you can't explain it clearly, you don't own it yet.**

---

## Interview-Ready Summary

**Question**: "What happens when you run a Spring Boot application?"

**Answer**:
> "When `SpringApplication.run()` executes, Spring Boot:
> 
> 1. Detects application type by scanning classpath (Servlet/Reactive/None)
> 2. Creates an `ApplicationContext` to manage beans and lifecycle
> 3. Applies auto-configuration using conditional logic (`@ConditionalOnClass`, `@ConditionalOnMissingBean`)
> 4. Scans components from the main class package and subpackages
> 5. Instantiates beans, resolves dependencies, and applies proxies
> 6. Starts the embedded server (Tomcat by default) if it's a web app
> 
> Auto-configuration isn't magic—it's conditional bean creation based on classpath and existing beans. You can see exactly what was applied using `--debug` flag."

---

## Quick Debugging Commands

```bash
# See auto-configuration report
java -jar app.jar --debug

# See all beans
java -jar app.jar --debug | grep "Positive matches"

# Exclude specific auto-configuration
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
```

---

## Common Interview Follow-ups

### Q: "How does Spring Boot know which beans to create?"
**A**: Conditional annotations on auto-configuration classes check classpath and existing beans.

### Q: "What if I want to disable auto-configuration?"
**A**: Use `exclude` in `@SpringBootApplication` or `spring.autoconfigure.exclude` property.

### Q: "How do you debug startup issues?"
**A**: Enable debug logs (`--debug`), check `ConditionEvaluationReport`, verify component scan package.

### Q: "What's the difference between @SpringBootApplication and @Configuration?"
**A**: `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`

### Q: "Can you customize the embedded server?"
**A**: Yes, via `application.properties` (`server.port`, `server.tomcat.*`) or by defining `WebServerFactoryCustomizer` bean.

---

## The Reality Check

**Framework User**: Knows what to type  
**Framework Engineer**: Knows what happens next

**Which one are you?**

---

## Key Takeaways

✅ Spring Boot startup is **predictable**, not magical  
✅ Auto-configuration = **conditional bean creation**  
✅ Understanding lifecycle makes **debugging easier**  
✅ Read the source code in `spring-boot-autoconfigure`  
✅ Use `--debug` flag to see what's happening  

**Knowing annotations ≠ Expertise**  
**Understanding lifecycle = Expertise**

---

**Pro Tip**: Next time you add a Spring Boot starter, open its auto-configuration class and read it. You'll learn more in 10 minutes than from hours of tutorials.
