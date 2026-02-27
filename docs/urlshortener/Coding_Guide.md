# URL Shortener - Complete Coding Guide

## System Design Overview

**Problem**: Convert long URLs to short URLs (like bit.ly)

**Requirements**:
1. Shorten URL → Generate unique short code
2. Redirect → Lookup original URL
3. Analytics → Track clicks

## SOLID Principles

- **SRP**: URLShortener only generates codes, Repository only stores data
- **OCP**: Add new encoding algorithms without modifying existing code
- **DIP**: Depend on Repository interface, not concrete implementation

## Design Patterns

1. **Strategy Pattern**: Different encoding strategies (Base62, MD5)
2. **Repository Pattern**: Abstract data access

## Complete Implementation

```java
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

class URLMapping {
    String shortCode, longUrl;
    int clicks = 0;
    
    URLMapping(String shortCode, String longUrl) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
    }
}

interface EncodingStrategy {
    String encode(long id);
}

class Base62Encoder implements EncodingStrategy {
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    public String encode(long id) {
        if (id == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(CHARS.charAt((int)(id % 62)));
            id /= 62;
        }
        return sb.reverse().toString();
    }
}

interface URLRepository {
    void save(URLMapping mapping);
    URLMapping findByShortCode(String code);
    URLMapping findByLongUrl(String url);
}

class InMemoryRepository implements URLRepository {
    private Map<String, URLMapping> byCode = new HashMap<>();
    private Map<String, URLMapping> byUrl = new HashMap<>();
    
    public void save(URLMapping m) {
        byCode.put(m.shortCode, m);
        byUrl.put(m.longUrl, m);
    }
    
    public URLMapping findByShortCode(String code) { return byCode.get(code); }
    public URLMapping findByLongUrl(String url) { return byUrl.get(url); }
}

class URLShortener {
    private URLRepository repo;
    private EncodingStrategy encoder;
    private AtomicLong counter = new AtomicLong(1000);
    
    URLShortener(URLRepository repo, EncodingStrategy encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }
    
    public String shorten(String longUrl) {
        URLMapping existing = repo.findByLongUrl(longUrl);
        if (existing != null) return existing.shortCode;
        
        String code = encoder.encode(counter.getAndIncrement());
        repo.save(new URLMapping(code, longUrl));
        System.out.println("Shortened: " + longUrl + " -> " + code);
        return code;
    }
    
    public String expand(String code) {
        URLMapping m = repo.findByShortCode(code);
        if (m == null) throw new IllegalArgumentException("Not found");
        m.clicks++;
        System.out.println("Redirect: " + code + " -> " + m.longUrl + " (clicks: " + m.clicks + ")");
        return m.longUrl;
    }
}

public class URLShortenerDemo {
    public static void main(String[] args) {
        System.out.println("=== URL Shortener ===\n");
        
        URLShortener service = new URLShortener(new InMemoryRepository(), new Base62Encoder());
        
        String s1 = service.shorten("https://example.com/very/long/url");
        String s2 = service.shorten("https://google.com/search?q=java");
        String s3 = service.shorten("https://example.com/very/long/url"); // Duplicate
        
        System.out.println("\nSame URL returns same code: " + s1.equals(s3));
        
        System.out.println();
        service.expand(s1);
        service.expand(s1);
        service.expand(s2);
    }
}
```

## Key Concepts

**Base62 Encoding**: 0-9, a-z, A-Z = 62 chars
- 6 chars = 62^6 = 56 billion URLs
- No collisions with sequential IDs

**Scalability**:
- Distributed ID: Use Snowflake algorithm
- Cache: Redis for hot URLs
- Sharding: Hash(shortCode) % N

## Interview Questions

**Q: Handle collisions?**
A: Sequential IDs + Base62 = no collisions

**Q: Custom short codes?**
A: Check availability, reserve in DB

**Q: Scale to billions?**
A: DB sharding, Redis cache, CDN redirects

Run: https://www.jdoodle.com/online-java-compiler
