package com.sosuisha.domain.service;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Plays an audio file.
 */
public interface AudioPlayer {
    /**
     * Plays the audio file from the given position to the end. The listener
     * is told the position as the playback moves on, and once when the
     * playback stops, either because the file reached its end or because
     * {@link #stop()} was called.
     *
     * @param file audio file to play
     * @param start time from the beginning of the file at which to start
     * @param listener receives the position and the stop
     * @throws NullPointerException if file, start, or listener is null
     */
    void play(Path file, Duration start, PlaybackListener listener);

    /**
     * Pauses the playback, keeping its position. The listener is not told a
     * stop. Does nothing while nothing is playing.
     */
    void pause();

    /**
     * Goes on with a paused playback from where it was paused. Does nothing
     * while nothing is paused.
     */
    void resume();

    /**
     * Stops the playback. Does nothing while nothing is playing.
     */
    void stop();
}
