package com.aiq.infrastructure.limit.codex;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.path.LocalPathNormalizer;
import com.aiq.infrastructure.executor.codex.CodexCliProperties;
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
public class CodexLimitCheckCommandBuilder {

    private final CodexCliProperties codexCliProperties;
    private final CodexLimitCheckerProperties limitCheckerProperties;

    public ProcessCommand build(AiLimitCheckRequest request) {
        Objects.requireNonNull(request, "AI limit check request must not be null");

        return new ProcessCommand(
            arguments(),
            workingDirectory(request),
            limitCheckerProperties.getProbePrompt(),
            new HashMap<>(codexCliProperties.getEnvironment()),
            limitCheckerProperties.getTimeout(),
            limitCheckerProperties.getMaxOutputBytes()
        );
    }

    public String buildSafeCommand(AiLimitCheckRequest request) {
        return String.join(" ", build(request).arguments());
    }

    private List<String> arguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(LocalPathNormalizer.executablePath(codexCliProperties.getExecutablePath()));
        arguments.addAll(limitCheckerProperties.getArguments());

        return arguments;
    }

    private Path workingDirectory(AiLimitCheckRequest request) {
        return LocalPathNormalizer.toDirectoryPath(request.workingDirectory());
    }
}
