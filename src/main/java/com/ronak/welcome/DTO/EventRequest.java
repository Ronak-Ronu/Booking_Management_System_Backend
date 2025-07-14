// src/main/java/com/ronak/welcome/DTO/EventRequest.java
package com.ronak.welcome.DTO;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

// This DTO is for creating/updating an Event, which is a type of BookableItem.
// It can contain event-specific fields in addition to bookable item fields.
public record EventRequest(
        @NotBlank(message = "Event name cannot be blank")
        String name,

        String description,

        @NotNull(message = "Event date cannot be null")
        @FutureOrPresent(message = "Event date must be in the present or future")
        LocalDateTime eventDate, // Maps to startTime in BookableItem

        LocalDateTime endTime, // Optional, maps to endTime in BookableItem

        @NotBlank(message = "Location cannot be blank")
        String location,

        @Min(value = 1, message = "Capacity must be at least 1")
        int capacity,

        @Min(value = 0, message = "Price cannot be negative")
        double price,

        String eventSpecificField // Example: a field specific to Event, not all BookableItems
) {}