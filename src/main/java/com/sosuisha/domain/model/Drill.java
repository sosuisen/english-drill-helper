package com.sosuisha.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A drill: an audio file together with its record.
 *
 * @param audioFile audio file of the drill
 * @param lastPlayedAt time when the drill was last played, or empty if it has never been played
 */
public record Drill(AudioFile audioFile, Optional<Instant> lastPlayedAt) {
    /**
     * Creates the drill.
     *
     * @throws NullPointerException if audioFile or lastPlayedAt is null
     */
    public Drill {
        Objects.requireNonNull(audioFile, "audioFile must not be null");
        Objects.requireNonNull(lastPlayedAt, "lastPlayedAt must not be null");
    }

    /**
     * Returns the file name shown to the user.
     *
     * @return the file name of the audio file
     */
    public String fileName() {
        return audioFile.fileName();
    }

    /**
     * Returns a copy of this drill with the given last played time.
     *
     * @param playedAt time when the drill was last played
     * @return the copy
     * @throws NullPointerException if playedAt is null
     */
    public Drill withLastPlayedAt(Instant playedAt) {
        Objects.requireNonNull(playedAt, "playedAt must not be null");
        return new Drill(audioFile, Optional.of(playedAt));
    }
}
