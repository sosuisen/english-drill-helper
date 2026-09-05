package com.sosuisha.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sosuisha.domain.exception.IrregularUnitException;

/**
 * A drill of the drill book: a fixed number of turns in which the recording
 * speaks and the learner repeats. Every unit of the book has
 * {@link #DRILLS_PER_UNIT} drills, and every drill of a unit has the same
 * number of turns.
 *
 * @param number position of the drill in its unit, from one
 * @param turns turns of the drill in time order
 */
public record Drill(int number, List<Turn> turns) {
    /** Number of drills in every unit of the drill book. */
    public static final int DRILLS_PER_UNIT = 5;

    private static final int SEGMENTS_PER_TURN = 2;

    /**
     * Creates the drill. The list is copied.
     *
     * @throws NullPointerException if turns is null
     * @throws IllegalArgumentException if number is below one or turns is empty
     */
    public Drill {
        Objects.requireNonNull(turns, "turns must not be null");
        if (number < 1) {
            throw new IllegalArgumentException("number must be one or more: " + number);
        }
        if (turns.isEmpty()) { throw new IllegalArgumentException("turns must not be empty"); }
        turns = List.copyOf(turns);
    }

    /**
     * Builds the drills of a unit from its segments, which alternate between
     * sound and silence in time order. A silence at the beginning of the
     * file belongs to no drill and is dropped. A sound at the end of the
     * file without a silence after it makes the last turn without silence.
     * The remaining segments, counted as if the last silence were there,
     * are split evenly into {@link #DRILLS_PER_UNIT} drills.
     *
     * @param segments segments of the unit in time order, sound and silence alternating
     * @return the drills in time order, empty if there are no segments
     * @throws NullPointerException if segments is null
     * @throws IrregularUnitException if the segments do not split evenly into the drills
     */
    public static List<Drill> drillsOf(List<Segment> segments) throws IrregularUnitException {
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.isEmpty()) { return List.of(); }
        var pairs = pairsOf(withoutLeadingSilence(segments));
        var normalizedSegments = pairs.size() * SEGMENTS_PER_TURN;
        if (normalizedSegments % (DRILLS_PER_UNIT * SEGMENTS_PER_TURN) != 0) {
            throw new IrregularUnitException(
                "The unit does not split into " + DRILLS_PER_UNIT + " drills: " + pairs.size()
                    + " turns",
                null
            );
        }
        return groupIntoDrills(pairs, pairs.size() / DRILLS_PER_UNIT);
    }

    /** A sound and the silence after it, before the turns are numbered. */
    private record Pair(int soundIndex, Optional<Integer> silenceIndex) {
    }

    private static List<Segment> withoutLeadingSilence(List<Segment> segments) {
        return segments.getFirst().kind() == Segment.Kind.SILENCE
            ? segments.subList(1, segments.size())
            : segments;
    }

    /** Pairs each sound with the silence after it; the last sound may have none. */
    private static List<Pair> pairsOf(List<Segment> body) {
        var pairs = new ArrayList<Pair>();
        for (var i = 0; i < body.size(); i += SEGMENTS_PER_TURN) {
            var silence = i + 1 < body.size()
                ? Optional.of(body.get(i + 1).index())
                : Optional.<Integer>empty();
            pairs.add(new Pair(body.get(i).index(), silence));
        }
        return pairs;
    }

    private static List<Drill> groupIntoDrills(List<Pair> pairs, int turnsPerDrill) {
        var drills = new ArrayList<Drill>();
        for (var drillNumber = 1; drillNumber <= DRILLS_PER_UNIT; drillNumber++) {
            var turns = new ArrayList<Turn>();
            for (var turnNumber = 1; turnNumber <= turnsPerDrill; turnNumber++) {
                var pair = pairs.get((drillNumber - 1) * turnsPerDrill + turnNumber - 1);
                turns.add(new Turn(turnNumber, pair.soundIndex(), pair.silenceIndex()));
            }
            drills.add(new Drill(drillNumber, turns));
        }
        return List.copyOf(drills);
    }
}
