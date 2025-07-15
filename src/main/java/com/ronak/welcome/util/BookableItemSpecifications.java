// src/main/java/com/ronak/welcome/util/BookableItemSpecifications.java
package com.ronak.welcome.util; // <-- ADD THIS PACKAGE DECLARATION

import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.enums.BookableItemType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Utility class to provide Spring Data JPA Specifications for BookableItem entities.
 * These specifications can be combined to build dynamic queries based on various criteria.
 */
public class BookableItemSpecifications {

    /**
     * Specification to search by keywords in name or description.
     * @param keywords The keywords to search for.
     * @return A Specification for keyword search.
     */
    public static Specification<BookableItem> hasKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return null; // No filtering if keywords are empty
        }
        String likePattern = "%" + keywords.toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
                );
    }

    /**
     * Specification to filter by BookableItemType.
     * @param type The BookableItemType to filter by.
     * @return A Specification for type filtering.
     */
    public static Specification<BookableItem> hasType(BookableItemType type) {
        if (type == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("type"), type);
    }

    /**
     * Specification to filter by location.
     * @param location The location to filter by.
     * @return A Specification for location filtering.
     */
    public static Specification<BookableItem> hasLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String likePattern = "%" + location.toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), likePattern);
    }

    /**
     * Specification to filter by minimum price.
     * @param minPrice The minimum price.
     * @return A Specification for minimum price filtering.
     */
    public static Specification<BookableItem> hasMinPrice(Double minPrice) {
        if (minPrice == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    /**
     * Specification to filter by maximum price.
     * @param maxPrice The maximum price.
     * @return A Specification for maximum price filtering.
     */
    public static Specification<BookableItem> hasMaxPrice(Double maxPrice) {
        if (maxPrice == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /**
     * Specification to filter by start date/time (items starting on or after this time).
     * @param startDate The start date/time.
     * @return A Specification for start date filtering.
     */
    public static Specification<BookableItem> startsAfter(LocalDateTime startDate) {
        if (startDate == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), startDate);
    }

    /**
     * Specification to filter by end date/time (items ending on or before this time).
     * @param endDate The end date/time.
     * @return A Specification for end date filtering.
     */
    public static Specification<BookableItem> endsBefore(LocalDateTime endDate) {
        if (endDate == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("endTime"), endDate);
    }

    /**
     * Specification to filter by items that are NOT private.
     * @return A Specification for non-private items.
     */
    public static Specification<BookableItem> isNotPrivate() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isFalse(root.get("isPrivate"));
    }
}
