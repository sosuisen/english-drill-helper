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

import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.NullAudioPlayer;

class DrillViewModelTest {
    @Test
    @DisplayName("渡された音声ファイルの一覧を、渡された順序のまま保持する")
    void holds_the_given_audio_files_in_the_given_order() {
        var files = List.of(Path.of("001_Unit 0.1.mp3"), Path.of("002_Unit 0.2.mp3"));

        var viewModel = new DrillViewModel(files, new NullAudioPlayer(), Clock.systemUTC());

        assertEquals(files, viewModel.getAudioFiles());
    }

    @Test
    @DisplayName("音声ファイルを選ぶと、そのファイル名が選択中のファイル名になる")
    void selecting_an_audio_file_makes_its_file_name_the_selected_file_name() {
        var file = Path.of("drills", "001_Unit 0.1.mp3");
        var viewModel = new DrillViewModel(List.of(file), new NullAudioPlayer(), Clock.systemUTC());

        viewModel.selectAudioFile(file);

        assertEquals("001_Unit 0.1.mp3", viewModel.selectedFileNameProperty().get());
    }

    @Test
    @DisplayName("音声ファイルを選んで再生すると、そのファイルがプレイヤーで再生される")
    void playing_with_a_selected_audio_file_plays_the_file_with_the_player() {
        var file = Path.of("drills", "001_Unit 0.1.mp3");
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel =
            new DrillViewModel(List.of(file), recordingPlayer(playedFile), Clock.systemUTC());
        viewModel.selectAudioFile(file);

        viewModel.play();

        assertEquals(file, playedFile.get());
    }

    @Test
    @DisplayName("音声ファイルを選んでいない状態で再生しても、何も再生されない")
    void playing_without_a_selected_audio_file_plays_nothing() {
        var playedFile = new AtomicReference<@Nullable Path>();
        var viewModel = new DrillViewModel(
            List.of(Path.of("001_Unit 0.1.mp3")), recordingPlayer(playedFile), Clock.systemUTC()
        );

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
        var viewModel =
            new DrillViewModel(List.of(Path.of("001_Unit 0.1.mp3")), player, Clock.systemUTC());

        viewModel.stop();

        assertTrue(stopped.get());
    }

    private static AudioPlayer recordingPlayer(AtomicReference<@Nullable Path> playedFile) {
        return new NullAudioPlayer() {
            @Override
            public void play(Path file, Runnable onStopped) {
                playedFile.set(file);
            }
        };
    }

    @Test
    @DisplayName("再生が停止すると、その時点の時刻が最終再生日時になる")
    void when_the_playback_stops_the_time_at_that_point_becomes_the_last_played_at() {
        var file = Path.of("001_Unit 0.1.mp3");
        var stopCallback = new AtomicReference<@Nullable Runnable>();
        var player = new NullAudioPlayer() {
            @Override
            public void play(Path file, Runnable onStopped) {
                stopCallback.set(onStopped);
            }
        };
        var stoppedAt = Instant.parse("2026-09-05T10:00:00Z");
        var viewModel =
            new DrillViewModel(List.of(file), player, Clock.fixed(stoppedAt, ZoneOffset.UTC));
        viewModel.selectAudioFile(file);
        viewModel.play();

        Objects.requireNonNull(stopCallback.get()).run();

        assertEquals(Optional.of(stoppedAt), viewModel.lastPlayedAtProperty().get());
    }
}
