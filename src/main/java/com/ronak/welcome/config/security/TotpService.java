// src/main/java/com/ronak/welcome/config/security/TotpService.java
package com.ronak.welcome.config.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
// REMOVED: import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator; // No longer needed

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TotpService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Generates a new TOTP secret key for a user.
     * @return The base32 encoded secret key.
     */
    public String generateNewSecret() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey(); // Returns the base32 encoded secret (String)
    }

    /**
     * Generates the QR code URL for the authenticator app by directly constructing the otpauth URI.
     * This URL can be converted into a QR code image on the frontend.
     * Format: otpauth://totp/{issuer}:{accountName}?secret={secret}&issuer={issuer}
     *
     * @param secret The user's base32 encoded TOTP secret (String).
     * @param accountName The user's identifier (e.g., email or username).
     * @param issuer The name of your application (e.g., "BookingApp").
     * @return The otpauth URI string.
     */
    public String generateQrCodeUrl(String secret, String accountName, String issuer) {
        // Encode accountName and issuer to be URL-safe
        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);

        // Directly construct the otpauth URI string based on the standard format
        // This bypasses the problematic GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL method
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedIssuer, encodedAccountName, secret, encodedIssuer);
    }

    /**
     * Verifies a TOTP code provided by the user.
     * @param secret The user's base32 encoded TOTP secret.
     * @param code The TOTP code entered by the user.
     * @return true if the code is valid, false otherwise.
     */
    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}
