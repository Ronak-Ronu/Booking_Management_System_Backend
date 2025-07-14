package com.ronak.welcome.DTO;

import com.ronak.welcome.enums.BookableItemType;
import java.time.LocalDateTime;

public record BookableItemResponse(
        Long id,
        String name,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        int capacity,
        double price,
        BookableItemType type,
        Long providerId,
        String providerUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String eventSpecificField,
        boolean isPrivate
) {}
