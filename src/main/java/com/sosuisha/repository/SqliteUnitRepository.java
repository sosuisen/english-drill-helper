package com.sosuisha.repository;

import static com.sosuisha.db.Tables.UNIT;

import java.nio.file.Path;
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
     * Creates the repository on the SQLite file resolved by
     * {@link SqliteDatabase#resolveFile()}.
     *
     * @throws RepositoryException if the parent folder cannot be created or the
     *             database cannot be opened
     */
    public SqliteUnitRepository() throws RepositoryException {
        this(SqliteDatabase.resolveFile());
    }

    /**
     * Creates the repository on the given SQLite file. The file, its parent
     * folder, and the schema are created when they do not exist.
     *
     * @param file path of the SQLite database file
     * @throws NullPointerException if file is null
     * @throws RepositoryException if the parent folder cannot be created or the
     *             database cannot be opened
     */
    public SqliteUnitRepository(Path file) throws RepositoryException {
        this.database = new SqliteDatabase(Objects.requireNonNull(file, "file must not be null"));
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
