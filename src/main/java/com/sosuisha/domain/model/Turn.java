package com.sosuisha.domain.model;

import java.util.Objects;
import java.util.Optional;

/**
 * A turn of a drill: the recording speaks one sentence, then the learner
 * repeats it in the silence that follows. The last turn of a file may have
 * no silence after it.
 *
 * @param number position of the turn in its drill, from one
 * @param soundIndex index of the sound segment of the turn
 * @param silenceIndex index of the silence segment after the sound, or empty
 *        when the file ends with the sound
 */
public record Turn(int number, int soundIndex, Optional<Integer> silenceIndex) {
    /**
     * Creates the turn.
     *
     * @throws NullPointerException if silenceIndex is null
     * @throws IllegalArgumentException if number is below one, or if soundIndex
     *             or the silence index is negative
     */
    public Turn {
        Objects.requireNonNull(silenceIndex, "silenceIndex must not be null");
        if (number < 1) {
            throw new IllegalArgumentException("number must be one or more: " + number);
        }
        if (soundIndex < 0) {
            throw new IllegalArgumentException("soundIndex must not be negative: " + soundIndex);
        }
        silenceIndex.filter(index -> index < 0).ifPresent(index -> {
            throw new IllegalArgumentException("silenceIndex must not be negative: " + index);
        });
    }
}
