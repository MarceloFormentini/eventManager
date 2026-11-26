package br.com.formentini.eventManager.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event from OutboxEvent event
            where event.status = br.com.formentini.eventManager.outbox.enums.OutboxStatus.PENDING
              and event.nextAttemptAt <= :now
            order by event.createdAt asc
            """)
    List<OutboxEvent> findPendingForDispatch(
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    long countByStatus(
            br.com.formentini.eventManager.outbox.enums.OutboxStatus status
    );

    @Query("""
            select coalesce(sum(event.attempts), 0)
            from OutboxEvent event
            where event.status = br.com.formentini.eventManager.outbox.enums.OutboxStatus.PENDING
            """)
    long sumPendingAttempts();

    @Query("""
            select min(event.createdAt)
            from OutboxEvent event
            where event.status = br.com.formentini.eventManager.outbox.enums.OutboxStatus.PENDING
            """)
    Optional<OffsetDateTime> findOldestPendingCreatedAt();
}