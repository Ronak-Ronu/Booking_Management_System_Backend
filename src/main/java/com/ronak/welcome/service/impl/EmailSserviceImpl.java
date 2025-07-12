package com.ronak.welcome.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ronak.welcome.service.EmailService;

@Service
public class EmailSserviceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    public EmailSserviceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    @Override
    public void sendWelcomeMessage(String username, String email) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom("ronaksutharb@gmail.com");
            helper.setTo(email);
            helper.setSubject("Welcome "+username);
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
        }catch (Exception e){
            throw new RuntimeException("Something went wrong in sending welcome mail : \n", e);
        }
    }
}
