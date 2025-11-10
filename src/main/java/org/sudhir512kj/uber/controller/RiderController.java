package org.sudhir512kj.uber.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sudhir512kj.uber.model.Ride;
import org.sudhir512kj.uber.model.User;
import org.sudhir512kj.uber.service.AuthService;
import org.sudhir512kj.uber.service.RideService;
import org.sudhir512kj.uber.service.UserService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/riders")
public class RiderController {
    private final RideService rideService;
    private final UserService userService;
    private final AuthService authService;
    
    public RiderController(RideService rideService, UserService userService, AuthService authService) {
        this.rideService = rideService;
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegistrationRequest request) {
        User user = userService.registerRider(request.name, request.phoneNumber, request.email);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request.phoneNumber, request.password);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @GetMapping("/{riderId}/profile")
    public ResponseEntity<User> getProfile(@PathVariable UUID riderId) {
        User user = userService.getUserById(riderId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{riderId}/history")
    public ResponseEntity<List<Ride>> getRideHistory(@PathVariable UUID riderId) {
        List<Ride> rides = rideService.getRideHistory(riderId);
        return ResponseEntity.ok(rides);
    }

    static class RegistrationRequest {
        public String name;
        public String phoneNumber;
        public String email;
    }

    static class LoginRequest {
        public String phoneNumber;
        public String password;
    }

    static class LoginResponse {
        public String token;
        
        public LoginResponse(String token) {
            this.token = token;
        }
    }
}
