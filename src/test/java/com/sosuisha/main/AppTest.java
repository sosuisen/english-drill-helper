package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sosuisha.presentation.screens.drill.DrillView;

import javafx.application.Application;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppTest {
    private Stage stage;
    private Path drillDb;

    @Start
    void setup(Stage stage) throws Exception {
        var folder = Files.createTempDirectory("english-drill-helper-test");
        drillDb = folder.resolve("drill.db");
        System.setProperty("edh.drill.db", drillDb.toString());
        // The injected primary stage is reused across tests and rejects
        // initStyle, so App gets a fresh stage.
        this.stage = new Stage();
        new App().start(this.stage);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("edh.drill.db");
    }

    @Test
    @DisplayName("起動すると、edh.drill.dbで指定した場所にDBファイルが作られる")
    void app_startup_creates_the_database_file_at_the_place_given_by_edh_drill_db() {
        assertTrue(Files.exists(drillDb));
    }

    @Test
    @DisplayName("アプリを起動すると、FIRST_VIEW定数で指定したViewのウィンドウが表示される")
    void app_startup_shows_the_window_of_the_view_specified_by_first_view_constant() {
        var expectedTitles = Map.of(DrillView.class, "English Drill Helper");

        assertTrue(stage.isShowing());
        assertEquals(expectedTitles.get(App.FIRST_VIEW), stage.getTitle());
    }

    @Test
    @DisplayName("起動時にAtlantaFXのPrimer Lightテーマが適用されている")
    void the_atlantafx_primer_light_theme_is_applied_at_startup() {
        var stylesheet = Application.getUserAgentStylesheet();

        assertNotNull(stylesheet);
        assertTrue(stylesheet.endsWith("primer-light.css"));
    }

    @Test
    @DisplayName("決められた音声フォルダは D:\\Dropbox\\英語のハノン_210407 である")
    void the_fixed_audio_folder_is_the_hanon_folder_in_dropbox() {
        assertEquals(Path.of("D:\\Dropbox\\英語のハノン_210407"), App.AUDIO_FOLDER);
    }
}
