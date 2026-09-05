package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;

class UnitLoaderTest {
    @Test
    @DisplayName("読み込んだ各ユニットには、フォルダの音声ファイルと、リポジトリに記録された最終再生日時が付く")
    void each_loaded_unit_carries_the_audio_file_and_the_last_played_at_recorded_in_the_repository(
        @TempDir Path folder) throws IOException {
        Files.createFile(folder.resolve("001_Unit 0.1.mp3"));
        Files.createFile(folder.resolve("002_Unit 0.2.mp3"));
        var playedAt = Instant.parse("2026-09-05T10:00:00Z");
        var repository = new NullUnitRepository() {
            @Override
            public Optional<Instant> findLastPlayedAt(String fingerprint) {
                return fingerprint.equals("fingerprint-of-001_Unit 0.1.mp3")
                    ? Optional.of(playedAt)
                    : Optional.empty();
            }
        };
        var scanner =
            new FileSystemAudioFolderScanner(file -> "fingerprint-of-" + file.getFileName());
        var loader = new UnitLoader(scanner, repository);

        var units = loader.load(folder);

        assertEquals(
            List.of(
                new Unit(
                    new AudioFile(
                        folder.resolve("001_Unit 0.1.mp3"), "fingerprint-of-001_Unit 0.1.mp3"
                    ),
                    Optional.of(playedAt)
                ),
                new Unit(
                    new AudioFile(
                        folder.resolve("002_Unit 0.2.mp3"), "fingerprint-of-002_Unit 0.2.mp3"
                    ),
                    Optional.empty()
                )
            ),
            units
        );
    }
}
