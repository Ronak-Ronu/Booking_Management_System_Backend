package com.ronak.welcome.controllers;


import com.ronak.welcome.DTO.TotpVerifyRequest;
import com.ronak.welcome.DTO.UserResponse;
import com.ronak.welcome.DTO.UserUpdateRequest;
import com.ronak.welcome.config.security.TotpService;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.ronak.welcome.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final TotpService totpService;

    public UserController(UserService userService, UserRepository userRepository, TotpService totpService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.totpService = totpService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user){
        try{
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating user: " + e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserResponse userResponse = userService.getCurrentUser(username);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Update user: User can update their own profile, ADMIN can update any
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    // If authentication.principal doesn't have ID directly, you'd use:
    // @PreAuthorize("hasRole('ADMIN') or @userService.getCurrentUser(authentication.name).id() == #id")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        UserResponse updatedUser = userService.updateUser(id, userUpdateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    // Delete user -. user can delete their own profile and admin can delete any
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/totp/verify")
    public ResponseEntity<?> verifyTotp(@RequestBody TotpVerifyRequest request, Authentication authentication) {
        // The currently logged-in user
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if secret is set (should be, right after /totp/register)
        if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
            return ResponseEntity.badRequest().body("TOTP not registered for this user.");
        }

        // Use your existing TotpService to verify the code
        boolean valid = totpService.verifyCode(user.getTotpSecret(), Integer.parseInt(request.getCode()));
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid TOTP code.");
        }

        // Enable TOTP for this user
        user.setTotpEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok("TOTP verified and enabled.");
    }

}
