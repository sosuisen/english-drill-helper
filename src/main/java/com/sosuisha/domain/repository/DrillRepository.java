package com.sosuisha.domain.repository;

import java.time.Instant;
import java.util.Optional;

import com.sosuisha.domain.exception.RepositoryException;

/**
 * The drill database of the app. It keeps the records of each drill, keyed
 * by the fingerprint of the audio file (see ADR 002).
 */
public interface DrillRepository {
    /**
     * Saves the time when the drill was last played. An existing record of the
     * same fingerprint is overwritten.
     *
     * @param fingerprint fingerprint of the audio file
     * @param playedAt time when the playback stopped
     * @throws RepositoryException if the database cannot be written
     */
    void saveLastPlayedAt(String fingerprint, Instant playedAt) throws RepositoryException;

    /**
     * Finds the time when the drill was last played.
     *
     * @param fingerprint fingerprint of the audio file
     * @return the time, or an empty Optional if the drill has never been played
     * @throws RepositoryException if the database cannot be read
     */
    Optional<Instant> findLastPlayedAt(String fingerprint) throws RepositoryException;
}
