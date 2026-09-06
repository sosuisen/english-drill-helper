package com.sosuisha.repository;

import static com.sosuisha.db.Tables.AUDIO_FOLDER;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.domain.repository.AudioFolderRepository;

/**
 * Registered audio folders stored in the SQLite file of the app, accessed
 * through jOOQ (see {@link SqliteDatabase} for the error handling).
 */
public class SqliteAudioFolderRepository implements AudioFolderRepository {
    private final SqliteDatabase database;

    /**
     * Creates the repository on the database of the app.
     *
     * @param database SQLite database of the app, shared by the repositories
     * @throws NullPointerException if database is null
     */
    public SqliteAudioFolderRepository(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    /**
     * {@inheritDoc} Only one folder is registered for now, so the folder
     * replaces whatever was registered before.
     *
     * @throws NullPointerException if folder is null
     * @throws RepositoryException if the database cannot be written
     */
    @Override
    public void save(AudioFolder folder) throws RepositoryException {
        Objects.requireNonNull(folder, "folder must not be null");
        database.run("Could not write to the database", dsl -> {
            dsl.deleteFrom(AUDIO_FOLDER).execute();
            var record = dsl.newRecord(AUDIO_FOLDER);
            record.setName(folder.name());
            record.setPath(folder.path().toString());
            record.insert();
        });
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryException if the database cannot be read
     */
    @Override
    public List<AudioFolder> findAll() throws RepositoryException {
        return database.query(
            "Could not read the database",
            dsl -> dsl.select(AUDIO_FOLDER.NAME, AUDIO_FOLDER.PATH)
                .from(AUDIO_FOLDER)
                .orderBy(AUDIO_FOLDER.ID)
                .fetch(record -> new AudioFolder(record.value1(), Path.of(record.value2())))
        );
    }
}
