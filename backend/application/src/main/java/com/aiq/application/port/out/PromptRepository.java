package com.aiq.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.aiq.domain.queue.Prompt;
import com.aiq.domain.queue.PromptStatus;

public interface PromptRepository {

    Prompt save(Prompt prompt);

    Optional<Prompt> findById(UUID promptId);

    List<Prompt> findByQueueId(UUID queueId);

    List<Prompt> findByQueueIdAndStatus(UUID queueId, PromptStatus status);

    long nextPosition(UUID queueId);
}
