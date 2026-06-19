package com.aiq.infrastructure.persistence.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.aiq.domain.queue.QueueStatus;
import com.aiq.infrastructure.persistence.entity.PromptQueueJpaEntity;

public interface PromptQueueJpaRepository extends JpaRepository<PromptQueueJpaEntity, UUID> {
    List<PromptQueueJpaEntity> findByProjectId(UUID projectId);

    List<PromptQueueJpaEntity> findByStatusIn(Collection<QueueStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select queue from PromptQueueJpaEntity queue where queue.id = :queueId")
    Optional<PromptQueueJpaEntity> findByIdForUpdate(@Param("queueId") UUID queueId);
}
