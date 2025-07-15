// src/main/java/com/ronak/welcome/service/BookingService.java
package com.ronak.welcome.service.impl; // Changed package to service

import com.ronak.welcome.DTO.BookingRequest;
import com.ronak.welcome.DTO.BookingResponse;
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.Booking;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.enums.BookingStatus;
import com.ronak.welcome.enums.Role;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.ValidationException;
import com.ronak.welcome.repository.BookableItemRepository;
import com.ronak.welcome.repository.BookingRepository;
import com.ronak.welcome.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 1. Prevent duplicate bookings for the same user and item
        if (bookingRepository.findByUserAndBookableItem(user, bookableItem).isPresent()) {
            throw new ValidationException("User " + username + " is already booked for item " + bookableItem.getName());
        }

        // 2. Capacity-based conflict detection (for items with limited spots like events/classes)
        // This check is relevant if the booking consumes a 'slot' from the item's total capacity.
        // It's generally applicable to all BookableItemTypes that have a finite 'capacity'.
        long currentConfirmedOrPendingBookings = bookingRepository.findByBookableItem(bookableItem).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .count();
        if (currentConfirmedOrPendingBookings >= bookableItem.getCapacity()) {
            throw new ValidationException("Bookable item '" + bookableItem.getName() + "' is fully booked.");
        }

        // 3. Time-based conflict detection (for items that occupy a specific time slot, like appointments/resources)
        // This check is crucial if the bookable item itself represents a single, time-bound resource.
        // For events where multiple people can book the same time, this check might not be needed.
        // We'll apply it if the BookableItem has a defined endTime.
        if (bookableItem.getEndTime() != null) { // Only perform time-based check if item has an end time
            List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                    bookableItem,
                    bookableItem.getStartTime(), // Use the item's start time as the booking's desired start
                    bookableItem.getEndTime()    // Use the item's end time as the booking's desired end
            );

            // If there's already a confirmed/pending booking for this time-bound item, it means it's occupied.
            // This assumes a BookableItem with an endTime can only be booked by one user at a time.
            if (!overlappingBookings.isEmpty()) {
                throw new ValidationException("Bookable item '" + bookableItem.getName() + "' is already booked for the requested time slot.");
            }
        }


        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookableItem(bookableItem);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToBookingResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return bookingRepository.findByUser(user).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByBookableItemId(Long bookableItemId, String currentUsername) {
        BookableItem bookableItem = bookableItemRepository.findById(bookableItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + bookableItemId));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        if (!bookableItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
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

        if (!booking.getUser().getId().equals(user.getId()) && !user.getRoles().contains(Role.ADMIN)) {
            throw new SecurityException("You are not authorized to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ValidationException("Booking cannot be cancelled as it is already " + booking.getStatus().name());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
    @Transactional(readOnly = true)
    public boolean isUserBooking(Long bookingId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return bookingRepository.findById(bookingId)
                .map(booking -> booking.getUser().getId().equals(user.getId()))
                .orElse(false);
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        String eventSpecificField = null;
        if (booking.getBookableItem() instanceof com.ronak.welcome.entity.Event) { // Fully qualified name
            eventSpecificField = ((com.ronak.welcome.entity.Event) booking.getBookableItem()).getEventSpecificField();
        }

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
