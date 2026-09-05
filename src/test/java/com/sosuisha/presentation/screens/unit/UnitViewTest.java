package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;
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

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

    private static final Path AUDIO_FOLDER = Path.of("D:", "drills");
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
            List.of(UNIT_1_1, UNIT_1_2, PLAYED_UNIT_1_3), AUDIO_FOLDER, player,
            new NullUnitRepository(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), UnitViewTest::segmentsOf, Runnable::run
        );
        var view = new UnitView(viewModel);
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
        stage.setHeight(SHORT_WINDOW_HEIGHT); // the turn list must scroll to show its last rows
    }

    @Test
    @DisplayName("画面のユニット一覧は TableView で、ViewModel のユニット一覧に接続されている")
    void the_unit_list_is_a_table_bound_to_the_units_of_the_view_model(FxRobot robot) {
        @SuppressWarnings("unchecked")
        TableView<Unit> table = robot.lookup("#units").queryAs(TableView.class);

        assertEquals(viewModel.getUnits(), table.getItems());
    }

    @Test
    @DisplayName("ユニット一覧の列は File と Last played の2列で、再生済みのユニットの行には File 列にファイル名、Last played 列に最終再生日時（yyyy-MM-dd HH:mm）が表示される")
    void the_table_has_a_file_column_and_a_last_played_column(FxRobot robot) {
        @SuppressWarnings("unchecked")
        TableView<Unit> table = robot.lookup("#units").queryAs(TableView.class);

        assertEquals(
            List.of("File", "Last played"),
            table.getColumns().stream().map(TableColumn::getText).toList()
        );
        assertTrue(robot.lookup("013_Unit 1.3.mp3").tryQuery().isPresent());
        assertTrue(robot.lookup("2026-09-05 10:00").tryQuery().isPresent());
    }

    @Test
    @DisplayName("ユニット一覧の File 列は残りの幅を使い、Last played 列は日時が収まる固定幅（130px）である")
    void the_file_column_takes_the_remaining_width_and_the_last_played_column_is_fixed(
        FxRobot robot) {
        @SuppressWarnings("unchecked")
        TableView<Unit> table = robot.lookup("#units").queryAs(TableView.class);
        var lastPlayed = table.getColumns().get(1);

        assertEquals(130, lastPlayed.getMinWidth());
        assertEquals(130, lastPlayed.getMaxWidth());
        assertEquals(
            TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS, table.getColumnResizePolicy()
        );
    }

    @Test
    @DisplayName("リストのファイル名をクリックすると、選ばれたユニットの表示名（番号と拡張子を除いた名前）がラベルに表示される")
    void clicking_a_file_name_in_the_list_shows_the_selected_unit_title_in_the_label(
        FxRobot robot) {
        robot.clickOn("012_Unit 1.2.mp3");

        assertEquals("Unit 1.2", viewModel.selectedUnitTitleProperty().get());
        verifyThat("#selectedUnitTitle", LabeledMatchers.hasText("Unit 1.2"));
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
    @DisplayName("ユニットを選ぶと、ターン行の一覧に「1-1」「1-3」などのラベルが表示される")
    void selecting_a_unit_shows_the_turn_labels_in_the_list(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(robot.lookup("1-1").tryQuery().isPresent());
        assertTrue(robot.lookup("1-3").tryQuery().isPresent());
    }

    @Test
    @DisplayName("ターン行をクリックすると、その行のターンの開始位置から再生される")
    void clicking_a_turn_row_plays_from_the_start_of_its_turn(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(cueCellOf(robot, 1));

        assertEquals(UNIT_1_1.audioFile().path(), playedFile.get());
        assertEquals(Duration.ofSeconds(10), playedStart.get()); // 3.0 + 2 + 3.0 + 2
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

        assertTrue(cellOf(robot, "1-1").getStyleClass().contains("drill-start"));
        assertTrue(cellOf(robot, "1-1").getStyle().contains("-color-accent-muted"));
        assertTrue(cellOf(robot, "1-1").getStyle().contains("-fx-border-width: 2 0 0 0"));
        assertTrue(cellOf(robot, "2-1").getStyleClass().contains("drill-start"));
        assertFalse(cellOf(robot, "1-2").getStyleClass().contains("drill-start"));
    }

    @Test
    @DisplayName("Cue の行には cue スタイルが付き、文字は表示されない（アイコンだけ）。Cue でない行はそうでない")
    void cue_rows_have_the_cue_style_and_no_text(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        var cue = cueCellOf(robot, 1);
        assertTrue(cue.getStyleClass().contains("cue"));
        assertEquals("", cue.getText());
        assertFalse(cellOf(robot, "1-3").getStyleClass().contains("cue"));
    }

    private static ListCell<?> cellOf(FxRobot robot, String label) {
        return robot.lookup(label).queryAs(ListCell.class);
    }

    // A cue row has no text, so it is found by its style class and its drill.
    private static ListCell<?> cueCellOf(FxRobot robot, int drillNumber) {
        for (var cell : robot.lookup(".cue").queryAllAs(ListCell.class)) {
            if (cell.getItem() instanceof TurnRow row && row.drillNumber() == drillNumber) {
                return cell;
            }
        }
        throw new AssertionError("no cue row of drill " + drillNumber);
    }

    @Test
    @DisplayName("再生中の行が一覧の可視範囲の外にあるとき、一覧が自動的にスクロールしてその行が見える")
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
    @DisplayName("Play ボタンと Stop ボタンは、文字ではなく Material 2 のアイコン（PLAY_ARROW と STOP）を持つアイコンボタンである")
    void the_play_and_stop_buttons_are_icon_buttons_with_material_icons(FxRobot robot) {
        var play = robot.lookup("#play").queryButton();
        var stop = robot.lookup("#stop").queryButton();

        assertEquals("", play.getText());
        assertEquals("", stop.getText());
        assertEquals(Material2MZ.PLAY_ARROW, ((FontIcon) play.getGraphic()).getIconCode());
        assertEquals(Material2MZ.STOP, ((FontIcon) stop.getGraphic()).getIconCode());
        assertTrue(play.getStyleClass().contains(Styles.BUTTON_ICON));
        assertTrue(stop.getStyleClass().contains(Styles.BUTTON_ICON));
    }

    @Test
    @DisplayName("ユニットが選択されていないときは Play と Stop のボタンは disabled で、ユニットを選ぶと有効になる")
    void the_play_and_stop_buttons_are_disabled_until_a_unit_is_selected(FxRobot robot) {
        var play = robot.lookup("#play").queryButton();
        var stop = robot.lookup("#stop").queryButton();
        assertTrue(play.isDisabled());
        assertTrue(stop.isDisabled());

        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(play.isDisabled());
        assertFalse(stop.isDisabled());
    }

    @Test
    @DisplayName("ウィンドウの既定の幅は 624px（元の 480px の3割増し）である。高さはテストの準備で縮めているので確かめない")
    void the_default_window_is_624_wide(FxRobot robot) {
        var scene = robot.lookup("#units").query().getScene();

        assertEquals(624, scene.getWidth());
    }

    @Test
    @DisplayName("選択中のユニット名のラベルは、見出しとして AtlantaFX の TITLE_3 のスタイルを持つ")
    void the_selected_unit_title_is_styled_as_a_heading(FxRobot robot) {
        assertTrue(
            robot.lookup("#selectedUnitTitle").query().getStyleClass().contains(Styles.TITLE_3)
        );
    }

    @Test
    @DisplayName("ユニット一覧を含む左ペイン（unitPane）と、見出し・ボタン・ターン一覧を含む右ペイン（drillPane）は AtlantaFX の Card である")
    void the_left_and_right_panes_are_cards(FxRobot robot) {
        var unitPane = robot.lookup("#unitPane").queryAs(Card.class);
        var drillPane = robot.lookup("#drillPane").queryAs(Card.class);

        assertTrue(unitPane.lookupAll("#units").size() == 1);
        assertTrue(drillPane.lookupAll("#turns").size() == 1);
        assertTrue(drillPane.lookupAll("#selectedUnitTitle").size() == 1);
        assertTrue(drillPane.lookupAll("#play").size() == 1);
    }

    @Test
    @SuppressWarnings("deprecation") // StageStyle.EXTENDED is a preview feature of JavaFX 26.
    @DisplayName("UnitView は HeaderBar を持つので、ウィンドウのスタイルとして EXTENDED（タイトルバーと一体のメニューバー拡張）を宣言する")
    void the_unit_view_declares_the_extended_stage_style() {
        assertEquals(StageStyle.EXTENDED, new UnitView(viewModel).stageStyle());
    }

    @Test
    @DisplayName("画面の上部に HeaderBar があり、その中にアプリ名「English Drill Helper」が表示される")
    void the_screen_has_a_header_bar_with_the_app_name(FxRobot robot) {
        var headerBar = robot.lookup("#headerBar").query();

        assertTrue(robot.from(headerBar).lookup("English Drill Helper").tryQuery().isPresent());
    }

    @Test
    @DisplayName("Card の中のユニット一覧とターン一覧は、枠が二重にならないよう AtlantaFX の EDGE_TO_EDGE スタイルで自身の枠を持たない")
    void the_lists_inside_the_cards_have_no_borders_of_their_own(FxRobot robot) {
        assertTrue(robot.lookup("#units").query().getStyleClass().contains(Tweaks.EDGE_TO_EDGE));
        assertTrue(robot.lookup("#turns").query().getStyleClass().contains(Tweaks.EDGE_TO_EDGE));
    }

    @Test
    @DisplayName("左右の Card は内側の余白を持たない flush スタイルで、そのスタイルを定義する画面のスタイルシート（unit.css）が読み込まれている。リストが Card の枠まで広がるようにするため")
    void the_cards_are_flush_and_the_screen_stylesheet_is_loaded(FxRobot robot) {
        var unitPane = robot.lookup("#unitPane").query();
        var drillPane = robot.lookup("#drillPane").query();

        assertTrue(unitPane.getStyleClass().contains("flush"));
        assertTrue(drillPane.getStyleClass().contains("flush"));
        assertTrue(
            unitPane.getScene().getStylesheets().stream()
                .anyMatch(s -> s.endsWith("/styles/unit.css"))
        );
    }

    @Test
    @DisplayName("ユニット一覧の Card の header には、現在開いている音声フォルダのパスが表示される")
    void the_unit_pane_header_shows_the_audio_folder(FxRobot robot) {
        var unitPane = robot.lookup("#unitPane").queryAs(Card.class);

        assertEquals(AUDIO_FOLDER.toString(), ((Label) unitPane.getHeader()).getText());
    }

    @Test
    @DisplayName("drillPane の Card の header には選択中ユニットの表示名のラベル（selectedUnitTitle）があり、body に Play/Stop とターン一覧が残る")
    void the_drill_pane_header_is_the_selected_unit_title(FxRobot robot) {
        var drillPane = robot.lookup("#drillPane").queryAs(Card.class);

        assertEquals("selectedUnitTitle", drillPane.getHeader().getId());
        assertTrue(drillPane.getBody().lookupAll("#play").size() == 1);
        assertTrue(drillPane.getBody().lookupAll("#turns").size() == 1);
    }

    @Test
    @DisplayName("Play/Stop のボタンの並び（ID playback）には左右に 8px の余白があり、Card の枠に付かない")
    void the_playback_buttons_have_a_margin_from_the_card_edge(FxRobot robot) {
        var playback = (Region) robot.lookup("#playback").query();

        assertEquals(8, playback.getPadding().getLeft());
        assertEquals(8, playback.getPadding().getRight());
    }

    @Test
    @DisplayName("unitPane の header（音声フォルダのパス）は、drillPane の見出しと揃うよう TITLE_3 のスタイルを持ち、文字色は Cue の行と同じ薄い色（-color-accent-muted）である")
    void the_unit_pane_header_is_a_heading_in_the_cue_color(FxRobot robot) {
        var header = robot.lookup("#audioFolder").query();

        assertTrue(header.getStyleClass().contains(Styles.TITLE_3));
        assertTrue(header.getStyle().contains("-fx-text-fill: -color-accent-muted"));
    }

    @Test
    @DisplayName("Play/Stop の並びの右に再生位置のラベル（ID position）があり、ユニットを選ぶと ViewModel の位置の文字列（00:00 / 総時間）が表示される")
    void the_position_label_next_to_the_buttons_shows_the_position_text(FxRobot robot) {
        var playback = robot.lookup("#playback").query();
        assertTrue(robot.from(playback).lookup("#position").tryQuery().isPresent());

        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#position", LabeledMatchers.hasText(viewModel.positionTextProperty().get()));
        assertEquals("00:00 / 02:23", viewModel.positionTextProperty().get());
    }

    @Test
    @DisplayName("再生位置が通知されると、再生中のターンの行に playing スタイルが付き、他の行には付かない。位置が進むと前の行から外れる。クリックで選んだ行（選択）は動かない")
    void the_playing_row_gets_the_playing_style_and_the_selection_stays(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("1-1"); // selects and plays the first key sentence
        var listener = Objects.requireNonNull(playbackListener.get());
        @SuppressWarnings("unchecked")
        ListView<TurnRow> listView = robot.lookup("#turns").queryAs(ListView.class);

        robot.interact(() -> listener.positionChanged(Duration.ofSeconds(11))); // inside 1-Cue
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(cueCellOf(robot, 1).getStyleClass().contains("playing"));
        assertFalse(cellOf(robot, "1-1").getStyleClass().contains("playing"));

        robot.interact(() -> listener.positionChanged(Duration.ofSeconds(13))); // inside 1-3
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(cellOf(robot, "1-3").getStyleClass().contains("playing"));
        assertFalse(cueCellOf(robot, 1).getStyleClass().contains("playing"));
        assertEquals("1-1", listView.getSelectionModel().getSelectedItem().label());
    }

    @Test
    @DisplayName("画面のスタイルシート（unit.css）には、再生中の行（.list-cell.playing）に左端の緑のバーと薄い緑の背景（縞模様や青系の区切りと色相で見分けられる）を描く規則がある")
    void the_stylesheet_styles_the_playing_row() throws Exception {
        var css = new String(
            Objects.requireNonNull(getClass().getResourceAsStream("/styles/unit.css"))
                .readAllBytes(),
            java.nio.charset.StandardCharsets.UTF_8
        );

        assertTrue(css.contains(".list-cell.playing"));
        assertTrue(css.contains("-color-success-emphasis"));
        assertTrue(css.contains("-color-success-muted"));
    }

    @Test
    @DisplayName("Key sentence の行には鍵のアイコン（VPN_KEY）、Cue の行には吹き出しのアイコン（ANNOUNCEMENT）が graphic として付き、色はどちらも黒の 60%（rgba(0, 0, 0, 0.6)。薄い青では見えにくかった）。アイコンのフォントは正しく描けている Play ボタンのアイコンと同じ（色の指定で Ikonli のフォント指定が消えない）。Answer の行にはアイコンがない")
    void key_and_cue_rows_have_role_icons_and_answer_rows_have_none(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

        var keyIcon = (FontIcon) cellOf(robot, "1-1").getGraphic();
        var cueIcon = (FontIcon) cueCellOf(robot, 1).getGraphic();
        assertEquals(Material2MZ.VPN_KEY, keyIcon.getIconCode());
        assertEquals(Material2AL.ANNOUNCEMENT, cueIcon.getIconCode());
        assertTrue(keyIcon.getStyle().contains("-fx-icon-color: rgba(0, 0, 0, 0.6)"));
        assertTrue(cueIcon.getStyle().contains("-fx-icon-color: rgba(0, 0, 0, 0.6)"));
        var iconFont = ((FontIcon) robot.lookup("#play").queryButton().getGraphic()).getFont();
        assertEquals(iconFont.getFamily(), keyIcon.getFont().getFamily());
        assertEquals(iconFont.getFamily(), cueIcon.getFont().getFamily());
        assertNull(cellOf(robot, "1-3").getGraphic());
    }

    @Test
    @DisplayName("再生を停止しても、選択中のユニット（見出しと一覧の選択）とターン行の一覧は残る。停止時刻の記録でユニット一覧の行が差し替わっても選択は外れない")
    void stopping_keeps_the_selected_unit_and_its_turn_rows(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn("#play");
        var listener = Objects.requireNonNull(playbackListener.get());
        @SuppressWarnings("unchecked")
        ListView<TurnRow> turns = robot.lookup("#turns").queryAs(ListView.class);
        @SuppressWarnings("unchecked")
        TableView<Unit> units = robot.lookup("#units").queryAs(TableView.class);

        robot.clickOn("#stop");
        robot.interact(listener::stopped); // the player reports the stop
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(25, turns.getItems().size());
        assertEquals("Unit 1.1", viewModel.selectedUnitTitleProperty().get());
        assertEquals(
            UNIT_1_1.audioFile(), units.getSelectionModel().getSelectedItem().audioFile()
        );
    }

    @Test
    @DisplayName("停止ボタンを押すと、再生が停止する")
    void clicking_stop_stops_the_playback(FxRobot robot) {
        robot.clickOn("011_Unit 1.1.mp3");
        WaitForAsyncUtils.waitForFxEvents();

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
