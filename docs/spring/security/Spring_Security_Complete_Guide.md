# Spring Security - Complete Deep Dive Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [Authentication](#authentication)
4. [Authorization](#authorization)
5. [JWT Implementation](#jwt-implementation)
6. [OAuth2 & OpenID Connect](#oauth2--openid-connect)
7. [Method Security](#method-security)
8. [Security Testing](#security-testing)
9. [Interview Questions](#interview-questions)
10. [Best Practices](#best-practices)

## Introduction

Spring Security is a comprehensive security framework providing authentication, authorization, and protection against common attacks for Spring applications.

### Core Features
- **Authentication**: Who you are
- **Authorization**: What you can do
- **Protection**: CSRF, Session Fixation, Clickjacking
- **Integration**: OAuth2, SAML, LDAP, JWT

## Architecture

### Security Filter Chain

```
HTTP Request → Security Filter Chain → Controller
    ↓
┌─────────────────────────────────────────────────┐
│ SecurityContextPersistenceFilter                │
│ LogoutFilter                                    │
│ UsernamePasswordAuthenticationFilter            │
│ BasicAuthenticationFilter                       │
│ RequestCacheAwareFilter                         │
│ SecurityContextHolderAwareRequestFilter         │
│ AnonymousAuthenticationFilter                   │
│ SessionManagementFilter                         │
│ ExceptionTranslationFilter                      │
│ FilterSecurityInterceptor                       │
└─────────────────────────────────────────────────┘
```

### Core Components

```java
// SecurityContext - holds authentication info
SecurityContext context = SecurityContextHolder.getContext();
Authentication auth = context.getAuthentication();

// Authentication - represents user credentials
public interface Authentication extends Principal {
    Collection<? extends GrantedAuthority> getAuthorities();
    Object getCredentials();
    Object getDetails();
    Object getPrincipal();
    boolean isAuthenticated();
}

// UserDetails - core user information
public interface UserDetails {
    Collection<? extends GrantedAuthority> getAuthorities();
    String getPassword();
    String getUsername();
    boolean isAccountNonExpired();
    boolean isAccountNonLocked();
    boolean isCredentialsNonExpired();
    boolean isEnabled();
}
```

## Authentication

### Basic Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Custom UserDetailsService

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return UserPrincipal.create(user);
    }
}

public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
        
        return new UserPrincipal(user.getId(), user.getUsername(), 
            user.getPassword(), authorities);
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return true; }
}
```

### Custom Authentication Provider

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {
        
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        
        UserDetails user = userDetailsService.loadUserByUsername(username);
        
        if (passwordEncoder.matches(password, user.getPassword())) {
            return new UsernamePasswordAuthenticationToken(
                user, password, user.getAuthorities());
        } else {
            throw new BadCredentialsException("Invalid credentials");
        }
    }
    
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
```

## Authorization

### Method-Level Security

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = 
            new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return handler;
    }
}

@Service
public class DocumentService {
    
    @PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#id, authentication.name)")
    public Document getDocument(Long id) {
        return documentRepository.findById(id);
    }
    
    @PostAuthorize("hasPermission(returnObject, 'READ')")
    public Document findDocument(Long id) {
        return documentRepository.findById(id);
    }
    
    @PreFilter("hasPermission(filterObject, 'READ')")
    public List<Document> getDocuments(List<Document> documents) {
        return documents;
    }
    
    public boolean isOwner(Long documentId, String username) {
        Document doc = documentRepository.findById(documentId);
        return doc != null && doc.getOwner().equals(username);
    }
}
```

### Custom Permission Evaluator

```java
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, 
                               Object permission) {
        if (auth == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }
        
        String targetType = targetDomainObject.getClass().getSimpleName().toUpperCase();
        return hasPrivilege(auth, targetType, permission.toString());
    }
    
    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, 
                               String targetType, Object permission) {
        if (auth == null || targetType == null || !(permission instanceof String)) {
            return false;
        }
        
        return hasPrivilege(auth, targetType.toUpperCase(), permission.toString());
    }
    
    private boolean hasPrivilege(Authentication auth, String targetType, String permission) {
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            if (grantedAuth.getAuthority().startsWith(targetType + "_" + permission)) {
                return true;
            }
        }
        return false;
    }
}
```

## JWT Implementation

### JWT Configuration

```java
@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpiration;
    
    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
}

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private int jwtExpiration;
    
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(Long.toString(userPrincipal.getId()))
            .setIssuedAt(new Date())
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        
        return Long.parseLong(claims.getSubject());
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### JWT Filter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String jwt = getJwtFromRequest(request);
        
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Long userId = tokenProvider.getUserIdFromToken(jwt);
            UserDetails userDetails = customUserDetailsService.loadUserById(userId);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, 
                    userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### Authentication Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);
        
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Long userId = tokenProvider.getUserIdFromToken(jwt);
            UserDetails userDetails = customUserDetailsService.loadUserById(userId);
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            
            String newToken = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new JwtAuthenticationResponse(newToken));
        }
        
        return ResponseEntity.badRequest().body("Invalid token");
    }
}
```

## OAuth2 & OpenID Connect

### OAuth2 Client Configuration

```java
@Configuration
@EnableOAuth2Client
public class OAuth2Config {
    
