// src/main/java/com/ronak/welcome/entity/OutboxEvent.java
package com.ronak.welcome.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp; // Keep UpdateTimestamp for auditing

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // e.g., "USER_CREATED", "BOOKING_CONFIRMED", "BOOKING_CANCELLED"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload; // JSON string of the event data (e.g., UserResponse, BookingResponse)

    @Column // Can be null if the event doesn't require an email recipient
    private String recipientEmail; // The email address to send notifications to

    @Column(nullable = false)
    private String status; // e.g., "PENDING", "PROCESSING", "COMPLETED", "FAILED"

    @Column(nullable = false)
    private int retryCount = 0; // Number of times processing has been attempted

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // Keep UpdateTimestamp for auditing changes to status/retryCount
    @Column(nullable = false)
    private LocalDateTime updatedAt; // When the event was last updated (status, retryCount)

    @Column
    private LocalDateTime processedAt; // When the event was successfully processed

    @Column
    private String errorMessage; // Store error message if processing fails

    // Constructor for events with a specific recipient email
    public OutboxEvent(String eventType, String payload, String recipientEmail) {
        this.eventType = eventType;
        this.payload = payload;
        this.recipientEmail = recipientEmail;
        this.status = "PENDING";
        this.retryCount = 0;
    }

    // Constructor for events without a specific email recipient (e.g., for internal processing)
    public OutboxEvent(String eventType, String payload) {
        this(eventType, payload, null); // Call the other constructor with null recipientEmail
    }
}
