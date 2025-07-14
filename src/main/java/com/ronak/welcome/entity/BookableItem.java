// src/main/java/com/ronak/welcome/entity/BookableItem.java
package com.ronak.welcome.entity;

import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.util.JsonListConverter; // Import the converter
import com.ronak.welcome.DTO.PriceTier; // Import PriceTier DTO
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List; // For the list of price tiers

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@Table(name = "bookable_items")
public abstract class BookableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column // Optional: End time for services/appointments
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private double price; // This will now be the default/base price

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookableItemType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isPrivate = false;

    // NEW FIELD: List of pricing tiers, stored as JSON
    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "JSON") // Use JSON column type for MySQL 5.7+
    private List<PriceTier> priceTiers;
}
