package org.sudhir512kj.ratelimiter.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.sudhir512kj.ratelimiter.annotation.RateLimit;
import org.sudhir512kj.ratelimiter.annotation.RateLimits;
import org.sudhir512kj.ratelimiter.service.AnnotationRateLimitService;
import org.sudhir512kj.ratelimiter.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {
    
    private final AnnotationRateLimitService rateLimitService;
    
    @Around("@annotation(rateLimit)")
    public Object handleMethodRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return processRateLimit(joinPoint, new RateLimit[]{rateLimit});
    }
    
    @Around("@annotation(rateLimits)")
    public Object handleMultipleRateLimits(ProceedingJoinPoint joinPoint, RateLimits rateLimits) throws Throwable {
        return processRateLimit(joinPoint, rateLimits.value());
    }
    
    @Around("@within(rateLimit)")
    public Object handleClassRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        if (method.isAnnotationPresent(RateLimit.class) || method.isAnnotationPresent(RateLimits.class)) {
            return joinPoint.proceed();
        }
        return processRateLimit(joinPoint, new RateLimit[]{rateLimit});
    }
    
    private Object processRateLimit(ProceedingJoinPoint joinPoint, RateLimit[] rateLimits) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        for (RateLimit rateLimit : rateLimits) {
            if (!rateLimit.enabled()) continue;
            
            boolean allowed = rateLimitService.checkRateLimit(request, joinPoint, rateLimit);
            if (!allowed) {
                throw new RateLimitExceededException(rateLimit.message());
            }
        }
        
        return joinPoint.proceed();
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}