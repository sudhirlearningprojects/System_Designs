# Module 5: Session Management

## 🎯 Learning Objectives

- Configure Spring Session with Redis
- Store and manage user sessions across multiple app instances
- Implement session-based authentication
- Handle session expiry and cleanup
- Understand session security best practices

---

## 5.1 Configuration

```java
// src/main/java/com/example/redis/config/SessionConfig.java

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // Use JSON so sessions are readable in RedisInsight
        return new GenericJackson2JsonRedisSerializer();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION_ID");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(true);   // HTTPS only in production
        serializer.setSameSite("Lax");
        return serializer;
    }
}
```

```yaml
# application.yml
spring:
  session:
    store-type: redis
    timeout: 30m
    redis:
      namespace: ecom:sessions
      flush-mode: on-save  # IMMEDIATE or ON_SAVE
```

---

## 5.2 Session-Based Auth Controller

```java
// src/main/java/com/example/redis/auth/controller/AuthController.java

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        User user = userService.authenticate(
            request.getEmail(), request.getPassword()
        );

        // Store user info in session (stored in Redis)
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", user.getEmail());
        session.setAttribute("roles", user.getRoles());
        session.setAttribute("loginTime", Instant.now().toString());

        return ResponseEntity.ok(Map.of(
            "message", "Login successful",
            "sessionId", session.getId()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();  // Removes from Redis
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "email", session.getAttribute("email"),
            "roles", session.getAttribute("roles")
        ));
    }
}
```

---

## 5.3 What Redis Stores (Session Internals)

```
# Key pattern: spring:session:sessions:{sessionId}

HGETALL spring:session:sessions:abc123-def456

# Fields stored:
creationTime          → "1705312800000"
lastAccessedTime      → "1705314600000"
maxInactiveInterval   → "1800"
sessionAttr:userId    → "user-500"
sessionAttr:email     → "john@example.com"
sessionAttr:roles     → ["ROLE_USER", "ROLE_ADMIN"]
sessionAttr:loginTime → "2025-01-15T10:00:00Z"

# Expiry index (for cleanup):
ZADD spring:session:expirations:{timestamp} 0 "abc123-def456"
```

---

## 5.4 Session Security

```java
// Session fixation protection + concurrent session control

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()  // New session ID after login
                .maximumSessions(2)                  // Max 2 concurrent sessions
                .maxSessionsPreventsLogin(false)     // Kick old session
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    // Expire sessions from other devices
    @Bean
    public FindByIndexNameSessionRepository<?> sessionRepository() {
        // Allows finding sessions by username (for admin/force-logout)
        return new RedisIndexedSessionRepository(/* ... */);
    }
}
```

```java
// Force logout user from all devices
@Service
@RequiredArgsConstructor
public class SessionManagementService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepo;

    public void forceLogoutUser(String username) {
        Map<String, ? extends Session> sessions =
            sessionRepo.findByPrincipalName(username);

        sessions.keySet().forEach(sessionRepo::deleteById);
    }

    public int getActiveSessionCount(String username) {
        return sessionRepo.findByPrincipalName(username).size();
    }
}
```

---

## 5.5 Limitations of Redis Sessions

| Limitation | Impact | Mitigation |
|-----------|--------|------------|
| Redis goes down | All sessions lost | Sentinel/Cluster for HA |
| Large session data | Slow serialization | Keep sessions lean (<5KB) |
| No lazy loading | Entire hash loaded on each request | Store only IDs, not objects |
| Cross-region latency | Slow if Redis is remote | Regional Redis replicas |
| Session bloat | Memory waste | Regular cleanup, short TTL |

**Best Practice**: Store only user ID and roles in session. Load full user data from DB/cache separately.

---

## ⏭️ Next Module

Proceed to **[Module 6: Pub/Sub & Messaging](06_PubSub_Messaging.md)** for real-time notifications.
