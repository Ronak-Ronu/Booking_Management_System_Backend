package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.DTO.PriceTier;
import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.Event; // Needed for eventSpecificField
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.enums.Role;
import com.ronak.welcome.exception.ResourceNotFoundException;
import com.ronak.welcome.repository.BookableItemRepository;
import com.ronak.welcome.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final BookableItemRepository bookableItemRepository;
    private final UserRepository userRepository; // Needed for private item access checks

    public RecommendationService(BookableItemRepository bookableItemRepository, UserRepository userRepository) {
        this.bookableItemRepository = bookableItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Recommends similar bookable items based on a given item's type and specific fields.
     * This is a content-based recommendation.
     *
     * @param bookableItemId The ID of the item for which to find recommendations.
     * @param limit The maximum number of recommendations to return.
     * @param currentUsername The username of the currently authenticated user (for private item access).
     * @return A list of recommended BookableItemResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<BookableItemResponse> getRecommendationsForBookableItem(
            Long bookableItemId,
            int limit,
            String currentUsername) {

        BookableItem targetItem = bookableItemRepository.findById(bookableItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Target bookable item not found with ID: " + bookableItemId));

        // Ensure the current user has access to the target item (if it's private)
        if (targetItem.isPrivate()) {
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Current user not found: " + currentUsername));

            if (!targetItem.getProvider().getId().equals(currentUser.getId()) && !currentUser.getRoles().contains(Role.ADMIN)) {
                throw new SecurityException("You are not authorized to get recommendations for this private item.");
            }
        }

        // Get all other bookable items
        List<BookableItem> allOtherItems = bookableItemRepository.findAll().stream()
                .filter(item -> !item.getId().equals(bookableItemId)) // Exclude the target item itself
                .collect(Collectors.toList());

        User currentUser = currentUsername != null && !currentUsername.isEmpty() ?
                userRepository.findByUsername(currentUsername).orElse(null) :
                null;

        // Filter and sort based on similarity
        return allOtherItems.stream()
                .filter(item -> {
                    // Filter out private items not accessible by the current user
                    if (item.isPrivate()) {
                        return currentUser != null && (item.getProvider().getId().equals(currentUser.getId()) || currentUser.getRoles().contains(Role.ADMIN));
                    }
                    return true; // Public items are always visible
                })
                .sorted(Comparator
                        .comparing((BookableItem item) -> calculateSimilarity(targetItem, item))
                        .reversed()) // Sort by similarity in descending order
                .limit(limit)
                .map(this::mapToBookableItemResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calculates a simple similarity score between two BookableItems.
     * This can be expanded with more sophisticated algorithms (e.g., TF-IDF, embedding similarity).
     *
     * @param item1 The first bookable item.
     * @param item2 The second bookable item.
     * @return A similarity score (higher is more similar).
     */
    private double calculateSimilarity(BookableItem item1, BookableItem item2) {
        double score = 0.0;


        // 1. Same Type (high importance)
        if (item1.getType() == item2.getType()) {
            score += 10.0; // Strong match for type
        }

        // 2. Event-Specific Field Similarity (if applicable)
        if (item1.getType() == BookableItemType.EVENT && item2.getType() == BookableItemType.EVENT) {
            Event event1 = (Event) item1;
            Event event2 = (Event) item2;

            if (event1.getEventSpecificField() != null && event2.getEventSpecificField() != null) {
                // Simple string equality for now. Could use string similarity (Levenshtein, Jaccard)
                // or keyword matching for more nuanced recommendations.
                if (event1.getEventSpecificField().equalsIgnoreCase(event2.getEventSpecificField())) {
                    score += 5.0; // Medium match for specific field
                }
            }
        }

        // 3. Location Similarity (if applicable and relevant)
        if (item1.getLocation() != null && item2.getLocation() != null) {
            if (item1.getLocation().equalsIgnoreCase(item2.getLocation())) {
                score += 2.0; // Lower importance for location
            }
        }

        // 4. Description Keyword Overlap (can be complex, but simple version here)
        // This is a very basic keyword match. For real-world, use NLP techniques.
        if (item1.getDescription() != null && item2.getDescription() != null) {
            String desc1 = item1.getDescription().toLowerCase();
            String desc2 = item2.getDescription().toLowerCase();
            String[] keywords1 = desc1.split("\\s+");
            String[] keywords2 = desc2.split("\\s+");

            long commonKeywords = java.util.Arrays.stream(keywords1)
                    .filter(keyword -> java.util.Arrays.asList(keywords2).contains(keyword))
                    .count();
            score += commonKeywords * 0.5; // Small bonus for common keywords
        }

        return score;
    }

    // Helper method to map BookableItem entity to BookableItemResponse DTO
    // (Copied from BookableItemService to avoid circular dependency issues if not careful,
    // or you can create a dedicated Mapper class)
    private BookableItemResponse mapToBookableItemResponse(BookableItem item) {
        String eventSpecificField = null;
        if (item instanceof Event) {
            eventSpecificField = ((Event) item).getEventSpecificField();
        }

        return new BookableItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getStartTime(),
                item.getEndTime(),
                item.getLocation(),
                item.getCapacity(),
                item.getPrice(),
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
