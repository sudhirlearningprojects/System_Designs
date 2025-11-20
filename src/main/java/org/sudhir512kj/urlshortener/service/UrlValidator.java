package org.sudhir512kj.urlshortener.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class UrlValidator {
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$"
    );
    
    private static final Set<String> BLOCKED_DOMAINS = Set.of(
        "malicious-site.com",
        "spam-domain.net"
    );
    
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Check URL format
        if (!URL_PATTERN.matcher(url).matches()) {
            return false;
        }
        
        // Check against blocked domains
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && !BLOCKED_DOMAINS.contains(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}