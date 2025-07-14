// src/main/java/com/ronak/welcome/repository/BookingRepository.java
package com.ronak.welcome.repository;

import com.ronak.welcome.entity.Booking;
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookingStatus; // Make sure this is imported
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);
    List<Booking> findByBookableItem(BookableItem bookableItem);
    Optional<Booking> findByUserAndBookableItem(User user, BookableItem bookableItem);

    @Query("SELECT b FROM Booking b WHERE b.bookableItem = :bookableItem " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " + // Only consider confirmed or pending bookings
            "AND (" +
            "   (b.bookableItem.startTime < :newBookingEndTime AND b.bookableItem.endTime > :newBookingStartTime)" + // Overlap condition
            "OR (b.bookableItem.startTime = :newBookingStartTime AND b.bookableItem.endTime = :newBookingEndTime)" + // Exact match
            ")")
    List<Booking> findOverlappingBookings(
            @Param("bookableItem") BookableItem bookableItem,
            @Param("newBookingStartTime") LocalDateTime newBookingStartTime,
            @Param("newBookingEndTime") LocalDateTime newBookingEndTime);

  }
