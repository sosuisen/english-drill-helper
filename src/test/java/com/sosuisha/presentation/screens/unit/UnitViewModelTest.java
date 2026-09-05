package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Drill;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.Turn;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.NullAudioPlayer;
import com.sosuisha.domain.service.PlaybackListener;

@ExtendWith(ApplicationExtension.class) // Task needs the JavaFX toolkit
class UnitViewModelTest {
    private static final Path AUDIO_FOLDER = Path.of("D:", "drills");
    private static final Function<AudioFile, List<Segment>> NO_SEGMENTS = _ -> List.of();
    private static final Unit UNIT_1_1 = new Unit(
        new AudioFile(Path.of("units", "011_Unit 1.1.mp3"), "fingerprint-of-unit-1-1"),
        Optional.empty()
    );
    private static final Unit UNIT_1_2 = new Unit(
        new AudioFile(Path.of("units", "012_Unit 1.2.mp3"), "fingerprint-of-unit-1-2"),
        Optional.empty()
    );

    @Test
    @DisplayName("渡されたユニットの一覧を、渡された順序のまま保持する")
    void holds_the_given_units_in_the_given_order() {
        var units = List.of(UNIT_1_1, UNIT_1_2);

        var viewModel = newViewModel(units, new NullAudioPlayer());

        assertEquals(units, viewModel.getUnits());
    }

    @Test
    @DisplayName("ユニットを選ぶと、その表示名（番号と拡張子を除いた名前）が選択中のユニットの表示名になる")
    void selecting_a_unit_makes_its_title_the_selected_unit_title() {
        var viewModel = newViewModel(List.of(UNIT_1_1), new NullAudioPlayer());

        viewModel.selectUnit(UNIT_1_1);

        assertEquals("Unit 1.1", viewModel.selectedUnitTitleProperty().get());
    }

    @Test
    @DisplayName("ユニットが選択されているかのプロパティは、未選択で false、選択で true、選択を外すと false になる")
    void the_unit_selected_property_follows_the_selection() {
        var viewModel = newViewModel(List.of(UNIT_1_1), new NullAudioPlayer());

        assertFalse(viewModel.unitSelectedProperty().get());
        viewModel.selectUnit(UNIT_1_1);
        assertTrue(viewModel.unitSelectedProperty().get());
        viewModel.selectUnit(null);
        assertFalse(viewModel.unitSelectedProperty().get());
    }

    @Test
    @DisplayName("音声フォルダのパスは、表示用の文字列として公開される")
    void the_audio_folder_is_exposed_as_text() {
        var viewModel = newViewModel(List.of(UNIT_1_1), new NullAudioPlayer());

        assertEquals(AUDIO_FOLDER.toString(), viewModel.getAudioFolderText());
    }

    @Test
    @DisplayName("ユニットを選んで再生すると、その音声ファイルがプレイヤーで再生される")
    void playing_with_a_selected_unit_plays_its_audio_file_with_the_player() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = newViewModel(List.of(UNIT_1_1), recordingPlayer(playedFile));
        viewModel.selectUnit(UNIT_1_1);

        viewModel.play();

