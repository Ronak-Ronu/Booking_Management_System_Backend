package com.ronak.welcome.controllers;


import com.ronak.welcome.DTO.EventRequest;
import com.ronak.welcome.DTO.EventResponse;
import com.ronak.welcome.service.impl.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Create a new event (only users with an EVENT_ORGANIZER or ADMIN role)
    @PostMapping
    @PreAuthorize("hasRole('EVENT_ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest eventRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String organizerUsername = authentication.getName(); // Get username of the logged-in organizer
        EventResponse createdEvent = eventService.createEvent(eventRequest, organizerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    // Get event by ID (publicly accessible)
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    // Get all events (publicly accessible)
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    // Update an event (only the organizer who created it OR ADMIN)
    @PutMapping("/{id}")
    // @eventService.getEventById(#id).organizerUsername == authentication.name ensures only the creator can update
    @PreAuthorize("hasRole('ADMIN') or @eventService.getEventById(#id).organizerUsername == authentication.name")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest eventRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        EventResponse updatedEvent = eventService.updateEvent(id, eventRequest, currentUsername);
        return ResponseEntity.ok(updatedEvent);
    }

    // Delete an event (only the organizer who created it OR ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @eventService.getEventById(#id).organizerUsername == authentication.name")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        eventService.deleteEvent(id, currentUsername);
        return ResponseEntity.noContent().build();
    }
}
