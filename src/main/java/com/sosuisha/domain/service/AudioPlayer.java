package com.sosuisha.domain.service;

import java.nio.file.Path;

/**
 * Plays an audio file.
 */
public interface AudioPlayer {
    /**
     * Plays the audio file from the beginning to the end.
     *
     * @param file audio file to play
     * @throws NullPointerException if file is null
     */
    void play(Path file);

    /**
     * Stops the playback. Does nothing while nothing is playing.
     */
    void stop();
}
