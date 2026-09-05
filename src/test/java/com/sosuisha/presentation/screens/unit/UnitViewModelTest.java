package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
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
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.NullAudioPlayer;

@ExtendWith(ApplicationExtension.class) // Task needs the JavaFX toolkit
class UnitViewModelTest {
    private static final Function<AudioFile, List<Segment>> NO_SEGMENTS = _ -> List.of();
    private static final Unit UNIT_0_1 = new Unit(
        new AudioFile(Path.of("units", "001_Unit 0.1.mp3"), "fingerprint-of-unit-0-1"),
        Optional.empty()
    );
    private static final Unit UNIT_0_2 = new Unit(
        new AudioFile(Path.of("units", "002_Unit 0.2.mp3"), "fingerprint-of-unit-0-2"),
        Optional.empty()
    );

    @Test
    @DisplayName("渡されたユニットの一覧を、渡された順序のまま保持する")
    void holds_the_given_units_in_the_given_order() {
        var units = List.of(UNIT_0_1, UNIT_0_2);

        var viewModel = newViewModel(units, new NullAudioPlayer());

        assertEquals(units, viewModel.getUnits());
    }

    @Test
    @DisplayName("ユニットを選ぶと、そのファイル名が選択中のファイル名になる")
    void selecting_a_unit_makes_its_file_name_the_selected_file_name() {
        var viewModel = newViewModel(List.of(UNIT_0_1), new NullAudioPlayer());

        viewModel.selectUnit(UNIT_0_1);

        assertEquals("001_Unit 0.1.mp3", viewModel.selectedFileNameProperty().get());
    }

    @Test
    @DisplayName("ユニットを選んで再生すると、その音声ファイルがプレイヤーで再生される")
    void playing_with_a_selected_unit_plays_its_audio_file_with_the_player() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = newViewModel(List.of(UNIT_0_1), recordingPlayer(playedFile));
        viewModel.selectUnit(UNIT_0_1);

        viewModel.play();

