package com.sosuisha.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sosuisha.domain.model.Segment;

/**
 * Segment cache that saves nothing and finds nothing. For tests that need a
 * {@link SegmentRepository} but do not care about the cache, and as a base of
 * test spies that record one call.
 */
public class NullSegmentRepository implements SegmentRepository {
    @Override
    public void saveSegments(String fingerprint, List<Segment> segments) {
        // does nothing
    }

    @Override
    public Optional<List<Segment>> findSegments(String fingerprint) {
        return Optional.empty();
    }
}
