package com.sosuisha.repository;

import static com.sosuisha.db.Tables.SEGMENT;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import com.sosuisha.db.tables.records.SegmentRecord;
import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.repository.SegmentRepository;

/**
 * Segment cache stored in the SQLite file of the app, accessed through jOOQ
 * (see {@link SqliteDatabase} for the error handling).
 */
public class SqliteSegmentRepository implements SegmentRepository {
    private final SqliteDatabase database;

    /**
     * Creates the cache on the database of the app.
     *
     * @param database SQLite database of the app, shared by the repositories
     * @throws NullPointerException if database is null
     */
    public SqliteSegmentRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if fingerprint or segments is null
     * @throws RepositoryException if the database cannot be written
     */
    @Override
    public void saveSegments(String fingerprint, List<Segment> segments)
        throws RepositoryException {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(segments, "segments must not be null");
        database.run("Could not write to the database", dsl -> dsl.transaction(tx -> {
            tx.dsl().deleteFrom(SEGMENT).where(SEGMENT.FINGERPRINT.eq(fingerprint)).execute();
            tx.dsl().batchInsert(toRecords(fingerprint, segments)).execute();
        }));
    }

    private static List<SegmentRecord> toRecords(String fingerprint, List<Segment> segments) {
        return IntStream.range(0, segments.size())
            .mapToObj(position -> toRecord(fingerprint, position, segments.get(position)))
            .toList();
    }

    private static SegmentRecord toRecord(String fingerprint, int position, Segment segment) {
        var record = new SegmentRecord();
        record.setFingerprint(fingerprint);
        record.setPosition(position);
        record.setDurationMs(segment.duration().toMillis());
        record.setKind(segment.kind().name());
        return record;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if fingerprint is null
     * @throws RepositoryException if the database cannot be read
     */
    @Override
    public Optional<List<Segment>> findSegments(String fingerprint) throws RepositoryException {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        var segments = database.query(
            "Could not read the database",
            dsl -> dsl.selectFrom(SEGMENT)
                .where(SEGMENT.FINGERPRINT.eq(fingerprint))
                .orderBy(SEGMENT.POSITION)
                .fetch(SqliteSegmentRepository::toSegment)
        );
        return segments.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(segments));
    }

    private static Segment toSegment(SegmentRecord record) {
        return new Segment(
            Duration.ofMillis(record.getDurationMs()), Segment.Kind.valueOf(record.getKind())
        );
    }
}
