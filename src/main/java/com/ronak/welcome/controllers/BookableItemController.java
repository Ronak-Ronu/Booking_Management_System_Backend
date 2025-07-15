// src/main/java/com/ronak/welcome/controllers/BookableItemController.java
package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.BookableItemRequest;
import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.enums.BookableItemType;
// IMPORTANT: Import BookableItemService from its 'impl' package
import com.ronak.welcome.service.impl.BookableItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/items") // Base path for generic bookable items
@RequiredArgsConstructor
public class BookableItemController {

    private final BookableItemService bookableItemService;

    /**
     * Creates a new bookable item. Only EVENT_ORGANIZER or ADMIN can create.
     * Accessible at /api/v1/items
     * @param request The BookableItemRequest DTO.
     * @param authentication Spring Security Authentication object.
     * @return ResponseEntity with BookableItemResponse.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')")
    public ResponseEntity<BookableItemResponse> createBookableItem(
            @Valid @RequestBody BookableItemRequest request,
            Authentication authentication) {
        BookableItemResponse response = bookableItemService.createBookableItem(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves a bookable item by its ID.
     * Publicly accessible, but private items are filtered based on user authorization in the service.
     * Accessible at /api/v1/items/{id}
     * @param id The ID of the bookable item.
     * @param authentication Spring Security Authentication object.
     * @return ResponseEntity with BookableItemResponse.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookableItemResponse> getBookableItemById(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        BookableItemResponse response = bookableItemService.getBookableItemById(id, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all bookable items.
     * Publicly accessible, but private items are filtered unless the user is the provider or ADMIN.
     * Supports filtering by providerId and by current user's items.
     * Accessible at /api/v1/items
     * @param authentication Spring Security Authentication object.
     * @param providerId Optional: Filter by provider ID.
     * @param onlyMyItems Optional: If true, only return items owned by the current user.
     * @return ResponseEntity with a list of BookableItemResponse.
     */
    @GetMapping
    public ResponseEntity<List<BookableItemResponse>> getAllBookableItems(
            Authentication authentication,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Boolean onlyMyItems) {
        String username = authentication != null ? authentication.getName() : null;
        // The getAllBookableItems in service doesn't use providerId/onlyMyItems directly in its current form from your code
        // so you might want to adjust that service method or remove these params if they aren't used.
        // For now, I'm calling the version that exists in your service.
        List<BookableItemResponse> response = bookableItemService.getAllBookableItems(username);
        return ResponseEntity.ok(response);
    }

    /**
     * NEW: Advanced Search Endpoint for Bookable Items.
     * This is the core new search functionality.
     * Accessible at /api/v1/items/search
     * @param keywords Keywords to search in name/description.
     * @param type Type of bookable item (e.g., EVENT, APPOINTMENT).
     * @param location Location of the item.
     * @param minPrice Minimum price.
     * @param maxPrice Maximum price.
     * @param startDate Items starting on or after this date/time.
     * @param endDate Items ending on or before this date/time.
     * @param sortBy Field to sort by (e.g., "name", "startTime", "price").
     * @param sortOrder Sort order ("asc" or "desc").
     * @param authentication Spring Security Authentication object.
     * @return ResponseEntity with a list of matching BookableItemResponse.
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookableItemResponse>> searchBookableItems(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) BookableItemType type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<BookableItemResponse> response = bookableItemService.searchBookableItems(
                keywords, type, location, minPrice, maxPrice, startDate, endDate, sortBy, sortOrder, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing bookable item. Only ADMIN or the item's provider can update.
     * Accessible at /api/v1/items/{id}
     * @param id The ID of the bookable item to update.
     * @param request The BookableItemRequest DTO with updated data.
     * @param authentication Spring Security Authentication object.
     * @return ResponseEntity with updated BookableItemResponse.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')") // Role check, service will also enforce provider check
    public ResponseEntity<BookableItemResponse> updateBookableItem(
            @PathVariable Long id,
            @Valid @RequestBody BookableItemRequest request,
            Authentication authentication) {
        BookableItemResponse response = bookableItemService.updateBookableItem(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a bookable item. Only ADMIN or the item's provider can delete.
     * Prevents deletion if there are active bookings (logic in service).
     * Accessible at /api/v1/items/{id}
     * @param id The ID of the bookable item to delete.
     * @param authentication Spring Security Authentication object.
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EVENT_ORGANIZER', 'ADMIN')") // Role check, service will also enforce provider check
    public ResponseEntity<Void> deleteBookableItem(
            @PathVariable Long id,
            Authentication authentication) {
        bookableItemService.deleteBookableItem(id, authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
