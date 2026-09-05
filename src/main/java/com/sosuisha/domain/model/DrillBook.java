package com.sosuisha.domain.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sosuisha.domain.exception.IrregularUnitException;

/**
 * Builds the drills of a unit from its segments by the structure of the
 * drill book (see ADR 004). A regular unit is a run of drills that each
 * start with a key sentence spoken twice, followed by sets of a cue and an
 * answer spoken twice. An introduction unit (Unit 0.x) is a title followed
 * by drills that each start with a short cue.
 */
public final class DrillBook {
    /** Number of drills in every regular unit of the drill book. */
    public static final int DRILLS_PER_UNIT = 5;

    /**
     * Two sounds are the same sentence when their lengths differ by no more
     * than this share of the longer one. The natural-speed recordings vary by
     * up to about 15 percent; a cue is at least 20 percent shorter than its
     * answer.
     */
    static final double SAME_LENGTH_RATIO = 0.2;
    /** The difference allowed for very short sounds, where the ratio would be too strict. */
    static final Duration SAME_LENGTH_MIN_TOLERANCE = Duration.ofMillis(100);
    /** A silence no longer than this inside a key sentence is a pause, not a turn boundary. */
    static final Duration PAUSE_MAX_SILENCE = Duration.ofMillis(1300);
    /** In an introduction unit, a sound shorter than this is a cue. */
    static final Duration CUE_MAX_DURATION = Duration.ofMillis(800);

    private static final int SEGMENTS_PER_TURN = 2;
    private static final int TURNS_PER_SET = 3;

    private DrillBook() {}

