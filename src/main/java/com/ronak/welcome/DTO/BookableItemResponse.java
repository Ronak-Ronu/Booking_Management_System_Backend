// src/main/java/com/ronak/welcome/DTO/BookableItemResponse.java
package com.ronak.welcome.DTO;

import com.ronak.welcome.enums.BookableItemType;
import java.time.LocalDateTime;
import java.util.List; // For the list of price tiers

public record BookableItemResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        int capacity,
        double price, // Base price
        BookableItemType type,
        Long providerId,
        String providerUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String eventSpecificField,
        boolean isPrivate,
        List<PriceTier> priceTiers // NEW FIELD: List of pricing tiers
) {}
