package com.ronak.welcome.DTO;

import java.time.LocalDateTime;

public record AvailableSlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long bookableItemId,
        String bookableItemName
) {}
