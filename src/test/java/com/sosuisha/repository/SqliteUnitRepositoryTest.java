package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteUnitRepositoryTest {
    @Test
    @DisplayName("指紋をキーに最終再生日時を保存すると、同じ指紋で読み出せる")
    void the_last_played_at_saved_by_a_fingerprint_is_found_by_the_same_fingerprint(
        @TempDir Path folder) {
        var repository = new SqliteUnitRepository(new SqliteDatabase(folder.resolve("drill.db")));
        var fingerprint = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        var playedAt = Instant.parse("2026-09-05T10:00:00Z");

        repository.saveLastPlayedAt(fingerprint, playedAt);

        assertEquals(Optional.of(playedAt), repository.findLastPlayedAt(fingerprint));
    }
}
