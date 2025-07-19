// src/main/java/com/ronak/welcome/service/AuthService.java
package com.ronak.welcome.service;

import com.ronak.welcome.DTO.AuthRequest;
import com.ronak.welcome.DTO.AuthResponse;
import com.ronak.welcome.DTO.RefreshTokenRequest;
import com.ronak.welcome.config.security.TotpService;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.config.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TotpService totpService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService, TotpService totpService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.totpService = totpService;
    }

    public AuthResponse login(AuthRequest authRequest) {
        // Authenticate username and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.username(),
                        authRequest.password()
                )
        );

        User user = userRepository.findByUsername(authRequest.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If TOTP is enabled for this user, require the TOTP code
        if (user.isTotpEnabled()) {
            if (authRequest.totpCode() == null ||
                    !totpService.verifyCode(user.getTotpSecret(), authRequest.totpCode())) {
                throw new RuntimeException("Invalid or missing TOTP code");
            }
        }

        UserDetails userDetails = buildSpringUserDetails(user);

        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        String username = jwtService.extractUsername(refreshToken);

        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found for refresh token: " + username));

            UserDetails userDetails = buildSpringUserDetails(user);

            if (jwtService.isTokenValid(refreshToken, userDetails)) {
                String newAccessToken = jwtService.generateToken(userDetails);
                String newRefreshToken = jwtService.generateRefreshToken(userDetails);

                return new AuthResponse(newAccessToken, newRefreshToken);
            }
        }
        throw new RuntimeException("Invalid or expired refresh token.");
    }

    // Utility method to convert your JPA user to a Spring Security UserDetails instance
    private UserDetails buildSpringUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}
