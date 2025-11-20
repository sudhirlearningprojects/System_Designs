package org.sudhir512kj.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.sudhir512kj.redis.service.RedisService;

@Configuration
@ComponentScan(basePackages = "org.sudhir512kj.redis")
@ConditionalOnProperty(name = "redis-clone.enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class RedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate redisTemplate(RedisService redisService) {
        return new RedisTemplate(redisService);
    }
}