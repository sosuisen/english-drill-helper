package com.sosuisha.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A unit: an audio file together with its record.
 *
 * @param audioFile audio file of the unit
 * @param lastPlayedAt time when the unit was last played, or empty if it has never been played
 */
public record Unit(AudioFile audioFile, Optional<Instant> lastPlayedAt) {
    private static final String INTRODUCTION_UNIT_MARK = "Unit 0.";
    private static final Pattern NUMBER_PREFIX = Pattern.compile("^\\d+_");
    private static final Pattern AUDIO_EXTENSION =
        Pattern.compile("\\.(mp3|wav)$", Pattern.CASE_INSENSITIVE);

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
     * Returns the name shown for the unit: the file name without the number
     * that orders the files (such as {@code 011_}) and without the audio
     * extension.
     *
     * @return the title, such as {@code Unit 1.1_slow}
     */
    public String title() {
        var withoutNumber = NUMBER_PREFIX.matcher(fileName()).replaceFirst("");
        return AUDIO_EXTENSION.matcher(withoutNumber).replaceFirst("");
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
