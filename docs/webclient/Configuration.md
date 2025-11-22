# WebClient Configuration

## Basic Configuration

### Simple Builder

```java
WebClient webClient = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultHeader(HttpHeaders.USER_AGENT, "MyApp/1.0")
    .defaultCookie("session", "abc123")
    .build();
```

### Spring Bean Configuration

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient userServiceClient() {
        return WebClient.builder()
            .baseUrl("http://user-service:8080")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    public WebClient productServiceClient() {
        return WebClient.builder()
            .baseUrl("http://product-service:8081")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
```

## Connection Pool Configuration

### Custom Connection Provider

```java
@Bean
public WebClient webClient() {
    ConnectionProvider provider = ConnectionProvider.builder("custom")
        .maxConnections(500)
        .maxIdleTime(Duration.ofSeconds(20))
        .maxLifeTime(Duration.ofSeconds(60))
        .pendingAcquireTimeout(Duration.ofSeconds(60))
        .evictInBackground(Duration.ofSeconds(120))
        .build();
    
    HttpClient httpClient = HttpClient.create(provider);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Connection Pool Metrics

```java
@Bean
public WebClient webClient(MeterRegistry meterRegistry) {
    ConnectionProvider provider = ConnectionProvider.builder("custom")
        .maxConnections(500)
        .metrics(true)
        .build();
    
    HttpClient httpClient = HttpClient.create(provider)
        .metrics(true, () -> meterRegistry);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Timeout Configuration

### Response Timeout

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(5));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Connection Timeout

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .responseTimeout(Duration.ofSeconds(5));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Read/Write Timeout

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(5))
            .addHandlerLast(new WriteTimeoutHandler(5)));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## SSL/TLS Configuration

### Trust All Certificates (Development Only)

```java
@Bean
public WebClient webClient() throws SSLException {
    SslContext sslContext = SslContextBuilder
        .forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build();
    
    HttpClient httpClient = HttpClient.create()
        .secure(sslSpec -> sslSpec.sslContext(sslContext));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Custom Trust Store

```java
@Bean
public WebClient webClient() throws Exception {
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (InputStream is = new FileInputStream("truststore.jks")) {
        trustStore.load(is, "password".toCharArray());
    }
    
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    
    SslContext sslContext = SslContextBuilder
        .forClient()
        .trustManager(tmf)
        .build();
    
    HttpClient httpClient = HttpClient.create()
        .secure(sslSpec -> sslSpec.sslContext(sslContext));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Mutual TLS (mTLS)

```java
@Bean
public WebClient webClient() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream is = new FileInputStream("client.p12")) {
        keyStore.load(is, "password".toCharArray());
    }
    
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, "password".toCharArray());
    
    SslContext sslContext = SslContextBuilder
        .forClient()
        .keyManager(kmf)
        .build();
    
    HttpClient httpClient = HttpClient.create()
        .secure(sslSpec -> sslSpec.sslContext(sslContext));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Proxy Configuration

### HTTP Proxy

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .proxy(proxy -> proxy
            .type(ProxyProvider.Proxy.HTTP)
            .host("proxy.example.com")
            .port(8080));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

### Authenticated Proxy

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .proxy(proxy -> proxy
            .type(ProxyProvider.Proxy.HTTP)
            .host("proxy.example.com")
            .port(8080)
            .username("user")
            .password(s -> "password"));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Filters

### Logging Filter

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .filter(logRequest())
        .filter(logResponse())
        .build();
}

private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        log.info("Request: {} {}", request.method(), request.url());
        request.headers().forEach((name, values) ->
            values.forEach(value -> log.info("{}={}", name, value)));
        return Mono.just(request);
    });
}

private ExchangeFilterFunction logResponse() {
    return ExchangeFilterFunction.ofResponseProcessor(response -> {
        log.info("Response: {}", response.statusCode());
        return Mono.just(response);
    });
}
```

### Authentication Filter

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .filter(addAuthHeader())
        .build();
}

private ExchangeFilterFunction addAuthHeader() {
    return ExchangeFilterFunction.ofRequestProcessor(request -> {
        ClientRequest filtered = ClientRequest.from(request)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken())
            .build();
        return Mono.just(filtered);
    });
}
```

