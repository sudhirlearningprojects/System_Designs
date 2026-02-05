# Spring Security - Authentication & Authorization

[← Back to Index](README.md) | [← Previous: Spring WebFlux](07_Spring_WebFlux.md) | [Next: Spring Cloud →](09_Spring_Cloud.md)

## Table of Contents
- [Theory: Understanding Security](#theory-understanding-security)
- [Basic Authentication](#basic-authentication)
- [JWT Authentication](#jwt-authentication)
- [Method Security](#method-security)

---

## Theory: Understanding Security

### Core Security Concepts

**1. Authentication (Who are you?)**
- Verifying user identity
- Username/password, tokens, certificates
- Answers: "Are you who you claim to be?"

**2. Authorization (What can you do?)**
- Verifying user permissions
- Roles, authorities, access control
- Answers: "Are you allowed to do this?"

**3. Principal**
- Currently authenticated user
- Contains user details and authorities

**4. Granted Authority**
- Permission or role
- Examples: ROLE_ADMIN, READ_PRIVILEGE

### Spring Security Architecture

```
HTTP Request
    ↓
Security Filter Chain
    ↓
1. Authentication Filter (extract credentials)
    ↓
2. Authentication Manager (validate credentials)
    ↓
3. Authentication Provider (check against user store)
    ↓
4. UserDetailsService (load user from database)
    ↓
5. SecurityContext (store authenticated user)
    ↓
6. Authorization Filter (check permissions)
    ↓
Controller
```

### Authentication Mechanisms

**1. Basic Authentication**
- Credentials in HTTP header: `Authorization: Basic base64(username:password)`
- Simple but insecure (credentials in every request)
- Use with HTTPS only

**2. Form-Based Authentication**
- HTML form with username/password
- Session-based (cookie stores session ID)
- Traditional web applications

**3. JWT (JSON Web Token)**
- Stateless authentication
- Token contains user info and signature
- No server-side session needed
- Ideal for microservices and SPAs

**4. OAuth2**
- Delegated authorization
- Third-party login (Google, Facebook)
- Access tokens with limited scope

### JWT Structure

```
Header.Payload.Signature

Header: {"alg": "HS256", "typ": "JWT"}
Payload: {"sub": "user123", "exp": 1234567890}
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

**Benefits**:
- ✅ Stateless (no server-side session)
- ✅ Scalable (works across multiple servers)
- ✅ Mobile-friendly
- ✅ Cross-domain (CORS-friendly)

**Drawbacks**:
- ❌ Cannot revoke before expiry
- ❌ Token size larger than session ID
- ❌ Secret key management critical

### Session vs Token Authentication

| Aspect | Session-Based | Token-Based (JWT) |
|--------|--------------|------------------|
| Storage | Server (memory/DB) | Client (localStorage) |
| Scalability | Requires sticky sessions | Stateless, scales easily |
| Mobile | Cookies don't work well | Perfect for mobile apps |
| Revocation | Easy (delete session) | Hard (wait for expiry) |
| Size | Small session ID | Larger token |
| CSRF | Vulnerable | Not vulnerable |

### Security Best Practices

✅ **DO**:
- Use HTTPS always
- Hash passwords (BCrypt, Argon2)
- Implement rate limiting
- Use short token expiry
- Validate all inputs
- Implement CSRF protection
- Use secure headers (HSTS, CSP)

❌ **DON'T**:
- Store passwords in plain text
- Use weak hashing (MD5, SHA1)
- Expose sensitive data in tokens
- Trust client-side validation
- Use default credentials
- Ignore security updates

---

## Basic Authentication

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(user);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## JWT Authentication

```java
@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secret;
    
    public String generateToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String token = getJwtFromRequest(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

---

## Method Security

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig { }

@Service
public class OrderService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long id) {
        // Only admins can delete
    }
    
    @PreAuthorize("hasRole('USER') and #username == authentication.name")
    public Order getOrder(String username, Long orderId) {
        // Users can only access their own orders
    }
    
    @PostAuthorize("returnObject.owner == authentication.name")
    public Order findOrder(Long id) {
        // Check after method execution
    }
}
```

---

[← Previous: Spring WebFlux](07_Spring_WebFlux.md) | [Next: Spring Cloud →](09_Spring_Cloud.md)
