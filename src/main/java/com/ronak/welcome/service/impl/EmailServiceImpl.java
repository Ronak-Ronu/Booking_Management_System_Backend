// src/main/java/com/ronak/welcome/service/impl/EmailServiceImpl.java
package com.ronak.welcome.service.impl;

import com.ronak.welcome.service.EmailService; // Ensure this import is correct
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service("emailServiceImpl") // Explicitly name the bean to avoid conflicts if needed
public class EmailServiceImpl implements EmailService { // Renamed class

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class); // Use correct class for logger

    private final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    @Override
    public void sendWelcomeMessage(String username, String email) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom("ronaksutharb@gmail.com"); // Your sender email
            helper.setTo(email);
            helper.setSubject("Welcome " + username);
            String htmlContent = String.format("""
                <html>
                    <body>
                        <h1>Welcome, %s!</h1>
                        <p>Thank you for registering.</p>
                    </body>
                </html>
                """, username);

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
            logger.info("Successfully sent welcome email to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Something went wrong in sending welcome mail", e);
        }
    }

    @Async // Make sure to enable async execution in your main application class (@EnableAsync)
    @Override
    public void sendBookingConfirmationEmail(String recipientEmail, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom("ronaksutharb@gmail.com"); // Your sender email
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // Assuming body can be HTML
            javaMailSender.send(message);
            logger.info("Successfully sent booking confirmation email to {} with subject: {}", recipientEmail, subject);
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation email to {}: {}", recipientEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send booking confirmation email", e);
        }
    }

    @Async // Make sure to enable async execution in your main application class (@EnableAsync)
    @Override
    public void sendGenericEmail(String recipientEmail, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom("ronaksutharb@gmail.com"); // Your sender email
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // Assuming body can be HTML
            javaMailSender.send(message);
            logger.info("Successfully sent generic email to {} with subject: {}", recipientEmail, subject);
        } catch (Exception e) {
            logger.error("Failed to send generic email to {}: {}", recipientEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send generic email", e);
        }
    }
}