    @Bean
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/login**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
                .successHandler(oauth2AuthenticationSuccessHandler())
                .failureHandler(oauth2AuthenticationFailureHandler())
            );
        
        return http.build();
    }
    
    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }
}

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }
    
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        OAuth2UserInfo oauth2UserInfo = OAuth2UserInfoFactory
            .getOAuth2UserInfo(userRequest.getClientRegistration().getRegistrationId(), 
                              oauth2User.getAttributes());
        
        Optional<User> userOptional = userRepository.findByEmail(oauth2UserInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oauth2UserInfo);
        } else {
            user = registerNewUser(userRequest, oauth2UserInfo);
        }
        
        return UserPrincipal.create(user, oauth2User.getAttributes());
    }
}
```

### Resource Server Configuration

```java
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation("https://your-auth-server.com");
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = 
            new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");
        
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

## Method Security

### Advanced Method Security

```java
@Service
public class BankingService {
    
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #accountId == authentication.principal.accountId)")
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId);
    }
    
    @PreAuthorize("@bankingService.canTransfer(#fromAccount, #toAccount, #amount, authentication)")
    public void transfer(Long fromAccount, Long toAccount, BigDecimal amount) {
        // Transfer logic
    }
    
    @PostFilter("hasPermission(filterObject, 'READ')")
    public List<Transaction> getTransactions() {
        return transactionRepository.findAll();
    }
    
    @Secured({"ROLE_ADMIN", "ROLE_MANAGER"})
    public void generateReport() {
        // Report generation
    }
    
    @RolesAllowed("ADMIN")
    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
    }
    
    public boolean canTransfer(Long fromAccount, Long toAccount, 
                              BigDecimal amount, Authentication auth) {
        // Custom business logic for transfer authorization
        Account from = accountRepository.findById(fromAccount);
        return from.getBalance().compareTo(amount) >= 0 && 
               from.getOwner().equals(auth.getName());
    }
}
```

### Security Expressions

```java
@RestController
public class DocumentController {
    
    // SpEL expressions
    @PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#id, authentication.name)")
    @GetMapping("/documents/{id}")
    public Document getDocument(@PathVariable Long id) {
        return documentService.getDocument(id);
    }
    
    // Complex expressions
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #document.owner == authentication.name)")
    @PostMapping("/documents")
    public Document createDocument(@RequestBody Document document) {
        return documentService.save(document);
    }
    
    // Method-level filtering
    @PostFilter("hasRole('ADMIN') or filterObject.owner == authentication.name")
    @GetMapping("/documents")
    public List<Document> getAllDocuments() {
        return documentService.findAll();
    }
}
```

## Security Testing

### Test Configuration

```java
@TestConfiguration
public class TestSecurityConfig {
    
    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password("{noop}admin")
            .roles("ADMIN")
            .build();
            
        UserDetails user = User.builder()
            .username("user")
            .password("{noop}user")
            .roles("USER")
            .build();
            
        return new InMemoryUserDetailsManager(admin, user);
    }
}
```

