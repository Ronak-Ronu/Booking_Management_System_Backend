// src/main/java/com/ronak/welcome/service/BookableItemService.java
package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.BookableItemRequest;
import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.DTO.PriceTier; // Import PriceTier
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.Event;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.enums.Role;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.ValidationException;
import com.ronak.welcome.repository.BookableItemRepository;
import com.ronak.welcome.repository.BookingRepository; // Needed for current booking count
import com.ronak.welcome.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookableItemService {

    private final BookableItemRepository bookableItemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository; // Inject BookingRepository for price calculation

    public BookableItemService(BookableItemRepository bookableItemRepository,
                               UserRepository userRepository,
                               BookingRepository bookingRepository) {
        this.bookableItemRepository = bookableItemRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public BookableItemResponse createBookableItem(BookableItemRequest request, String providerUsername) {
        User provider = userRepository.findByUsername(providerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerUsername));

        if (!provider.getRoles().contains(Role.ADMIN) && !provider.getRoles().contains(Role.EVENT_ORGANIZER)) {
            throw new SecurityException("Only ADMINs or EVENT_ORGANIZERs can create bookable items.");
        }

        if (request.endTime() != null && request.startTime().isAfter(request.endTime())) {
            throw new ValidationException("Start time cannot be after end time.");
        }

        BookableItem bookableItem;

        if (request.type() == BookableItemType.EVENT) {
            Event event = new Event();
            event.setEventSpecificField(request.eventSpecificField());
            bookableItem = event;
        } else {
            throw new ValidationException("Unsupported or unhandled BookableItemType for creation: " + request.type());
        }

        bookableItem.setName(request.name());
        bookableItem.setDescription(request.description());
        bookableItem.setStartTime(request.startTime());
        bookableItem.setEndTime(request.endTime());
        bookableItem.setLocation(request.location());
        bookableItem.setCapacity(request.capacity());
        bookableItem.setPrice(request.price()); // Base price
        bookableItem.setType(request.type());
        bookableItem.setProvider(provider);
        bookableItem.setPrivate(request.isPrivate());
        bookableItem.setPriceTiers(request.priceTiers()); // SET THE PRICE TIERS

        BookableItem savedItem = bookableItemRepository.save(bookableItem);
        return mapToBookableItemResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public BookableItemResponse getBookableItemById(Long id, String currentUsername) {
        BookableItem item = bookableItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + id));

        if (item.isPrivate()) {
            if (currentUsername == null || currentUsername.isEmpty() ||
                    (!item.getProvider().getUsername().equals(currentUsername) &&
                            !userRepository.findByUsername(currentUsername).map(u -> u.getRoles().contains(Role.ADMIN)).orElse(false))) {
                throw new SecurityException("You are not authorized to view this private item.");
            }
        }
        return mapToBookableItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> getAllBookableItems(String currentUsername) {
        User currentUser = currentUsername != null && !currentUsername.isEmpty() ?
                userRepository.findByUsername(currentUsername).orElse(null) :
                null;

        return bookableItemRepository.findAll().stream()
                .filter(item -> {
                    if (item.isPrivate()) {
                        return currentUser != null && (item.getProvider().getId().equals(currentUser.getId()) || currentUser.getRoles().contains(Role.ADMIN));
                    }
                    return true;
                })
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> getBookableItemsByType(BookableItemType type, String currentUsername) {
        User currentUser = currentUsername != null && !currentUsername.isEmpty() ?
                userRepository.findByUsername(currentUsername).orElse(null) :
                null;

        return bookableItemRepository.findByType(type).stream()
                .filter(item -> {
                    if (item.isPrivate()) {
                        return currentUser != null && (item.getProvider().getId().equals(currentUser.getId()) || currentUser.getRoles().contains(Role.ADMIN));
                    }
                    return true;
                })
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookableItemResponse updateBookableItem(Long id, BookableItemRequest request, String currentUsername) {
        BookableItem existingItem = bookableItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + id));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        if (!existingItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new SecurityException("You are not authorized to update this bookable item.");
        }

        if (!currentUser.getRoles().contains(Role.ADMIN) && !currentUser.getRoles().contains(Role.EVENT_ORGANIZER)) {
            throw new SecurityException("Only ADMINs or EVENT_ORGANIZERs can update bookable items.");
        }

        if (request.endTime() != null && request.startTime().isAfter(request.endTime())) {
            throw new ValidationException("Start time cannot be after end time.");
        }

        existingItem.setName(request.name());
        existingItem.setDescription(request.description());
        existingItem.setStartTime(request.startTime());
        existingItem.setEndTime(request.endTime());
        existingItem.setLocation(request.location());
        existingItem.setCapacity(request.capacity());
        existingItem.setPrice(request.price()); // Base price
        existingItem.setType(request.type());
        existingItem.setPrivate(request.isPrivate());
        existingItem.setPriceTiers(request.priceTiers()); // UPDATE THE PRICE TIERS

        if (existingItem instanceof Event) {
            ((Event) existingItem).setEventSpecificField(request.eventSpecificField());
        }

        BookableItem updatedItem = bookableItemRepository.save(existingItem);
        return mapToBookableItemResponse(updatedItem);
    }

    @Transactional
    public void deleteBookableItem(Long id, String currentUsername) {
        BookableItem existingItem = bookableItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + id));

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

        if (!existingItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
            throw new SecurityException("You are not authorized to delete this bookable item.");
        }

        bookableItemRepository.delete(existingItem);
    }

    /**
     * Calculates the effective price of a bookable item based on its price tiers,
     * current time, and current number of bookings.
     *
     * @param bookableItem The BookableItem for which to calculate the price.
     * @return The effective price.
     */
    public double calculateEffectivePrice(BookableItem bookableItem) {
        if (bookableItem.getPriceTiers() == null || bookableItem.getPriceTiers().isEmpty()) {
            return bookableItem.getPrice(); // Return base price if no tiers are defined
        }

        LocalDateTime now = LocalDateTime.now();
        long currentBookingsCount = bookingRepository.findByBookableItem(bookableItem).stream()
                .filter(b -> b.getStatus() == com.ronak.welcome.enums.BookingStatus.CONFIRMED || b.getStatus() == com.ronak.welcome.enums.BookingStatus.PENDING)
                .count();

        // Sort tiers by startDate (earliest first) and then by maxQuantity (smallest first)
        // This ensures that "early bird" tiers are considered first, and then smaller quantity tiers.
        List<PriceTier> sortedTiers = bookableItem.getPriceTiers().stream()
                .sorted(Comparator
                        .comparing(PriceTier::startDate)
                        .thenComparing(PriceTier::maxQuantity))
                .collect(Collectors.toList());

        for (PriceTier tier : sortedTiers) {
            // Check time-based validity
            boolean isTimeValid = (now.isEqual(tier.startDate()) || now.isAfter(tier.startDate())) &&
                    (now.isEqual(tier.endDate()) || now.isBefore(tier.endDate()));

            // Check quantity-based validity
            boolean isQuantityValid = currentBookingsCount >= tier.minQuantity() &&
                    currentBookingsCount < tier.maxQuantity(); // Use < for maxQuantity to define the range for this tier

            if (isTimeValid && isQuantityValid) {
                return tier.price();
            }
        }

        // If no specific tier applies, return the base price
        return bookableItem.getPrice();
    }


    // Helper method to map BookableItem entity to BookableItemResponse DTO
    private BookableItemResponse mapToBookableItemResponse(BookableItem item) {
        String eventSpecificField = null;
        if (item instanceof Event) {
            eventSpecificField = ((Event) item).getEventSpecificField();
        }

        // Calculate the effective price when mapping to response
        double effectivePrice = calculateEffectivePrice(item);

        return new BookableItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getStartTime(),
                item.getEndTime(),
                item.getLocation(),
                item.getCapacity(),
                effectivePrice, // Use the effective price here
                item.getType(),
                item.getProvider().getId(),
                item.getProvider().getUsername(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                eventSpecificField,
                item.isPrivate(),
                item.getPriceTiers() // Include the raw price tiers in the response
        );
    }
}
