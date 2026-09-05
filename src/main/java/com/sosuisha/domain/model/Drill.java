package com.sosuisha.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A drill: one sentence of a unit, made of one sound segment and, when the
 * file has one, the silence after it that is meant for shadowing.
 *
 * @param segmentIndexes indexes of the segments of the drill in time order:
 *        the sound, then the silence when there is one
 */
public record Drill(List<Integer> segmentIndexes) {
    /**
     * Creates the drill. The list is copied.
     *
     * @throws NullPointerException if segmentIndexes is null
     * @throws IllegalArgumentException if segmentIndexes is empty
     */
    public Drill {
        Objects.requireNonNull(segmentIndexes, "segmentIndexes must not be null");
        if (segmentIndexes.isEmpty()) {
            throw new IllegalArgumentException("segmentIndexes must not be empty");
        }
        segmentIndexes = List.copyOf(segmentIndexes);
    }

    /**
     * Builds the drills of a unit from its segments, which alternate between
     * sound and silence in time order. A silence at the beginning of the
     * file belongs to no drill and is dropped. A sound at the end of the
     * file without a silence after it makes a drill of the sound only.
     *
     * @param segments segments of the unit in time order, sound and silence alternating
     * @return the drills in time order, empty if there are no segments
     * @throws NullPointerException if segments is null
     */
    public static List<Drill> drillsOf(List<Segment> segments) {
        Objects.requireNonNull(segments, "segments must not be null");
        var drills = new ArrayList<Drill>();
        var i = startOfFirstSound(segments);
        while (i < segments.size()) {
            var indexes = new ArrayList<Integer>();
            indexes.add(segments.get(i).index());
            i++;
            if (i < segments.size() && segments.get(i).kind() == Segment.Kind.SILENCE) {
                indexes.add(segments.get(i).index());
                i++;
            }
            drills.add(new Drill(indexes));
        }
        return List.copyOf(drills);
    }

    private static int startOfFirstSound(List<Segment> segments) {
        return !segments.isEmpty() && segments.getFirst().kind() == Segment.Kind.SILENCE ? 1 : 0;
    }
}
