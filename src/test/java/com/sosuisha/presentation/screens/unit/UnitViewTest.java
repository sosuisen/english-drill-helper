package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.NullAudioPlayer;

import javafx.scene.control.ListView;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class UnitViewTest {
    private static final Unit UNIT_0_1 = new Unit(
        new AudioFile(Path.of("001_Unit 0.1.mp3"), "fingerprint-of-unit-0-1"), Optional.empty()
    );
    private static final Unit UNIT_0_2 = new Unit(
        new AudioFile(Path.of("002_Unit 0.2.mp3"), "fingerprint-of-unit-0-2"), Optional.empty()
    );
    private static final Unit PLAYED_UNIT_0_3 = new Unit(
        new AudioFile(Path.of("003_Unit 0.3.mp3"), "fingerprint-of-unit-0-3"),
        Optional.of(Instant.parse("2026-09-05T10:00:00Z"))
    );

    private UnitViewModel viewModel;
    private final AtomicReference<@Nullable Path> playedFile = new AtomicReference<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Start
    void setup(Stage stage) {
        var player = new NullAudioPlayer() {
            @Override
            public void play(Path file, Runnable onStopped) {
                playedFile.set(file);
            }

            @Override
            public void stop() {
                stopped.set(true);
            }
        };
        viewModel = new UnitViewModel(
            List.of(UNIT_0_1, UNIT_0_2, PLAYED_UNIT_0_3), player, new NullUnitRepository(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
        );
        var view = new UnitView(viewModel);
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }

    @Test
    @DisplayName("画面に音声ファイルの一覧が表示される")
    void the_screen_shows_the_list_of_audio_files(FxRobot robot) {
        @SuppressWarnings("unchecked")
        ListView<Unit> listView = robot.lookup("#units").queryAs(ListView.class);

        assertEquals(viewModel.getUnits(), listView.getItems());
    }

    @Test
    @DisplayName("再生済みのユニットのセルには、ファイル名と最終再生日時が表示される")
    void the_cell_of_a_played_unit_shows_the_file_name_and_the_last_played_at(FxRobot robot) {
        assertTrue(robot.lookup("003_Unit 0.3.mp3  2026-09-05 10:00").tryQuery().isPresent());
    }

    @Test
    @DisplayName("リストのファイル名をクリックすると、選ばれたファイル名がラベルに表示される")
    void clicking_a_file_name_in_the_list_shows_the_selected_file_name_in_the_label(
        FxRobot robot) {
        robot.clickOn("002_Unit 0.2.mp3");

        assertEquals("002_Unit 0.2.mp3", viewModel.selectedFileNameProperty().get());
        verifyThat("#selectedFileName", LabeledMatchers.hasText("002_Unit 0.2.mp3"));
    }

    @Test
    @DisplayName("ファイルを選んで再生ボタンを押すと、そのファイルが再生される")
    void selecting_a_file_and_clicking_play_plays_the_file(FxRobot robot) {
        robot.clickOn("001_Unit 0.1.mp3");

        robot.clickOn("#play");

        assertEquals(UNIT_0_1.audioFile().path(), playedFile.get());
    }

    @Test
    @DisplayName("停止ボタンを押すと、再生が停止する")
    void clicking_stop_stops_the_playback(FxRobot robot) {
        robot.clickOn("#stop");

        assertTrue(stopped.get());
    }
}
