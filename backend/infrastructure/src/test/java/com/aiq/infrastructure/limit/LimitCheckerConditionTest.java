package com.aiq.infrastructure.limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.aiq.application.port.out.AiLimitChecker;
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

class LimitCheckerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(ProcessRunner.class, () -> mock(ProcessRunner.class))
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

    @Configuration(proxyBeanMethods = false)
    @Import({
        FakeAiLimitChecker.class,
        CodexCliLimitChecker.class,
        CodexLimitCheckCommandBuilder.class,
        CodexLimitOutputParser.class,
        CodexCliProperties.class,
        CodexLimitCheckerProperties.class
    })
    static class LimitCheckerTestConfiguration {
    }
}
