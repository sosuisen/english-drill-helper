package com.sosuisha.presentation.screens.drill;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.Drill;
import com.sosuisha.domain.repository.DrillRepository;
import com.sosuisha.domain.service.AudioPlayer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the drill screen. It holds the drills and the selected drill,
 * plays the selected drill, and records the time when the playback stopped.
 */
public class DrillViewModel {
    private static final DateTimeFormatter LAST_PLAYED_AT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<Drill> drills = FXCollections.observableArrayList();
    private final ObservableList<Drill> readOnlyDrills =
        FXCollections.unmodifiableObservableList(drills);
    private final ObjectProperty<Optional<Drill>> selectedDrill =
        new SimpleObjectProperty<>(Optional.empty());
    private final ReadOnlyStringWrapper selectedFileName = new ReadOnlyStringWrapper();
    private final AudioPlayer player;
    private final DrillRepository repository;
    private final Clock clock;

    /**
     * Creates the view model.
     *
     * @param drills drills, in the order shown to the user
     * @param player player that plays the selected drill
     * @param repository database that keeps the records of the drills
     * @param clock clock that gives the time when the playback stops
     * @throws NullPointerException if any argument is null
     */
    public DrillViewModel(
        List<Drill> drills, AudioPlayer player, DrillRepository repository, Clock clock) {
        Objects.requireNonNull(drills, "drills must not be null");
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.drills.setAll(drills);
        selectedFileName.bind(
            selectedDrill.map(drill -> drill.map(Drill::fileName).orElse(""))
        );
    }

    /**
     * Returns the drills, in the order shown to the user. The list cannot be
     * modified by the caller.
     *
     * @return read-only observable list of the drills
     */
    ObservableList<Drill> getDrills() {
        return readOnlyDrills;
    }

    /**
     * Selects a drill.
     *
     * @param drill drill to select, or null to clear the selection
     */
    public void selectDrill(@Nullable Drill drill) {
        selectedDrill.set(Optional.ofNullable(drill));
    }

    /**
     * Returns the file name of the selected drill. It is an empty string while
     * no drill is selected.
     *
     * @return read-only string property of the selected file name
     */
    public ReadOnlyStringProperty selectedFileNameProperty() {
        return selectedFileName.getReadOnlyProperty();
    }

    /**
     * Plays the selected drill. Does nothing while no drill is selected. When
     * the playback stops, the time is recorded as the last played time of the
     * drill.
     */
    public void play() {
        selectedDrill.get()
            .ifPresent(drill -> player.play(drill.audioFile().path(), () -> recordStop(drill)));
    }

    /**
     * Returns the text of the last played time of the drill, in the time zone
     * of the clock.
     *
     * @param drill drill whose last played time is shown
     * @return the time as {@code yyyy-MM-dd HH:mm}, or an empty string if the drill has never
     *         been played
     * @throws NullPointerException if drill is null
     */
    public String lastPlayedAtTextOf(Drill drill) {
        Objects.requireNonNull(drill, "drill must not be null");
        return drill.lastPlayedAt()
            .map(LAST_PLAYED_AT_FORMAT.withZone(clock.getZone())::format)
            .orElse("");
    }

    /**
     * Stops the playback.
     */
    public void stop() {
        player.stop();
    }

    private void recordStop(Drill drill) {
        var stoppedAt = clock.instant();
        repository.saveLastPlayedAt(drill.audioFile().fingerprint(), stoppedAt);
        var updated = drill.withLastPlayedAt(stoppedAt);
        replaceDrill(updated);
        selectedDrill.get()
            .filter(selected -> isSameDrill(selected, updated))
            .ifPresent(_ -> selectedDrill.set(Optional.of(updated)));
    }

    /** Replaces the row of the same drill, found by fingerprint, with the updated one. */
    private void replaceDrill(Drill updated) {
        for (var i = 0; i < drills.size(); i++) {
            if (isSameDrill(drills.get(i), updated)) {
                drills.set(i, updated);
                return;
            }
        }
    }

    private static boolean isSameDrill(Drill a, Drill b) {
        return a.audioFile().fingerprint().equals(b.audioFile().fingerprint());
    }
}