### Security Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class SecurityIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testPublicEndpointAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity("/public/info", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
    
    @Test
    void testProtectedEndpointWithoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminEndpointWithAdminRole() {
        ResponseEntity<String> response = restTemplate.getForEntity("/admin/users", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

@WebMvcTest(DocumentController.class)
class DocumentControllerSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DocumentService documentService;
    
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testGetDocumentAsOwner() throws Exception {
        Document doc = new Document();
        doc.setOwner("user");
        
        when(documentService.getDocument(1L)).thenReturn(doc);
        when(documentService.isOwner(1L, "user")).thenReturn(true);
        
        mockMvc.perform(get("/documents/1"))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(username = "other", roles = "USER")
    void testGetDocumentAsNonOwner() throws Exception {
        when(documentService.isOwner(1L, "other")).thenReturn(false);
        
        mockMvc.perform(get("/documents/1"))
            .andExpect(status().isForbidden());
    }
}
```

## Interview Questions

### Basic Level

**Q1: What is Spring Security and what are its core features?**

**Answer:** Spring Security is a comprehensive security framework for Java applications providing:
- **Authentication**: Verifying user identity
- **Authorization**: Controlling access to resources
- **Protection**: Against CSRF, session fixation, clickjacking
- **Integration**: OAuth2, SAML, LDAP support
- **Method Security**: Annotation-based security
- **Password Encoding**: BCrypt, SCrypt, Argon2

**Q2: Explain the Spring Security filter chain.**

**Answer:** The filter chain processes security concerns in order:
1. **SecurityContextPersistenceFilter**: Loads/stores SecurityContext
2. **LogoutFilter**: Handles logout requests
3. **UsernamePasswordAuthenticationFilter**: Processes login forms
4. **BasicAuthenticationFilter**: Handles HTTP Basic auth
5. **AnonymousAuthenticationFilter**: Creates anonymous authentication
6. **ExceptionTranslationFilter**: Handles security exceptions
7. **FilterSecurityInterceptor**: Makes authorization decisions

### Intermediate Level

**Q3: How does JWT authentication work in Spring Security?**

**Answer:**
```java
// 1. User login with credentials
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
    );
    
    String jwt = jwtTokenProvider.generateToken(auth);
    return ResponseEntity.ok(new JwtResponse(jwt));
}

// 2. JWT Filter validates token on each request
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) {
        String jwt = getJwtFromRequest(request);
        
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            Long userId = tokenProvider.getUserIdFromToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, 
                    userDetails.getAuthorities());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

**Q4: Explain method-level security with examples.**

**Answer:**
```java
@Service
public class DocumentService {
    
    // Pre-authorization: Check before method execution
    @PreAuthorize("hasRole('ADMIN') or @documentService.isOwner(#id, authentication.name)")
    public Document getDocument(Long id) {
        return documentRepository.findById(id);
    }
    
    // Post-authorization: Check after method execution
    @PostAuthorize("hasPermission(returnObject, 'READ')")
    public Document findDocument(Long id) {
        return documentRepository.findById(id);
    }
    
    // Pre-filtering: Filter method parameters
    @PreFilter("hasPermission(filterObject, 'DELETE')")
    public void deleteDocuments(List<Document> documents) {
        documents.forEach(doc -> documentRepository.delete(doc));
    }
    
    // Post-filtering: Filter return values
    @PostFilter("hasRole('ADMIN') or filterObject.owner == authentication.name")
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
}
```

### Advanced Level

**Q5: Design a multi-tenant security system with Spring Security.**

**Answer:**
```java
@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenant) {
        currentTenant.set(tenant);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}

@Component
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = extractTenantId(httpRequest);
        
        try {
            TenantContext.setCurrentTenant(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    
    private String extractTenantId(HttpServletRequest request) {
        // Extract from header, subdomain, or JWT token
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader != null) {
            return tenantHeader;
        }
        
        // Extract from subdomain
        String serverName = request.getServerName();
        if (serverName.contains(".")) {
            return serverName.split("\\.")[0];
        }
        
        return "default";
    }
}

@Service
public class TenantAwareUserDetailsService implements UserDetailsService {
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantId = TenantContext.getCurrentTenant();
        
        User user = userRepository.findByUsernameAndTenantId(username, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found in tenant: " + tenantId));
        
        return UserPrincipal.create(user);
    }
}
```

**Q6: Implement OAuth2 Resource Server with custom JWT validation.**

