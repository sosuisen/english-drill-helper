package com.sosuisha.domain.service;

import java.nio.file.Path;

/**
 * Plays an audio file.
 */
public interface AudioPlayer {
    /**
     * Plays the audio file from the beginning to the end. The callback runs
     * once when the playback stops, either because the file reached its end
     * or because {@link #stop()} was called.
     *
     * @param file audio file to play
     * @param onStopped callback that runs when the playback stops
     * @throws NullPointerException if file or onStopped is null
     */
    void play(Path file, Runnable onStopped);

    /**
     * Stops the playback. Does nothing while nothing is playing.
     */
    void stop();
}
