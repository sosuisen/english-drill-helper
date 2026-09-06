package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import com.sosuisha.presentation.screens.unit.UnitView;

import javafx.application.Application;
import javafx.stage.Modality;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AppTest {
    private Stage stage;
    private Path unitDb;

    @Start
    void setup(Stage stage) throws Exception {
        var folder = Files.createTempDirectory("english-drill-player-test");
        unitDb = folder.resolve("drill.db");
        System.setProperty("edp.drill.db", unitDb.toString());
        // The injected primary stage is reused across tests and rejects
        // initStyle, so App gets a fresh stage.
        this.stage = new Stage();
        new App().start(this.stage);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("edp.drill.db");
    }

    @Test
    @DisplayName("起動すると、edp.drill.dbで指定した場所にDBファイルが作られる")
    void app_startup_creates_the_database_file_at_the_place_given_by_edh_unit_db() {
        assertTrue(Files.exists(unitDb));
    }

    @Test
    @DisplayName("アプリを起動すると、FIRST_VIEW定数で指定したViewのウィンドウが表示される")
    void app_startup_shows_the_window_of_the_view_specified_by_first_view_constant() {
        var expectedTitles = Map.of(UnitView.class, "English Drill Player");

        assertTrue(stage.isShowing());
        assertEquals(expectedTitles.get(App.FIRST_VIEW), stage.getTitle());
    }

    @Test
    @DisplayName("起動時にAtlantaFXのNord Lightテーマが適用されている")
    void the_atlantafx_nord_light_theme_is_applied_at_startup() {
        var stylesheet = Application.getUserAgentStylesheet();

        assertNotNull(stylesheet);
        assertTrue(stylesheet.endsWith("nord-light.css"));
    }

    @Test
    @DisplayName("起動時に音声フォルダが未登録なら、音声フォルダの登録ダイアログが本体のウィンドウをオーナーとするモーダルで開く。本体のユニット一覧は空で、header のドリル名も空")
    void app_startup_opens_the_audio_folder_dialog_as_a_modal_when_no_folder_is_registered(
        FxRobot robot) {
        var dialog = (Stage) robot.window("音声フォルダの登録");

        assertTrue(dialog.isShowing());
        assertEquals(Modality.WINDOW_MODAL, dialog.getModality());
        assertEquals(stage, dialog.getOwner());
        verifyThat("#audioFolder", LabeledMatchers.hasText(""));
    }
}
