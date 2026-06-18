package com.aiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.aiq.application.port.out.PromptRepository;
import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptStatus;
import com.aiq.infrastructure.persistence.entity.PromptJpaEntity;
import com.aiq.infrastructure.persistence.mapper.PromptPersistenceMapper;
import com.aiq.infrastructure.persistence.repositories.PromptJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PromptRepositoryAdapter implements PromptRepository {
    private final PromptJpaRepository promptJpaRepository;

    @Override
    public Prompt save(Prompt prompt) {
        var entity = PromptPersistenceMapper.toEntity(prompt);
        return PromptPersistenceMapper.toDomain(promptJpaRepository.save(entity));
    }

    @Override
    public Optional<Prompt> findById(UUID promptId) {
        return promptJpaRepository.findById(promptId).map(PromptPersistenceMapper::toDomain);
    }

    @Override
    public List<Prompt> findByQueueId(UUID queueId) {
        return promptJpaRepository.findByQueueId(queueId).stream().map(PromptPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Prompt> findByQueueIdAndStatus(UUID queueId, PromptStatus status) {
        return promptJpaRepository.findByQueueIdAndStatus(queueId, status).stream().map(PromptPersistenceMapper::toDomain).toList();
    }

    @Override
    public long nextPosition(UUID queueId) {
        return promptJpaRepository.findTopByQueueIdOrderByPositionDesc(queueId)
            .map(PromptJpaEntity::getPosition)
            .map(position -> position + 1)
            .orElse(0L);
    }
}
