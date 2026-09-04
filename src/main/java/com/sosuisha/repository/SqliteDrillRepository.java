package com.sosuisha.repository;

import static com.sosuisha.db.Tables.DRILL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.repository.DrillRepository;

/**
 * Drill database stored in a SQLite file, accessed through jOOQ.
 * <p>
 * I/O errors are reported as runtime exceptions, in line with jOOQ, which
 * wraps every {@code SQLException} in its unchecked {@code DataAccessException}.
 * This class translates both into {@link RepositoryException} so that
 * callers see one exception type and jOOQ types do not leak out.
 */
public class SqliteDrillRepository implements DrillRepository {
    /** Default SQLite file, in the user home. */
    public static final Path DEFAULT_FILE =
        Path.of(System.getProperty("user.home"), ".english-drill-helper", "drill.db");

    private static final String FILE_PROPERTY = "edh.drill.db";
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private final Path file;
    private final String url;

    /**
     * Resolves the path of the SQLite database file. The system property
     * {@code edh.drill.db} takes precedence over {@link #DEFAULT_FILE}.
     *
     * @return path of the SQLite database file
     */
    static Path resolveFile() {
        var override = System.getProperty(FILE_PROPERTY);
        if (override != null) { return Path.of(override); }
        return DEFAULT_FILE;
    }

    /**
     * Creates the database on the SQLite file resolved by {@link #resolveFile()}.
     *
     * @throws RepositoryException if the parent folder cannot be created or the
     *             database cannot be opened
     */
    public SqliteDrillRepository() throws RepositoryException {
        this(resolveFile());
    }

    /**
     * Creates the database on the given SQLite file. The file, its parent
     * folder, and the schema are created when they do not exist.
     *
     * @param file path of the SQLite database file
     * @throws NullPointerException if file is null
     * @throws RepositoryException if the parent folder cannot be created or the
     *             database cannot be opened
     */
    public SqliteDrillRepository(Path file) throws RepositoryException {
        Objects.requireNonNull(file, "file must not be null");
        createParentFolder(file);
        this.file = file;
        this.url = "jdbc:sqlite:" + file;
        createSchema();
    }

    private static void createParentFolder(Path file) {
        var parent = file.getParent();
        if (parent == null) { return; }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RepositoryException(
                "Could not create the folder for the drill database: " + parent, e
            );
        }
    }

    private void createSchema() {
        runWithDsl(
            "Could not initialize the drill database", dsl -> dsl.execute(loadSchemaSql())
        );
    }

    private static String loadSchemaSql() {
        try (var in = SqliteDrillRepository.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositoryException("Could not read the schema of the drill database", e);
        }
    }

    private <T extends @Nullable Object> T withDsl(
        String errorMessage, Function<DSLContext, T> operation) {
        try (var connection = DriverManager.getConnection(url)) {
            return operation.apply(DSL.using(connection, SQLDialect.SQLITE));
        } catch (SQLException | DataAccessException e) {
            throw new RepositoryException(errorMessage + ": " + file, e);
        }
    }

    private void runWithDsl(String errorMessage, Consumer<DSLContext> operation) {
        this.<@Nullable Void>withDsl(errorMessage, dsl -> {
            operation.accept(dsl);
            return null;
        });
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
        runWithDsl("Could not write to the drill database", dsl -> {
            var record = dsl.newRecord(DRILL);
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
        return withDsl(
            "Could not read the drill database",
            dsl -> dsl.select(DRILL.LAST_PLAYED_AT)
                .from(DRILL)
                .where(DRILL.FINGERPRINT.eq(fingerprint))
                .fetchOptional(record -> Instant.ofEpochMilli(record.value1()))
        );
    }
}
