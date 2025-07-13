package com.ronak.welcome.service;

import com.ronak.welcome.DTO.UserResponse;
import com.ronak.welcome.DTO.UserUpdateRequest;
import com.ronak.welcome.entity.User;

import java.util.List;

public interface UserService {
    User createUser(User user);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest);
    void deleteUser(Long id);
    UserResponse getCurrentUser(String username);
}
