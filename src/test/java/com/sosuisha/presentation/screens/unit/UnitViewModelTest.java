package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.NullAudioPlayer;

class UnitViewModelTest {
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
            Clock.fixed(stoppedAt, ZoneOffset.UTC)
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
            Clock.fixed(Instant.EPOCH, ZoneOffset.ofHours(9))
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
            new NullUnitRepository(), Clock.fixed(stoppedAt, ZoneOffset.UTC)
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
            settableClock(now)
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

    private static UnitViewModel newViewModel(List<Unit> units, AudioPlayer player) {
        return new UnitViewModel(units, player, new NullUnitRepository(), Clock.systemUTC());
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
