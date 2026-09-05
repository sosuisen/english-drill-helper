package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import com.sosuisha.domain.service.PlaybackListener;

import atlantafx.base.theme.Styles;
import javafx.scene.control.ListCell;
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

    private static final double SHORT_WINDOW_HEIGHT = 300;

    private UnitViewModel viewModel;
    private final AtomicReference<@Nullable Path> playedFile = new AtomicReference<>();
    private final AtomicReference<@Nullable Duration> playedStart = new AtomicReference<>();
    private final AtomicReference<@Nullable PlaybackListener> playbackListener =
        new AtomicReference<>();
    private final AtomicInteger playCount = new AtomicInteger();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Start
    void setup(Stage stage) {
        var player = new NullAudioPlayer() {
            @Override
            public void play(Path file, Duration start, PlaybackListener listener) {
                playedFile.set(file);
                playedStart.set(start);
                playbackListener.set(listener);
                playCount.incrementAndGet();
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
        stage.setHeight(SHORT_WINDOW_HEIGHT); // the turn list must scroll to show its last rows
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
    @DisplayName("ターン行をクリックすると、その行のターンの開始位置から再生される")
    void clicking_a_turn_row_plays_from_the_start_of_its_turn(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("1-Cue");

        assertEquals(UNIT_1_1.audioFile().path(), playedFile.get());
        assertEquals(Duration.ofSeconds(10), playedStart.get()); // 3.0 + 2 + 3.0 + 2
    }

    @Test
    @DisplayName("再生中のターン行が変わると、ターン一覧の選択行がそれに追従する。この選択変更では再生は始まらない")
    void the_selected_turn_row_follows_the_playing_turn_without_starting_a_playback(
        FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#play");
        var listener = Objects.requireNonNull(playbackListener.get());

        robot.interact(() -> listener.positionChanged(Duration.ofSeconds(11))); // inside 1-Cue
        WaitForAsyncUtils.waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<TurnRow> listView = robot.lookup("#turns").queryAs(ListView.class);
        assertEquals("1-Cue", listView.getSelectionModel().getSelectedItem().label());
        assertEquals(1, playCount.get());
    }

    @Test
    @DisplayName("ユニット一覧とターン一覧は、行の高さを詰めた AtlantaFX の dense スタイルである")
    void the_unit_list_and_the_turn_list_use_the_dense_style(FxRobot robot) {
        assertTrue(robot.lookup("#units").query().getStyleClass().contains(Styles.DENSE));
        assertTrue(robot.lookup("#turns").query().getStyleClass().contains(Styles.DENSE));
    }

    @Test
    @DisplayName("ユニット一覧とターン一覧は、行の色が交互に変わる AtlantaFX の striped スタイルである")
    void the_unit_list_and_the_turn_list_use_the_striped_style(FxRobot robot) {
        assertTrue(robot.lookup("#units").query().getStyleClass().contains(Styles.STRIPED));
        assertTrue(robot.lookup("#turns").query().getStyleClass().contains(Styles.STRIPED));
    }

    @Test
    @DisplayName("各ドリルの先頭の行には、ドリルの区切りとして薄い青（-color-accent-muted）の 2px の上線が引かれる。先頭でない行には引かれない")
    void the_first_row_of_each_drill_has_a_top_border(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(cellOf(robot, "1-1 [Key]").getStyleClass().contains("drill-start"));
        assertTrue(cellOf(robot, "1-1 [Key]").getStyle().contains("-color-accent-muted"));
        assertTrue(cellOf(robot, "1-1 [Key]").getStyle().contains("-fx-border-width: 2 0 0 0"));
        assertTrue(cellOf(robot, "2-1 [Key]").getStyleClass().contains("drill-start"));
        assertFalse(cellOf(robot, "1-2 [Key]").getStyleClass().contains("drill-start"));
    }

    @Test
    @DisplayName("Cue の行は文字色がドリルの区切りと同じ薄い青（cue スタイル、色は -color-accent-muted）。Cue でない行はそうでない")
    void cue_rows_have_the_muted_cue_style(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(cellOf(robot, "1-Cue").getStyleClass().contains("cue"));
        assertTrue(
            cellOf(robot, "1-Cue").getStyle().contains("-fx-text-fill: -color-accent-muted")
        );
        assertFalse(cellOf(robot, "1-3").getStyleClass().contains("cue"));
    }

    private static ListCell<?> cellOf(FxRobot robot, String label) {
        return robot.lookup(label).queryAs(ListCell.class);
    }

    @Test
    @DisplayName("再生位置に追従した選択行が一覧の可視範囲の外にあるとき、一覧が自動的にスクロールしてその行が見える")
    void the_turn_list_scrolls_to_the_playing_turn_when_it_is_out_of_view(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#play");
        var listener = Objects.requireNonNull(playbackListener.get());
        assertFalse(robot.lookup("5-4").tryQuery().isPresent()); // the last row is not shown yet

        robot.interact(() -> listener.positionChanged(Duration.ofSeconds(9_999))); // past every
                                                                                   // turn
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(robot.lookup("5-4").tryQuery().isPresent());
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
