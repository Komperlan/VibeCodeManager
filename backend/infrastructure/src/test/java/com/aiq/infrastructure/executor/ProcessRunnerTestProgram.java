package com.aiq.infrastructure.executor;

import java.io.File;
import java.nio.charset.StandardCharsets;

final class ProcessRunnerTestProgram {

    private ProcessRunnerTestProgram() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "stdout" -> System.out.print("hello from stdout");
            case "stderr" -> System.err.print("hello from stderr");
            case "stdin" -> System.out.print(new String(System.in.readAllBytes(), StandardCharsets.UTF_8));
            case "exit" -> System.exit(Integer.parseInt(args[1]));
            case "sleep" -> Thread.sleep(Long.parseLong(args[1]));
            case "large-output" -> System.out.print("x".repeat(Integer.parseInt(args[1])));
            case "working-directory" -> System.out.print(new File(".").getCanonicalPath());
            case "environment" -> System.out.print(System.getenv(args[1]));
            default -> throw new IllegalArgumentException("Unknown test command: " + args[0]);
        }
    }
}
