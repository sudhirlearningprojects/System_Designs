package org.sudhir512kj.ratelimiter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    int requests() default 100;
    int window() default 3600;
    Algorithm algorithm() default Algorithm.SLIDING_WINDOW;
    Scope scope() default Scope.IP;
    int burstCapacity() default 0;
    double refillRate() default 1.0;
    String key() default "";
    String message() default "Rate limit exceeded";
    int priority() default 0;
    boolean enabled() default true;
    
    enum Algorithm {
        SLIDING_WINDOW, TOKEN_BUCKET, FIXED_WINDOW, LEAKY_BUCKET
    }
    
    enum Scope {
        USER, IP, API_KEY, TENANT, GLOBAL, CUSTOM
    }
}