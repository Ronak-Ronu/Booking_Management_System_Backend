package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.AvailableSlotResponse;
import com.ronak.welcome.service.impl.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    /**
     * Retrieves available time slots for a specific bookable item within a given date range.
     * This endpoint is publicly accessible, but the AvailabilityService will enforce
     * access rules for private items.
     *
     * @param bookableItemId The ID of the bookable item.
     * @param startDate The start date for the availability search (format: YYYY-MM-DDTHH:MM:SS).
     * @param endDate The end date for the availability search (format: YYYY-MM-DDTHH:MM:SS).
     * @return A list of available slots.
     */
    @GetMapping("/items/{bookableItemId}")
    // This endpoint is publicly accessible, as the service layer handles private item visibility
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @PathVariable Long bookableItemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // Get the current authenticated username. It might be anonymous if no one is logged in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName()))
                ? authentication.getName() : null;

        List<AvailableSlotResponse> availableSlots = availabilityService.getAvailableSlots(
                bookableItemId,
                startDate,
                endDate,
                currentUsername
        );
        return ResponseEntity.ok(availableSlots);
    }
}
