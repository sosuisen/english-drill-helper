package com.sosuisha.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.sosuisha.domain.exception.AudioFolderScanException;
import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.UnitRepository;

/**
 * Builds the list of units: scans the audio folder and joins each audio file
 * with its record in the database.
 */
public class UnitLoader {
    private final FileSystemAudioFolderScanner scanner;
    private final UnitRepository repository;

    /**
     * Creates the loader.
     *
     * @param scanner lists the audio files of a folder with their fingerprints
     * @param repository database that keeps the records of the units
     * @throws NullPointerException if scanner or repository is null
     */
    public UnitLoader(FileSystemAudioFolderScanner scanner, UnitRepository repository) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Loads the units of the folder in name order.
     *
     * @param folder folder that holds the audio files
     * @return the units, sorted by file name
     * @throws NullPointerException if folder is null
     * @throws AudioFolderScanException if the folder or an audio file cannot be read
     * @throws RepositoryException if the database cannot be read
     */
    public List<Unit> load(Path folder) throws AudioFolderScanException, RepositoryException {
        Objects.requireNonNull(folder, "folder must not be null");
        return scanner.scan(folder).stream().map(this::toUnit).toList();
    }

    private Unit toUnit(AudioFile audioFile) {
        return new Unit(audioFile, repository.findLastPlayedAt(audioFile.fingerprint()));
    }
}
