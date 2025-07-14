package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.AvailableSlotResponse;
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
import com.ronak.welcome.repository.UserRepository; // Needed for role checks
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final BookableItemRepository bookableItemRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository; // Inject UserRepository for role checks

    public AvailabilityService(BookableItemRepository bookableItemRepository,
                               BookingRepository bookingRepository,
                               UserRepository userRepository) {
        this.bookableItemRepository = bookableItemRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Calculates available time slots for a specific BookableItem within a given date range.
     * This method handles different logic based on BookableItemType.
     *
     * @param bookableItemId The ID of the BookableItem.
     * @param startDate The start date for the availability check.
     * @param endDate The end date for the availability check.
     * @param currentUsername The username of the currently authenticated user (for private item access).
     * @return A list of AvailableSlotResponse.
     */
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(
            Long bookableItemId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String currentUsername) {

        BookableItem bookableItem = bookableItemRepository.findById(bookableItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + bookableItemId));

        // Check for private item access
        if (bookableItem.isPrivate()) {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

            if (!bookableItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
                throw new SecurityException("You are not authorized to view availability for this private item.");
            }
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        // Logic based on BookableItemType
        if (bookableItem.getType() == BookableItemType.EVENT || bookableItem.getType() == BookableItemType.CLASS) {
            // For events/classes, availability is usually about capacity, not time slots within the event.
            // We'll return a single "slot" representing the event itself if it's not fully booked.
            long currentBookings = bookingRepository.findByBookableItem(bookableItem).stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                    .count();

            if (currentBookings < bookableItem.getCapacity()) {
                // Return the event's own time range as an available slot
                availableSlots.add(new AvailableSlotResponse(
                        bookableItem.getStartTime(),
                        bookableItem.getEndTime(),
                        bookableItem.getId(),
                        bookableItem.getName()
                ));
            }
        } else if (bookableItem.getType() == BookableItemType.APPOINTMENT ||
                bookableItem.getType() == BookableItemType.RESOURCE ||
                bookableItem.getType() == BookableItemType.SERVICE) {
            // For time-based bookable items, generate slots and check for conflicts.
            // Assuming a default slot duration for these types. You might make this configurable per BookableItem.
            Duration slotDuration = Duration.ofMinutes(60); // Example: 60-minute slots
            Duration bufferTime = Duration.ofMinutes(15);   // Example: 15-minute buffer between bookings

            LocalDateTime currentSlotStart = startDate;
            while (currentSlotStart.isBefore(endDate)) {
                LocalDateTime currentSlotEnd = currentSlotStart.plus(slotDuration);

                // Ensure the slot doesn't go beyond the item's defined endTime (if it has one)
                if (bookableItem.getEndTime() != null && currentSlotEnd.isAfter(bookableItem.getEndTime())) {
                    break; // Stop if the slot extends past the item's overall availability
                }

                // Check for overlapping bookings for this specific slot
                List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                        bookableItem,
                        currentSlotStart,
                        currentSlotEnd
                );

                if (overlappingBookings.isEmpty()) {
                    // If no overlaps, this slot is available
                    availableSlots.add(new AvailableSlotResponse(
                            currentSlotStart,
                            currentSlotEnd,
                            bookableItem.getId(),
                            bookableItem.getName()
                    ));
                }

                // Move to the next slot, considering buffer time
                currentSlotStart = currentSlotEnd.plus(bufferTime);
            }
        }
        // Add more else if blocks for other BookableItemTypes as needed

        return availableSlots;
    }
}
