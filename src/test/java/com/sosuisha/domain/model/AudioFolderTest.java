package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullUnmarked;

@NullUnmarked // the null test passes null on purpose
class AudioFolderTest {
    private static final Path FOLDER = Path.of("D:", "drills", "hanon");

    @Test
    @DisplayName("登録した音声フォルダは、学習者が付けたドリル名とフォルダのパスを持つ")
    void audio_folder_has_a_name_given_by_the_learner_and_the_path_of_the_folder() {
        var folder = new AudioFolder("英語のハノン", FOLDER);

        assertEquals("英語のハノン", folder.name());
        assertEquals(FOLDER, folder.path());
    }

    @Test
    @DisplayName("ドリル名が空欄（空文字か空白だけ）の音声フォルダは作れない")
    void audio_folder_with_a_blank_name_cannot_be_created() {
        assertThrows(IllegalArgumentException.class, () -> new AudioFolder("", FOLDER));
        assertThrows(IllegalArgumentException.class, () -> new AudioFolder("  ", FOLDER));
    }

    @Test
    @DisplayName("パスが null の音声フォルダは作れない")
    void audio_folder_without_a_path_cannot_be_created() {
        assertThrows(NullPointerException.class, () -> new AudioFolder("英語のハノン", null));
    }
}
