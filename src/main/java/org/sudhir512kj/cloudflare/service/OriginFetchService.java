package org.sudhir512kj.cloudflare.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OriginFetchService {
    
    private final RestTemplate restTemplate;
    
    public OriginResponse fetch(String domain, String path, 
                              HttpServletRequest request) {
        try {
            // Build origin URL
            String originUrl = buildOriginUrl(domain, path, request.getQueryString());
            
            // Copy headers from original request
            HttpHeaders headers = copyHeaders(request);
            headers.set("Host", domain);
            headers.set("X-Forwarded-For", getClientIp(request));
            headers.set("X-Forwarded-Proto", request.getScheme());
            
            // Create request entity
            RequestEntity<Void> requestEntity = new RequestEntity<>(
                headers, 
                HttpMethod.valueOf(request.getMethod()), 
                URI.create(originUrl)
            );
            
            // Fetch from origin
            ResponseEntity<byte[]> response = restTemplate.exchange(
                requestEntity, 
                byte[].class
            );
            
            log.info("Origin fetch successful - URL: {}, Status: {}", 
                    originUrl, response.getStatusCode());
            
            return OriginResponse.builder()
                .content(response.getBody())
                .headers(response.getHeaders())
                .statusCode(response.getStatusCode().value())
                .cacheable(isCacheable(response))
                .ttl(getCacheTTL(response))
                .build();
            
        } catch (Exception e) {
            log.error("Origin fetch failed - Domain: {}, Path: {}", domain, path, e);
            
            // Return error response
            return OriginResponse.builder()
                .content("Origin server unavailable".getBytes())
                .headers(new HttpHeaders())
                .statusCode(502)
                .cacheable(false)
                .ttl(Duration.ZERO)
                .build();
        }
    }
    
    private String buildOriginUrl(String domain, String path, String queryString) {
        // In production, this would resolve to actual origin servers
        // For demo, we'll use a placeholder
        StringBuilder url = new StringBuilder("http://origin-")
            .append(domain.replace(".", "-"))
            .append(".internal")
            .append(path);
            
        if (queryString != null && !queryString.isEmpty()) {
            url.append("?").append(queryString);
        }
        
        return url.toString();
    }
    
    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Skip hop-by-hop headers
            if (isHopByHopHeader(headerName)) {
                continue;
            }
            
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                headers.add(headerName, headerValues.nextElement());
            }
        }
        
        return headers;
    }
    
    private boolean isHopByHopHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.equals("connection") ||
               lowerName.equals("keep-alive") ||
               lowerName.equals("proxy-authenticate") ||
               lowerName.equals("proxy-authorization") ||
               lowerName.equals("te") ||
               lowerName.equals("trailers") ||
               lowerName.equals("transfer-encoding") ||
               lowerName.equals("upgrade");
    }
    
    private boolean isCacheable(ResponseEntity<byte[]> response) {
        int statusCode = response.getStatusCode().value();
        
        // Only cache successful responses
        if (statusCode < 200 || statusCode >= 300) {
            return false;
        }
        
        // Check Cache-Control header
        String cacheControl = response.getHeaders().getFirst("Cache-Control");
        if (cacheControl != null) {
            if (cacheControl.contains("no-cache") || 
                cacheControl.contains("no-store") ||
                cacheControl.contains("private")) {
                return false;
            }
        }
        
        // Check for dynamic content indicators
        String contentType = response.getHeaders().getFirst("Content-Type");
        if (contentType != null && contentType.contains("text/html")) {
            // HTML content might be dynamic, cache for shorter time
            return true;
        }
        
        return true;
    }
    
    private Duration getCacheTTL(ResponseEntity<byte[]> response) {
        // Check Cache-Control max-age
        String cacheControl = response.getHeaders().getFirst("Cache-Control");
        if (cacheControl != null && cacheControl.contains("max-age=")) {
            try {
                String maxAge = cacheControl.substring(
                    cacheControl.indexOf("max-age=") + 8
                ).split(",")[0].trim();
                return Duration.ofSeconds(Long.parseLong(maxAge));
            } catch (Exception e) {
                log.warn("Failed to parse max-age from Cache-Control: {}", cacheControl);
            }
        }
        
        // Default TTL based on content type
        String contentType = response.getHeaders().getFirst("Content-Type");
        if (contentType != null) {
            if (contentType.startsWith("image/") || 
                contentType.startsWith("video/") ||
                contentType.contains("css") ||
                contentType.contains("javascript")) {
                return Duration.ofHours(24); // Static assets
            } else if (contentType.contains("text/html")) {
                return Duration.ofMinutes(5); // Dynamic content
            }
        }
        
        return Duration.ofHours(1); // Default
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    public static class OriginResponse {
        private byte[] content;
        private HttpHeaders headers;
        private int statusCode;
        private boolean cacheable;
        private Duration ttl;
        
        public static OriginResponseBuilder builder() {
            return new OriginResponseBuilder();
        }
        
        public static class OriginResponseBuilder {
            private byte[] content;
            private HttpHeaders headers;
            private int statusCode;
            private boolean cacheable;
            private Duration ttl;
            
            public OriginResponseBuilder content(byte[] content) {
                this.content = content;
                return this;
            }
            
            public OriginResponseBuilder headers(HttpHeaders headers) {
                this.headers = headers;
                return this;
            }
            
            public OriginResponseBuilder statusCode(int statusCode) {
                this.statusCode = statusCode;
                return this;
            }
            
            public OriginResponseBuilder cacheable(boolean cacheable) {
                this.cacheable = cacheable;
                return this;
            }
            
            public OriginResponseBuilder ttl(Duration ttl) {
                this.ttl = ttl;
                return this;
            }
            
            public OriginResponse build() {
                OriginResponse response = new OriginResponse();
                response.content = this.content;
                response.headers = this.headers;
                response.statusCode = this.statusCode;
                response.cacheable = this.cacheable;
                response.ttl = this.ttl;
                return response;
            }
        }
        
        public byte[] getContent() { return content; }
        public HttpHeaders getHeaders() { return headers; }
        public int getStatusCode() { return statusCode; }
        public boolean isCacheable() { return cacheable; }
        public Duration getTtl() { return ttl; }
    }
}