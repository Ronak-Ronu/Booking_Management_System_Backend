// src/main/java/com/ronak/welcome/service/EventService.java
package com.ronak.welcome.service.impl;

import com.ronak.welcome.DTO.BookableItemRequest;
import com.ronak.welcome.DTO.BookableItemResponse;
import com.ronak.welcome.enums.BookableItemType;
import com.ronak.welcome.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder; // Import for current user info

import java.util.List;

@Service
public class EventService {

    private final BookableItemService bookableItemService;

    public EventService(BookableItemService bookableItemService) {
        this.bookableItemService = bookableItemService;
    }

    @Transactional
    public BookableItemResponse createEvent(BookableItemRequest eventRequest, String organizerUsername) {
        if (eventRequest.type() != BookableItemType.EVENT) {
            throw new IllegalArgumentException("Event creation must have type set to EVENT.");
        }
        return bookableItemService.createBookableItem(eventRequest, organizerUsername);
    }

    @Transactional(readOnly = true)
    public BookableItemResponse getEventById(Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        BookableItemResponse item = bookableItemService.getBookableItemById(id, currentUsername); // Pass username
        if (item.type() != BookableItemType.EVENT) {
            throw new ResourceNotFoundException("Item with ID " + id + " is not an event.");
        }
        return item;
    }

    @Transactional(readOnly = true)
    public List<BookableItemResponse> getAllEvents() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // Delegate to BookableItemService to get items of type EVENT, respecting private flag
        return bookableItemService.getBookableItemsByType(BookableItemType.EVENT, currentUsername); // Pass username
    }

    @Transactional
    public BookableItemResponse updateEvent(Long id, BookableItemRequest eventRequest, String currentUsername) {
        if (eventRequest.type() != BookableItemType.EVENT) {
            throw new IllegalArgumentException("Event update must have type set to EVENT.");
        }
        return bookableItemService.updateBookableItem(id, eventRequest, currentUsername);
    }

    @Transactional
    public void deleteEvent(Long id, String currentUsername) {
        // Before deleting, ensure it's an event (good for type safety)
        // Note: getBookableItemById now requires currentUsername
        BookableItemResponse item = bookableItemService.getBookableItemById(id, currentUsername);
        if (item.type() != BookableItemType.EVENT) {
            throw new ResourceNotFoundException("Item with ID " + id + " is not an event and cannot be deleted via EventService.");
        }
        bookableItemService.deleteBookableItem(id, currentUsername);
    }
}
