# Module 11: Security

## 1. SSL/TLS Configuration

### Kafka with SSL

```yaml
camel:
  component:
    kafka:
      brokers: kafka:9093
      security-protocol: SSL
      ssl-truststore-location: /certs/truststore.jks
      ssl-truststore-password: ${TRUSTSTORE_PASSWORD}
      ssl-keystore-location: /certs/keystore.jks
      ssl-keystore-password: ${KEYSTORE_PASSWORD}
      ssl-key-password: ${KEY_PASSWORD}
```

### HTTP with mTLS

```java
@Bean("sslContextParameters")
public SSLContextParameters sslContextParameters() {
    KeyStoreParameters keyStore = new KeyStoreParameters();
    keyStore.setResource("/certs/client-keystore.p12");
    keyStore.setPassword("changeit");

    KeyManagersParameters keyManagers = new KeyManagersParameters();
    keyManagers.setKeyStore(keyStore);
    keyManagers.setKeyPassword("changeit");

    KeyStoreParameters trustStore = new KeyStoreParameters();
    trustStore.setResource("/certs/truststore.jks");
    trustStore.setPassword("changeit");

    TrustManagersParameters trustManagers = new TrustManagersParameters();
    trustManagers.setKeyStore(trustStore);

    SSLContextParameters sslContext = new SSLContextParameters();
    sslContext.setKeyManagers(keyManagers);
    sslContext.setTrustManagers(trustManagers);
    return sslContext;
}

from("direct:secure-call")
    .to("https://secure-service.com/api?sslContextParameters=#sslContextParameters");
```

---

## 2. Authentication

### OAuth2 Token in HTTP Calls

```java
@Component
public class OAuth2Routes extends RouteBuilder {

    @Value("${oauth2.token-url}")
    private String tokenUrl;

    @Value("${oauth2.client-id}")
    private String clientId;

    @Value("${oauth2.client-secret}")
    private String clientSecret;

    @Override
    public void configure() {
        // Token refresh route (runs every 50 minutes)
        from("timer:token-refresh?period=3000000&delay=0")
            .routeId("oauth2-token-refresh")
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
            .setBody(simple("grant_type=client_credentials&client_id=" + clientId 
                + "&client_secret=" + clientSecret))
            .to(tokenUrl)
            .unmarshal().json(JsonLibrary.Jackson, Map.class)
            .process(exchange -> {
                String token = (String) exchange.getIn().getBody(Map.class).get("access_token");
                exchange.getContext().getPropertiesComponent()
                    .addOverrideProperty("oauth2.token", token);
            })
            .log("OAuth2 token refreshed");

        // Use token in API calls
        from("direct:call-protected-api")
            .setHeader("Authorization", simple("Bearer {{oauth2.token}}"))
            .to("http://protected-service/api/data");
    }
}
```

### API Key Authentication

```java
rest("/api/v1")
    .get("/data")
        .to("direct:authenticated-endpoint");

from("direct:authenticated-endpoint")
    .process(exchange -> {
        String apiKey = exchange.getIn().getHeader("X-API-Key", String.class);
        if (apiKey == null || !isValidApiKey(apiKey)) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
            exchange.getIn().setBody("{\"error\":\"Invalid or missing API key\"}");
            exchange.setRouteStop(true);
        }
    })
    .bean("dataService", "getData");
```

### JWT Validation

```java
@Component("jwtValidator")
public class JwtValidator {
    
    private final JWTVerifier verifier;
    
    public JwtValidator(@Value("${jwt.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public void validate(Exchange exchange) {
        String auth = exchange.getIn().getHeader("Authorization", String.class);
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new SecurityException("Missing Bearer token");
        }
        
        DecodedJWT jwt = verifier.verify(auth.substring(7));
        exchange.getIn().setHeader("userId", jwt.getSubject());
        exchange.getIn().setHeader("roles", jwt.getClaim("roles").asList(String.class));
    }
}

// Use in route
from("platform-http:/api/secure")
    .bean("jwtValidator", "validate")
    .bean("secureService", "processRequest");
```

---

## 3. Vault Integration (Secrets Management)

### AWS Secrets Manager

```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-aws-secrets-manager-starter</artifactId>
</dependency>
```

```yaml
camel:
  vault:
    aws:
      region: us-east-1
      use-default-credentials-provider: true
      # OR explicit credentials
      # access-key: ${AWS_ACCESS_KEY}
      # secret-key: ${AWS_SECRET_KEY}
```

