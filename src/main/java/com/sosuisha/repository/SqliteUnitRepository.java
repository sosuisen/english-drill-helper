package com.sosuisha.repository;

import static com.sosuisha.db.Tables.UNIT;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.repository.UnitRepository;

/**
 * Unit records stored in the SQLite file of the app, accessed through jOOQ
 * (see {@link SqliteDatabase} for the error handling).
 */
public class SqliteUnitRepository implements UnitRepository {
    private final SqliteDatabase database;

    /**
     * Creates the repository on the database of the app.
     *
     * @param database SQLite database of the app, shared by the repositories
     * @throws NullPointerException if database is null
     */
    public SqliteUnitRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if fingerprint or playedAt is null
     * @throws RepositoryException if the database cannot be written
     */
    @Override
    public void saveLastPlayedAt(String fingerprint, Instant playedAt)
        throws RepositoryException {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(playedAt, "playedAt must not be null");
        database.run("Could not write to the database", dsl -> {
            var record = dsl.newRecord(UNIT);
            record.setFingerprint(fingerprint);
            record.setLastPlayedAt(playedAt.toEpochMilli());
            record.merge(); // upsert
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if fingerprint is null
     * @throws RepositoryException if the database cannot be read
     */
    @Override
    public Optional<Instant> findLastPlayedAt(String fingerprint) throws RepositoryException {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        return database.query(
            "Could not read the database",
            dsl -> dsl.select(UNIT.LAST_PLAYED_AT)
                .from(UNIT)
                .where(UNIT.FINGERPRINT.eq(fingerprint))
                .fetchOptional(record -> Instant.ofEpochMilli(record.value1()))
        );
    }
}
