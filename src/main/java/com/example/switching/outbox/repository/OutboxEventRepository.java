package com.example.switching.outbox.repository;

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
}