```java
// Reference secrets in routes using {{aws:secret-name/key}}
from("kafka:orders"
    + "?brokers={{aws:kafka-config/brokers}}"
    + "&saslJaasConfig={{aws:kafka-config/sasl-config}}")
    .to("sql:INSERT INTO orders VALUES(:#${body.id})"
        + "?dataSource=#dataSource"
        + "&password={{aws:db-credentials/password}}");
```

### HashiCorp Vault

```yaml
camel:
  vault:
    hashicorp:
      host: vault.internal
      port: 8200
      token: ${VAULT_TOKEN}
      scheme: https
```

```java
// Use in routes
from("direct:connect")
    .to("http://api.example.com"
        + "?authenticationPreemptive=true"
        + "&authUsername={{hashicorp:secret/data/api-creds#username}}"
        + "&authPassword={{hashicorp:secret/data/api-creds#password}}");
```

### Dynamic Secret Refresh

```java
// Secrets auto-refresh (Camel 4.x)
camel:
  vault:
    aws:
      region: us-east-1
      refresh-enabled: true
      refresh-period: 30000       # Check every 30s
      secrets: db-password,api-key  # Watch these secrets
```

---

## 4. Data Masking & Sanitization

```java
@Component("dataMasker")
public class DataMasker {

    public void maskSensitiveFields(Exchange exchange) {
        Map<String, Object> body = exchange.getIn().getBody(Map.class);
        
        if (body.containsKey("creditCard")) {
            String cc = (String) body.get("creditCard");
            body.put("creditCard", "****" + cc.substring(cc.length() - 4));
        }
        if (body.containsKey("ssn")) {
            body.put("ssn", "***-**-****");
        }
        if (body.containsKey("email")) {
            String email = (String) body.get("email");
            body.put("email", email.replaceAll("(.)(.*)(@.*)", "$1***$3"));
        }
        
        exchange.getIn().setBody(body);
    }
}

// Mask before logging or sending to external systems
from("kafka:customer-data")
    .wireTap("direct:audit-log")
        .onPrepare(exchange -> {
            // Mask the copy going to audit
            dataMasker.maskSensitiveFields(exchange);
        })
    .end()
    .to("direct:process");  // Original (unmasked) continues
```

---

## 5. Route Policy (Authorization)

```java
@Component
public class RoleBasedRoutePolicy implements RoutePolicy {

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        List<String> roles = exchange.getIn().getHeader("roles", List.class);
        String routeId = route.getRouteId();

        Map<String, List<String>> requiredRoles = Map.of(
            "admin-route", List.of("ADMIN"),
            "payment-route", List.of("ADMIN", "FINANCE"),
            "read-route", List.of("ADMIN", "USER", "READONLY")
        );

        List<String> required = requiredRoles.getOrDefault(routeId, List.of());
        if (!required.isEmpty() && roles.stream().noneMatch(required::contains)) {
            throw new SecurityException("Insufficient permissions for route: " + routeId);
        }
    }

    @Override public void onExchangeDone(Route route, Exchange exchange) {}
    @Override public void onInit(Route route) {}
    @Override public void onRemove(Route route) {}
    @Override public void onStart(Route route) {}
    @Override public void onStop(Route route) {}
    @Override public void onSuspend(Route route) {}
    @Override public void onResume(Route route) {}
}

// Apply policy
from("direct:admin-action")
    .routeId("admin-route")
    .routePolicy(roleBasedPolicy)
    .bean("adminService", "performAction");
```

---

## 6. Kafka SASL Authentication

```yaml
camel:
  component:
    kafka:
      brokers: kafka:9093
      security-protocol: SASL_SSL
      sasl-mechanism: PLAIN
      sasl-jaas-config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${KAFKA_USER}"
        password="${KAFKA_PASSWORD}";
      ssl-truststore-location: /certs/kafka-truststore.jks
      ssl-truststore-password: ${TRUSTSTORE_PASS}
```

### SCRAM-SHA-256

```yaml
camel:
  component:
    kafka:
      security-protocol: SASL_SSL
      sasl-mechanism: SCRAM-SHA-256
      sasl-jaas-config: >
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USER}"
        password="${KAFKA_PASSWORD}";
```

---

## 7. Security Best Practices Checklist

| Practice | Implementation |
|----------|---------------|
| Never log sensitive data | Use data masking in wireTap/log |
| Rotate secrets regularly | Vault integration + refresh |
| Validate all inputs | Bean validation + custom processors |
| Use mTLS for service-to-service | SSLContextParameters |
| Encrypt data at rest | Database TDE, S3 encryption |
| Encrypt data in transit | TLS everywhere |
| Rate limit APIs | Throttler EIP |
| Audit all access | WireTap to audit topic |
| Principle of least privilege | Route policies + RBAC |
| Scan dependencies | OWASP dependency check |
