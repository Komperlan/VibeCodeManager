package com.aiq.application.project;

public record CreateProjectCommand(
    String name,
    String rootDirectory
) {
}
