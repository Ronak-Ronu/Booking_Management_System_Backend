package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.UserResponse;
import com.ronak.welcome.DTO.UserUpdateRequest;
import com.ronak.welcome.entity.Address;
import com.ronak.welcome.entity.City;
import com.ronak.welcome.entity.OutboxEvent;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.UserAlreadyExistsException;
import com.ronak.welcome.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.service.EmailService;
import com.ronak.welcome.service.UserService;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {
    public final UserRepository userRepository;
    private final EmailService emailService;
    private final CityServiceImpl cityService;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    public UserServiceImpl(UserRepository userRepository, EmailService emailService, CityServiceImpl cityService, OutboxEventRepository outboxEventRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.cityService = cityService;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("User " + user.getUsername() + " already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        if (user.getAddresses() != null) {
            for (Address address : user.getAddresses()) {
                address.setUser(user);
                City resolvedCity = cityService.resolveCity(address.getCity());
                address.setCity(resolvedCity);
            }
        }

        User savedUser = userRepository.save(user);
        OutboxEvent event = new OutboxEvent("USER_CREATED", savedUser.getUsername(), savedUser.getEmail(), "PENDING");
        outboxEventRepository.save(event);

        return savedUser;
    }
    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest userUpdateRequest) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (!existingUser.getUsername().equals(userUpdateRequest.username()) &&
                userRepository.existsByUsername(userUpdateRequest.username())) {
            throw new UserAlreadyExistsException("Username " + userUpdateRequest.username() + " already taken");
        }
        if (!existingUser.getEmail().equals(userUpdateRequest.email()) &&
                userRepository.existsByEmail(userUpdateRequest.email())) {
            throw new UserAlreadyExistsException("Email " + userUpdateRequest.email() + " already taken");
        }

        existingUser.setUsername(userUpdateRequest.username());
        existingUser.setEmail(userUpdateRequest.email());
        // Only update roles if the caller has ADMIN role (enforced by @PreAuthorize in controller)
        if (userUpdateRequest.roles() != null && !userUpdateRequest.roles().isEmpty()) {
            existingUser.setRoles(userUpdateRequest.roles());
        }

        User updatedUser = userRepository.save(existingUser);
        return new UserResponse(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getEmail(), updatedUser.getRoles());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRoles());
    }
}

