package com.sosuisha.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import com.sosuisha.domain.service.Fingerprinter;

/**
 * Fingerprinter that uses the SHA-256 of the whole file content, written as
 * lower-case hex.
 */
public class Sha256Fingerprinter implements Fingerprinter {
    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 64 * 1024;

    @Override
    public String fingerprint(Path file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        var digest = newDigest();
        try (var in = new DigestInputStream(Files.newInputStream(file), digest)) {
            drain(in);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Every JDK must support SHA-256, so this is a bug, not an I/O error.
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        }
    }

    private static void drain(InputStream in) throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        while (in.read(buffer) != -1) {
            // reading updates the digest
        }
    }
}
