package com.chargegrid.session_service.outbox;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claims a batch of unpublished events.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} is what makes the relay safe to run on every instance: each
     * one takes rows nobody else holds instead of all of them fighting over the same head of the
     * queue, and no event is published twice concurrently.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt")
    List<OutboxEvent> claimUnpublished(Limit limit);

    long countByPublishedAtIsNull();
}
