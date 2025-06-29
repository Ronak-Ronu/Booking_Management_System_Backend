package com.ronak.welcome.service.impl;

import com.ronak.welcome.entity.User;
import com.ronak.welcome.exception.UserAlreadyExistsException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.service.EmailService;
import com.ronak.welcome.service.UserService;


@Service
public class UserServiceImpl implements UserService {
    public final UserRepository userRepository;
    private final EmailService emailService;

    public UserServiceImpl(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if(userRepository.existsByUsername(user.getUsername())){
            throw new UserAlreadyExistsException("User  " + user.getUsername() + " already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        if(user.getAddresses()!=null){
            user.getAddresses().forEach(address -> address.setUser(user));
        }
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeMessage(savedUser.getUsername(), savedUser.getEmail() );
        return savedUser;

    }

    public static class EmailServiceImpl {
    }
}

