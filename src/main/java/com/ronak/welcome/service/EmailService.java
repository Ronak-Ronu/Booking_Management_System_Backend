// src/main/java/com/ronak/welcome/service/EmailService.java
package com.ronak.welcome.service;

public interface EmailService {
    // Keep your existing welcome message method if you still use it directly somewhere
    void sendWelcomeMessage(String username, String email);

    // New generic methods for the OutboxProcessorService
    void sendBookingConfirmationEmail(String recipientEmail, String subject, String body);
    void sendGenericEmail(String recipientEmail, String subject, String body);
}
