// src/main/java/com/ronak/welcome/DTO/BookableItemRequest.java
package com.ronak.welcome.DTO;

import com.ronak.welcome.enums.BookableItemType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public record BookableItemRequest(
        @NotBlank(message = "Name cannot be blank")
        String name,

        String description,

        @NotNull(message = "Start time cannot be null")
        @FutureOrPresent(message = "Start time must be in the present or future")
        LocalDateTime startTime,

        LocalDateTime endTime, // Optional

        @NotBlank(message = "Location cannot be blank")
        String location,

        @Min(value = 1, message = "Capacity must be at least 1")
        int capacity,

        @Min(value = 0, message = "Price cannot be negative")
        double price,

        @NotNull(message = "Bookable item type cannot be null")
        BookableItemType type,

        String eventSpecificField,
        boolean isPrivate
) {}
