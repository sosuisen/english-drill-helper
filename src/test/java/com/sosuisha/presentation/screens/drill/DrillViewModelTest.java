package com.sosuisha.presentation.screens.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
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
import com.sosuisha.domain.model.Drill;
import com.sosuisha.domain.repository.NullDrillRepository;
import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.NullAudioPlayer;

class DrillViewModelTest {
    private static final Drill UNIT_0_1 = new Drill(
        new AudioFile(Path.of("drills", "001_Unit 0.1.mp3"), "fingerprint-of-unit-0-1"),
        Optional.empty()
    );
    private static final Drill UNIT_0_2 = new Drill(
        new AudioFile(Path.of("drills", "002_Unit 0.2.mp3"), "fingerprint-of-unit-0-2"),
        Optional.empty()
    );

    @Test
    @DisplayName("渡されたドリルの一覧を、渡された順序のまま保持する")
    void holds_the_given_drills_in_the_given_order() {
        var drills = List.of(UNIT_0_1, UNIT_0_2);

        var viewModel = newViewModel(drills, new NullAudioPlayer());

        assertEquals(drills, viewModel.getDrills());
    }

    @Test
    @DisplayName("ドリルを選ぶと、そのファイル名が選択中のファイル名になる")
    void selecting_a_drill_makes_its_file_name_the_selected_file_name() {
        var viewModel = newViewModel(List.of(UNIT_0_1), new NullAudioPlayer());

        viewModel.selectDrill(UNIT_0_1);

        assertEquals("001_Unit 0.1.mp3", viewModel.selectedFileNameProperty().get());
    }

    @Test
    @DisplayName("ドリルを選んで再生すると、その音声ファイルがプレイヤーで再生される")
    void playing_with_a_selected_drill_plays_its_audio_file_with_the_player() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = newViewModel(List.of(UNIT_0_1), recordingPlayer(playedFile));
        viewModel.selectDrill(UNIT_0_1);

        viewModel.play();

        assertEquals(UNIT_0_1.audioFile().path(), playedFile.get());
    }

    @Test
    @DisplayName("ドリルを選んでいない状態で再生しても、何も再生されない")
    void playing_without_a_selected_drill_plays_nothing() {
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
    @DisplayName("再生が停止すると、その時点の時刻が最終再生日時になる")
    void when_the_playback_stops_the_time_at_that_point_becomes_the_last_played_at() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var stoppedAt = Instant.parse("2026-09-05T10:00:00Z");
        var viewModel = new DrillViewModel(
            List.of(UNIT_0_1), stopCapturingPlayer(stopCallback), new NullDrillRepository(),
            Clock.fixed(stoppedAt, ZoneOffset.UTC)
        );
        viewModel.selectDrill(UNIT_0_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals(Optional.of(stoppedAt), viewModel.lastPlayedAtProperty().get());
    }

    @Test
    @DisplayName("再生が停止すると、選択中ドリルの指紋をキーに停止時刻がリポジトリへ保存される")
    void when_the_playback_stops_the_stop_time_is_saved_by_the_fingerprint_of_the_selected_drill() {
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var savedFingerprint = new AtomicReference<@Nullable String>();
        var savedPlayedAt = new AtomicReference<@Nullable Instant>();
        var repository = new NullDrillRepository() {
            @Override
            public void saveLastPlayedAt(String fingerprint, Instant playedAt) {
                savedFingerprint.set(fingerprint);
                savedPlayedAt.set(playedAt);
            }
        };
        var stoppedAt = Instant.parse("2026-09-05T10:00:00Z");
        var viewModel = new DrillViewModel(
            List.of(UNIT_0_1), stopCapturingPlayer(stopCallback), repository,
            Clock.fixed(stoppedAt, ZoneOffset.UTC)
        );
        viewModel.selectDrill(UNIT_0_1);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals("fingerprint-of-unit-0-1", savedFingerprint.get());
        assertEquals(stoppedAt, savedPlayedAt.get());
    }

    @Test
    @DisplayName("最終再生日時の表示文字は、Clockの時刻帯での「yyyy-MM-dd HH:mm」である")
    void the_text_of_the_last_played_at_is_yyyy_mm_dd_hh_mm_in_the_zone_of_the_clock() {
        var played = UNIT_0_1.withLastPlayedAt(Instant.parse("2026-09-05T10:05:00Z"));
        var viewModel = new DrillViewModel(
            List.of(played), new NullAudioPlayer(), new NullDrillRepository(),
            Clock.fixed(Instant.EPOCH, ZoneOffset.ofHours(9))
        );

        assertEquals("2026-09-05 19:05", viewModel.lastPlayedAtTextOf(played));
    }

    private static DrillViewModel newViewModel(List<Drill> drills, AudioPlayer player) {
        return new DrillViewModel(drills, player, new NullDrillRepository(), Clock.systemUTC());
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
