package com.sosuisha.domain.service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Computes the fingerprint that identifies a file by its content.
 */
public interface Fingerprinter {
    /**
     * Computes the fingerprint of the file.
     *
     * @param file file to fingerprint
     * @return fingerprint of the file
     * @throws NullPointerException if file is null
     * @throws IOException if the file cannot be read
     */
    String fingerprint(Path file) throws IOException;
}
