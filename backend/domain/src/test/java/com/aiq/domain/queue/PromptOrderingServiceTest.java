package com.aiq.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptOrderingServiceTest {

    private final PromptOrderingService orderingService = new PromptOrderingService();

    @Test
    void shouldOrderPromptsByPositionPriorityAndCreatedAt() {
        Prompt lowPriority = prompt(1, 0);
        Prompt highPriorityLater = prompt(2, 1);
        Prompt highPrioritySamePosition = prompt(2, 0);

        List<Prompt> orderedPrompts = orderingService.order(List.of(
            lowPriority,
            highPriorityLater,
            highPrioritySamePosition
        ));

        assertThat(orderedPrompts).containsExactly(
            highPrioritySamePosition,
            lowPriority,
            highPriorityLater
        );
    }

    private Prompt prompt(int priority, long position) {
        return Prompt.createQueued(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Prompt",
            "Prompt content",
            priority,
            position,
            3,
            null
        );
    }
}
