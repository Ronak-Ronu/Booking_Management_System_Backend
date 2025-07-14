// src/main/java/com/ronak/welcome/service/impl/BookingService.java
package com.ronak.welcome.service.impl; // Assuming this is the correct package based on your input

import com.ronak.welcome.DTO.BookingRequest;
import com.ronak.welcome.DTO.BookingResponse;
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.Booking;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookingStatus;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.ValidationException;
import com.ronak.welcome.repository.BookableItemRepository;
import com.ronak.welcome.repository.BookingRepository;
import com.ronak.welcome.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ADD THIS IMPORT (Spring's @Transactional)

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookableItemRepository bookableItemRepository;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          BookableItemRepository bookableItemRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookableItemRepository = bookableItemRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        BookableItem bookableItem = bookableItemRepository.findById(request.bookableItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + request.bookableItemId()));

        // Add capacity check
        long currentBookings = bookingRepository.findByBookableItem(bookableItem).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .count();
        if (currentBookings >= bookableItem.getCapacity()) {
            throw new ValidationException("Bookable item '" + bookableItem.getName() + "' is fully booked.");
        }

        if (bookingRepository.findByUserAndBookableItem(user, bookableItem).isPresent()) {
            throw new ValidationException("User " + username + " is already booked for item " + bookableItem.getName());
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookableItem(bookableItem);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToBookingResponse(savedBooking);
    }

    @Transactional(readOnly = true) // This is now valid with Spring's @Transactional
    public List<BookingResponse> getUserBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return bookingRepository.findByUser(user).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true) // This is now valid with Spring's @Transactional
    public List<BookingResponse> getBookingsByBookableItemId(Long bookableItemId, String currentUsername) {
        BookableItem bookableItem = bookableItemRepository.findById(bookableItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + bookableItemId));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        // Authorization check: Only the provider of the item or an ADMIN can view all bookings for an item
        if (!bookableItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(com.ronak.welcome.enums.Role.ADMIN)) {
            throw new SecurityException("You are not authorized to view all bookings for this item.");
        }

        return bookingRepository.findByBookableItem(bookableItem).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Authorization: Only the user who made the booking or an ADMIN can cancel
        if (!booking.getUser().getId().equals(user.getId()) && !user.getRoles().contains(com.ronak.welcome.enums.Role.ADMIN)) {
            throw new SecurityException("You are not authorized to cancel this booking.");
        }

        // Prevent cancelling already cancelled/completed bookings
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ValidationException("Booking cannot be cancelled as it is already " + booking.getStatus().name());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getUsername(),
                booking.getBookableItem().getId(),
                booking.getBookableItem().getName(),
                booking.getBookableItem().getType(),
                booking.getBookingDate(),
                booking.getStatus()
        );
    }
}
