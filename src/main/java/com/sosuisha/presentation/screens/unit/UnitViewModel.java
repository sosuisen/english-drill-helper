package com.sosuisha.presentation.screens.unit;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.UnitRepository;
import com.sosuisha.domain.service.AudioPlayer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the unit screen. It holds the units and the selected unit,
 * plays the selected unit, and records the time when the playback stopped.
 */
public class UnitViewModel {
    private static final DateTimeFormatter LAST_PLAYED_AT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<Unit> units = FXCollections.observableArrayList();
    private final ObservableList<Unit> readOnlyUnits =
        FXCollections.unmodifiableObservableList(units);
    private final ObjectProperty<Optional<Unit>> selectedUnit =
        new SimpleObjectProperty<>(Optional.empty());
    private final ReadOnlyStringWrapper selectedFileName = new ReadOnlyStringWrapper();
    private final AudioPlayer player;
    private final UnitRepository repository;
    private final Clock clock;

    /**
     * Creates the view model.
     *
     * @param units units, in the order shown to the user
     * @param player player that plays the selected unit
     * @param repository database that keeps the records of the units
     * @param clock clock that gives the time when the playback stops
     * @throws NullPointerException if any argument is null
     */
    public UnitViewModel(
        List<Unit> units, AudioPlayer player, UnitRepository repository, Clock clock) {
        Objects.requireNonNull(units, "units must not be null");
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.units.setAll(units);
        selectedFileName.bind(
            selectedUnit.map(unit -> unit.map(Unit::fileName).orElse(""))
        );
    }

    /**
     * Returns the units, in the order shown to the user. The list cannot be
     * modified by the caller.
     *
     * @return read-only observable list of the units
     */
    ObservableList<Unit> getUnits() {
        return readOnlyUnits;
    }

    /**
     * Selects a unit.
     *
     * @param unit unit to select, or null to clear the selection
     */
    public void selectUnit(@Nullable Unit unit) {
        selectedUnit.set(Optional.ofNullable(unit));
    }

    /**
     * Returns the file name of the selected unit. It is an empty string while
     * no unit is selected.
     *
     * @return read-only string property of the selected file name
     */
    public ReadOnlyStringProperty selectedFileNameProperty() {
        return selectedFileName.getReadOnlyProperty();
    }

    /**
     * Plays the selected unit. Does nothing while no unit is selected. When
     * the playback stops, the time is recorded as the last played time of the
     * unit.
     */
    public void play() {
        selectedUnit.get()
            .ifPresent(unit -> player.play(unit.audioFile().path(), () -> recordStop(unit)));
    }

    /**
     * Returns the text of the last played time of the unit, in the time zone
     * of the clock.
     *
     * @param unit unit whose last played time is shown
     * @return the time as {@code yyyy-MM-dd HH:mm}, or an empty string if the unit has never
     *         been played
     * @throws NullPointerException if unit is null
     */
    public String lastPlayedAtTextOf(Unit unit) {
        Objects.requireNonNull(unit, "unit must not be null");
        return unit.lastPlayedAt()
            .map(LAST_PLAYED_AT_FORMAT.withZone(clock.getZone())::format)
            .orElse("");
    }

    /**
     * Stops the playback.
     */
    public void stop() {
        player.stop();
    }

    private void recordStop(Unit unit) {
        var stoppedAt = clock.instant();
        repository.saveLastPlayedAt(unit.audioFile().fingerprint(), stoppedAt);
        var updated = unit.withLastPlayedAt(stoppedAt);
        replaceUnit(updated);
        selectedUnit.get()
            .filter(selected -> isSameUnit(selected, updated))
            .ifPresent(_ -> selectedUnit.set(Optional.of(updated)));
    }

    /** Replaces the row of the same unit, found by fingerprint, with the updated one. */
    private void replaceUnit(Unit updated) {
        for (var i = 0; i < units.size(); i++) {
            if (isSameUnit(units.get(i), updated)) {
                units.set(i, updated);
                return;
            }
        }
    }

    private static boolean isSameUnit(Unit a, Unit b) {
        return a.audioFile().fingerprint().equals(b.audioFile().fingerprint());
    }
}
