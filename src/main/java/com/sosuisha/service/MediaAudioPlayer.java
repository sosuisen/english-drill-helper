package com.sosuisha.service;

import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.service.AudioPlayer;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Audio player backed by JavaFX Media. Starting a file stops the file that
 * is playing.
 */
public class MediaAudioPlayer implements AudioPlayer {
    private @Nullable MediaPlayer current;

    @Override
    public void play(Path file, Runnable onStopped) {
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(onStopped, "onStopped must not be null");
        disposeCurrent();
        var player = new MediaPlayer(new Media(file.toUri().toString()));
        player.setOnEndOfMedia(onStopped);
        player.setOnStopped(onStopped);
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
