package com.sosuisha.presentation.screens.audiofolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.domain.repository.NullAudioFolderRepository;

class AudioFolderSettingsViewModelTest {
    private static final Path HANON = Path.of("D:", "drills", "hanon");

    @Test
    @DisplayName("初期状態では、フォルダは未選択、ドリル名は空、ドリルの場所の表示は空で、保存できない")
    void initially_no_folder_is_chosen_the_name_and_the_location_are_empty_and_saving_is_off() {
        var viewModel = new AudioFolderSettingsViewModel(new NullAudioFolderRepository(), _ -> {
        });

        assertEquals(Optional.empty(), viewModel.chosenFolderProperty().get());
        assertEquals("", viewModel.nameProperty().get());
        assertEquals("", viewModel.locationTextProperty().get());
        assertFalse(viewModel.canSaveProperty().get());
    }

    @Test
    @DisplayName("フォルダを選ぶと、ドリル名にフォルダ名、ドリルの場所にフォルダのパスが入り、保存できるようになる")
    void choosing_a_folder_fills_the_name_and_the_location_and_turns_saving_on() {
        var viewModel = new AudioFolderSettingsViewModel(new NullAudioFolderRepository(), _ -> {
        });

        viewModel.chooseFolder(HANON);

        assertEquals(Optional.of(HANON), viewModel.chosenFolderProperty().get());
        assertEquals("hanon", viewModel.nameProperty().get());
        assertEquals(HANON.toString(), viewModel.locationTextProperty().get());
        assertTrue(viewModel.canSaveProperty().get());
    }

    @Test
    @DisplayName("ドリル名を空欄（空白だけ）にすると保存できなくなり、入れ直すと保存できるようになる")
    void a_blank_name_turns_saving_off_and_a_name_turns_it_on_again() {
        var viewModel = new AudioFolderSettingsViewModel(new NullAudioFolderRepository(), _ -> {
        });
        viewModel.chooseFolder(HANON);

        viewModel.nameProperty().set("  ");
        assertFalse(viewModel.canSaveProperty().get());

        viewModel.nameProperty().set("英語のハノン");
        assertTrue(viewModel.canSaveProperty().get());
    }

    @Test
    @DisplayName("保存すると、ドリル名とフォルダのパスの音声フォルダがリポジトリに保存され、保存したフォルダが呼び出し元に知らされる")
    void saving_stores_the_folder_with_its_name_and_tells_the_caller() {
        var saved = new AtomicReference<@Nullable AudioFolder>();
        var told = new AtomicReference<@Nullable AudioFolder>();
        var repository = new NullAudioFolderRepository() {
            @Override
            public void save(AudioFolder folder) {
                saved.set(folder);
            }
        };
        var viewModel = new AudioFolderSettingsViewModel(repository, told::set);
        viewModel.chooseFolder(HANON);
        viewModel.nameProperty().set("英語のハノン");

        viewModel.save();

        assertEquals(new AudioFolder("英語のハノン", HANON), saved.get());
        assertEquals(new AudioFolder("英語のハノン", HANON), told.get());
    }

    @Test
    @DisplayName("登録済みのフォルダがあれば、開いたときにそのドリル名と場所が既定値として入っていて、そのまま保存できる")
    void a_registered_folder_is_the_default_of_the_dialog() {
        var repository = new NullAudioFolderRepository() {
            @Override
            public List<AudioFolder> findAll() {
                return List.of(new AudioFolder("英語のハノン", HANON));
            }
        };

        var viewModel = new AudioFolderSettingsViewModel(repository, _ -> {
        });

        assertEquals(Optional.of(HANON), viewModel.chosenFolderProperty().get());
        assertEquals("英語のハノン", viewModel.nameProperty().get());
        assertEquals(HANON.toString(), viewModel.locationTextProperty().get());
        assertTrue(viewModel.canSaveProperty().get());
    }
}
