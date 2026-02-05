# Spring AOP - Aspect-Oriented Programming

[← Back to Index](README.md) | [← Previous: Spring Kafka](10_Spring_Kafka.md) | [Next: Spring Batch Part 1 →](12_Spring_Batch_Part1.md)

## Table of Contents
- [Advice Types](#advice-types)
- [Pointcut Expressions](#pointcut-expressions)
- [Custom Annotations](#custom-annotations)

---

## Advice Types

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("Before: " + joinPoint.getSignature().getName());
    }
    
    @After("execution(* com.example.service.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        System.out.println("After: " + joinPoint.getSignature().getName());
    }
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        System.out.println(joinPoint.getSignature().getName() + " took " + duration + "ms");
        return result;
    }
    
    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        System.out.println("Returned: " + result);
    }
    
    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        System.out.println("Exception: " + error.getMessage());
    }
}
```

---

## Pointcut Expressions

```java
@Aspect
@Component
public class PointcutExamples {
    
    // All methods in service package
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceMethods() {}
    
    // All public methods
    @Pointcut("execution(public * *(..))")
    public void publicMethods() {}
    
    // Methods with specific annotation
    @Pointcut("@annotation(com.example.Timed)")
    public void timedMethods() {}
    
    // Combine pointcuts
    @Around("serviceMethods() && publicMethods()")
    public Object aroundServicePublicMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
```

---

## Custom Annotations

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
}

@Aspect
@Component
public class TimingAspect {
    
    @Around("@annotation(Timed)")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        
        System.out.println(joinPoint.getSignature().getName() + " executed in " + duration + "ms");
        return result;
    }
}

@Service
public class UserService {
    
    @Timed
    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

---

[← Previous: Spring Kafka](10_Spring_Kafka.md) | [Next: Spring Batch Part 1 →](12_Spring_Batch_Part1.md)
