package com.ronak.welcome.DTO;

import com.ronak.welcome.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UserUpdateRequest(
        @NotBlank(message = "Username cannot be blank") String username,
        @NotBlank(message = "Email cannot be blank") @Email(message = "Invalid email format") String email,
        Set<Role> roles
) {}
