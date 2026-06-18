package com.aiq.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aiq.domain.execution.PromptExecution;

public interface PromptExecutionRepository {

    PromptExecution save(PromptExecution execution);

    Optional<PromptExecution> findById(UUID executionId);

    List<PromptExecution> findByPromptId(UUID promptId);

    Optional<PromptExecution> findLatestByPromptId(UUID promptId);
}
