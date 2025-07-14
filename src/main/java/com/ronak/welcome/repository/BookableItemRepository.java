package com.ronak.welcome.repository;

import com.ronak.welcome.entity.BookableItem; // Ensure this import is correct
import com.ronak.welcome.enums.BookableItemType; // Ensure this import is correct
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookableItemRepository extends JpaRepository<BookableItem, Long> {
    /**
     * Finds all bookable items of a specific type.
     * @param type The type of bookable item (e.g., EVENT, APPOINTMENT).
     * @return A list of BookableItem entities matching the type.
     */
    List<BookableItem> findByType(BookableItemType type);

    /**
     * Finds all bookable items provided by a specific user.
     * @param providerId The ID of the user who provides the items.
     * @return A list of BookableItem entities provided by the user.
     */
    List<BookableItem> findByProviderId(Long providerId);
}
