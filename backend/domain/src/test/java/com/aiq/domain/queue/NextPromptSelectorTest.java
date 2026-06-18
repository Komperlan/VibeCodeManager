package com.aiq.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NextPromptSelectorTest {

    private final NextPromptSelector selector = new NextPromptSelector();

    @Test
    void shouldSelectHighestPriorityQueuedPrompt() {
        Prompt completed = prompt(PromptStatus.COMPLETED, 10, 0);
        Prompt queuedLow = prompt(PromptStatus.QUEUED, 1, 0);
        Prompt queuedHigh = prompt(PromptStatus.QUEUED, 2, 0);

        assertThat(selector.selectNext(List.of(completed, queuedLow, queuedHigh)))
            .containsSame(queuedHigh);
    }

    @Test
    void shouldReturnEmptyWhenNoQueuedPromptExists() {
        Prompt completed = prompt(PromptStatus.COMPLETED, 10, 0);

        assertThat(selector.selectNext(List.of(completed))).isEmpty();
    }

    private Prompt prompt(PromptStatus status, int priority, long position) {
        Instant now = Instant.now();
        return Prompt.restore(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Prompt",
            "Prompt content",
            status,
            priority,
            position,
            null,
            0,
            3,
            now,
            now,
            null,
            null,
            null
        );
    }
}
