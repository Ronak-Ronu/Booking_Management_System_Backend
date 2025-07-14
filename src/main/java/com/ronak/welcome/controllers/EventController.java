// src/main/java/com/ronak/welcome/controllers/EventController.java
package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.BookableItemRequest;
import com.ronak.welcome.DTO.BookableItemResponse;
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

    @PostMapping
    @PreAuthorize("hasRole('EVENT_ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<BookableItemResponse> createEvent(@Valid @RequestBody BookableItemRequest eventRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String organizerUsername = authentication.getName();
        BookableItemResponse createdEvent = eventService.createEvent(eventRequest, organizerUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    // Get event by ID (publicly accessible, but service will enforce private access)
    @GetMapping("/{id}")
    public ResponseEntity<BookableItemResponse> getEventById(@PathVariable Long id) {
        // The service layer will handle whether the current user can view this private item
        BookableItemResponse event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    // Get all events (publicly accessible, but service will filter private items)
    @GetMapping
    public ResponseEntity<List<BookableItemResponse>> getAllEvents() {
        // The service layer will filter private events based on the current user's roles
        List<BookableItemResponse> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @eventService.getEventById(#id).providerUsername() == authentication.name")
    public ResponseEntity<BookableItemResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody BookableItemRequest eventRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        BookableItemResponse updatedEvent = eventService.updateEvent(id, eventRequest, currentUsername);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @eventService.getEventById(#id).providerUsername() == authentication.name")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        eventService.deleteEvent(id, currentUsername);
        return ResponseEntity.noContent().build();
    }
}
