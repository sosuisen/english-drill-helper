package com.sosuisha.domain.service;

import java.time.Duration;

/**
 * Receives what happens during the playback of one file. The methods are
 * called on the thread of the user interface.
 */
public interface PlaybackListener {
    /**
     * Called as the playback moves on.
     *
     * @param position time from the beginning of the file to the current position
     */
    void positionChanged(Duration position);

    /**
     * Called once when the playback stops, either because the file reached
     * its end or because {@link AudioPlayer#stop()} was called.
     */
    void stopped();
}
