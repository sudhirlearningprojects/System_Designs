package org.sudhir512kj.netflix.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    
    private final SecretKey secretKey = Jwts.SIG.HS256.key().build();
    private final Duration tokenExpiry = Duration.ofHours(1);
    
    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(tokenExpiry)))
            .signWith(secretKey)
            .compact();
    }
    
    public Optional<UUID> validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    public boolean isTokenValid(String token) {
        return validateTokenAndGetUserId(token).isPresent();
    }
}
