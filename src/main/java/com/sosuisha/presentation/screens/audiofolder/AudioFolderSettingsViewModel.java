package com.sosuisha.presentation.screens.audiofolder;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.domain.repository.AudioFolderRepository;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * State of the dialog that registers the folder of the drill audio files:
 * the folder the learner chose, the drill name given to it, and whether the
 * folder can be saved. A folder registered before is the default.
 */
public class AudioFolderSettingsViewModel {
    private final AudioFolderRepository repository;
    private final Consumer<AudioFolder> onSaved;
    private final ReadOnlyObjectWrapper<Optional<Path>> chosenFolder =
        new ReadOnlyObjectWrapper<>(Optional.empty());
    private final StringProperty name = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper locationText = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper canSave = new ReadOnlyBooleanWrapper(false);

    /**
     * Creates the view model.
     *
     * @param repository where the registered folder is saved
     * @param onSaved told the folder once it is saved
     * @throws NullPointerException if repository or onSaved is null
     */
    public AudioFolderSettingsViewModel(
        AudioFolderRepository repository, Consumer<AudioFolder> onSaved) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.onSaved = Objects.requireNonNull(onSaved, "onSaved must not be null");
        canSave.bind(
            chosenFolder.map(Optional::isPresent)
                .flatMap(chosen -> name.map(text -> chosen && !text.isBlank()))
        );
        // A registered folder is the default, so the learner sees what is set and may change it.
        var registered = repository.findAll();
        if (!registered.isEmpty()) {
            var folder = registered.getLast();
            chooseFolder(folder.path());
            name.set(folder.name());
        }
    }

    /**
     * Takes the folder the learner chose. Its name becomes the drill name, which
     * the learner may still change.
     *
     * @param folder folder of the drill audio files
     * @throws NullPointerException if folder is null
     */
    public void chooseFolder(Path folder) {
        Objects.requireNonNull(folder, "folder must not be null");
        chosenFolder.set(Optional.of(folder));
        locationText.set(folder.toString());
        name.set(String.valueOf(folder.getFileName()));
    }

    /**
     * Saves the chosen folder under the drill name and tells the caller.
     *
     * @throws IllegalStateException if the folder cannot be saved (see
     *             {@link #canSaveProperty()})
     * @throws com.sosuisha.domain.exception.RepositoryException if the database cannot be written
     */
    public void save() {
        if (!canSave.get()) {
            throw new IllegalStateException(
                "a folder must be chosen and the name must not be blank"
            );
        }
        var folder = new AudioFolder(name.get().strip(), chosenFolder.get().orElseThrow());
        repository.save(folder);
        onSaved.accept(folder);
    }

    /**
     * Returns the folder the learner chose.
     *
     * @return the folder, or empty while none is chosen
     */
    public ReadOnlyObjectProperty<Optional<Path>> chosenFolderProperty() {
        return chosenFolder.getReadOnlyProperty();
    }

    /**
     * Returns the drill name the learner gives the folder. The dialog edits it.
     *
     * @return the name, empty at first
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the location of the chosen folder as text to show.
     *
     * @return the path of the folder, or an empty string while none is chosen
     */
    public ReadOnlyStringProperty locationTextProperty() {
        return locationText.getReadOnlyProperty();
    }

    /**
     * Tells whether the folder can be saved: a folder is chosen and the name
     * is not blank.
     *
     * @return read-only property, false at first
     */
    public ReadOnlyBooleanProperty canSaveProperty() {
        return canSave.getReadOnlyProperty();
    }
}
