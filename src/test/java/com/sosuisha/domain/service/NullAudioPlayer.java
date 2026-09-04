package com.sosuisha.domain.service;

import java.nio.file.Path;

/**
 * Audio player that does nothing. Used by tests that do not care about
 * playback, and as a base of test spies that record one call.
 */
public class NullAudioPlayer implements AudioPlayer {
    @Override
    public void play(Path file, Runnable onStopped) {
        // does nothing
    }

    @Override
    public void stop() {
        // does nothing
    }
}
