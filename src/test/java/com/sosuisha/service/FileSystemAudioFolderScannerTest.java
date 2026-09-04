package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.exception.AudioFolderScanException;

class FileSystemAudioFolderScannerTest {
    @Test
    @DisplayName("フォルダを開くと、その中の音声ファイルの一覧がファイル名順に得られる")
    void opening_a_folder_lists_the_audio_files_in_it_in_name_order(@TempDir Path folder)
        throws IOException {
        Files.createFile(folder.resolve("002_Unit 0.2.mp3"));
        Files.createFile(folder.resolve("001_Unit 0.1.mp3"));
        var scanner = new FileSystemAudioFolderScanner();

        var files = scanner.scan(folder);

        assertEquals(
            List.of(folder.resolve("001_Unit 0.1.mp3"), folder.resolve("002_Unit 0.2.mp3")), files
        );
    }

    @Test
    @DisplayName("フォルダにmp3とm4a以外のファイルがあっても、一覧にはmp3とm4aだけが含まれる")
    void the_list_contains_only_mp3_and_m4a_files(@TempDir Path folder) throws IOException {
        Files.createFile(folder.resolve("001_Unit 0.1.mp3"));
        Files.createFile(folder.resolve("002_Unit 0.2.m4a"));
        Files.createFile(folder.resolve("notes.txt"));
        var scanner = new FileSystemAudioFolderScanner();

        var files = scanner.scan(folder);

        assertEquals(
            List.of(folder.resolve("001_Unit 0.1.mp3"), folder.resolve("002_Unit 0.2.m4a")), files
        );
    }

    @Test
    @DisplayName("フォルダが存在しないときは、AudioFolderScanException が投げられる")
    void scanning_a_missing_folder_throws_audio_folder_scan_exception(@TempDir Path tempDir) {
        var missingFolder = tempDir.resolve("missing");
        var scanner = new FileSystemAudioFolderScanner();

        assertThrows(AudioFolderScanException.class, () -> scanner.scan(missingFolder));
    }

    @Test
    @DisplayName("拡張子が大文字（.MP3、.M4A）のファイルも一覧に含まれる")
    void files_with_upper_case_extensions_are_listed(@TempDir Path folder) throws IOException {
        Files.createFile(folder.resolve("001_Unit 0.1.MP3"));
        Files.createFile(folder.resolve("002_Unit 0.2.M4A"));
        var scanner = new FileSystemAudioFolderScanner();

        var files = scanner.scan(folder);

        assertEquals(
            List.of(folder.resolve("001_Unit 0.1.MP3"), folder.resolve("002_Unit 0.2.M4A")), files
        );
    }
}
