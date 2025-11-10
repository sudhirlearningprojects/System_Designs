package org.sudhir512kj.uber.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.sudhir512kj.uber.model.User;
import org.sudhir512kj.uber.repository.UserRepository;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String login(String phoneNumber, String password) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        
        // In production: verify password hash
        String token = "jwt_" + UUID.randomUUID().toString();
        log.info("User {} logged in", user.getUserId());
        return token;
    }

    public String generateToken(UUID userId) {
        return "jwt_" + userId.toString() + "_" + System.currentTimeMillis();
    }
}
