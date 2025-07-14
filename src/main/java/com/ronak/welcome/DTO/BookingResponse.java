package com.ronak.welcome.DTO;

import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.enums.BookingStatus;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        String username,
        Long bookableItemId,
        String bookableItemName,
        BookableItemType bookableItemType,
        LocalDateTime bookingDate,
        BookingStatus status
) {}
