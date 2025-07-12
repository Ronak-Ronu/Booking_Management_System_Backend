package com.ronak.welcome.service.impl;

import com.ronak.welcome.entity.OutboxEvent;
import com.ronak.welcome.repository.OutboxEventRepository;
import com.ronak.welcome.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxEventProcessor {
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final int MAX_RETRIES = 5;
    private final OutboxEventRepository outboxEventRepository;
    private final EmailService emailService;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository, EmailService emailService) {
        this.outboxEventRepository = outboxEventRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processPendingEvents() {
        logger.info("Processing pending outbox events...");
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusIn(List.of("PENDING"));
        for (OutboxEvent event : pendingEvents) {
            try {
                if ("USER_CREATED".equals(event.getEventType())) {
                    logger.info("Sending welcome email for user: {}", event.getUsername());
                    emailService.sendWelcomeMessage(event.getEmail(), event.getUsername());
                    event.setStatus("PROCESSED");
                }
                outboxEventRepository.save(event);
            } catch (Exception e) {
                logger.error("Failed to process event ID {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                event.setStatus(event.getRetryCount() >= MAX_RETRIES ? "FAILED" : "PENDING");
                outboxEventRepository.save(event);
            }
        }
        logger.info("Finished processing {} pending events", pendingEvents.size());
    }
}