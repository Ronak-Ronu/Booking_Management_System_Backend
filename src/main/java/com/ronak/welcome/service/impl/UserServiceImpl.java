package com.ronak.welcome.service.impl;

import com.ronak.welcome.entity.Address;
import com.ronak.welcome.entity.City;
import com.ronak.welcome.entity.OutboxEvent;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.exception.UserAlreadyExistsException;
import com.ronak.welcome.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.service.EmailService;
import com.ronak.welcome.service.UserService;


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
}

