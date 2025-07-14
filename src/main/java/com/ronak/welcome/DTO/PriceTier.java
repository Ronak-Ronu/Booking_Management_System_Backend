package com.ronak.welcome.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record PriceTier(
        @NotBlank(message = "Tier name cannot be blank")
        String name,

        @Min(value = 0, message = "Price cannot be negative")
        double price,

        @NotNull(message = "Start date for tier cannot be null")
        LocalDateTime startDate,

        @NotNull(message = "End date for tier cannot be null")
        LocalDateTime endDate,

        @Min(value = 0, message = "Minimum quantity for tier must be non-negative")
        int minQuantity, // E.g., apply this tier if at least X items are booked

        @Min(value = 1, message = "Maximum quantity for tier must be at least 1")
        int maxQuantity // E.g., apply this tier for the first Y items booked
) {}
