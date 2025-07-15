// src/main/java/com/ronak/welcome/repository/BookableItemRepository.java
package com.ronak.welcome.repository;

import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.enums.BookableItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookableItemRepository extends JpaRepository<BookableItem, Long>, JpaSpecificationExecutor<BookableItem> {

    List<BookableItem> findByProvider(User provider);

    // REMOVED: The problematic method findByIsPrivateFalseOrProvider
    // Its logic is now handled by JpaSpecificationExecutor in BookableItemService

    List<BookableItem> findByType(BookableItemType type);

    // This method is for conflict detection for time-based items.
    // It finds any confirmed/pending bookings for the same item that overlap with the given time range.
    // (Keeping this as it seems to be for a specific use case, not the general visibility filter)
    List<BookableItem> findByProviderAndTypeAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            User provider, BookableItemType type, LocalDateTime endTime, LocalDateTime startTime);
}