        assertEquals(UNIT_0_1.audioFile().path(), playedFile.get());
    }

    @Test
    @DisplayName("ユニットを選んでいない状態で再生しても、何も再生されない")
    void playing_without_a_selected_unit_plays_nothing() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = newViewModel(List.of(UNIT_0_1), recordingPlayer(playedFile));

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
        var viewModel = newViewModel(List.of(UNIT_0_1), player);

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
            List.of(UNIT_0_1), stopCapturingPlayer(stopCallback), repository,
            Clock.fixed(stoppedAt, ZoneOffset.UTC), NO_SEGMENTS, Runnable::run
        );
        viewModel.selectUnit(UNIT_0_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals("fingerprint-of-unit-0-1", savedFingerprint.get());
        assertEquals(stoppedAt, savedPlayedAt.get());
    }

    @Test
    @DisplayName("最終再生日時の表示文字は、Clockの時刻帯での「yyyy-MM-dd HH:mm」である")
    void the_text_of_the_last_played_at_is_yyyy_mm_dd_hh_mm_in_the_zone_of_the_clock() {
        var played = UNIT_0_1.withLastPlayedAt(Instant.parse("2026-09-05T10:05:00Z"));
        var viewModel = new UnitViewModel(
            List.of(played), new NullAudioPlayer(), new NullUnitRepository(),
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
            List.of(UNIT_0_1, UNIT_0_2), stopCapturingPlayer(stopCallback),
            new NullUnitRepository(), Clock.fixed(stoppedAt, ZoneOffset.UTC), NO_SEGMENTS,
            Runnable::run
        );
        viewModel.selectUnit(UNIT_0_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals(
            List.of(UNIT_0_1.withLastPlayedAt(stoppedAt), UNIT_0_2), viewModel.getUnits()
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
            List.of(UNIT_0_1), stopCapturingPlayer(stopCallback), new NullUnitRepository(),
            settableClock(now), NO_SEGMENTS, Runnable::run
        );
        viewModel.selectUnit(UNIT_0_1);
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

    // セグメント読み込みのテスト。ローダーは指紋ごとの固定の対応表、Executor は同期実行または手動実行
    private static final List<Segment> SEGMENTS_0_1 = List.of(
        new Segment(0, Duration.ZERO, Duration.ofMillis(1000), Segment.Kind.SOUND),
        new Segment(1, Duration.ofMillis(1000), Duration.ofMillis(3000), Segment.Kind.SILENCE)
    );
    private static final List<Segment> SEGMENTS_0_2 =
        List.of(new Segment(0, Duration.ZERO, Duration.ofMillis(2500), Segment.Kind.SOUND));
    private static final Function<AudioFile, List<Segment>> SEGMENT_TABLE = audioFile -> Objects
        .requireNonNull(
            Map.of(
                UNIT_0_1.audioFile().fingerprint(), SEGMENTS_0_1,
                UNIT_0_2.audioFile().fingerprint(), SEGMENTS_0_2
            ).get(audioFile.fingerprint())
        );

    @Test
    @DisplayName("ユニットを選ぶと、そのユニットのセグメントがローダーで読み込まれ、セグメント一覧に入る")
    void selecting_a_unit_loads_its_segments_into_the_segment_list() {
        var viewModel = newViewModel(List.of(UNIT_0_1, UNIT_0_2), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_0_1);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_0_1, viewModel.getSegments());
    }

    @Test
    @DisplayName("選択を外すと、セグメント一覧は空になる")
    void clearing_the_selection_empties_the_segment_list() {
        var viewModel = newViewModel(List.of(UNIT_0_1), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_0_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), viewModel.getSegments());
    }

    @Test
    @DisplayName("別のユニットを選ぶと、セグメント一覧は新しいユニットのものに置き換わり、前のものは残らない")
    void selecting_another_unit_replaces_the_segment_list_with_its_segments() {
        var viewModel = newViewModel(List.of(UNIT_0_1, UNIT_0_2), SEGMENT_TABLE, Runnable::run);
        viewModel.selectUnit(UNIT_0_1);
        WaitForAsyncUtils.waitForFxEvents();

        viewModel.selectUnit(UNIT_0_2);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_0_2, viewModel.getSegments());
    }

    @Test
    @DisplayName("セグメントの読み込みはExecutorに渡され、Executorが実行するまでセグメント一覧は空のままである")
    void loading_is_handed_to_the_executor_and_the_list_stays_empty_until_it_runs() {
        var queue = new ArrayDeque<Runnable>();
        var viewModel = newViewModel(List.of(UNIT_0_1), SEGMENT_TABLE, queue::add);

        viewModel.selectUnit(UNIT_0_1);
        WaitForAsyncUtils.waitForFxEvents();
        var beforeRun = List.copyOf(viewModel.getSegments());
        Objects.requireNonNull(queue.poll()).run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(), beforeRun);
        assertEquals(SEGMENTS_0_1, viewModel.getSegments());
    }

    @Test
    @DisplayName("前に選んだユニットの読み込み結果が遅れて届いても、いま選んでいるユニットのセグメント一覧は上書きされない")
    void a_late_result_of_a_previously_selected_unit_does_not_overwrite_the_current_list() {
        var queue = new ArrayDeque<Runnable>();
        var viewModel = newViewModel(List.of(UNIT_0_1, UNIT_0_2), SEGMENT_TABLE, queue::add);
        viewModel.selectUnit(UNIT_0_1);
        viewModel.selectUnit(UNIT_0_2);
        var loadOf0_1 = Objects.requireNonNull(queue.poll());
        var loadOf0_2 = Objects.requireNonNull(queue.poll());

        loadOf0_2.run();
        loadOf0_1.run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(SEGMENTS_0_2, viewModel.getSegments());
    }

    @Test
    @DisplayName("セグメント一覧が更新されると、ドリル一覧がそれに基づいて置き換わる。選択を外してセグメント一覧が空になると、ドリル一覧も空になる")
    void the_drill_list_follows_the_segment_list_and_empties_with_it() {
        var viewModel = newViewModel(List.of(UNIT_0_1), SEGMENT_TABLE, Runnable::run);

        viewModel.selectUnit(UNIT_0_1);
        WaitForAsyncUtils.waitForFxEvents();
        var drillsOfSelected = List.copyOf(viewModel.getDrills());
        viewModel.selectUnit(null);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(List.of(new Drill(List.of(0, 1))), drillsOfSelected);
        assertEquals(List.of(), viewModel.getDrills());
    }

    private static UnitViewModel newViewModel(
        List<Unit> units, Function<AudioFile, List<Segment>> segmentLoader, Executor executor) {
        return new UnitViewModel(
            units, new NullAudioPlayer(), new NullUnitRepository(), Clock.systemUTC(),
            segmentLoader, executor
        );
    }

    private static UnitViewModel newViewModel(List<Unit> units, AudioPlayer player) {
        return new UnitViewModel(
            units, player, new NullUnitRepository(), Clock.systemUTC(), NO_SEGMENTS, Runnable::run
        );
    }

    private static AudioPlayer recordingPlayer(AtomicReference<@Nullable Path> playedFile) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Runnable onStopped) {
                playedFile.set(file);
            }
        };
    }

    private static AudioPlayer stopCapturingPlayer(
        AtomicReference<@Nullable Runnable> stopCallback) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Runnable onStopped) {
                stopCallback.set(onStopped);
            }
        };
    }
}
