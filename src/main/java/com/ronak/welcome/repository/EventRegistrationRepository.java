// src/main/java/com/ronak/welcome/repository/EventRegistrationRepository.java
package com.ronak.welcome.repository;

import com.ronak.welcome.entity.EventRegistration;
import com.ronak.welcome.entity.Event; // Make sure this points to your Event entity
import com.ronak.welcome.entity.User;   // Make sure this points to your User entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    /**
     * Finds all event registrations for a specific user.
     * @param user The user entity.
     * @return A list of EventRegistration entities.
     */
    List<EventRegistration> findByUser(User user);

    /**
     * Finds all event registrations for a specific event.
     * @param event The event entity.
     * @return A list of EventRegistration entities.
     */
    List<EventRegistration> findByEvent(Event event);

    /**
     * Finds an event registration by both user and event.
     * Used to check for duplicate registrations.
     * @param user The user entity.
     * @param event The event entity.
     * @return An Optional containing the EventRegistration if found, empty otherwise.
     */
    Optional<EventRegistration> findByUserAndEvent(User user, Event event);
}
