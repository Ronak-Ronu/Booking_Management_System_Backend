// src/main/java/com/ronak/welcome/controllers/BookingController.java
package com.ronak.welcome.controllers;

import com.ronak.welcome.DTO.BookingRequest; // New DTO for booking creation
import com.ronak.welcome.DTO.BookingResponse; // New DTO for booking responses
import com.ronak.welcome.service.impl.BookingService; // Renamed service
import com.ronak.welcome.service.impl.BookableItemService; // New service for @PreAuthorize checks
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings") // Renamed base path
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService; // Injected new BookingService
    private final BookableItemService bookableItemService; // Injected for @PreAuthorize expressions

    /**
     * Creates a new booking for the current authenticated user for a specific bookable item.
     * Accessible by any authenticated user.
     *
     * @param bookingRequest DTO containing the ID of the bookable item.
     * @return ResponseEntity with BookingResponse and HTTP status 201 Created.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest bookingRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        BookingResponse booking = bookingService.createBooking(bookingRequest, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /**
     * Retrieves all bookings made by the current authenticated user.
     * Accessible by any authenticated user.
     *
     * @return ResponseEntity with a list of BookingResponse and HTTP status 200 OK.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponse>> getMyBookings() { // Renamed method
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        List<BookingResponse> bookings = bookingService.getUserBookings(username); // Call new service method
        return ResponseEntity.ok(bookings);
    }

    /**
     * Retrieves all bookings for a specific bookable item.
     * Accessible only by the item's provider (organizer) or an ADMIN.
     *
     * @param bookableItemId The ID of the bookable item to get bookings for.
     * @return ResponseEntity with a list of BookingResponse and HTTP status 200 OK.
     */
    @GetMapping("/items/{bookableItemId}") // New path for getting bookings by item
    @PreAuthorize("hasRole('ADMIN') or @bookableItemService.getBookableItemById(#bookableItemId).providerUsername() == authentication.name") // Updated @PreAuthorize
    public ResponseEntity<List<BookingResponse>> getBookingsByBookableItemId(@PathVariable Long bookableItemId) { // Renamed method and parameter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        List<BookingResponse> bookings = bookingService.getBookingsByBookableItemId(bookableItemId, currentUsername); // Call new service method
        return ResponseEntity.ok(bookings);
    }

    @DeleteMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @bookingService.isUserBooking(#bookingId, authentication.name)")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        bookingService.cancelBooking(bookingId, username);
        return ResponseEntity.noContent().build();
    }
}
