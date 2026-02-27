package org.sudhir512kj.netflix.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.sudhir512kj.netflix.model.User;
import org.sudhir512kj.netflix.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User registerUser(String email, String password, String name, String region) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists with email: " + email);
        }
        
        User user = new User(email, passwordEncoder.encode(password), name, region);
        return userRepository.save(user);
    }
    
    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            User user = userOpt.get();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            return Optional.of(user);
        }
        
        return Optional.empty();
    }
    
    public User updateSubscriptionPlan(String userId, User.SubscriptionPlan newPlan) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPlan(newPlan);
        return userRepository.save(user);
    }
    
    public User updateUserPreferences(String userId, List<String> preferredGenres) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPreferredGenres(preferredGenres);
        return userRepository.save(user);
    }
    
    public Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public boolean canStreamQuality(String userId, String quality) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;
        
        User.SubscriptionPlan plan = userOpt.get().getPlan();
        
        return switch (quality) {
            case "4K" -> plan == User.SubscriptionPlan.PREMIUM;
            case "1080p" -> plan == User.SubscriptionPlan.STANDARD || plan == User.SubscriptionPlan.PREMIUM;
            case "720p", "360p" -> true;
            default -> false;
        };
    }
    
    public int getMaxConcurrentStreams(String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return 0;
        
        return switch (userOpt.get().getPlan()) {
            case BASIC -> 1;
            case STANDARD -> 2;
            case PREMIUM -> 4;
        };
    }
    
    public User updateProfile(String userId, String name, String region) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (name != null) user.setName(name);
        if (region != null) user.setRegion(region);
        
        return userRepository.save(user);
    }
    
    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}