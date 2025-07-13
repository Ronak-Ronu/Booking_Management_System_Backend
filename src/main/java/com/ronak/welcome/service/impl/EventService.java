package com.ronak.welcome.service.impl;


import com.ronak.welcome.DTO.EventRequest;
import com.ronak.welcome.DTO.EventResponse;
import com.ronak.welcome.entity.Event;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.Role;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.repository.EventRepository;
import com.ronak.welcome.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse createEvent(EventRequest eventRequest, String organizerUsername) {
        User organizer = userRepository.findByUsername(organizerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found: " + organizerUsername));

        Event event = new Event();
        event.setName(eventRequest.name());
        event.setDescription(eventRequest.description());
        event.setEventDate(eventRequest.eventDate());
        event.setLocation(eventRequest.location());
        event.setOrganizer(organizer);

        Event savedEvent = eventRepository.save(event);
        return mapToEventResponse(savedEvent);
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        return mapToEventResponse(event);
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest eventRequest, String currentUsername) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        // Authorization check: Only the organizer or an ADMIN can update the event
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        if (!existingEvent.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new SecurityException("You are not authorized to update this event.");
        }

        existingEvent.setName(eventRequest.name());
        existingEvent.setDescription(eventRequest.description());
        existingEvent.setEventDate(eventRequest.eventDate());
        existingEvent.setLocation(eventRequest.location());

        Event updatedEvent = eventRepository.save(existingEvent);
        return mapToEventResponse(updatedEvent);
    }

    @Transactional
    public void deleteEvent(Long id, String currentUsername) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        // Authorization check: Only the organizer or an ADMIN can delete the event
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        if (!existingEvent.getOrganizer().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new SecurityException("You are not authorized to delete this event.");
        }

        eventRepository.delete(existingEvent);
    }

    // Helper method to map Event entity to EventResponse DTO
    private EventResponse mapToEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer().getId(),
                event.getOrganizer().getUsername(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
