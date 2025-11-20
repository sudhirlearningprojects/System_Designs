package org.sudhir512kj.whatsapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.whatsapp.dto.UserDTO;
import org.sudhir512kj.whatsapp.model.User;
import org.sudhir512kj.whatsapp.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestParam String phoneNumber, @RequestParam String name) {
        UserDTO user = userService.registerUser(phoneNumber, name);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<UserDTO> getUserByPhone(@PathVariable String phoneNumber) {
        UserDTO user = userService.getUserByPhoneNumber(phoneNumber);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String userId, @RequestParam User.UserStatus status) {
        userService.updateUserStatus(userId, status);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{userId}/profile")
    public ResponseEntity<Void> updateProfile(
            @PathVariable String userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String about,
            @RequestParam(required = false) String profilePicture) {
        userService.updateProfile(userId, name, about, profilePicture);
        return ResponseEntity.ok().build();
    }
}