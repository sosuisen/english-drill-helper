package com.sosuisha.domain.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * An audio file of a unit, identified by the fingerprint of its content
 * (see ADR 002).
 *
 * @param path path of the audio file
 * @param fingerprint fingerprint of the file content
 */
public record AudioFile(Path path, String fingerprint) {
    /**
     * Creates the audio file.
     *
     * @throws NullPointerException if path or fingerprint is null
     */
    public AudioFile {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
    }

    /**
     * Returns the file name shown to the user.
     *
     * @return the last element of the path
     */
    public String fileName() {
        var name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }
}
