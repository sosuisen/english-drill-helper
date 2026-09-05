package com.sosuisha.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.exception.RepositoryException;

/**
 * The SQLite file of the app, shared by the repositories. It creates the
 * file, its parent folder, and the schema when they do not exist, and runs
 * jOOQ operations on a connection.
 * <p>
 * I/O errors are reported as runtime exceptions, in line with jOOQ, which
 * wraps every {@code SQLException} in its unchecked {@code DataAccessException}.
 * This class translates both into {@link RepositoryException} so that
 * callers see one exception type and jOOQ types do not leak out.
 */
class SqliteDatabase {
    /** Default SQLite file, in the user home. */
    static final Path DEFAULT_FILE =
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
     * Opens the database on the given SQLite file. The file, its parent
     * folder, and the schema are created when they do not exist.
     *
     * @param file path of the SQLite database file
     * @throws NullPointerException if file is null
     * @throws RepositoryException if the parent folder cannot be created or the
     *             database cannot be opened
     */
    SqliteDatabase(Path file) throws RepositoryException {
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
                "Could not create the folder for the database: " + parent, e
            );
        }
    }

    // The SQLite driver runs only the first statement of a string, so the
    // schema is executed one statement at a time.
    private void createSchema() {
        run("Could not initialize the database", dsl -> {
            for (var statement : loadSchemaSql().split(";")) {
                if (!statement.isBlank()) {
                    dsl.execute(statement);
                }
            }
        });
    }

    private static String loadSchemaSql() {
        try (var in = SqliteDatabase.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositoryException("Could not read the schema of the database", e);
        }
    }

    /**
     * Runs a jOOQ operation on a new connection and returns its result.
     *
     * @param <T> type of the result
     * @param errorMessage message of the {@link RepositoryException} when the operation fails
     * @param operation operation to run
     * @return the result of the operation
     * @throws RepositoryException if the connection cannot be opened or the operation fails
     */
    <T extends @Nullable Object> T query(String errorMessage, Function<DSLContext, T> operation)
        throws RepositoryException {
        try (var connection = DriverManager.getConnection(url)) {
            return operation.apply(DSL.using(connection, SQLDialect.SQLITE));
        } catch (SQLException | DataAccessException e) {
            throw new RepositoryException(errorMessage + ": " + file, e);
        }
    }

    /**
     * Runs a jOOQ operation on a new connection.
     *
     * @param errorMessage message of the {@link RepositoryException} when the operation fails
     * @param operation operation to run
     * @throws RepositoryException if the connection cannot be opened or the operation fails
     */
    void run(String errorMessage, Consumer<DSLContext> operation) throws RepositoryException {
        this.<@Nullable Void>query(errorMessage, dsl -> {
            operation.accept(dsl);
            return null;
        });
    }
}
