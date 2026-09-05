package com.sosuisha.service;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.PlaybackListener;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Audio player backed by JavaFX Media. Starting a file stops the file that
 * is playing. The listener is called on the FX thread, where JavaFX Media
 * reports its progress.
 */
public class MediaAudioPlayer implements AudioPlayer {
    private @Nullable MediaPlayer current;

    @Override
    public void play(Path file, Duration start, PlaybackListener listener) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        disposeCurrent();
        var player = new MediaPlayer(new Media(file.toUri().toString()));
        player.setStartTime(javafx.util.Duration.millis(start.toMillis()));
        player.currentTimeProperty()
            .subscribe(time -> listener.positionChanged(Duration.ofMillis((long) time.toMillis())));
        player.setOnEndOfMedia(listener::stopped);
        player.setOnStopped(listener::stopped);
        player.play();
        current = player;
    }

    @Override
    public void stop() {
        if (current != null) {
            current.stop();
        }
    }

    private void disposeCurrent() {
        if (current != null) {
            current.dispose();
            current = null;
        }
    }
}
