package com.aiq.infrastructure.persistence.adapter;

import com.aiq.application.port.out.PromptExecutionRepository;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.infrastructure.persistence.mapper.PromptExecutionPersistenceMapper;
import com.aiq.infrastructure.persistence.repositories.PromptExecutionJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PromptExecutionRepositoryAdapter implements PromptExecutionRepository {

    private final PromptExecutionJpaRepository promptExecutionJpaRepository;

    @Override
    public PromptExecution save(PromptExecution execution) {
        var entity = PromptExecutionPersistenceMapper.toEntity(execution);
        var savedEntity = promptExecutionJpaRepository.save(entity);
        return PromptExecutionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PromptExecution> findById(UUID executionId) {
        return promptExecutionJpaRepository.findById(executionId)
            .map(PromptExecutionPersistenceMapper::toDomain);
    }

    @Override
    public List<PromptExecution> findByPromptId(UUID promptId) {
        return promptExecutionJpaRepository.findByPromptId(promptId).stream()
            .map(PromptExecutionPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<PromptExecution> findLatestByPromptId(UUID promptId) {
        return promptExecutionJpaRepository.findTopByPromptIdOrderByFinishedAtDescStartedAtDesc(promptId)
            .map(PromptExecutionPersistenceMapper::toDomain);
    }
}
