package com.sosuisha.presentation.screens.drill;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.repository.DrillRepository;
import com.sosuisha.domain.service.AudioPlayer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the drill screen. It holds the audio files of the drills and
 * the selected audio file, plays the selected audio file, and records the
 * time when the playback stopped.
 */
public class DrillViewModel {
    private final ObservableList<AudioFile> audioFiles = FXCollections.observableArrayList();
    private final ObservableList<AudioFile> readOnlyAudioFiles =
        FXCollections.unmodifiableObservableList(audioFiles);
    private final ObjectProperty<Optional<AudioFile>> selectedAudioFile =
        new SimpleObjectProperty<>(Optional.empty());
    private final ReadOnlyStringWrapper selectedFileName = new ReadOnlyStringWrapper();
    private final ReadOnlyObjectWrapper<Optional<Instant>> lastPlayedAt =
        new ReadOnlyObjectWrapper<>(Optional.empty());
    private final AudioPlayer player;
    private final DrillRepository repository;
    private final Clock clock;

    /**
     * Creates the view model.
     *
     * @param audioFiles audio files of the drills, in the order shown to the user
     * @param player player that plays the selected audio file
     * @param repository database that keeps the records of the drills
     * @param clock clock that gives the time when the playback stops
     * @throws NullPointerException if any argument is null
     */
    public DrillViewModel(
        List<AudioFile> audioFiles, AudioPlayer player, DrillRepository repository, Clock clock) {
        Objects.requireNonNull(audioFiles, "audioFiles must not be null");
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.audioFiles.setAll(audioFiles);
        selectedFileName.bind(
            selectedAudioFile.map(file -> file.map(AudioFile::fileName).orElse(""))
        );
    }

    /**
     * Returns the audio files of the drills, in the order shown to the user.
     * The list cannot be modified by the caller.
     *
     * @return read-only observable list of the audio files
     */
    ObservableList<AudioFile> getAudioFiles() {
        return readOnlyAudioFiles;
    }

    /**
     * Selects an audio file.
     *
     * @param file audio file to select, or null to clear the selection
     */
    public void selectAudioFile(@Nullable AudioFile file) {
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

    /**
     * Plays the selected audio file. Does nothing while no file is selected.
     * When the playback stops, the time is recorded as the last played time
     * of the file.
     */
    public void play() {
        selectedAudioFile.get()
            .ifPresent(file -> player.play(file.path(), () -> recordStop(file)));
    }

    /**
     * Returns the time when the last playback stopped. It is empty until a
     * playback stops.
     *
     * @return read-only property of the time when the last playback stopped
     */
    public ReadOnlyObjectProperty<Optional<Instant>> lastPlayedAtProperty() {
        return lastPlayedAt.getReadOnlyProperty();
    }

    /**
     * Stops the playback.
     */
    public void stop() {
        player.stop();
    }

    private void recordStop(AudioFile file) {
        var stoppedAt = clock.instant();
        lastPlayedAt.set(Optional.of(stoppedAt));
        repository.saveLastPlayedAt(file.fingerprint(), stoppedAt);
    }
}
