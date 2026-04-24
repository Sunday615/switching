package com.example.switching.outbox.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.switching.outbox.entity.OutboxEventEntity;
import com.example.switching.outbox.enums.OutboxStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop20ByStatusOrderByIdAsc(OutboxStatus status);

    List<OutboxEventEntity> findAllByTransferRefOrderByIdAsc(String transferRef);

    long countByStatus(OutboxStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update OutboxEventEntity e
              set e.status = :nextStatus
            where e.id = :id
              and e.status = :currentStatus
           """)
    int claimPendingEvent(@Param("id") Long id,
                          @Param("currentStatus") OutboxStatus currentStatus,
                          @Param("nextStatus") OutboxStatus nextStatus);

    @Query(
            value = """
                    select *
                      from outbox_events
                     where status = :processingStatus
                       and updated_at < :cutoff
                     order by id asc
                     limit :limit
                    """,
            nativeQuery = true
    )
    List<OutboxEventEntity> findStuckProcessingEvents(
            @Param("processingStatus") String processingStatus,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    update outbox_events
                       set status = :pendingStatus,
                           retry_count = coalesce(retry_count, 0) + 1,
                           updated_at = now()
                     where id = :id
                       and status = :processingStatus
                    """,
            nativeQuery = true
    )
    int recoverProcessingEvent(
            @Param("id") Long id,
            @Param("processingStatus") String processingStatus,
            @Param("pendingStatus") String pendingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    update outbox_events
                       set status = :failedStatus,
                           retry_count = coalesce(retry_count, 0) + 1,
                           updated_at = now()
                     where id = :id
                       and status = :processingStatus
                    """,
            nativeQuery = true
    )
    int markProcessingEventAsFailed(
            @Param("id") Long id,
            @Param("processingStatus") String processingStatus,
            @Param("failedStatus") String failedStatus
    );
}