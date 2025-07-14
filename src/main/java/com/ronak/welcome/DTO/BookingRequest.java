package com.ronak.welcome.DTO;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "Bookable item ID cannot be null")
        Long bookableItemId
) {}
