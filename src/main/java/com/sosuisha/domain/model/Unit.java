package com.sosuisha.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A unit: an audio file together with its record.
 *
 * @param audioFile audio file of the unit
 * @param lastPlayedAt time when the unit was last played, or empty if it has never been played
 */
public record Unit(AudioFile audioFile, Optional<Instant> lastPlayedAt) {
    private static final String INTRODUCTION_UNIT_MARK = "Unit 0.";

    /**
     * Creates the unit.
     *
     * @throws NullPointerException if audioFile or lastPlayedAt is null
     */
    public Unit {
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
     * Tells whether this unit is one of the introduction (Unit 0.x), whose
     * file name contains {@code Unit 0.}. The introduction units are built
     * differently from the other units (see ADR 004).
     *
     * @return true if this unit is an introduction unit
     */
    public boolean isIntroduction() {
        return fileName().contains(INTRODUCTION_UNIT_MARK);
    }

    /**
     * Returns a copy of this unit with the given last played time.
     *
     * @param playedAt time when the unit was last played
     * @return the copy
     * @throws NullPointerException if playedAt is null
     */
    public Unit withLastPlayedAt(Instant playedAt) {
        Objects.requireNonNull(playedAt, "playedAt must not be null");
        return new Unit(audioFile, Optional.of(playedAt));
    }
}
