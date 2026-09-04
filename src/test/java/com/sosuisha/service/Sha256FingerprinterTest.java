package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Sha256FingerprinterTest {
    @Test
    @DisplayName("ファイルの指紋は、内容全体のSHA-256を小文字16進で表したものである")
    void the_fingerprint_is_the_sha256_of_the_whole_content_in_lower_case_hex(
        @TempDir Path folder) throws IOException {
        var file = folder.resolve("001_Unit 0.1.mp3");
        Files.writeString(file, "hello");
        var fingerprinter = new Sha256Fingerprinter();

        var fingerprint = fingerprinter.fingerprint(file);

        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", fingerprint
        );
    }
}
