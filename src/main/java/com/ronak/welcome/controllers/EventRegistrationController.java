package com.ronak.welcome.controllers;



import com.ronak.welcome.DTO.EventRegistrationResponse;
import com.ronak.welcome.service.impl.EventRegistrationService;
import com.ronak.welcome.service.impl.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class EventRegistrationController {

    private final EventRegistrationService registrationService;
    private final EventService eventService;

    @PostMapping("/events/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(@PathVariable Long eventId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Get username of the logged-in user
        EventRegistrationResponse registration = registrationService.registerForEvent(eventId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EventRegistrationResponse>> getUserRegistrations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<EventRegistrationResponse> registrations = registrationService.getUserRegistrations(username);
        return ResponseEntity.ok(registrations);
    }


    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ADMIN') or @eventService.getEventById(#eventId).organizerUsername == authentication.name")
    public ResponseEntity<List<EventRegistrationResponse>> getEventRegistrations(@PathVariable Long eventId) {
        List<EventRegistrationResponse> registrations = registrationService.getEventRegistrations(eventId);
        return ResponseEntity.ok(registrations);
    }

    @DeleteMapping("/events/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unregisterFromEvent(@PathVariable Long eventId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        registrationService.unregisterFromEvent(eventId, username);
        return ResponseEntity.noContent().build();
    }
}
