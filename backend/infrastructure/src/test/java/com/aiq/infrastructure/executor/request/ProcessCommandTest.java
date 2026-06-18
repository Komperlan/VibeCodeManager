package com.aiq.infrastructure.executor.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProcessCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptValidCommand() {
        ProcessCommand command = validCommand();

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectEmptyArguments() {
        ProcessCommand command = command(List.of(), Path.of("/tmp/project"), Map.of(), Duration.ofSeconds(30), 10_000);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("process arguments must not be empty");
    }

    @Test
    void shouldRejectBlankArgument() {
        ProcessCommand command = command(
            List.of("codex", " "),
            Path.of("/tmp/project"),
            Map.of(),
            Duration.ofSeconds(30),
            10_000
        );

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("process argument must not be blank");
    }

    @Test
    void shouldRejectMissingWorkingDirectory() {
        ProcessCommand command = command(List.of("codex"), null, Map.of(), Duration.ofSeconds(30), 10_000);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("working directory must not be null");
    }

    @Test
    void shouldRejectMissingEnvironment() {
        ProcessCommand command = command(List.of("codex"), Path.of("/tmp/project"), null, Duration.ofSeconds(30), 10_000);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("environment must not be null");
    }

    @Test
    void shouldRejectInvalidEnvironmentEntry() {
        Map<String, String> environment = new HashMap<>();
        environment.put(" ", "value");
        environment.put("VALID_KEY", null);
        ProcessCommand command = command(
            List.of("codex"),
            Path.of("/tmp/project"),
            environment,
            Duration.ofSeconds(30),
            10_000
        );

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations))
            .contains(
                "environment variable name must not be blank",
                "environment variable value must not be null"
            );
    }

    @Test
    void shouldRejectMissingTimeout() {
        ProcessCommand command = command(List.of("codex"), Path.of("/tmp/project"), Map.of(), null, 10_000);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("timeout must not be null");
    }

    @Test
    void shouldRejectZeroTimeout() {
        ProcessCommand command = command(List.of("codex"), Path.of("/tmp/project"), Map.of(), Duration.ZERO, 10_000);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("timeout must be positive");
    }

    @Test
    void shouldRejectNegativeTimeout() {
        ProcessCommand command = command(
            List.of("codex"),
            Path.of("/tmp/project"),
            Map.of(),
            Duration.ofSeconds(-1),
            10_000
        );

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("timeout must be positive");
    }

    @Test
    void shouldRejectNonPositiveOutputLimit() {
        ProcessCommand command = command(List.of("codex"), Path.of("/tmp/project"), Map.of(), Duration.ofSeconds(30), 0);

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);

        assertThat(messages(violations)).contains("max output bytes must be positive");
    }

    private ProcessCommand validCommand() {
        return command(
            List.of("codex", "exec", "--json"),
            Path.of("/tmp/project"),
            Map.of("NO_COLOR", "1"),
            Duration.ofSeconds(30),
            10_000
        );
    }

    private ProcessCommand command(
        List<String> arguments,
        Path workingDirectory,
        Map<String, String> environment,
        Duration timeout,
        int maxOutputBytes
    ) {
        return new ProcessCommand(
            arguments,
            workingDirectory,
            "Implement task",
            environment,
            timeout,
            maxOutputBytes
        );
    }

    private List<String> messages(Set<ConstraintViolation<ProcessCommand>> violations) {
        return violations.stream()
            .map(ConstraintViolation::getMessage)
            .toList();
    }
}
