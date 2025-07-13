package com.ronak.welcome.DTO;


import com.ronak.welcome.enums.Role;

import java.util.Set;

public record UserResponse(Long id, String username, String email, Set<Role> roles) {}
