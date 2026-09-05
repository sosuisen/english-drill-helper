package com.sosuisha.presentation.screens.unit;

/**
 * State of the playback on the unit screen.
 */
public enum PlaybackState {
    /** Nothing is playing. */
    STOPPED,
    /** The selected unit is playing. */
    PLAYING,
    /** The playback is paused and can go on from where it stopped. */
    PAUSED
}
