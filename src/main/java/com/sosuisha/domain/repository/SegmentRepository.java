package com.sosuisha.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.Segment;

/**
 * Cache of the segments of each unit, keyed by the fingerprint of the audio
 * file (see ADR 002). The segments can be rebuilt from the audio file, so
 * they are a cache, not a record of the user.
 */
public interface SegmentRepository {
    /**
     * Saves the segments of a unit in time order. The segments already saved
     * for the same fingerprint are replaced.
     *
     * @param fingerprint fingerprint of the audio file
     * @param segments segments in time order
     * @throws RepositoryException if the database cannot be written
     */
    void saveSegments(String fingerprint, List<Segment> segments) throws RepositoryException;

    /**
     * Finds the segments of a unit in time order.
     *
     * @param fingerprint fingerprint of the audio file
     * @return the segments, or an empty Optional if none are saved for the fingerprint
     * @throws RepositoryException if the database cannot be read
     */
    Optional<List<Segment>> findSegments(String fingerprint) throws RepositoryException;
}
