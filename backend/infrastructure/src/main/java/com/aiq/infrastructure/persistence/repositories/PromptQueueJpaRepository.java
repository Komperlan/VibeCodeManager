package com.aiq.infrastructure.persistence.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiq.domain.queue.QueueStatus;
import com.aiq.infrastructure.persistence.entity.PromptQueueJpaEntity;

public interface PromptQueueJpaRepository extends JpaRepository<PromptQueueJpaEntity, UUID> {
    List<PromptQueueJpaEntity> findByProjectId(UUID projectId);

    List<PromptQueueJpaEntity> findByStatusIn(Collection<QueueStatus> statuses);
}
