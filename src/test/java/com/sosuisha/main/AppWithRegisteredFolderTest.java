package com.sosuisha.main;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.repository.SqliteAudioFolderRepository;
import com.sosuisha.repository.SqliteDatabase;

import javafx.stage.Stage;

/**
 * Startup with an audio folder that was registered in an earlier run. The
 * folder is an empty temporary folder, so the unit list is empty but the
 * folder is shown.
 */
@ExtendWith(ApplicationExtension.class)
class AppWithRegisteredFolderTest {
    private Stage stage;

    @Start
    void setup(Stage stage) throws Exception {
        var folder = Files.createTempDirectory("english-drill-player-test");
        var db = folder.resolve("drill.db");
        System.setProperty("edp.drill.db", db.toString());
        new SqliteAudioFolderRepository(new SqliteDatabase(db))
            .save(new AudioFolder("英語のハノン", Files.createDirectory(folder.resolve("hanon"))));
        this.stage = new Stage();
        new App().start(this.stage);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("edp.drill.db");
    }

    @Test
    @DisplayName("起動時に音声フォルダが登録済みなら、登録ダイアログは開かず、そのフォルダを走査して本体に示す（header にそのドリル名）")
    void app_startup_shows_the_registered_folder_without_the_dialog(FxRobot robot) {
        assertTrue(stage.isShowing());
        assertTrue(
            robot.listWindows()
                .stream()
                .noneMatch(
                    window -> window instanceof Stage dialog
                        && "音声フォルダの登録".equals(dialog.getTitle())
                        && dialog.isShowing()
                ),
            "the audio folder dialog is not open"
        );
        verifyThat("#audioFolder", LabeledMatchers.hasText("英語のハノン"));
    }
}