    /**
     * Builds the drills of the unit from its segments, which alternate
     * between sound and silence in time order. A silence at the beginning
     * of the file belongs to no drill and is dropped.
     *
     * @param segments segments of the unit in time order, sound and silence alternating
     * @param unit unit the segments belong to; it decides which rules apply
     * @return the drills in time order, empty if there are no segments
     * @throws NullPointerException if segments or unit is null
     * @throws IrregularUnitException if a regular unit does not split into
     *             {@link #DRILLS_PER_UNIT} drills
     */
    public static List<Drill> drillsOf(List<Segment> segments, Unit unit)
        throws IrregularUnitException {
        Objects.requireNonNull(segments, "segments must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (segments.isEmpty()) { return List.of(); }
        var candidates = candidatesOf(withoutLeadingSilence(segments));
        return unit.isIntroduction()
            ? introductionDrills(candidates)
            : regularDrills(candidates);
    }

    /**
     * A turn whose number and role are not known yet. The length is the time
     * from its first sound to the end of its last sound, so two key sentences
     * compare equal whether or not a pause splits one of them.
     */
    private record Candidate(
        List<Integer> segmentIndexes, Duration length, Optional<Duration> trailingSilence) {
        boolean endsWithPause() {
            return trailingSilence.filter(s -> s.compareTo(PAUSE_MAX_SILENCE) <= 0).isPresent();
        }

        Candidate joinedWith(Candidate next) {
            var indexes = new ArrayList<>(segmentIndexes);
            indexes.addAll(next.segmentIndexes());
            var pause = trailingSilence.orElse(Duration.ZERO);
            return new Candidate(
                indexes, length.plus(pause).plus(next.length()), next.trailingSilence()
            );
        }

        boolean hasSameLengthAs(Candidate other) {
            var longer = length.compareTo(other.length()) >= 0 ? length : other.length();
            var byRatio = Duration.ofMillis(Math.round(longer.toMillis() * SAME_LENGTH_RATIO));
            var tolerance = byRatio.compareTo(SAME_LENGTH_MIN_TOLERANCE) >= 0
                ? byRatio
                : SAME_LENGTH_MIN_TOLERANCE;
            return length.minus(other.length()).abs().compareTo(tolerance) <= 0;
        }

        Turn toTurn(int number, Turn.Role role) {
            return new Turn(number, role, segmentIndexes);
        }
    }

    /** A turn whose role is known but whose number is not. */
    private record RoledCandidate(Turn.Role role, Candidate candidate) {
    }

    /** A key sentence pair found at a position, and how many candidates it took. */
    private record KeyPair(Candidate first, Candidate second, int consumed) {
    }

    private static List<Segment> withoutLeadingSilence(List<Segment> segments) {
        return segments.getFirst().kind() == Segment.Kind.SILENCE
            ? segments.subList(1, segments.size())
            : segments;
    }

    /** Pairs each sound with the silence after it; the last sound may have none. */
    private static List<Candidate> candidatesOf(List<Segment> body) {
        var candidates = new ArrayList<Candidate>();
        for (var i = 0; i < body.size(); i += SEGMENTS_PER_TURN) {
            var indexes = new ArrayList<Integer>();
            indexes.add(body.get(i).index());
            Optional<Duration> silence = Optional.empty();
            if (i + 1 < body.size()) {
                indexes.add(body.get(i + 1).index());
                silence = Optional.of(body.get(i + 1).duration());
            }
            candidates.add(new Candidate(indexes, body.get(i).duration(), silence));
        }
        return candidates;
    }

    // --- regular units ---

    private static List<Drill> regularDrills(List<Candidate> turns) {
        var drills = groupByKeySentencePairs(turns);
        if (drills.size() == 1) {
            drills = splitBySets(drills.getFirst());
        }
        if (drills.size() != DRILLS_PER_UNIT) {
            throw new IrregularUnitException(
                "The unit does not split into " + DRILLS_PER_UNIT + " drills: " + drills.size()
                    + " drills from " + turns.size() + " turns",
                null
            );
        }
        return numbered(drills, 1);
    }

    /**
     * The unit starts with a key sentence pair. After each set of a cue and
     * two answers, another key sentence pair starts the next drill; anything
     * else is the next cue of the same drill.
     */
    private static List<List<RoledCandidate>> groupByKeySentencePairs(List<Candidate> turns) {
        var drills = new ArrayList<List<RoledCandidate>>();
        var start = keyPairAt(turns, 0, true)
            .orElseThrow(
                () -> new IrregularUnitException("The unit has no key sentence pair", null)
            );
        var current = newDrillWith(start);
        var i = start.consumed();
        while (i < turns.size()) {
            if (i + TURNS_PER_SET > turns.size()) {
                throw new IrregularUnitException(
                    "The unit ends with a cue that has no two answers after it", null
                );
            }
            current.add(new RoledCandidate(Turn.Role.CUE, turns.get(i)));
            current.add(new RoledCandidate(Turn.Role.ANSWER, turns.get(i + 1)));
            current.add(new RoledCandidate(Turn.Role.ANSWER, turns.get(i + 2)));
            i += TURNS_PER_SET;
            var next = keyPairAt(turns, i, false);
            if (next.isPresent()) {
                drills.add(current);
                current = newDrillWith(next.get());
                i += next.get().consumed();
            }
        }
        drills.add(current);
        return drills;
    }

    private static List<RoledCandidate> newDrillWith(KeyPair keyPair) {
        var drill = new ArrayList<RoledCandidate>();
        drill.add(new RoledCandidate(Turn.Role.KEY_SENTENCE, keyPair.first()));
        drill.add(new RoledCandidate(Turn.Role.KEY_SENTENCE, keyPair.second()));
        return drill;
    }

    /**
     * Looks for a key sentence pair at the position. A key sentence with a
     * pause inside is detected as two turns; such a turn is joined with the
     * next one when the pair then has the same length. At the start of the
     * unit the title may be merged into the first key sentence, so a pair
     * that is split the same way on both sides is joined even when the
     * lengths differ, and the first two turns are a pair in any case.
     */
    private static Optional<KeyPair> keyPairAt(List<Candidate> turns, int at, boolean atStart) {
        if (at + 1 >= turns.size()) { return Optional.empty(); }
        if (atStart && isSplitTheSameWayOnBothSides(turns, at)) {
            return Optional.of(joinedPair(turns, at, true, true));
        }
        for (var firstJoined : List.of(true, false)) {
            for (var secondJoined : List.of(true, false)) {
                var pair = pairIfPossible(turns, at, firstJoined, secondJoined)
                    .filter(p -> p.first().hasSameLengthAs(p.second()))
                    .filter(p -> isSetAt(turns, at + p.consumed()));
                if (pair.isPresent()) { return pair; }
            }
        }
        return atStart ? Optional.of(joinedPair(turns, at, false, false)) : Optional.empty();
    }

    /**
     * Tells whether a set of a cue and two answers starts at the position: the
     * two answers have the same length and the cue is shorter than both. A key
     * sentence pair is always followed by a set, so this tells a pair from a
     * cue and an answer that happen to have the same length.
     */
    private static boolean isSetAt(List<Candidate> turns, int at) {
        if (at + TURNS_PER_SET > turns.size()) { return false; }
        var cue = turns.get(at);
        var first = turns.get(at + 1);
        var second = turns.get(at + 2);
        return first.hasSameLengthAs(second)
            && cue.length().compareTo(first.length()) < 0
            && cue.length().compareTo(second.length()) < 0;
    }

    private static boolean isSplitTheSameWayOnBothSides(List<Candidate> turns, int at) {
        return at + 3 < turns.size()
            && turns.get(at).endsWithPause()
            && turns.get(at + 2).endsWithPause()
            && turns.get(at + 1).hasSameLengthAs(turns.get(at + 3));
    }

    /** The pair with the given joins, when the turns to join exist and end with a pause. */
    private static Optional<KeyPair> pairIfPossible(
        List<Candidate> turns, int at, boolean firstJoined, boolean secondJoined) {
        var consumed = (firstJoined ? 2 : 1) + (secondJoined ? 2 : 1);
        if (at + consumed > turns.size()) { return Optional.empty(); }
        if (firstJoined && !turns.get(at).endsWithPause()) { return Optional.empty(); }
        var secondAt = at + (firstJoined ? 2 : 1);
        if (secondJoined && !turns.get(secondAt).endsWithPause()) { return Optional.empty(); }
        return Optional.of(joinedPair(turns, at, firstJoined, secondJoined));
    }

    private static KeyPair joinedPair(
        List<Candidate> turns, int at, boolean firstJoined, boolean secondJoined) {
        var first = firstJoined ? turns.get(at).joinedWith(turns.get(at + 1)) : turns.get(at);
        var secondAt = at + (firstJoined ? 2 : 1);
        var second = secondJoined
            ? turns.get(secondAt).joinedWith(turns.get(secondAt + 1))
            : turns.get(secondAt);
        return new KeyPair(first, second, (firstJoined ? 2 : 1) + (secondJoined ? 2 : 1));
    }

    /**
     * A unit with one key sentence pair at its start has one drill per set;
     * the key sentences belong to the first drill.
     */
    private static List<List<RoledCandidate>> splitBySets(List<RoledCandidate> single) {
        var drills = new ArrayList<List<RoledCandidate>>();
        var current = new ArrayList<RoledCandidate>();
        for (var turn : single) {
            if (turn.role() == Turn.Role.CUE && !current.isEmpty()
                && current.getLast().role() != Turn.Role.KEY_SENTENCE) {
                drills.add(current);
                current = new ArrayList<>();
            }
            current.add(turn);
        }
        drills.add(current);
        return drills;
    }

    // --- introduction units ---

    /** The title is drill 0; every short sound is a cue that starts the next drill. */
    private static List<Drill> introductionDrills(List<Candidate> turns) {
        var drills = new ArrayList<List<RoledCandidate>>();
        var current = new ArrayList<RoledCandidate>();
        current.add(new RoledCandidate(Turn.Role.SENTENCE, turns.getFirst()));
        for (var turn : turns.subList(1, turns.size())) {
            if (turn.length().compareTo(CUE_MAX_DURATION) < 0) {
                drills.add(current);
                current = new ArrayList<>();
                current.add(new RoledCandidate(Turn.Role.CUE, turn));
            } else {
                current.add(new RoledCandidate(Turn.Role.SENTENCE, turn));
            }
        }
        drills.add(current);
        return numbered(drills, 0);
    }

    // --- numbering ---

    private static List<Drill> numbered(List<List<RoledCandidate>> drills, int firstDrillNumber) {
        var result = new ArrayList<Drill>();
        for (var d = 0; d < drills.size(); d++) {
            var turns = new ArrayList<Turn>();
            var roled = drills.get(d);
            for (var t = 0; t < roled.size(); t++) {
                turns.add(roled.get(t).candidate().toTurn(t + 1, roled.get(t).role()));
            }
            result.add(new Drill(firstDrillNumber + d, turns));
        }
        return List.copyOf(result);
    }
}
