package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.service.impl.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Retrieves recommended bookable items similar to a given item.
     * This endpoint is publicly accessible, but the service will enforce
     * access rules for private items.
     *
     * @param bookableItemId The ID of the bookable item for which to find recommendations.
     * @param limit The maximum number of recommendations to return (default: 5).
     * @return A list of recommended BookableItemResponse DTOs.
     */
    @GetMapping("/for-item/{bookableItemId}")
    public ResponseEntity<List<BookableItemResponse>> getRecommendationsForItem(
            @PathVariable Long bookableItemId,
            @RequestParam(defaultValue = "5") int limit) {

        // Get the current authenticated username. It might be anonymous if no one is logged in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName()))
                ? authentication.getName() : null;

        List<BookableItemResponse> recommendations = recommendationService.getRecommendationsForBookableItem(
                bookableItemId,
                limit,
                currentUsername
        );
        return ResponseEntity.ok(recommendations);
    }

    // You could later add endpoints for user-based recommendations:
    // @GetMapping("/for-user/me")
    // @PreAuthorize("isAuthenticated()")
    // public ResponseEntity<List<BookableItemResponse>> getRecommendationsForCurrentUser(
    //         @RequestParam(defaultValue = "5") int limit) {
    //     String username = SecurityContextHolder.getContext().getAuthentication().getName();
    //     List<BookableItemResponse> recommendations = recommendationService.getRecommendationsForUser(username, limit);
    //     return ResponseEntity.ok(recommendations);
    // }
}
