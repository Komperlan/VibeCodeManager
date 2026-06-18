package com.aiq.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptTest {

    @Test
    void shouldCreateQueuedPrompt() {
        Prompt prompt = queuedPrompt(3);

        assertThat(prompt.getId()).isNotNull();
        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(prompt.getAttemptCount()).isZero();
        assertThat(prompt.getMaxAttempts()).isEqualTo(3);
    }

    @Test
    void shouldMoveDraftPromptToQueued() {
        Prompt prompt = Prompt.createDraft(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Prompt",
            "Prompt content",
            0,
            0,
            3,
            null
        );

        prompt.enqueue();

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(prompt.isQueued()).isTrue();
    }

    @Test
    void shouldCompleteRunningPrompt() {
        Prompt prompt = queuedPrompt(3);

        prompt.start();
        prompt.complete();

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.COMPLETED);
        assertThat(prompt.isTerminal()).isTrue();
        assertThat(prompt.finishedAt()).isPresent();
    }

    @Test
    void shouldFailRunningPromptAndRetryIt() {
        Prompt prompt = queuedPrompt(2);

        prompt.start();
        prompt.fail("Process failed");
        prompt.retry();

        assertThat(prompt.getStatus()).isEqualTo(PromptStatus.QUEUED);
        assertThat(prompt.failureReason()).contains("Process failed");
    }

    @Test
    void shouldRejectRetryWhenMaxAttemptsReached() {
        Prompt prompt = queuedPrompt(1);

        prompt.start();
        prompt.fail("Process failed");

        assertThatIllegalStateException()
            .isThrownBy(prompt::retry)
            .withMessage("Prompt has no retry attempts left");
    }

    @Test
    void shouldRejectStartingDraftPrompt() {
        Prompt prompt = Prompt.createDraft(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Prompt",
            "Prompt content",
            0,
            0,
            3,
            null
        );

        assertThatIllegalStateException()
            .isThrownBy(prompt::start)
            .withMessage("Prompt can be started only from QUEUED status or retryable FAILED status");
    }

    @Test
    void shouldRejectCancellingTerminalPrompt() {
        Prompt prompt = queuedPrompt(3);
        prompt.start();
        prompt.complete();

        assertThatIllegalStateException()
            .isThrownBy(prompt::cancel)
            .withMessage("Terminal prompt cannot be cancelled");
    }

    @Test
    void shouldRejectBlankWorkingDirectoryOverride() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Prompt.createQueued(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Prompt",
                "Prompt content",
                0,
                0,
                3,
                " "
            ))
            .withMessage("Working directory override must not be blank");
    }

    private Prompt queuedPrompt(int maxAttempts) {
        return Prompt.createQueued(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Prompt",
            "Prompt content",
            0,
            0,
            maxAttempts,
            null
        );
    }
}
