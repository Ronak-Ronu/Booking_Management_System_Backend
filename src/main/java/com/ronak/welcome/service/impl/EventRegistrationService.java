// src/main/java/com/ronak/welcome/service/impl/EventRegistrationService.java
package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.EventRegistrationResponse;
import com.ronak.welcome.entity.Event;
import com.ronak.welcome.entity.EventRegistration;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.EventStatus;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.ValidationException;
import com.ronak.welcome.repository.EventRegistrationRepository;
import com.ronak.welcome.repository.EventRepository;
import com.ronak.welcome.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public EventRegistrationService(EventRegistrationRepository registrationRepository,
                                    UserRepository userRepository,
                                    EventRepository eventRepository) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventRegistrationResponse registerForEvent(Long eventId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (registrationRepository.findByUserAndEvent(user, event).isPresent()) {
            throw new ValidationException("User " + username + " is already registered for event " + event.getName());
        }

        EventRegistration registration = new EventRegistration();
        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(EventStatus.REGISTERED);

        EventRegistration savedRegistration = registrationRepository.save(registration);
        return mapToEventRegistrationResponse(savedRegistration);
    }

    @Transactional // ADDED: Ensures lazy-loaded user and event are accessible
    public List<EventRegistrationResponse> getUserRegistrations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        // Accessing registration.getUser() and registration.getEvent() here now safe
        return registrationRepository.findByUser(user).stream()
                .map(this::mapToEventRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<EventRegistrationResponse> getEventRegistrations(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));
        // Accessing registration.getUser() and registration.getEvent() here now safe
        return registrationRepository.findByEvent(event).stream()
                .map(this::mapToEventRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void unregisterFromEvent(Long eventId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        EventRegistration registration = registrationRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ResourceNotFoundException("User is not registered for this event."));

        registrationRepository.delete(registration);
    }

    private EventRegistrationResponse mapToEventRegistrationResponse(EventRegistration registration) {
        return new EventRegistrationResponse(
                registration.getId(),
                registration.getUser().getId(),
                registration.getUser().getUsername(),
                registration.getEvent().getId(),
                registration.getEvent().getName(),  // Accessing user data
                registration.getRegistrationDate(),
                registration.getStatus().name()
        );
    }
}
