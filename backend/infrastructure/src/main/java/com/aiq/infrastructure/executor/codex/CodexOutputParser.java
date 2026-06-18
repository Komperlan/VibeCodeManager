package com.aiq.infrastructure.executor.codex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CodexOutputParser {

    private static final List<String> TEXT_FIELD_NAMES = List.of(
        "final_output",
        "last_message",
        "output_text",
        "message",
        "content",
        "text",
        "output",
        "delta"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionResult parse(ProcessRunResult result) {
        Objects.requireNonNull(result, "Process run result must not be null");

        return new ExecutionResult(
            result.exitCode(),
            stdout(result),
            result.stderr(),
            result.rawOutput(),
            errorMessage(result)
        );
    }

    private String stdout(ProcessRunResult result) {
        return extractJsonLinesText(result.stdout()).orElse(result.stdout());
    }

    private Optional<String> extractJsonLinesText(String stdout) {
        if (stdout.isBlank()) {
            return Optional.empty();
        }

        List<String> textChunks = new ArrayList<>();
        boolean hasJsonLine = false;

        for (String line : stdout.lines().toList()) {
            String normalizedLine = line.trim();
            if (normalizedLine.isEmpty() || !normalizedLine.startsWith("{")) {
                continue;
            }

            try {
                JsonNode node = objectMapper.readTree(normalizedLine);
                hasJsonLine = true;
                extractText(node).ifPresent(textChunks::add);
            } catch (JsonProcessingException ignored) {
                // Keep parser tolerant: unknown or partial JSONL falls back to raw stdout.
            }
        }

        if (!hasJsonLine || textChunks.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(String.join(System.lineSeparator(), textChunks));
    }

    private Optional<String> extractText(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isTextual()) {
            return nonBlank(node.asText());
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                extractText(item).ifPresent(parts::add);
            }
            if (parts.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(String.join(System.lineSeparator(), parts));
        }
        if (!node.isObject()) {
            return Optional.empty();
        }

        for (String fieldName : TEXT_FIELD_NAMES) {
            Optional<String> text = extractText(node.get(fieldName));
            if (text.isPresent()) {
                return text;
            }
        }

        return Optional.empty();
    }

    private Optional<String> nonBlank(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    private String errorMessage(ProcessRunResult result) {
        if (result.timedOut()) {
            return result.errorMessage() == null
                ? "Codex CLI execution timed out"
                : result.errorMessage();
        }

        if (result.errorMessage() != null) {
            return result.errorMessage();
        }

        if (result.exitCode() == 0) {
            return null;
        }

        if (!result.stderr().isBlank()) {
            return result.stderr().trim();
        }

        return "Codex CLI exited with code " + result.exitCode();
    }
}
