package com.ronak.welcome.repository;

import com.ronak.welcome.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    /**
     * Finds pending outbox events that have not exceeded the maximum retry count,
     * ordered by creation time for FIFO processing.
     *
     * @param status The status to look for (e.g., "PENDING").
     * @param maxRetries The maximum retry count allowed.
     * @return A list of OutboxEvent entities.
     */
    List<OutboxEvent> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(String status, int maxRetries);

    // You can keep findByStatusIn if you still need it for other purposes,
    // but the above is more robust for the processor.
    // List<OutboxEvent> findByStatusIn(List<String> statuses);
}
