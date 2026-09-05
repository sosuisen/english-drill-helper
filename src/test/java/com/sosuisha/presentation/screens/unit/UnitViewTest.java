package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.NullAudioPlayer;

import javafx.scene.control.ListView;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class UnitViewTest {
    private static final Unit UNIT_1_1 = new Unit(
        new AudioFile(Path.of("011_Unit 1.1.mp3"), "fingerprint-of-unit-1-1"), Optional.empty()
    );
    private static final Unit UNIT_1_2 = new Unit(
        new AudioFile(Path.of("012_Unit 1.2.mp3"), "fingerprint-of-unit-1-2"), Optional.empty()
    );
    private static final Unit PLAYED_UNIT_1_3 = new Unit(
        new AudioFile(Path.of("013_Unit 1.3.mp3"), "fingerprint-of-unit-1-3"),
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
            List.of(UNIT_1_1, UNIT_1_2, PLAYED_UNIT_1_3), player, new NullUnitRepository(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), UnitViewTest::segmentsOf, Runnable::run
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
        assertTrue(robot.lookup("013_Unit 1.3.mp3  2026-09-05 10:00").tryQuery().isPresent());
    }

    @Test
    @DisplayName("リストのファイル名をクリックすると、選ばれたファイル名がラベルに表示される")
    void clicking_a_file_name_in_the_list_shows_the_selected_file_name_in_the_label(
        FxRobot robot) {
        robot.clickOn("012_Unit 1.2.mp3");

        assertEquals("012_Unit 1.2.mp3", viewModel.selectedFileNameProperty().get());
        verifyThat("#selectedFileName", LabeledMatchers.hasText("012_Unit 1.2.mp3"));
    }

    @Test
    @DisplayName("ファイルを選んで再生ボタンを押すと、そのファイルが再生される")
    void selecting_a_file_and_clicking_play_plays_the_file(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");

        robot.clickOn("#play");

        assertEquals(UNIT_1_1.audioFile().path(), playedFile.get());
    }

    @Test
    @DisplayName("画面には、選択中のファイル名の下にターン行の一覧があり、ViewModelのターン行一覧に接続されている")
    void the_screen_shows_the_turn_rows_of_the_view_model_below_the_selected_file_name(
        FxRobot robot) {
        @SuppressWarnings("unchecked")
        ListView<TurnRow> listView = robot.lookup("#turns").queryAs(ListView.class);

        assertEquals(viewModel.getTurnRows(), listView.getItems());
    }

    @Test
    @DisplayName("ユニットを選ぶと、ターン行の一覧に「1-1 [Key]」「1-3」などのラベルが表示される")
    void selecting_a_unit_shows_the_turn_labels_in_the_list(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(robot.lookup("1-1 [Key]").tryQuery().isPresent());
        assertTrue(robot.lookup("1-3").tryQuery().isPresent());
    }

    @Test
    @DisplayName("停止ボタンを押すと、再生が停止する")
    void clicking_stop_stops_the_playback(FxRobot robot) {
        robot.clickOn("#stop");

        assertTrue(stopped.get());
    }

    /** A regular unit of five drills for UNIT_1_1; nothing for others. */
    private static List<Segment> segmentsOf(AudioFile audioFile) {
        if (!audioFile.fingerprint().equals(UNIT_1_1.audioFile().fingerprint())) {
            return List.of();
        }
        var segments = new ArrayList<Segment>();
        var start = Duration.ZERO;
        for (var d = 0; d < 5; d++) {
            for (var seconds : List.of(3.0 + d, 3.0 + d, 0.6, 2.0 + d, 2.0 + d)) {
                var sound = Duration.ofMillis(Math.round(seconds * 1000));
                segments.add(new Segment(segments.size(), start, sound, Segment.Kind.SOUND));
                start = start.plus(sound);
                var silence = Duration.ofSeconds(2);
                segments.add(new Segment(segments.size(), start, silence, Segment.Kind.SILENCE));
                start = start.plus(silence);
            }
        }
        return segments;
    }
}
