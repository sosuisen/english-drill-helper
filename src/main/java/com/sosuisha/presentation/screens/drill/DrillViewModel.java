package com.sosuisha.presentation.screens.drill;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the drill screen. It holds the audio files of the drills and
 * the selected audio file.
 */
public class DrillViewModel {
    private final ObservableList<Path> audioFiles = FXCollections.observableArrayList();
    private final ObservableList<Path> readOnlyAudioFiles =
        FXCollections.unmodifiableObservableList(audioFiles);
    private final ObjectProperty<Optional<Path>> selectedAudioFile =
        new SimpleObjectProperty<>(Optional.empty());
    private final ReadOnlyStringWrapper selectedFileName = new ReadOnlyStringWrapper();

    /**
     * Creates the view model.
     *
     * @param audioFiles audio files of the drills, in the order shown to the user
     * @throws NullPointerException if audioFiles is null
     */
    public DrillViewModel(List<Path> audioFiles) {
        Objects.requireNonNull(audioFiles, "audioFiles must not be null");
        this.audioFiles.setAll(audioFiles);
        selectedFileName.bind(
            selectedAudioFile.map(file -> file.map(DrillViewModel::fileNameOf).orElse(""))
        );
    }

    /**
     * Returns the audio files of the drills, in the order shown to the user.
     * The list cannot be modified by the caller.
     *
     * @return read-only observable list of the audio files
     */
    ObservableList<Path> getAudioFiles() {
        return readOnlyAudioFiles;
    }

    /**
     * Selects an audio file.
     *
     * @param file audio file to select, or null to clear the selection
     */
    public void selectAudioFile(@Nullable Path file) {
        selectedAudioFile.set(Optional.ofNullable(file));
    }

    /**
     * Returns the file name of the selected audio file. It is an empty string
     * while no file is selected.
     *
     * @return read-only string property of the selected file name
     */
    public ReadOnlyStringProperty selectedFileNameProperty() {
        return selectedFileName.getReadOnlyProperty();
    }

    private static String fileNameOf(Path file) {
        var name = file.getFileName();
        return name == null ? file.toString() : name.toString();
    }
}
