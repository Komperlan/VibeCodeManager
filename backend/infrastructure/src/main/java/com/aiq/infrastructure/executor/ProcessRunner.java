package com.aiq.infrastructure.executor;

import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessRunner {

    private static final int FAILED_EXIT_CODE = -1;
    private static final int STREAM_BUFFER_SIZE = 8 * 1024;

    private final Validator validator;

    public ProcessRunResult run(ProcessCommand command) {
        validate(command);

        Instant startedAt = Instant.now();
        if (!Files.isDirectory(command.workingDirectory())) {
            return failedResult(
                Duration.between(startedAt, Instant.now()),
                "Working directory does not exist or is not a directory: " + command.workingDirectory()
            );
        }

        Process process;

        try {
            process = startProcess(command);
        } catch (IOException exception) {
            return failedResult(
                Duration.between(startedAt, Instant.now()),
                "Failed to start process: " + exception.getMessage()
            );
        }

        OutputLimit outputLimit = new OutputLimit(command.maxOutputBytes());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> stdoutFuture = executor.submit(() -> readStream(process.getInputStream(), outputLimit, process));
            Future<byte[]> stderrFuture = executor.submit(() -> readStream(process.getErrorStream(), outputLimit, process));
            Future<String> stdinFuture = executor.submit(() -> writeStdin(process.getOutputStream(), command.stdin()));

            boolean finished = waitForProcess(process, command.timeout());
            if (!finished) {
                destroyProcess(process);
            }

            byte[] stdoutBytes = waitForOutput(stdoutFuture);
            byte[] stderrBytes = waitForOutput(stderrFuture);
            String stdinError = waitForStdin(stdinFuture);
            Duration duration = Duration.between(startedAt, Instant.now());

            String stdout = new String(stdoutBytes, StandardCharsets.UTF_8);
            String stderr = new String(stderrBytes, StandardCharsets.UTF_8);
            String rawOutput = combineRawOutput(stdout, stderr);

            if (!finished) {
                return new ProcessRunResult(
                    FAILED_EXIT_CODE,
                    stdout,
                    stderr,
                    rawOutput,
                    duration,
                    true,
                    "Process timed out after " + command.timeout()
                );
            }

            if (outputLimit.isExceeded()) {
                return new ProcessRunResult(
                    FAILED_EXIT_CODE,
                    stdout,
                    stderr,
                    rawOutput,
                    duration,
                    false,
                    "Process output exceeded maxOutputBytes limit: " + command.maxOutputBytes()
                );
            }

            if (stdinError != null && process.exitValue() == 0) {
                return new ProcessRunResult(
                    FAILED_EXIT_CODE,
                    stdout,
                    stderr,
                    rawOutput,
                    duration,
                    false,
                    stdinError
                );
            }

            return new ProcessRunResult(
                process.exitValue(),
                stdout,
                stderr,
                rawOutput,
                duration,
                false,
                stdinError
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            destroyProcess(process);
            return failedResult(
                Duration.between(startedAt, Instant.now()),
                "Process execution was interrupted"
            );
        } catch (ExecutionException exception) {
            destroyProcess(process);
            return failedResult(
                Duration.between(startedAt, Instant.now()),
                "Failed to read process output: " + rootMessage(exception)
            );
        }
    }

    private void validate(ProcessCommand command) {
        Objects.requireNonNull(command, "process command must not be null");

        Set<ConstraintViolation<ProcessCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private Process startProcess(ProcessCommand command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command.arguments()));
        processBuilder.directory(command.workingDirectory().toFile());
        processBuilder.environment().putAll(command.environment());

        return processBuilder.start();
    }

    private boolean waitForProcess(Process process, Duration timeout) throws InterruptedException {
        long timeoutMillis = Math.max(1, timeout.toMillis());

        return process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private byte[] readStream(InputStream inputStream, OutputLimit outputLimit, Process process) throws IOException {
        try (inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[STREAM_BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int allowedBytes = outputLimit.reserve(bytesRead);
                if (allowedBytes > 0) {
                    outputStream.write(buffer, 0, allowedBytes);
                }
                if (allowedBytes < bytesRead) {
                    destroyProcess(process);
                    break;
                }
            }

            return outputStream.toByteArray();
        }
    }

    private String writeStdin(OutputStream outputStream, String stdin) {
        try (outputStream) {
            if (stdin != null) {
                outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            return null;
        } catch (IOException exception) {
            return "Failed to write process stdin: " + exception.getMessage();
        }
    }

    private byte[] waitForOutput(Future<byte[]> future) throws InterruptedException, ExecutionException {
        return future.get();
    }

    private String waitForStdin(Future<String> future) throws InterruptedException, ExecutionException {
        return future.get();
    }

    private void destroyProcess(Process process) {
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private ProcessRunResult failedResult(Duration duration, String errorMessage) {
        return new ProcessRunResult(
            FAILED_EXIT_CODE,
            "",
            "",
            "",
            duration,
            false,
            errorMessage
        );
    }

    private String combineRawOutput(String stdout, String stderr) {
        if (stdout.isEmpty()) {
            return stderr;
        }
        if (stderr.isEmpty()) {
            return stdout;
        }

        return stdout + System.lineSeparator() + stderr;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current.getMessage();
    }

    private static final class OutputLimit {

        private final int maxBytes;
        private final AtomicInteger reservedBytes = new AtomicInteger();
        private final AtomicBoolean exceeded = new AtomicBoolean();

        private OutputLimit(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        private int reserve(int requestedBytes) {
            while (true) {
                int currentBytes = reservedBytes.get();
                int remainingBytes = maxBytes - currentBytes;

                if (remainingBytes <= 0) {
                    exceeded.set(true);
                    return 0;
                }

                int allowedBytes = Math.min(requestedBytes, remainingBytes);
                if (reservedBytes.compareAndSet(currentBytes, currentBytes + allowedBytes)) {
                    if (allowedBytes < requestedBytes) {
                        exceeded.set(true);
                    }
                    return allowedBytes;
                }
            }
        }

        private boolean isExceeded() {
            return exceeded.get();
        }
    }
}
