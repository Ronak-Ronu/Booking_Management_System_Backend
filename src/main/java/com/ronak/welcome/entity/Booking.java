// src/main/java/com/ronak/welcome/entity/Booking.java
package com.ronak.welcome.entity;

import com.ronak.welcome.enums.BookingStatus; // Use new BookingStatus enum
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "bookings", uniqueConstraints = { // Renamed table
        @UniqueConstraint(columnNames = {"user_id", "bookable_item_id"}) // Updated unique constraint
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookable_item_id", nullable = false) // Changed to bookable_item_id
    private BookableItem bookableItem; // Changed to BookableItem

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime bookingDate; // Renamed from registrationDate

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // Changed to BookingStatus enum
}
