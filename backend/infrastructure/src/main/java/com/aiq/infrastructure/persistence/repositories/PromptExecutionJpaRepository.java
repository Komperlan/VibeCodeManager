package com.aiq.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiq.infrastructure.persistence.entity.PromptExecutionJpaEntity;

public interface PromptExecutionJpaRepository extends JpaRepository<PromptExecutionJpaEntity, UUID> {
    List<PromptExecutionJpaEntity> findByPromptId(UUID promptId);

    Optional<PromptExecutionJpaEntity> findTopByPromptIdOrderByFinishedAtDescStartedAtDesc(UUID promptId);
}
