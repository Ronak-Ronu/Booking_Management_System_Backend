// src/main/java/com/ronak/welcome/service/AuthService.java
package com.ronak.welcome.service;

import com.ronak.welcome.DTO.AuthRequest;
import com.ronak.welcome.DTO.AuthResponse;
import com.ronak.welcome.DTO.RefreshTokenRequest;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.config.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResponse login(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            UserDetails userDetails = (UserDetails) userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found for refresh token: " + username));

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String newAccessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                return new AuthResponse(newAccessToken, newRefreshToken);
            }
        }
        throw new RuntimeException("Invalid or expired refresh token.");
    }
}
