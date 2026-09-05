package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqliteDatabaseTest {
    @AfterEach
    void cleanup() {
        System.clearProperty("edp.drill.db");
    }

    @Test
    @DisplayName("システムプロパティedp.drill.dbが指定されていると、その値がDBファイルのパスとして解決される")
    void resolves_the_database_file_from_the_system_property_when_it_is_set() {
        System.setProperty("edp.drill.db", "custom/drill.db");

        assertEquals(Path.of("custom", "drill.db"), SqliteDatabase.resolveFile());
    }

    @Test
    @DisplayName("システムプロパティedp.drill.dbが未指定の場合、ユーザーホームの.english-drill-player/drill.dbに解決される")
    void resolves_the_database_file_to_drill_db_in_the_user_home_when_the_system_property_is_not_set() {
        System.clearProperty("edp.drill.db");

        assertEquals(
            Path.of(System.getProperty("user.home"), ".english-drill-player", "drill.db"),
            SqliteDatabase.resolveFile()
        );
    }
}
