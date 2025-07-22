package com.ronak.welcome.repository;

import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.User;
import com.ronak.welcome.entity.UserInteraction;
import com.ronak.welcome.enums.InteractionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    List<UserInteraction> findByUserAndBookableItem(User user, BookableItem bookableItem);

    List<UserInteraction> findByUser(User user);

    List<UserInteraction> findByBookableItem(BookableItem bookableItem);

    List<UserInteraction> findByUserAndInteractionType(User user, InteractionType interactionType);

    @Query("SELECT ui FROM UserInteraction ui WHERE ui.user = :user AND ui.interactionDate >= :since")
    List<UserInteraction> findRecentInteractions(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT ui.bookableItem, SUM(ui.weight) as totalWeight FROM UserInteraction ui " +
            "WHERE ui.user = :user GROUP BY ui.bookableItem ORDER BY totalWeight DESC")
    List<Object[]> findTopInteractedItems(@Param("user") User user);

    @Query("SELECT COUNT(ui) FROM UserInteraction ui WHERE ui.bookableItem = :item AND ui.interactionType = :type")
    Long countInteractionsByType(@Param("item") BookableItem item, @Param("type") InteractionType type);
}
