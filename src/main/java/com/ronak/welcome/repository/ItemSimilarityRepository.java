package com.ronak.welcome.repository;

import com.ronak.welcome.entity.BookableItem;
import com.ronak.welcome.entity.ItemSimilarity;
import com.ronak.welcome.enums.SimilarityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemSimilarityRepository extends JpaRepository<ItemSimilarity, Long> {

    List<ItemSimilarity> findByItem1OrItem2OrderBySimilarityScoreDesc(BookableItem item1, BookableItem item2);

    List<ItemSimilarity> findByItem1OrderBySimilarityScoreDesc(BookableItem item1);

    List<ItemSimilarity> findByItem2OrderBySimilarityScoreDesc(BookableItem item2);

    List<ItemSimilarity> findBySimilarityType(SimilarityType similarityType);

    @Query("SELECT s FROM ItemSimilarity s WHERE (s.item1 = :item OR s.item2 = :item) " +
            "AND s.similarityScore >= :minScore ORDER BY s.similarityScore DESC")
    List<ItemSimilarity> findSimilarItems(@Param("item") BookableItem item, @Param("minScore") double minScore);

    @Query("SELECT s FROM ItemSimilarity s WHERE " +
            "((s.item1 = :item1 AND s.item2 = :item2) OR (s.item1 = :item2 AND s.item2 = :item1))")
    ItemSimilarity findSimilarityBetweenItems(@Param("item1") BookableItem item1, @Param("item2") BookableItem item2);
}
