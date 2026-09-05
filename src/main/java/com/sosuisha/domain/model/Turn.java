package com.sosuisha.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * A turn of a drill: one time the recording or the learner speaks (see
 * ADR 004). A turn usually holds a sound segment and the silence after it,
 * in which the learner repeats. A key sentence with a pause inside holds
 * sound, pause, sound, and silence. The last turn of a file may have no
 * silence after it.
 *
 * @param number position of the turn in its drill, from one
 * @param role what the turn is in the drill
 * @param segmentIndexes indexes of the segments of the turn in time order,
 *        at least one
 */
public record Turn(int number, Role role, List<Integer> segmentIndexes) {
    /** What a turn is in a drill. */
    public enum Role {
        /** The model sentence of a drill, spoken twice at its start. */
        KEY_SENTENCE,
        /** The short prompt the learner answers to. */
        CUE,
        /** The answer to a cue, spoken twice by the recording. */
        ANSWER,
        /** A sentence of an introduction unit, or its title. */
        SENTENCE
    }

    /**
     * Creates the turn. The list is copied.
     *
     * @throws NullPointerException if role or segmentIndexes is null
     * @throws IllegalArgumentException if number is below one, if segmentIndexes
     *             is empty, or if an index is negative
     */
    public Turn {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(segmentIndexes, "segmentIndexes must not be null");
        if (number < 1) {
            throw new IllegalArgumentException("number must be one or more: " + number);
        }
        if (segmentIndexes.isEmpty()) {
            throw new IllegalArgumentException("segmentIndexes must not be empty");
        }
        if (segmentIndexes.stream().anyMatch(index -> index < 0)) {
            throw new IllegalArgumentException(
                "segmentIndexes must not be negative: " + segmentIndexes
            );
        }
        segmentIndexes = List.copyOf(segmentIndexes);
    }

    /**
     * Returns the index of the first segment of the turn, where playback of
     * the turn starts.
     *
     * @return the first segment index
     */
    public int firstSegmentIndex() {
        return segmentIndexes.getFirst();
    }
}
