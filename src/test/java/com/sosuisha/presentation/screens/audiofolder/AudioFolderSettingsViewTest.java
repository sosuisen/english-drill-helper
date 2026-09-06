package com.sosuisha.presentation.screens.audiofolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.domain.repository.NullAudioFolderRepository;

import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class AudioFolderSettingsViewTest {
    private static final Path HANON = Path.of("D:", "drills", "hanon");

    private AudioFolderSettingsViewModel viewModel;
    private final AtomicInteger chooserCalls = new AtomicInteger();

    @Start
    void setup(Stage stage) {
        viewModel = new AudioFolderSettingsViewModel(new NullAudioFolderRepository(), _ -> {
        });
        var view = new AudioFolderSettingsView(viewModel, _ -> {
            chooserCalls.incrementAndGet();
            return Optional.of(HANON);
        });
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }

    @Test
    @DisplayName("ダイアログには案内のラベル「音声ファイルのあるフォルダを追加してください」、「追加」ボタン、ドリル名のフィールド、ドリルの場所のラベル、「保存」ボタンがあり、保存ボタンはフォルダを選ぶまで押せない")
    void the_dialog_has_the_guide_the_add_button_the_name_field_the_location_and_the_save_button(
        FxRobot robot) {
        verifyThat("#guide", LabeledMatchers.hasText("音声ファイルのあるフォルダを追加してください"));
        verifyThat("#add", LabeledMatchers.hasText("追加"));
        verifyThat("#name", TextInputControlMatchers.hasText(""));
        verifyThat("#location", LabeledMatchers.hasText(""));
        verifyThat("#save", LabeledMatchers.hasText("保存"));
        assertTrue(robot.lookup("#save").queryButton().isDisabled());

        robot.interact(() -> viewModel.chooseFolder(HANON));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#name", TextInputControlMatchers.hasText("hanon"));
        verifyThat("#location", LabeledMatchers.hasText(HANON.toString()));
        assertFalse(robot.lookup("#save").queryButton().isDisabled());
    }

    @Test
    @DisplayName("「追加」を押すとフォルダ選択（差し替えた関数）が呼ばれ、選んだフォルダが ViewModel に入る")
    void clicking_add_asks_the_folder_chooser_and_passes_the_folder_to_the_view_model(
        FxRobot robot) {
        robot.clickOn("#add");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, chooserCalls.get());
        assertEquals(Optional.of(HANON), viewModel.chosenFolderProperty().get());
    }
}
