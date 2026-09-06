package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.model.AudioFolder;

class SqliteAudioFolderRepositoryTest {
    @Test
    @DisplayName("保存した音声フォルダは一覧として読み出せる。何も保存していなければ一覧は空")
    void the_saved_audio_folder_is_found_in_the_list_and_the_list_is_empty_before(
        @TempDir Path folder) {
        var repository =
            new SqliteAudioFolderRepository(new SqliteDatabase(folder.resolve("drill.db")));
        var hanon = new AudioFolder("英語のハノン", Path.of("D:", "drills", "hanon"));
        assertEquals(List.of(), repository.findAll());

        repository.save(hanon);

        assertEquals(List.of(hanon), repository.findAll());
    }

    @Test
    @DisplayName("同じパスのフォルダをもう一度保存しても一覧は1件のままで、ドリル名は新しいものに置き換わる")
    void saving_the_same_folder_again_keeps_one_entry_with_the_new_name(@TempDir Path folder) {
        var repository =
            new SqliteAudioFolderRepository(new SqliteDatabase(folder.resolve("drill.db")));
        var path = Path.of("D:", "drills", "hanon");
        repository.save(new AudioFolder("英語のハノン", path));

        repository.save(new AudioFolder("ハノン初級", path));

        assertEquals(List.of(new AudioFolder("ハノン初級", path)), repository.findAll());
    }

    @Test
    @DisplayName("別のパスのフォルダを保存すると、前のフォルダは置き換わり、一覧はその1件だけになる（登録できるフォルダは1つだけ）")
    void saving_a_folder_of_another_path_replaces_the_registered_folder(@TempDir Path folder) {
        var repository =
            new SqliteAudioFolderRepository(new SqliteDatabase(folder.resolve("drill.db")));
        repository.save(new AudioFolder("英語のハノン", Path.of("D:", "drills", "hanon")));
        var other = new AudioFolder("別の教材", Path.of("D:", "drills", "other"));

        repository.save(other);

        assertEquals(List.of(other), repository.findAll());
    }
}
