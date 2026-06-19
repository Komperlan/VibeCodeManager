package com.aiq.application.path;

import java.nio.file.Path;

public final class LocalPathNormalizer {

    private LocalPathNormalizer() {
    }

    public static String normalizeDirectory(String value) {
        return toDirectoryPath(value).toString();
    }

    public static Path toDirectoryPath(String value) {
        if (value == null || value.isBlank()) {
            return Path.of("").toAbsolutePath().normalize();
        }

        return Path.of(expandUserHome(value.trim())).toAbsolutePath().normalize();
    }

    public static String executablePath(Path value) {
        return expandUserHome(value.toString());
    }

    public static String expandUserHome(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.equals("~")) {
            return userHome();
        }
        if (trimmedValue.startsWith("~/") || trimmedValue.startsWith("~\\")) {
            return Path.of(userHome(), trimmedValue.substring(2)).toString();
        }

        return trimmedValue;
    }

    private static String userHome() {
        return System.getProperty("user.home");
    }
}
