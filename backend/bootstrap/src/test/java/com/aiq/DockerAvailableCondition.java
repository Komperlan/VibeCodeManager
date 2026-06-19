package com.aiq;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.URI;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

final class DockerAvailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
        ConditionEvaluationResult.enabled("Docker is available for Testcontainers");

    private static final ConditionEvaluationResult DISABLED =
        ConditionEvaluationResult.disabled("Docker is not available for Testcontainers");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return dockerAvailable() ? ENABLED : DISABLED;
    }

    private static boolean dockerAvailable() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.isBlank()) {
            return dockerHostAvailable(dockerHost);
        }

        return unixSocketAvailable(Path.of("/var/run/docker.sock"));
    }

    private static boolean dockerHostAvailable(String dockerHost) {
        URI uri = URI.create(dockerHost);
        if ("unix".equals(uri.getScheme())) {
            return unixSocketAvailable(Path.of(uri.getPath()));
        }
        if (!"tcp".equals(uri.getScheme())) {
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 500);
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean unixSocketAvailable(Path socketPath) {
        if (!Files.exists(socketPath)) {
            return false;
        }

        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(UnixDomainSocketAddress.of(socketPath));
            return true;
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }
}
