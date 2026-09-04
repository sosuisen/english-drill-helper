package com.sosuisha.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.sosuisha.domain.exception.AudioFolderScanException;

/**
 * Lists the audio files in a folder on the file system.
 */
public class FileSystemAudioFolderScanner {
    private static final Set<String> AUDIO_EXTENSIONS = Set.of(".mp3", ".m4a");

    /**
     * Lists the audio files (mp3 and m4a, any letter case) in the folder in name order.
     *
     * @param folder folder that holds the audio files
     * @return paths of the audio files, sorted by name
     * @throws NullPointerException if folder is null
     * @throws AudioFolderScanException if the folder does not exist or cannot be read
     */
    public List<Path> scan(Path folder) throws AudioFolderScanException {
        Objects.requireNonNull(folder, "folder must not be null");
        try (var entries = Files.list(folder)) {
            return entries.filter(FileSystemAudioFolderScanner::isAudioFile).sorted().toList();
        } catch (IOException e) {
            throw new AudioFolderScanException("Cannot read the audio folder: " + folder, e);
        }
    }

    private static boolean isAudioFile(Path path) {
        var name = path.toString().toLowerCase(Locale.ROOT);
        return AUDIO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
