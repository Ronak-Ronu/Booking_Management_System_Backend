package com.ronak.welcome.DTO;

import java.time.LocalDateTime;

public record EventRegistrationResponse(
        Long id, Long userId, String username, Long eventId,
        String eventName, LocalDateTime registrationDate, String status
) {}