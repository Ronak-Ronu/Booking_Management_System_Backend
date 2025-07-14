// src/main/java/com/ronak/welcome/service/OutboxProcessorService.java
package com.ronak.welcome.service.impl; // Note: This is in the 'service' package, not 'impl'

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ronak.welcome.DTO.BookingResponse;
import com.ronak.welcome.DTO.UserResponse;
import com.ronak.welcome.entity.OutboxEvent;
import com.ronak.welcome.repository.OutboxEventRepository;
import com.ronak.welcome.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessorService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final EmailService emailService; // Inject the EmailService interface
    private final ObjectMapper objectMapper;

    @Value("${outbox.processor.batch-size:10}")
    private int batchSize;

    @Value("${outbox.processor.max-retries:5}")
    private int maxRetries;

    public OutboxProcessorService(OutboxEventRepository outboxEventRepository, EmailService emailService) {
        this.outboxEventRepository = outboxEventRepository;
        this.emailService = emailService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void processOutboxEvents() {
        logger.info("Starting Outbox Event processing...");

        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc("PENDING", maxRetries);

        if (pendingEvents.isEmpty()) {
            logger.info("No pending outbox events to process.");
            return;
        }

        logger.info("Found {} pending outbox events to process.", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                event.setStatus("PROCESSING");
                outboxEventRepository.save(event);

                handleEvent(event);

                event.setStatus("COMPLETED");
                event.setProcessedAt(LocalDateTime.now());
                event.setErrorMessage(null);
                logger.info("Outbox event {} (Type: {}) processed successfully.", event.getId(), event.getEventType());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(event.getRetryCount() >= maxRetries ? "FAILED" : "PENDING");
                event.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 255)) : "Unknown error");
                logger.error("Failed to process outbox event {} (Type: {}). Retry count: {}. Error: {}",
                        event.getId(), event.getEventType(), event.getRetryCount(), event.getErrorMessage(), e);
            } finally {
                outboxEventRepository.save(event);
            }
        }
        logger.info("Finished Outbox Event processing.");
    }

    private void handleEvent(OutboxEvent event) throws JsonProcessingException {
        String recipient = event.getRecipientEmail();
        if (recipient == null || recipient.isEmpty()) {
            logger.warn("Outbox event {} (Type: {}) has no recipient email. Skipping email sending.", event.getId(), event.getEventType());
            return;
        }

        switch (event.getEventType()) {
            case "USER_CREATED":
                UserResponse userPayload = objectMapper.readValue(event.getPayload(), UserResponse.class);
                // Using sendGenericEmail for welcome message
                emailService.sendGenericEmail(
                        recipient,
                        "Welcome to Our Platform, " + userPayload.username() + "!",
                        String.format("Dear %s,\n\nWelcome to our platform! We're excited to have you onboard.\n\nBest regards,\nYour Team", userPayload.username())
                );
                break;
            case "BOOKING_CONFIRMED":
                BookingResponse bookingConfirmedPayload = objectMapper.readValue(event.getPayload(), BookingResponse.class);
                emailService.sendBookingConfirmationEmail(
                        recipient,
                        "Booking Confirmed: " + bookingConfirmedPayload.bookableItemName(),
                        String.format("Dear %s,\n\nYour booking for '%s' on %s is confirmed. Booking ID: %d\n\nThank you!",
                                bookingConfirmedPayload.username(),
                                bookingConfirmedPayload.bookableItemName(),
                                bookingConfirmedPayload.bookingDate(),
                                bookingConfirmedPayload.id())
                );
                break;
            case "BOOKING_CANCELLED":
                BookingResponse bookingCancelledPayload = objectMapper.readValue(event.getPayload(), BookingResponse.class);
                emailService.sendGenericEmail(
                        recipient,
                        "Booking Cancelled: " + bookingCancelledPayload.bookableItemName(),
                        String.format("Dear %s,\n\nYour booking for '%s' on %s has been successfully cancelled. Booking ID: %d\n\nWe hope to see you again soon.",
                                bookingCancelledPayload.username(),
                                bookingCancelledPayload.bookableItemName(),
                                bookingCancelledPayload.bookingDate(),
                                bookingCancelledPayload.id())
                );
                break;
            default:
                logger.warn("Unknown event type: {} for event ID {}. Skipping processing.", event.getEventType(), event.getId());
                break;
        }
    }
}