        assertEquals(UNIT_1_1.audioFile().path(), playedFile.get());
    }

    @Test
    @DisplayName("ユニットを選んでいない状態で再生しても、何も再生されない")
    void playing_without_a_selected_unit_plays_nothing() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = newViewModel(List.of(UNIT_1_1), recordingPlayer(playedFile));

        viewModel.play();

        assertNull(playedFile.get());
    }

    @Test
    @DisplayName("停止すると、プレイヤーが停止する")
    void stopping_stops_the_player() {
        var stopped = new AtomicBoolean(false);
        var player = new NullAudioPlayer() {
            @Override
            public void stop() {
                stopped.set(true);
            }
        };
        var viewModel = newViewModel(List.of(UNIT_1_1), player);

        viewModel.stop();

        assertTrue(stopped.get());
    }

    @Test
    @DisplayName("再生が停止すると、選択中ユニットの指紋をキーに停止時刻がリポジトリへ保存される")
    void when_the_playback_stops_the_stop_time_is_saved_by_the_fingerprint_of_the_selected_unit() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var savedFingerprint = new AtomicReference<@Nullable String>();
        var savedPlayedAt = new AtomicReference<@Nullable Instant>();
        var repository = new NullUnitRepository() {
            @Override
            public void saveLastPlayedAt(String fingerprint, Instant playedAt) {
                savedFingerprint.set(fingerprint);
                savedPlayedAt.set(playedAt);
            }
        };
        var stoppedAt = Instant.parse("2026-09-05T10:00:00Z");
        var viewModel = new UnitViewModel(
            List.of(UNIT_1_1), AUDIO_FOLDER, stopCapturingPlayer(stopCallback), repository,
            Clock.fixed(stoppedAt, ZoneOffset.UTC), NO_SEGMENTS, Runnable::run
        );
        viewModel.selectUnit(UNIT_1_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals("fingerprint-of-unit-1-1", savedFingerprint.get());
        assertEquals(stoppedAt, savedPlayedAt.get());
    }

    @Test
    @DisplayName("最終再生日時の表示文字は、Clockの時刻帯での「yyyy-MM-dd HH:mm」である")
    void the_text_of_the_last_played_at_is_yyyy_mm_dd_hh_mm_in_the_zone_of_the_clock() {
        var played = UNIT_1_1.withLastPlayedAt(Instant.parse("2026-09-05T10:05:00Z"));
        var viewModel = new UnitViewModel(
            List.of(played), AUDIO_FOLDER, new NullAudioPlayer(), new NullUnitRepository(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.ofHours(9)), NO_SEGMENTS, Runnable::run
        );

        assertEquals("2026-09-05 19:05", viewModel.lastPlayedAtTextOf(played));
    }

    @Test
    @DisplayName("再生が停止すると、一覧のそのユニットの行の最終再生日時が停止時刻になる")
    void when_the_playback_stops_the_row_of_the_unit_in_the_list_gets_the_stop_time() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var stoppedAt = Instant.parse("2026-09-05T10:00:00Z");
        var viewModel = new UnitViewModel(
            List.of(UNIT_1_1, UNIT_1_2), AUDIO_FOLDER, stopCapturingPlayer(stopCallback),
            new NullUnitRepository(), Clock.fixed(stoppedAt, ZoneOffset.UTC), NO_SEGMENTS,
            Runnable::run
        );
        viewModel.selectUnit(UNIT_1_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals(
            List.of(UNIT_1_1.withLastPlayedAt(stoppedAt), UNIT_1_2), viewModel.getUnits()
        );
    }

    @Test
    @DisplayName("同じユニットを2回続けて停止すると、一覧のその行は2回目の停止時刻になる")
    void stopping_the_same_unit_twice_gives_its_row_the_second_stop_time() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var firstStop = Instant.parse("2026-09-05T10:00:00Z");
        var secondStop = Instant.parse("2026-09-05T11:00:00Z");
        var now = new AtomicReference<Instant>(firstStop);
        var viewModel = new UnitViewModel(
            List.of(UNIT_1_1), AUDIO_FOLDER, stopCapturingPlayer(stopCallback),
            new NullUnitRepository(),
            settableClock(now), NO_SEGMENTS, Runnable::run
        );
        viewModel.selectUnit(UNIT_1_1);
        viewModel.play();
        Objects.requireNonNull(stopCallback.get()).run();
        now.set(secondStop);

        viewModel.play();
        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals(Optional.of(secondStop), viewModel.getUnits().get(0).lastPlayedAt());
    }

    private static Clock settableClock(AtomicReference<Instant> now) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
    }

    // セグメント読み込みのテスト。ローダーは指紋ごとの固定の対応表、Executor は同期実行または手動実行。
    // セグメントは、ハノンの構成（1ユニット5ドリル）に合うよう有音・無音を5組にする
    private static final List<Segment> SEGMENTS_1_1 = regularUnit(0.0);
    private static final List<Segment> SEGMENTS_1_2 = regularUnit(0.3);
    private static final Function<AudioFile, List<Segment>> SEGMENT_TABLE = audioFile -> Objects
        .requireNonNull(
            Map.of(
                UNIT_1_1.audioFile().fingerprint(), SEGMENTS_1_1,
                UNIT_1_2.audioFile().fingerprint(), SEGMENTS_1_2
            ).get(audioFile.fingerprint())
        );

    @Test
    @DisplayName("ユニットを選ぶと、そのユニットのセグメントがローダーで読み込まれ、セグメント一覧に入る")
    void selecting_a_unit_loads_its_segments_into_the_segment_list() {
        var viewModel = newViewModel(List.of(UNIT_1_1, UNIT_1_2), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_1_1, viewModel.getSegments());
    }

    @Test
    @DisplayName("選択を外すと、セグメント一覧は空になる")
    void clearing_the_selection_empties_the_segment_list() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), viewModel.getSegments());
    }

    @Test
    @DisplayName("別のユニットを選ぶと、セグメント一覧は新しいユニットのものに置き換わり、前のものは残らない")
    void selecting_another_unit_replaces_the_segment_list_with_its_segments() {
        var viewModel = newViewModel(List.of(UNIT_1_1, UNIT_1_2), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(UNIT_1_2);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_1_2, viewModel.getSegments());
    }

    @Test
    @DisplayName("セグメントの読み込みはExecutorに渡され、Executorが実行するまでセグメント一覧は空のままである")
    void loading_is_handed_to_the_executor_and_the_list_stays_empty_until_it_runs() {
        var queue = new ArrayDeque<Runnable>();
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, queue::add);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        var beforeRun = List.copyOf(viewModel.getSegments());
        Objects.requireNonNull(queue.poll()).run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), beforeRun);
        assertEquals(SEGMENTS_1_1, viewModel.getSegments());
    }

    @Test
    @DisplayName("前に選んだユニットの読み込み結果が遅れて届いても、いま選んでいるユニットのセグメント一覧は上書きされない")
    void a_late_result_of_a_previously_selected_unit_does_not_overwrite_the_current_list() {
        var queue = new ArrayDeque<Runnable>();
        var viewModel = newViewModel(List.of(UNIT_1_1, UNIT_1_2), SEGMENT_TABLE, queue::add);
        viewModel.selectUnit(UNIT_1_1);
        viewModel.selectUnit(UNIT_1_2);
        var loadOf1_1 = Objects.requireNonNull(queue.poll());
        var loadOf1_2 = Objects.requireNonNull(queue.poll());

        loadOf1_2.run();
        loadOf1_1.run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_1_2, viewModel.getSegments());
    }

    @Test
    @DisplayName("セグメント一覧が更新されると、ドリル一覧がそれに基づいて置き換わる。選択を外してセグメント一覧が空になると、ドリル一覧も空になる")
    void the_drill_list_follows_the_segment_list_and_empties_with_it() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        var drillsOfSelected = List.copyOf(viewModel.getDrills());
        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(5, drillsOfSelected.size());
        assertEquals(
            new Turn(1, Turn.Role.KEY_SENTENCE, List.of(0, 1)),
            drillsOfSelected.get(0).turns().get(0)
        );
        assertEquals(List.of(), viewModel.getDrills());
    }

    @Test
    @DisplayName("ターン行の一覧は、ドリル一覧から「ドリル番号-ターン番号」の順に作られ、Key sentence には [Key] が付く。Cue の行は「ドリル番号-Cue」で、番号は Cue を飛ばして数える。5ドリル × 5ターンなら 1-1 [Key]、1-2 [Key]、1-Cue、1-3、1-4、2-1 [Key] … の25行")
    void turn_rows_are_labeled_drill_number_dash_turn_number_with_key_for_key_sentences() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        var labels = viewModel.getTurnRows().stream().map(TurnRow::label).toList();
        assertEquals(25, labels.size());
        assertEquals(
            List.of("1-1 [Key]", "1-2 [Key]", "1-Cue", "1-3", "1-4", "2-1 [Key]"),
            labels.subList(0, 6)
        );
        assertEquals("5-4", labels.getLast());
    }

    @Test
    @DisplayName("Unit 1.1 型（Key の対が冒頭に1組だけ）のユニットを選ぶと、Key の対はドリル0になり、ターン行は 0-1 [Key]、0-2 [Key]、1-Cue、1-1、1-2、2-Cue … になる")
    void turn_rows_of_a_unit_with_one_key_pair_start_with_the_key_pair_as_drill_zero() {
        Function<AudioFile, List<Segment>> oneKeyPair = _ -> unitWithOneKeyPair();
        var viewModel = newViewModel(List.of(UNIT_1_1), oneKeyPair, Runnable::run);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        var labels = viewModel.getTurnRows().stream().map(TurnRow::label).toList();
        assertEquals(17, labels.size());
        assertEquals(
            List.of("0-1 [Key]", "0-2 [Key]", "1-Cue", "1-1", "1-2", "2-Cue", "2-1", "2-2"),
            labels.subList(0, 8)
        );
    }

    @Test
    @DisplayName("ターン行の一覧は、選択を外してドリル一覧が空になると空になる")
    void turn_rows_empty_when_the_drill_list_empties() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), viewModel.getTurnRows());
    }

    @Test
    @DisplayName("Unit 0.x のユニットを選ぶと、タイトルがドリル0、Cue ごとにドリルが作られ、ターン行は 0-1、1-Cue、1-1 … 1-4、2-Cue … 2-4 の11行になる")
    void an_introduction_unit_has_the_title_as_drill_zero_and_a_drill_per_cue() {
        var unit0_1 = new Unit(
            new AudioFile(Path.of("units", "001_Unit 0.1.mp3"), "fingerprint-of-unit-0-1"),
            Optional.empty()
        );
        Function<AudioFile, List<Segment>> introduction = _ -> introductionUnit();
        var viewModel = newViewModel(List.of(unit0_1), introduction, Runnable::run);

        viewModel.selectUnit(unit0_1);
        WaitForAsyncUtils.waitForFxEvents();

        var labels = viewModel.getTurnRows().stream().map(TurnRow::label).toList();
        assertEquals(11, labels.size());
        assertEquals(List.of("0-1", "1-Cue", "1-1"), labels.subList(0, 3));
        assertEquals("2-4", labels.getLast());
    }

    @Test
    @DisplayName("ターン行を指定して再生すると、選択中ユニットの音声ファイルが、そのターンの最初のセグメントの開始位置から再生される")
    void playing_a_turn_row_plays_the_file_of_the_selected_unit_from_the_start_of_its_first_segment() {
        var played = new AtomicReference<@Nullable Playback>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run, playbackRecordingPlayer(played)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        var cueOfDrill1 = viewModel.getTurnRows().get(2); // 1-3, the cue after the key pair

        viewModel.playTurn(cueOfDrill1);

        var playback = Objects.requireNonNull(played.get());
        assertEquals(UNIT_1_1.audioFile().path(), playback.file());
        assertEquals(SEGMENTS_1_1.get(4).start(), playback.start());
    }

    @Test
    @DisplayName("ターンから再生して停止したときも、選択中ユニットの最終再生日時が記録される")
    void stopping_a_playback_started_from_a_turn_records_the_last_played_time_too() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var savedFingerprint = new AtomicReference<@Nullable String>();
        var repository = new NullUnitRepository() {
            @Override
            public void saveLastPlayedAt(String fingerprint, Instant playedAt) {
                savedFingerprint.set(fingerprint);
            }
        };
        var viewModel = new UnitViewModel(
            List.of(UNIT_1_1), AUDIO_FOLDER, stopCapturingPlayer(stopCallback), repository,
            Clock.systemUTC(),
            SEGMENT_TABLE, Runnable::run
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.playTurn(viewModel.getTurnRows().get(2));

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals("fingerprint-of-unit-1-1", savedFingerprint.get());
    }

    @Test
    @DisplayName("Play ボタンの再生は、従来どおりファイルの先頭（0秒）から再生される")
    void playing_with_the_play_button_starts_from_the_beginning_of_the_file() {
        var played = new AtomicReference<@Nullable Playback>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run, playbackRecordingPlayer(played)
        );
        viewModel.selectUnit(UNIT_1_1);

        viewModel.play();

        assertEquals(Duration.ZERO, Objects.requireNonNull(played.get()).start());
    }

    @Test
    @DisplayName("再生位置が通知されると、その位置を含むターン（最初のセグメントの開始位置が再生位置以下である最後のターン）が再生中のターン行になる")
    void the_playing_turn_row_is_the_last_turn_that_starts_at_or_before_the_position() {
        var listener = new AtomicReference<@Nullable PlaybackListener>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run, listenerCapturingPlayer(listener)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();

        // 1-3 (the cue) starts at 10 s and 1-4 (the first answer) at 12.6 s
        Objects.requireNonNull(listener.get()).positionChanged(Duration.ofMillis(11_000));

        assertEquals(
            Optional.of(viewModel.getTurnRows().get(2)), viewModel.playingTurnRowProperty().get()
        );
    }

    @Test
    @DisplayName("再生位置がどのターンの開始位置より前（冒頭の無音）のときは、再生中のターン行は空である")
    void the_playing_turn_row_is_empty_before_the_first_turn() {
        var listener = new AtomicReference<@Nullable PlaybackListener>();
        Function<AudioFile, List<Segment>> withLeadIn = _ -> withLeadingSilence(SEGMENTS_1_1);
        var viewModel = newViewModel(
            List.of(UNIT_1_1), withLeadIn, Runnable::run, listenerCapturingPlayer(listener)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();

        Objects.requireNonNull(listener.get()).positionChanged(Duration.ofMillis(300));

        assertEquals(Optional.empty(), viewModel.playingTurnRowProperty().get());
    }

    @Test
    @DisplayName("選択を外してターン一覧が空になると、再生中のターン行も空になる")
    void the_playing_turn_row_empties_with_the_turn_rows() {
        var listener = new AtomicReference<@Nullable PlaybackListener>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run, listenerCapturingPlayer(listener)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();
        Objects.requireNonNull(listener.get()).positionChanged(Duration.ofMillis(11_000));

        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(Optional.empty(), viewModel.playingTurnRowProperty().get());
    }

    private static AudioPlayer listenerCapturingPlayer(
        AtomicReference<@Nullable PlaybackListener> captured) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Duration start, PlaybackListener listener) {
                captured.set(listener);
            }
        };
    }

    /** The segments with one second of silence put before them, renumbered from zero. */
    private static List<Segment> withLeadingSilence(List<Segment> segments) {
        var leadIn = Duration.ofSeconds(1);
        var result = new ArrayList<Segment>();
        result.add(new Segment(0, Duration.ZERO, leadIn, Segment.Kind.SILENCE));
        for (var segment : segments) {
            result.add(
                new Segment(
                    segment.index() + 1, segment.start().plus(leadIn), segment.duration(),
                    segment.kind()
                )
            );
        }
        return result;
    }

    @Test
    @DisplayName("再生中に別のユニットを選ぶと、再生が停止する")
    void selecting_another_unit_while_playing_stops_the_playback() {
        var stopped = new AtomicBoolean(false);
        var player = new NullAudioPlayer() {
            @Override
            public void stop() {
                stopped.set(true);
            }
        };
        var viewModel =
            newViewModel(List.of(UNIT_1_1, UNIT_1_2), SEGMENT_TABLE, Runnable::run, player);
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();

        viewModel.selectUnit(UNIT_1_2);

        assertTrue(stopped.get());
    }

    @Test
    @DisplayName("再生中に別のユニットを選ぶと、再生中のターン行は空になる")
    void selecting_another_unit_while_playing_clears_the_playing_turn_row() {
        var listener = new AtomicReference<@Nullable PlaybackListener>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1, UNIT_1_2), SEGMENT_TABLE, Runnable::run,
            listenerCapturingPlayer(listener)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();
        Objects.requireNonNull(listener.get()).positionChanged(Duration.ofMillis(11_000));

        viewModel.selectUnit(UNIT_1_2);

        assertEquals(Optional.empty(), viewModel.playingTurnRowProperty().get());
    }

    @Test
    @DisplayName("ユニットを選ぶと、再生位置の文字列は「00:00 / 総時間」になる。総時間はセグメント列の末尾の終了時刻（合成ユニットは有音93秒 + 無音50秒 = 143秒）")
    void selecting_a_unit_shows_zero_over_the_total_length_of_its_segments() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("00:00 / 02:23", viewModel.positionTextProperty().get());
    }

    @Test
    @DisplayName("再生位置が通知されると、文字列の前半がその位置になる。秒は切り捨て（83.9秒 → 01:23）")
    void a_reported_position_updates_the_first_half_of_the_position_text() {
        var listener = new AtomicReference<@Nullable PlaybackListener>();
        var viewModel = newViewModel(
            List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run, listenerCapturingPlayer(listener)
        );
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();
        viewModel.play();

        Objects.requireNonNull(listener.get()).positionChanged(Duration.ofMillis(83_900));

        assertEquals("01:23 / 02:23", viewModel.positionTextProperty().get());
    }

    @Test
    @DisplayName("選択を外すと、再生位置の文字列は空になる")
    void clearing_the_selection_empties_the_position_text() {
        var viewModel = newViewModel(List.of(UNIT_1_1), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_1_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("", viewModel.positionTextProperty().get());
    }

    /** What a player was asked to play: the file and the start position. */
    private record Playback(Path file, Duration start) {
    }

    private static AudioPlayer playbackRecordingPlayer(AtomicReference<@Nullable Playback> played) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Duration start, PlaybackListener listener) {
                played.set(new Playback(file, start));
            }
        };
    }

    private static UnitViewModel newViewModel(
        List<Unit> units, Function<AudioFile, List<Segment>> segmentLoader, Executor executor,
        AudioPlayer player) {
        return new UnitViewModel(
            units, AUDIO_FOLDER, player, new NullUnitRepository(), Clock.systemUTC(), segmentLoader,
            executor
        );
    }

    /**
     * Segments of a regular unit: five drills of a key sentence pair, a cue,
     * and an answer pair, each sound followed by two seconds of silence. The
     * offset makes the sounds of one unit differ from those of another.
     */
    private static List<Segment> regularUnit(double offsetSeconds) {
        var sounds = new ArrayList<Double>();
        for (var d = 0; d < 5; d++) {
            var key = 3.0 + d + offsetSeconds;
            var answer = 2.0 + d + offsetSeconds;
            sounds.addAll(List.of(key, key, 0.6, answer, answer));
        }
        return segmentsOf(sounds);
    }

    /** Segments of a Unit 1.1 type unit: one key sentence pair, then five sets. */
    private static List<Segment> unitWithOneKeyPair() {
        var sounds = new ArrayList<Double>(List.of(3.0, 3.0));
        for (var s = 0; s < 5; s++) {
            sounds.addAll(List.of(0.6, 2.0 + s, 2.0 + s));
        }
        return segmentsOf(sounds);
    }

    /** Segments of an introduction unit: a title, then two drills of a cue and four sentences. */
    private static List<Segment> introductionUnit() {
        return segmentsOf(List.of(1.5, 0.4, 1.4, 1.4, 1.3, 1.3, 0.35, 1.6, 1.6, 1.4, 1.4));
    }

    /** Sounds of the given lengths in seconds, each followed by two seconds of silence. */
    private static List<Segment> segmentsOf(List<Double> soundSeconds) {
        var segments = new ArrayList<Segment>();
        var start = Duration.ZERO;
        for (var seconds : soundSeconds) {
            var sound = Duration.ofMillis(Math.round(seconds * 1000));
            segments.add(new Segment(segments.size(), start, sound, Segment.Kind.SOUND));
            start = start.plus(sound);
            var silence = Duration.ofSeconds(2);
            segments.add(new Segment(segments.size(), start, silence, Segment.Kind.SILENCE));
            start = start.plus(silence);
        }
        return segments;
    }

    private static UnitViewModel newViewModel(
        List<Unit> units, Function<AudioFile, List<Segment>> segmentLoader, Executor executor) {
        return new UnitViewModel(
            units, AUDIO_FOLDER, new NullAudioPlayer(), new NullUnitRepository(), Clock.systemUTC(),
            segmentLoader, executor
        );
    }

    private static UnitViewModel newViewModel(List<Unit> units, AudioPlayer player) {
        return new UnitViewModel(
            units, AUDIO_FOLDER, player, new NullUnitRepository(), Clock.systemUTC(), NO_SEGMENTS,
            Runnable::run
        );
    }

    private static AudioPlayer recordingPlayer(AtomicReference<@Nullable Path> playedFile) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Duration start, PlaybackListener listener) {
                playedFile.set(file);
            }
        };
    }

    private static AudioPlayer stopCapturingPlayer(
        AtomicReference<@Nullable Runnable> stopCallback) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Duration start, PlaybackListener listener) {
                stopCallback.set(listener::stopped);
            }
        };
    }
}
