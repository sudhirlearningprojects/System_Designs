package org.sudhir512kj.whatsapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sudhir512kj.whatsapp.constants.WhatsAppConstants;
import org.sudhir512kj.whatsapp.dto.UserDTO;
import org.sudhir512kj.whatsapp.exception.WhatsAppException;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.repository.UserRepository;
import org.sudhir512kj.whatsapp.util.ValidationUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserDTO registerUser(String phoneNumber, String name) {
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            throw new WhatsAppException.InvalidOperationException("Invalid phone number format");
        }
        if (!ValidationUtils.isValidUserName(name)) {
            throw new WhatsAppException.InvalidOperationException("Invalid user name");
        }
        
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .name(name)
                .status(User.UserStatus.OFFLINE)
                .about(WhatsAppConstants.DEFAULT_ABOUT)
                .build();
        
        user = userRepository.save(user);
        log.info("User registered: {}", phoneNumber);
        
        return convertToDTO(user);
    }
    
    public UserDTO getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(this::convertToDTO)
                .orElse(null);
    }
    
    public List<UserDTO> searchUsers(String query) {
        return userRepository.searchUsers(query)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public void updateUserStatus(String userId, User.UserStatus status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            if (status == User.UserStatus.OFFLINE) {
                user.setLastSeen(LocalDateTime.now());
            }
            userRepository.save(user);
        });
    }
    
    public void updateProfile(String userId, String name, String about, String profilePicture) {
        userRepository.findById(userId).ifPresent(user -> {
            if (name != null) user.setName(name);
            if (about != null) user.setAbout(about);
            if (profilePicture != null) user.setProfilePicture(profilePicture);
            userRepository.save(user);
        });
    }
    
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .about(user.getAbout())
                .status(user.getStatus())
                .lastSeen(user.getLastSeen())
                .build();
    }
}