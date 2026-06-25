package com.aiq.infrastructure.executor.codex;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "aiq.executor.codex")
public class CodexCliProperties {

    private boolean enabled;

    @NotNull(message = "codex executablePath must not be null")
    private Path executablePath = Path.of("codex");

    @NotNull(message = "codex timeout must not be null")
    private Duration timeout = Duration.ofMinutes(30);

    @Positive(message = "codex maxOutputBytes must be positive")
    private int maxOutputBytes = 1_000_000;

    @NotEmpty(message = "codex defaultArguments must not be empty")
    private List<@NotBlank(message = "codex default argument must not be blank") String> defaultArguments =
        new ArrayList<>(List.of(
            "exec",
            "--json",
            "--color",
            "never",
            "--sandbox",
            "workspace-write",
            "--skip-git-repo-check",
            "-"
        ));

    @NotEmpty(message = "codex resumeArguments must not be empty")
    private List<@NotBlank(message = "codex resume argument must not be blank") String> resumeArguments =
        new ArrayList<>(List.of(
            "--sandbox",
            "workspace-write",
            "exec",
            "resume",
            "--json",
            "--skip-git-repo-check"
        ));

    @NotNull(message = "codex environment must not be null")
    private Map<
        @NotBlank(message = "codex environment variable name must not be blank") String,
        @NotNull(message = "codex environment variable value must not be null") String
    > environment = new HashMap<>();

    @AssertTrue(message = "codex timeout must be positive")
    public boolean isTimeoutPositive() {
        return timeout == null || (!timeout.isZero() && !timeout.isNegative());
    }
}
