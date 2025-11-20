package org.sudhir512kj.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RedisAutoConfiguration.class)
@TestPropertySource(properties = "redis-clone.enabled=true")
public class RedisTemplateTest {
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    @Test
    public void testStringOperations() {
        redisTemplate.set("test:key", "test:value");
        assertEquals("test:value", redisTemplate.get("test:key"));
        
        redisTemplate.set("test:ttl", "value", Duration.ofSeconds(1));
        assertTrue(redisTemplate.exists("test:ttl"));
        
        assertTrue(redisTemplate.delete("test:key"));
        assertFalse(redisTemplate.exists("test:key"));
    }
    
    @Test
    public void testListOperations() {
        int size = redisTemplate.leftPush("test:list", "item1", "item2");
        assertEquals(2, size);
        
        String item = redisTemplate.leftPop("test:list");
        assertEquals("item2", item);
    }
    
    @Test
    public void testSetOperations() {
        int added = redisTemplate.addToSet("test:set", "member1", "member2");
        assertEquals(2, added);
        
        assertTrue(redisTemplate.isMember("test:set", "member1"));
        assertFalse(redisTemplate.isMember("test:set", "nonexistent"));
    }
    
    @Test
    public void testHashOperations() {
        redisTemplate.hashSet("test:hash", "field1", "value1");
        assertEquals("value1", redisTemplate.hashGet("test:hash", "field1"));
        assertNull(redisTemplate.hashGet("test:hash", "nonexistent"));
    }
}