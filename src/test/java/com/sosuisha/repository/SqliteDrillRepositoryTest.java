package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteDrillRepositoryTest {
    @AfterEach
    void cleanup() {
        System.clearProperty("edh.drill.db");
    }

    @Test
    @DisplayName("システムプロパティedh.drill.dbが指定されていると、その値がDBファイルのパスとして解決される")
    void resolves_the_database_file_from_the_system_property_when_it_is_set() {
        System.setProperty("edh.drill.db", "custom/drill.db");

        assertEquals(Path.of("custom", "drill.db"), SqliteDrillRepository.resolveFile());
    }

    @Test
    @DisplayName("システムプロパティedh.drill.dbが未指定の場合、ユーザーホームの.english-drill-helper/drill.dbに解決される")
    void resolves_the_database_file_to_drill_db_in_the_user_home_when_the_system_property_is_not_set() {
        System.clearProperty("edh.drill.db");

        assertEquals(
            Path.of(System.getProperty("user.home"), ".english-drill-helper", "drill.db"),
            SqliteDrillRepository.resolveFile()
        );
    }

    @Test
    @DisplayName("指紋をキーに最終再生日時を保存すると、同じ指紋で読み出せる")
    void the_last_played_at_saved_by_a_fingerprint_is_found_by_the_same_fingerprint(
        @TempDir Path folder) {
        var repository = new SqliteDrillRepository(folder.resolve("drill.db"));
        var fingerprint = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        var playedAt = Instant.parse("2026-09-05T10:00:00Z");

        repository.saveLastPlayedAt(fingerprint, playedAt);

        assertEquals(Optional.of(playedAt), repository.findLastPlayedAt(fingerprint));
    }
}