**Answer:**
```java
@Configuration
@EnableResourceServer
public class ResourceServerConfig {
    
    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(customJwtDecoder())
                    .jwtAuthenticationConverter(customJwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder customJwtDecoder() {
        return new CustomJwtDecoder();
    }
    
    @Bean
    public JwtAuthenticationConverter customJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> authorities = jwt.getClaimAsStringList("authorities");
            return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        });
        return converter;
    }
}

public class CustomJwtDecoder implements JwtDecoder {
    
    private final JwtDecoder delegate;
    
    public CustomJwtDecoder() {
        this.delegate = JwtDecoders.fromIssuerLocation("https://auth-server.com");
    }
    
    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        
        // Custom validation logic
        validateCustomClaims(jwt);
        
        return jwt;
    }
    
    private void validateCustomClaims(Jwt jwt) {
        // Validate tenant claim
        String tenant = jwt.getClaimAsString("tenant");
        if (tenant == null || !isValidTenant(tenant)) {
            throw new JwtValidationException("Invalid tenant claim");
        }
        
        // Validate custom scopes
        List<String> scopes = jwt.getClaimAsStringList("scope");
        if (!hasRequiredScopes(scopes)) {
            throw new JwtValidationException("Missing required scopes");
        }
    }
}
```

### Expert Level

**Q7: Design a comprehensive audit system with Spring Security.**

**Answer:**
```java
@Entity
public class SecurityAuditEvent {
    @Id
    @GeneratedValue
    private Long id;
    
    private String username;
    private String eventType;
    private String resource;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private boolean success;
    private String details;
}

@Component
public class SecurityAuditListener {
    
    @Autowired
    private SecurityAuditService auditService;
    
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        SecurityAuditEvent auditEvent = new SecurityAuditEvent();
        auditEvent.setUsername(event.getAuthentication().getName());
        auditEvent.setEventType("LOGIN_SUCCESS");
        auditEvent.setTimestamp(LocalDateTime.now());
        auditEvent.setSuccess(true);
        
        auditService.saveAuditEvent(auditEvent);
    }
    
    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        SecurityAuditEvent auditEvent = new SecurityAuditEvent();
        auditEvent.setUsername(event.getAuthentication().getName());
        auditEvent.setEventType("LOGIN_FAILURE");
        auditEvent.setTimestamp(LocalDateTime.now());
        auditEvent.setSuccess(false);
        auditEvent.setDetails(event.getException().getMessage());
        
        auditService.saveAuditEvent(auditEvent);
    }
    
    @EventListener
    public void handleAuthorizationFailure(AuthorizationDeniedEvent event) {
        SecurityAuditEvent auditEvent = new SecurityAuditEvent();
        auditEvent.setUsername(event.getAuthentication().getName());
        auditEvent.setEventType("ACCESS_DENIED");
        auditEvent.setResource(event.getResource().toString());
        auditEvent.setTimestamp(LocalDateTime.now());
        auditEvent.setSuccess(false);
        
        auditService.saveAuditEvent(auditEvent);
    }
}

@Aspect
@Component
public class SecurityAuditAspect {
    
    @Autowired
    private SecurityAuditService auditService;
    
    @Around("@annotation(auditable)")
    public Object auditSecureMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        try {
            Object result = joinPoint.proceed();
            
            // Log successful operation
            auditService.logMethodAccess(username, methodName, true, null);
            
            return result;
        } catch (Exception e) {
            // Log failed operation
            auditService.logMethodAccess(username, methodName, false, e.getMessage());
            throw e;
        }
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String value() default "";
}
```

## Best Practices

### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class ProductionSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/public/**")
            )
            
            // Headers security
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            )
            
            // Session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry())
            )
            
            // Authorization
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
```

### Input Validation and Sanitization

```java
@RestController
@Validated
public class SecureController {
    
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Input validation handled by @Valid
        User user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam @Pattern(regexp = "^[a-zA-Z0-9\\s]+$") String query) {
        // Prevent SQL injection with regex validation
        List<User> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
}

public class CreateUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$")
    private String password;
}
```

### Rate Limiting and DDoS Protection

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final int maxRequests = 100;
    private final int windowSeconds = 60;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIpAddress(httpRequest);
        
        if (isRateLimited(clientIp)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean isRateLimited(String clientIp) {
        String key = "rate_limit:" + clientIp;
        String currentCount = redisTemplate.opsForValue().get(key);
        
        if (currentCount == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSeconds));
            return false;
        }
        
        int count = Integer.parseInt(currentCount);
        if (count >= maxRequests) {
            return true;
        }
        
        redisTemplate.opsForValue().increment(key);
        return false;
    }
}
```

This comprehensive Spring Security guide covers authentication, authorization, JWT, OAuth2, method security, testing, and production-ready security configurations with real-world examples and best practices.