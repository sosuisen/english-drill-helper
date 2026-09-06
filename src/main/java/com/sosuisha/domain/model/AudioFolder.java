package com.sosuisha.domain.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A folder of drill audio files that the learner registered, with the name
 * the learner gave it (shown as the drill name).
 *
 * @param name name given by the learner, not blank
 * @param path path of the folder
 */
public record AudioFolder(String name, Path path) {
    /**
     * Creates the folder.
     *
     * @throws NullPointerException if name or path is null
     * @throws IllegalArgumentException if name is blank
     */
    public AudioFolder {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(path, "path must not be null");
        if (name.isBlank()) { throw new IllegalArgumentException("name must not be blank"); }
    }
}
