package com.sosuisha.presentation.screens.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DrillViewModelTest {
    @Test
    @DisplayName("渡された音声ファイルの一覧を、渡された順序のまま保持する")
    void holds_the_given_audio_files_in_the_given_order() {
        var files = List.of(Path.of("001_Unit 0.1.mp3"), Path.of("002_Unit 0.2.mp3"));

        var viewModel = new DrillViewModel(files);

        assertEquals(files, viewModel.getAudioFiles());
    }

    @Test
    @DisplayName("音声ファイルを選ぶと、そのファイル名が選択中のファイル名になる")
    void selecting_an_audio_file_makes_its_file_name_the_selected_file_name() {
        var file = Path.of("drills", "001_Unit 0.1.mp3");
        var viewModel = new DrillViewModel(List.of(file));

        viewModel.selectAudioFile(file);

        assertEquals("001_Unit 0.1.mp3", viewModel.selectedFileNameProperty().get());
    }
}
