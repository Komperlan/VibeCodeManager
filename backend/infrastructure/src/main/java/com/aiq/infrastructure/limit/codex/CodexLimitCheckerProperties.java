package com.aiq.infrastructure.limit.codex;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "aiq.limit.codex")
public class CodexLimitCheckerProperties {

    @NotNull(message = "codex limit checker timeout must not be null")
    private Duration timeout = Duration.ofSeconds(30);

    @Positive(message = "codex limit checker maxOutputBytes must be positive")
    private int maxOutputBytes = 100_000;

    @NotEmpty(message = "codex limit checker arguments must not be empty")
    private List<@NotBlank(message = "codex limit checker argument must not be blank") String> arguments =
        new ArrayList<>(List.of(
            "exec",
            "--json",
            "--color",
            "never",
            "--skip-git-repo-check",
            "--ephemeral",
            "-"
        ));

    @NotBlank(message = "codex limit checker probePrompt must not be blank")
    private String probePrompt = "Reply exactly AVAILABLE. Do not inspect files, run commands, or modify anything.";

    @AssertTrue(message = "codex limit checker timeout must be positive")
    public boolean isTimeoutPositive() {
        return timeout == null || (!timeout.isZero() && !timeout.isNegative());
    }
}
