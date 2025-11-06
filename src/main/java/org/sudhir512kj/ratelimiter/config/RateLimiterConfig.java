package org.sudhir512kj.ratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.sudhir512kj.ratelimiter.algorithm.*;
import org.sudhir512kj.ratelimiter.algorithm.RedisSlidingWindowAlgorithm;
import org.sudhir512kj.ratelimiter.algorithm.RedisTokenBucketAlgorithm;
import org.sudhir512kj.ratelimiter.algorithm.FixedWindowAlgorithm;
import org.sudhir512kj.ratelimiter.algorithm.LeakyBucketAlgorithm;

import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@RequiredArgsConstructor
@EnableAspectJAutoProxy
public class RateLimiterConfig {
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
    
    @Bean
    public Map<String, RateLimitAlgorithm> rateLimitAlgorithms(
            RedisSlidingWindowAlgorithm slidingWindow,
            RedisTokenBucketAlgorithm tokenBucket,
            FixedWindowAlgorithm fixedWindow,
            LeakyBucketAlgorithm leakyBucket) {
        Map<String, RateLimitAlgorithm> algorithms = new HashMap<>();
        algorithms.put("REDIS_SLIDING_WINDOW", slidingWindow);
        algorithms.put("REDIS_TOKEN_BUCKET", tokenBucket);
        algorithms.put("REDIS_FIXED_WINDOW", fixedWindow);
        algorithms.put("REDIS_LEAKY_BUCKET", leakyBucket);
        return algorithms;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    

}