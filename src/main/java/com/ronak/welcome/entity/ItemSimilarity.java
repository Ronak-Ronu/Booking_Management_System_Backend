package com.ronak.welcome.entity;

import com.ronak.welcome.enums.SimilarityType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "item_similarities")
public class ItemSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item1_id", nullable = false)
    private BookableItem item1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item2_id", nullable = false)
    private BookableItem item2;

    @Column(nullable = false)
    private double similarityScore; // 0.0 to 1.0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimilarityType similarityType; // CONTENT_BASED, COLLABORATIVE, HYBRID
}
