package com.sosuisha.domain.repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Database that saves nothing and finds nothing. For tests that need a
 * {@link UnitRepository} but do not care about the records, and as a base of
 * test spies that record one call.
 */
public class NullUnitRepository implements UnitRepository {
    @Override
    public void saveLastPlayedAt(String fingerprint, Instant playedAt) {
        // does nothing
    }

    @Override
    public Optional<Instant> findLastPlayedAt(String fingerprint) {
        return Optional.empty();
    }
}
