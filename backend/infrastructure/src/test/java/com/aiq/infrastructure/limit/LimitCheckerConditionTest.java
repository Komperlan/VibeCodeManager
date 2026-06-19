package com.aiq.infrastructure.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.aiq.application.port.out.AiLimitChecker;
import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.scheduler.LimitResumeSchedulerProperties;
import com.aiq.application.scheduler.WaitingLimitQueueScheduler;
import com.aiq.application.service.QueueRunnerApplicationService;
import com.aiq.infrastructure.executor.ProcessRunner;
import com.aiq.infrastructure.executor.codex.CodexCliProperties;
import com.aiq.infrastructure.limit.codex.CodexCliLimitChecker;
import com.aiq.infrastructure.limit.codex.CodexLimitCheckCommandBuilder;
import com.aiq.infrastructure.limit.codex.CodexLimitCheckerProperties;
import com.aiq.infrastructure.limit.codex.CodexLimitOutputParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

class LimitCheckerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(ProcessRunner.class, () -> mock(ProcessRunner.class))
        .withBean(PromptQueueRepository.class, () -> mock(PromptQueueRepository.class))
        .withBean(QueueRunnerApplicationService.class, () -> mock(QueueRunnerApplicationService.class))
        .withUserConfiguration(LimitCheckerTestConfiguration.class);

    @Test
    void shouldUseFakeLimitCheckerByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiLimitChecker.class);
            assertThat(context).hasSingleBean(FakeAiLimitChecker.class);
            assertThat(context).doesNotHaveBean(CodexCliLimitChecker.class);
        });
    }

    @Test
    void shouldUseCodexLimitCheckerWhenCodexExecutorIsEnabled() {
        contextRunner
            .withPropertyValues("aiq.executor.codex.enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(AiLimitChecker.class);
                assertThat(context).hasSingleBean(CodexCliLimitChecker.class);
                assertThat(context).doesNotHaveBean(FakeAiLimitChecker.class);
            });
    }

    @Test
    void shouldEnableLimitResumeSchedulerOnlyForCodexRuntime() {
        contextRunner.run(context ->
            assertThat(context).doesNotHaveBean(WaitingLimitQueueScheduler.class)
        );

        contextRunner
            .withPropertyValues(
                "aiq.executor.codex.enabled=true",
                "aiq.queue.limit-resume.enabled=true"
            )
            .run(context -> assertThat(context).hasSingleBean(WaitingLimitQueueScheduler.class));

        contextRunner
            .withPropertyValues(
                "aiq.executor.codex.enabled=true",
                "aiq.queue.limit-resume.enabled=false"
            )
            .run(context -> assertThat(context).doesNotHaveBean(WaitingLimitQueueScheduler.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Import({
        FakeAiLimitChecker.class,
        CodexCliLimitChecker.class,
        CodexLimitCheckCommandBuilder.class,
        CodexLimitOutputParser.class,
        CodexCliProperties.class,
        CodexLimitCheckerProperties.class,
        LimitResumeSchedulerProperties.class,
        WaitingLimitQueueScheduler.class
    })
    static class LimitCheckerTestConfiguration {
    }
}
