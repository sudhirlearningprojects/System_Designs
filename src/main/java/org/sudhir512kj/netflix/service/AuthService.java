package org.sudhir512kj.netflix.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {
    
    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final Duration tokenExpiry = Duration.ofHours(1);
    
    public Mono<String> generateToken(UUID userId, String email) {
        return Mono.fromCallable(() -> {
            Instant now = Instant.now();
            return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(tokenExpiry)))
                .signWith(secretKey)
                .compact();
        });
    }
    
    public Mono<UUID> validateTokenAndGetUserId(String token) {
        return Mono.fromCallable(() -> {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            return UUID.fromString(claims.getSubject());
        }).onErrorReturn(UUID.randomUUID()); // Return empty UUID on error
    }
    
    public Mono<Boolean> isTokenValid(String token) {
        return validateTokenAndGetUserId(token)
            .map(userId -> !userId.equals(UUID.randomUUID()))
            .onErrorReturn(false);
    }
}