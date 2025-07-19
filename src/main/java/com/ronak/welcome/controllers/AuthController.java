package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.AuthRequest;
import com.ronak.welcome.DTO.AuthResponse;
import com.ronak.welcome.DTO.RefreshTokenRequest;
import com.ronak.welcome.config.security.JwtService;
import com.ronak.welcome.config.security.TotpService;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.login(authRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest));
    }

    @PostMapping("/totp/register")
    public ResponseEntity<Map<String, String>> registerTotp(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String secret = totpService.generateNewSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        String qrUrl = totpService.generateQrCodeUrl(
                secret,
                user.getUsername(),
                "bms"
        );

        Map<String, String> response = new HashMap<>();
        response.put("qrUrl", qrUrl);
        response.put("secret", secret);
        return ResponseEntity.ok(response);
    }

}