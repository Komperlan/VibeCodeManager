package com.aiq.application.project;

public record CreateProjectCommand(
    String name,
    String rootDirectory,
    String codexSessionId
) {

    public CreateProjectCommand(String name, String rootDirectory) {
        this(name, rootDirectory, null);
    }
}
