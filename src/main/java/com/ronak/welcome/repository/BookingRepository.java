package com.ronak.welcome.repository;


import com.ronak.welcome.entity.Booking; // Ensure this import is correct (your renamed entity)
import com.ronak.welcome.entity.BookableItem; // Ensure this import is correct (the new generic item)
import com.ronak.welcome.entity.User;   // Ensure this import is correct
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    /**
     * Finds all bookings made by a specific user.
     * @param user The user entity.
     * @return A list of Booking entities.
     */
    List<Booking> findByUser(User user);

    /**
     * Finds all bookings for a specific bookable item.
     * @param bookableItem The bookable item entity.
     * @return A list of Booking entities.
     */
    List<Booking> findByBookableItem(BookableItem bookableItem);

    /**
     * Finds a specific booking by both user and bookable item.
     * Used to check for duplicate bookings.
     * @param user The user entity.
     * @param bookableItem The bookable item entity.
     * @return An Optional containing the Booking if found, empty otherwise.
     */
    Optional<Booking> findByUserAndBookableItem(User user, BookableItem bookableItem);
}

