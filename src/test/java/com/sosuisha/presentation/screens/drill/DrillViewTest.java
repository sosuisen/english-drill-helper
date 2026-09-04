package com.sosuisha.presentation.screens.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import javafx.scene.control.ListView;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class DrillViewTest {
    private DrillViewModel viewModel;

    @Start
    void setup(Stage stage) {
        viewModel = new DrillViewModel(
            List.of(Path.of("001_Unit 0.1.mp3"), Path.of("002_Unit 0.2.mp3"))
        );
        var view = new DrillView(viewModel);
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }

    @Test
    @DisplayName("画面に音声ファイルの一覧が表示される")
    void the_screen_shows_the_list_of_audio_files(FxRobot robot) {
        @SuppressWarnings("unchecked")
        ListView<Path> listView = robot.lookup("#audioFiles").queryAs(ListView.class);

        assertEquals(viewModel.getAudioFiles(), listView.getItems());
    }

    @Test
    @DisplayName("リストのファイル名をクリックすると、選ばれたファイル名がラベルに表示される")
    void clicking_a_file_name_in_the_list_shows_the_selected_file_name_in_the_label(
        FxRobot robot) {
        robot.clickOn("002_Unit 0.2.mp3");

        assertEquals("002_Unit 0.2.mp3", viewModel.selectedFileNameProperty().get());
        verifyThat("#selectedFileName", LabeledMatchers.hasText("002_Unit 0.2.mp3"));
    }
}
