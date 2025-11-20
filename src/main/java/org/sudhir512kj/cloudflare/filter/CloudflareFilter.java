package org.sudhir512kj.cloudflare.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.sudhir512kj.cloudflare.service.DDoSProtectionService;
import org.sudhir512kj.cloudflare.service.WAFEngine;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CloudflareFilter implements Filter {
    
    private final DDoSProtectionService ddosService;
    private final WAFEngine wafEngine;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String domain = httpRequest.getServerName();
        String clientIp = getClientIp(httpRequest);
        
        // Add Cloudflare headers
        httpResponse.setHeader("Server", "cloudflare");
        httpResponse.setHeader("CF-Ray", generateRayId());
        
        try {
            // DDoS Protection
            if (!ddosService.isRequestAllowed(httpRequest)) {
                log.warn("Request blocked by DDoS protection - IP: {}, Domain: {}", clientIp, domain);
                sendBlockedResponse(httpResponse, "DDoS Protection", 429);
                return;
            }
            
            // WAF Evaluation
            WAFEngine.WAFResult wafResult = wafEngine.evaluateRequest(httpRequest, domain);
            if (wafResult.getAction().equals("BLOCK")) {
                log.warn("Request blocked by WAF - IP: {}, Domain: {}, Rule: {}", 
                        clientIp, domain, wafResult.getRuleId());
                sendBlockedResponse(httpResponse, "Web Application Firewall", 403);
                return;
            }
            
            if (wafResult.getAction().equals("CHALLENGE")) {
                log.info("Request challenged by WAF - IP: {}, Domain: {}", clientIp, domain);
                sendChallengeResponse(httpResponse);
                return;
            }
            
            // Request allowed, continue to CDN
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in Cloudflare filter", e);
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private String generateRayId() {
        return Long.toHexString(System.currentTimeMillis()) + 
               Integer.toHexString((int)(Math.random() * 1000));
    }
    
    private void sendBlockedResponse(HttpServletResponse response, String reason, int status) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("text/html");
        response.getWriter().write(String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Access Denied</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
                    .error { color: #e74c3c; }
                </style>
            </head>
            <body>
                <h1 class="error">Access Denied</h1>
                <p>Your request was blocked by %s</p>
                <p>Ray ID: %s</p>
                <hr>
                <p><small>Cloudflare</small></p>
            </body>
            </html>
            """, reason, response.getHeader("CF-Ray")));
    }
    
    private void sendChallengeResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("text/html");
        response.getWriter().write("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Checking your browser...</title>
                <meta http-equiv="refresh" content="5">
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; margin-top: 100px; }
                    .spinner { border: 4px solid #f3f3f3; border-top: 4px solid #3498db; 
                              border-radius: 50%; width: 40px; height: 40px; 
                              animation: spin 2s linear infinite; margin: 20px auto; }
                    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                </style>
            </head>
            <body>
                <h1>Checking your browser before accessing the website.</h1>
                <div class="spinner"></div>
                <p>This process is automatic. Your browser will redirect to your requested content shortly.</p>
                <p>Please allow up to 5 seconds...</p>
                <hr>
                <p><small>Cloudflare</small></p>
            </body>
            </html>
            """);
    }
}