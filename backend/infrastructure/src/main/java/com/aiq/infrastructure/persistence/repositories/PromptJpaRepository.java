package com.aiq.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiq.domain.queue.PromptStatus;
import com.aiq.infrastructure.persistence.entity.PromptJpaEntity;

public interface PromptJpaRepository extends JpaRepository<PromptJpaEntity, UUID> {
    List<PromptJpaEntity> findByQueueId(UUID queueId);

    List<PromptJpaEntity> findByQueueIdAndStatus(UUID queueId, PromptStatus status);

    Optional<PromptJpaEntity> findTopByQueueIdOrderByPositionDesc(UUID queueId);
}
