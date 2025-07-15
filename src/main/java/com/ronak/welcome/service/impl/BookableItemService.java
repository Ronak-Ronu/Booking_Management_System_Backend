// src/main/java/com/ronak/welcome/service/impl/BookableItemService.java
package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.BookableItemRequest;
import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.DTO.PriceTier;
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.Event;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.enums.Role;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.exception.ValidationException;
import com.ronak.welcome.repository.BookableItemRepository;
import com.ronak.welcome.repository.BookingRepository;
import com.ronak.welcome.repository.UserRepository;
import com.ronak.welcome.util.BookableItemSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookableItemService {

    private final BookableItemRepository bookableItemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

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

        switch (request.type()) {
            case EVENT:
                Event event = new Event();
                event.setEventSpecificField(request.eventSpecificField());
                bookableItem = event;
                break;
            case APPOINTMENT:
            case RESOURCE:
            case CLASS:
            case SERVICE:
                bookableItem = new BookableItem();
                break;
            default:
                throw new ValidationException("Unsupported BookableItemType for creation: " + request.type());
        }

        bookableItem.setName(request.name());
        bookableItem.setDescription(request.description());
        bookableItem.setStartTime(request.startTime());
        bookableItem.setEndTime(request.endTime());
        bookableItem.setLocation(request.location());
        bookableItem.setCapacity(request.capacity());
        bookableItem.setPrice(request.price());
        bookableItem.setType(request.type());
        bookableItem.setProvider(provider);
        bookableItem.setPrivate(request.isPrivate());
        bookableItem.setPriceTiers(request.priceTiers());

        BookableItem savedItem = bookableItemRepository.save(bookableItem);
        return mapToBookableItemResponse(savedItem);
    }

    @Transactional(readOnly = true)
    public BookableItemResponse getBookableItemById(Long id, String currentUsername) {
        BookableItem item = bookableItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bookable item not found with ID: " + id));

        if (item.isPrivate()) {
            User currentUser = null; // Declare here
            if (currentUsername != null && !currentUsername.isEmpty()) {
                currentUser = userRepository.findByUsername(currentUsername).orElse(null);
            }

            if (currentUser == null || (!item.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN))) {
                throw new SecurityException("You are not authorized to view this private item.");
            }
        }
        return mapToBookableItemResponse(item);
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> getAllBookableItems(String currentUsername) {
        User currentUser = null;
        if (currentUsername != null && !currentUsername.isEmpty()) {
            currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        }

        Specification<BookableItem> spec = null;

        // --- FIX STARTS HERE ---
        final User finalCurrentUser = currentUser; // Create an effectively final copy

        // Apply private item visibility rules
        if (finalCurrentUser == null || !finalCurrentUser.getRoles().contains(Role.ADMIN)) {
            Specification<BookableItem> visibilitySpec = BookableItemSpecifications.isNotPrivate();
            if (finalCurrentUser != null) {
                visibilitySpec = visibilitySpec.or((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("provider"), finalCurrentUser));
            }
            spec = visibilitySpec;
        }
        // --- FIX ENDS HERE ---

        return bookableItemRepository.findAll(spec).stream()
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> getBookableItemsByType(BookableItemType type, String currentUsername) {
        User currentUser = null;
        if (currentUsername != null && !currentUsername.isEmpty()) {
            currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        }

        Specification<BookableItem> spec = BookableItemSpecifications.hasType(type);

        // --- FIX STARTS HERE ---
        final User finalCurrentUser = currentUser; // Create an effectively final copy

        if (finalCurrentUser == null || !finalCurrentUser.getRoles().contains(Role.ADMIN)) {
            Specification<BookableItem> visibilitySpec = BookableItemSpecifications.isNotPrivate();
            if (finalCurrentUser != null) {
                visibilitySpec = visibilitySpec.or((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("provider"), finalCurrentUser));
            }
            spec = spec.and(visibilitySpec);
        }
        // --- FIX ENDS HERE ---

        return bookableItemRepository.findAll(spec).stream()
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> searchBookableItems(
            String keywords,
            BookableItemType type,
            String location,
            Double minPrice,
            Double maxPrice,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String sortBy,
            String sortOrder,
            String currentUsername) {

        User currentUser = null;
        if (currentUsername != null && !currentUsername.isEmpty()) {
            currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));
        }

        Specification<BookableItem> finalSpec = null;

        // --- FIX STARTS HERE ---
        final User finalCurrentUser = currentUser; // Create an effectively final copy

        if (finalCurrentUser == null || !finalCurrentUser.getRoles().contains(Role.ADMIN)) {
            Specification<BookableItem> visibilitySpec = BookableItemSpecifications.isNotPrivate();
            if (finalCurrentUser != null) {
                visibilitySpec = visibilitySpec.or((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("provider"), finalCurrentUser));
            }
            finalSpec = visibilitySpec;
        }
        // --- FIX ENDS HERE ---

        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.hasKeywords(keywords));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.hasType(type));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.hasLocation(location));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.hasMinPrice(minPrice));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.hasMaxPrice(maxPrice));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.startsAfter(startDate));
        finalSpec = combineSpecs(finalSpec, BookableItemSpecifications.endsBefore(endDate));

        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }

        List<BookableItem> items = bookableItemRepository.findAll(finalSpec, sort);
        return items.stream()
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    private Specification<BookableItem> combineSpecs(Specification<BookableItem> existingSpec, Specification<BookableItem> newSpec) {
        if (newSpec == null) {
            return existingSpec;
        }
        if (existingSpec == null) {
            return newSpec;
        }
        return existingSpec.and(newSpec);
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

        if (request.endTime() != null && request.startTime().isAfter(request.endTime())) {
            throw new ValidationException("Start time cannot be after end time.");
        }

        existingItem.setName(request.name());
        existingItem.setDescription(request.description());
        existingItem.setStartTime(request.startTime());
        existingItem.setEndTime(request.endTime());
        existingItem.setLocation(request.location());
        existingItem.setCapacity(request.capacity());
        existingItem.setPrice(request.price());
        existingItem.setType(request.type());
        existingItem.setPrivate(request.isPrivate());
        existingItem.setPriceTiers(request.priceTiers());

        if (existingItem instanceof Event && request.type() == BookableItemType.EVENT) {
            ((Event) existingItem).setEventSpecificField(request.eventSpecificField());
        } else if (existingItem instanceof Event && request.type() != BookableItemType.EVENT) {
            throw new ValidationException("Cannot change type from EVENT to " + request.type() + " for existing item.");
        } else if (request.type() == BookableItemType.EVENT && !(existingItem instanceof BookableItem)) {
            throw new ValidationException("Cannot change type from " + existingItem.getType() + " to EVENT for existing item.");
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

    public double calculateEffectivePrice(BookableItem bookableItem) {
        if (bookableItem.getPriceTiers() == null || bookableItem.getPriceTiers().isEmpty()) {
            return bookableItem.getPrice();
        }

        LocalDateTime now = LocalDateTime.now();
        long currentBookingsCount = bookingRepository.findByBookableItem(bookableItem).stream()
                .filter(b -> b.getStatus() == com.ronak.welcome.enums.BookingStatus.CONFIRMED || b.getStatus() == com.ronak.welcome.enums.BookingStatus.PENDING)
                .count();

        List<PriceTier> sortedTiers = bookableItem.getPriceTiers().stream()
                .sorted(Comparator
                        .comparing(PriceTier::startDate)
                        .thenComparing(PriceTier::maxQuantity))
                .collect(Collectors.toList());

        for (PriceTier tier : sortedTiers) {
            boolean isTimeValid = (now.isEqual(tier.startDate()) || now.isAfter(tier.startDate())) &&
                    (now.isEqual(tier.endDate()) || now.isBefore(tier.endDate()));

            boolean isQuantityValid = currentBookingsCount >= tier.minQuantity() &&
                    currentBookingsCount < tier.maxQuantity();

            if (isTimeValid && isQuantityValid) {
                return tier.price();
            }
        }
        return bookableItem.getPrice();
    }

    private BookableItemResponse mapToBookableItemResponse(BookableItem item) {
        String eventSpecificField = null;
        if (item instanceof Event) {
            eventSpecificField = ((Event) item).getEventSpecificField();
        }

        double effectivePrice = calculateEffectivePrice(item);

        return new BookableItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getStartTime(),
                item.getEndTime(),
                item.getLocation(),
                item.getCapacity(),
                effectivePrice,
                item.getType(),
                item.getProvider().getId(),
                item.getProvider().getUsername(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                eventSpecificField,
                item.isPrivate(),
                item.getPriceTiers()
        );
    }
}
