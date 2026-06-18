package com.aiq.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueStatus;
import com.aiq.infrastructure.persistence.mapper.PromptQueuePersistenceMapper;
import com.aiq.infrastructure.persistence.repositories.PromptQueueJpaRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class PromptQueueRepositoryAdapter implements PromptQueueRepository {
    private final PromptQueueJpaRepository promptQueueJpaRepository;

    @Override
    public PromptQueue save(PromptQueue queue) {
        var entity = PromptQueuePersistenceMapper.toEntity(queue);
        var savedEntity = promptQueueJpaRepository.save(entity);
        return PromptQueuePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PromptQueue> findById(UUID queueId) {
        return promptQueueJpaRepository.findById(queueId).map(PromptQueuePersistenceMapper::toDomain);
    }

    @Override
    public List<PromptQueue> findByProjectId(UUID projectId) {
        return promptQueueJpaRepository.findByProjectId(projectId).stream().map(PromptQueuePersistenceMapper::toDomain).toList();
    }

    @Override
    public List<PromptQueue> findByStatuses(Collection<QueueStatus> statuses) {
        return promptQueueJpaRepository.findByStatusIn(statuses).stream().map(PromptQueuePersistenceMapper::toDomain).toList();
    }

    
}
