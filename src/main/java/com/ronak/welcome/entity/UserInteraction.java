package com.ronak.welcome.entity;

import com.ronak.welcome.enums.InteractionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "user_interactions")
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookable_item_id", nullable = false)
    private BookableItem bookableItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType interactionType; // VIEW, BOOK, CANCEL, FAVORITE

    @Column(nullable = false)
    private double weight = 1.0; // Interaction strength

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime interactionDate;
}
