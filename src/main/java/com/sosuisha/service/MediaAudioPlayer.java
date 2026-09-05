package com.sosuisha.service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.PlaybackListener;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Subscription;

/**
 * Audio player backed by JavaFX Media. Starting a file stops the file that
 * is playing. The listener is called on the FX thread, where JavaFX Media
 * reports its progress.
 */
public class MediaAudioPlayer implements AudioPlayer {
    private @Nullable MediaPlayer current;
    private @Nullable Subscription positions;

    @Override
    public void play(Path file, Duration start, PlaybackListener listener) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        disposeCurrent();
        var player = new MediaPlayer(new Media(file.toUri().toString()));
        player.setStartTime(javafx.util.Duration.millis(start.toMillis()));
        positions = player.currentTimeProperty()
            .subscribe(time -> listener.positionChanged(Duration.ofMillis((long) time.toMillis())));
        player.setOnEndOfMedia(listener::stopped);
        player.setOnStopped(listener::stopped);
        player.play();
        current = player;
    }

    @Override
    public void pause() {
        if (current != null) {
            current.pause();
        }
    }

    @Override
    public void resume() {
        if (current != null && current.getStatus() == MediaPlayer.Status.PAUSED) {
            current.play();
        }
    }

    @Override
    public void stop() {
        if (current != null) {
            // MediaPlayer.stop() moves the position back to the start time. That position
            // does not belong to the playback, so the positions are no longer reported.
            stopReportingPositions();
            current.stop();
        }
    }

    private void stopReportingPositions() {
        if (positions != null) {
            positions.unsubscribe();
            positions = null;
        }
    }

    private void disposeCurrent() {
        stopReportingPositions();
        if (current != null) {
            current.dispose();
            current = null;
        }
    }
}