### Retry Filter

```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .filter(retryFilter())
        .build();
}

private ExchangeFilterFunction retryFilter() {
    return (request, next) -> next.exchange(request)
        .flatMap(response -> {
            if (response.statusCode().is5xxServerError()) {
                return Mono.error(new ServerException("Server error"));
            }
            return Mono.just(response);
        })
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
}
```

## Codec Configuration

### Custom Codecs

```java
@Bean
public WebClient webClient() {
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> {
            configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
            configurer.defaultCodecs().jackson2JsonEncoder(
                new Jackson2JsonEncoder(objectMapper(), MediaType.APPLICATION_JSON));
            configurer.defaultCodecs().jackson2JsonDecoder(
                new Jackson2JsonDecoder(objectMapper(), MediaType.APPLICATION_JSON));
        })
        .build();
    
    return WebClient.builder()
        .exchangeStrategies(strategies)
        .build();
}

private ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```

### Increase Buffer Size

```java
@Bean
public WebClient webClient() {
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(16 * 1024 * 1024)) // 16MB
        .build();
    
    return WebClient.builder()
        .exchangeStrategies(strategies)
        .build();
}
```

## HTTP/2 Configuration

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .protocol(HttpProtocol.H2, HttpProtocol.HTTP11);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Compression

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .compress(true);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Keep-Alive Configuration

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Wire Logging

```java
@Bean
public WebClient webClient() {
    HttpClient httpClient = HttpClient.create()
        .wiretap("reactor.netty.http.client.HttpClient", 
                 LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

## Multiple WebClient Beans

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @Qualifier("userService")
    public WebClient userServiceClient() {
        return WebClient.builder()
            .baseUrl("http://user-service:8080")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    @Qualifier("productService")
    public WebClient productServiceClient() {
        return WebClient.builder()
            .baseUrl("http://product-service:8081")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    @Qualifier("paymentGateway")
    public WebClient paymentGatewayClient() {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30));
        
        return WebClient.builder()
            .baseUrl("https://payment-gateway.com")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(addApiKeyHeader())
            .build();
    }
    
    private ExchangeFilterFunction addApiKeyHeader() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            ClientRequest filtered = ClientRequest.from(request)
                .header("X-API-Key", "secret-key")
                .build();
            return Mono.just(filtered);
        });
    }
}
```

## Configuration Properties

```yaml
# application.yml
webclient:
  user-service:
    base-url: http://user-service:8080
    timeout: 5000
    max-connections: 100
  
  product-service:
    base-url: http://product-service:8081
    timeout: 3000
    max-connections: 50
```

```java
@Configuration
@ConfigurationProperties(prefix = "webclient")
@Data
public class WebClientProperties {
    private ServiceConfig userService;
    private ServiceConfig productService;
    
    @Data
    public static class ServiceConfig {
        private String baseUrl;
        private int timeout;
        private int maxConnections;
    }
}

@Configuration
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfig {
    
    @Bean
    public WebClient userServiceClient(WebClientProperties properties) {
        ServiceConfig config = properties.getUserService();
        
        ConnectionProvider provider = ConnectionProvider.builder("user-service")
            .maxConnections(config.getMaxConnections())
            .build();
        
        HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(Duration.ofMillis(config.getTimeout()));
        
        return WebClient.builder()
            .baseUrl(config.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

## Best Practices

1. **Connection pooling**: Configure appropriate pool sizes
2. **Timeouts**: Always set timeouts to prevent hanging
3. **SSL/TLS**: Use proper certificate validation in production
4. **Filters**: Use for cross-cutting concerns (logging, auth)
5. **Codecs**: Configure buffer sizes for large payloads
6. **Reuse instances**: Create WebClient beans, don't recreate
7. **Metrics**: Enable metrics for monitoring
8. **HTTP/2**: Use for better performance when supported

## Next Steps

- [Request Building](Request_Building.md) - Building complex requests
- [Response Handling](Response_Handling.md) - Processing responses
- [Error Handling](Error_Handling.md) - Exception management
