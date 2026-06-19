package com.aiq.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProcessRunnerTest {

    private static final int DEFAULT_MAX_OUTPUT_BYTES = 10_000;

    @TempDir
    private Path workingDirectory;

    private final ProcessRunner runner = new ProcessRunner(
        Validation.buildDefaultValidatorFactory().getValidator()
    );

    @Test
    void shouldRunSuccessfulProcessAndCaptureStdout() {
        ProcessRunResult result = runner.run(command("stdout"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("hello from stdout");
        assertThat(result.stderr()).isEmpty();
        assertThat(result.rawOutput()).isEqualTo("hello from stdout");
        assertThat(result.duration()).isPositive();
        assertThat(result.timedOut()).isFalse();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void shouldPassStdinToProcess() {
        ProcessRunResult result = runner.run(command("stdin", "prompt body"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("prompt body");
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void shouldCaptureStderrSeparately() {
        ProcessRunResult result = runner.run(command("stderr"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).isEqualTo("hello from stderr");
        assertThat(result.rawOutput()).isEqualTo("hello from stderr");
    }

    @Test
    void shouldReturnNonZeroExitCodeWithoutThrowing() {
        ProcessRunResult result = runner.run(command(
            List.of("exit", "7"),
            null,
            Map.of(),
            Duration.ofSeconds(5),
            DEFAULT_MAX_OUTPUT_BYTES
        ));

        assertThat(result.exitCode()).isEqualTo(7);
        assertThat(result.timedOut()).isFalse();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void shouldKillProcessWhenTimeoutIsExceeded() {
        ProcessRunResult result = runner.run(command(
            List.of("sleep", "5000"),
            null,
            Map.of(),
            Duration.ofMillis(100),
            DEFAULT_MAX_OUTPUT_BYTES
        ));

        assertThat(result.exitCode()).isEqualTo(-1);
        assertThat(result.timedOut()).isTrue();
        assertThat(result.errorMessage()).contains("timed out");
    }

    @Test
    void shouldFailFastWhenOutputLimitIsExceeded() {
        ProcessRunResult result = runner.run(command(
            List.of("large-output", "10000"),
            null,
            Map.of(),
            Duration.ofSeconds(5),
            100
        ));

        assertThat(result.exitCode()).isEqualTo(-1);
        assertThat(result.timedOut()).isFalse();
        assertThat(result.errorMessage()).contains("output exceeded");
        assertThat(result.rawOutput().getBytes()).hasSizeLessThanOrEqualTo(100);
    }

    @Test
    void shouldValidateCommandBeforeRunningProcess() {
        ProcessCommand invalidCommand = new ProcessCommand(
            List.of(),
            workingDirectory,
            null,
            Map.of(),
            Duration.ofSeconds(1),
            DEFAULT_MAX_OUTPUT_BYTES
        );

        assertThatThrownBy(() -> runner.run(invalidCommand))
            .isInstanceOf(ConstraintViolationException.class)
            .hasMessageContaining("process arguments must not be empty");
    }

    @Test
    void shouldRunProcessInConfiguredWorkingDirectory() throws Exception {
        ProcessRunResult result = runner.run(command("working-directory"));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(workingDirectory.toFile().getCanonicalPath());
    }

    @Test
    void shouldReturnClearErrorWhenWorkingDirectoryDoesNotExist() {
        Path missingDirectory = workingDirectory.resolve("missing");
        ProcessRunResult result = runner.run(new ProcessCommand(
            javaCommand(List.of("stdout")),
            missingDirectory,
            null,
            Map.of(),
            Duration.ofSeconds(5),
            DEFAULT_MAX_OUTPUT_BYTES
        ));

        assertThat(result.exitCode()).isEqualTo(-1);
        assertThat(result.errorMessage()).contains("Working directory does not exist");
        assertThat(result.errorMessage()).contains(missingDirectory.toString());
    }

    @Test
    void shouldMergeCustomEnvironment() {
        ProcessRunResult result = runner.run(command(
            List.of("environment", "AIQ_TEST_ENV"),
            null,
            Map.of("AIQ_TEST_ENV", "available"),
            Duration.ofSeconds(5),
            DEFAULT_MAX_OUTPUT_BYTES
        ));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("available");
    }

    private ProcessCommand command(String testProgramCommand) {
        return command(List.of(testProgramCommand), null, Map.of(), Duration.ofSeconds(5), DEFAULT_MAX_OUTPUT_BYTES);
    }

    private ProcessCommand command(String testProgramCommand, String stdin) {
        return command(List.of(testProgramCommand), stdin, Map.of(), Duration.ofSeconds(5), DEFAULT_MAX_OUTPUT_BYTES);
    }

    private ProcessCommand command(
        List<String> testProgramArguments,
        String stdin,
        Map<String, String> environment,
        Duration timeout,
        int maxOutputBytes
    ) {
        return new ProcessCommand(
            javaCommand(testProgramArguments),
            workingDirectory,
            stdin,
            environment,
            timeout,
            maxOutputBytes
        );
    }

    private List<String> javaCommand(List<String> testProgramArguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add(javaExecutable().toString());
        arguments.add("-cp");
        arguments.add(System.getProperty("java.class.path"));
        arguments.add(ProcessRunnerTestProgram.class.getName());
        arguments.addAll(testProgramArguments);

        return arguments;
    }

    private Path javaExecutable() {
        String executableName = System.getProperty("os.name").toLowerCase().contains("win")
            ? "java.exe"
            : "java";

        return Path.of(System.getProperty("java.home"), "bin", executableName);
    }
}
