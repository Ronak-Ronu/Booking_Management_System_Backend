package com.ronak.welcome.DTO;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String description,
        LocalDateTime eventDate,
        String location,
        Long organizerId,
        String organizerUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}