package com.aiq.infrastructure.executor.codex;

import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.application.path.LocalPathNormalizer;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodexCommandBuilder {

    private final CodexCliProperties properties;

    public ProcessCommand build(PromptExecutionRequest request) {
        Objects.requireNonNull(request, "Prompt execution request must not be null");

        return new ProcessCommand(
            arguments(),
            workingDirectory(request),
            stdin(request),
            new HashMap<>(properties.getEnvironment()),
            properties.getTimeout(),
            properties.getMaxOutputBytes()
        );
    }

    public String buildSafeCommand(PromptExecutionRequest request) {
        return String.join(" ", build(request).arguments());
    }

    private List<String> arguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(LocalPathNormalizer.executablePath(properties.getExecutablePath()));
        arguments.addAll(properties.getDefaultArguments());

        return arguments;
    }

    private Path workingDirectory(PromptExecutionRequest request) {
        return LocalPathNormalizer.toDirectoryPath(request.workingDirectoryOverride());
    }

    private String stdin(PromptExecutionRequest request) {
        return """
            %s

            %s
            """.formatted(request.title(), request.content()).stripTrailing();
    }
}
