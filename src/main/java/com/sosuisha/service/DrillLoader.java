package com.sosuisha.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.sosuisha.domain.exception.AudioFolderScanException;
import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Drill;
import com.sosuisha.domain.repository.DrillRepository;

/**
 * Builds the list of drills: scans the audio folder and joins each audio file
 * with its record in the drill database.
 */
public class DrillLoader {
    private final FileSystemAudioFolderScanner scanner;
    private final DrillRepository repository;

    /**
     * Creates the loader.
     *
     * @param scanner lists the audio files of a folder with their fingerprints
     * @param repository database that keeps the records of the drills
     * @throws NullPointerException if scanner or repository is null
     */
    public DrillLoader(FileSystemAudioFolderScanner scanner, DrillRepository repository) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Loads the drills of the folder in name order.
     *
     * @param folder folder that holds the audio files
     * @return the drills, sorted by file name
     * @throws NullPointerException if folder is null
     * @throws AudioFolderScanException if the folder or an audio file cannot be read
     * @throws RepositoryException if the database cannot be read
     */
    public List<Drill> load(Path folder) throws AudioFolderScanException, RepositoryException {
        Objects.requireNonNull(folder, "folder must not be null");
        return scanner.scan(folder).stream().map(this::toDrill).toList();
    }

    private Drill toDrill(AudioFile audioFile) {
        return new Drill(audioFile, repository.findLastPlayedAt(audioFile.fingerprint()));
    }
}
