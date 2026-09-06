package com.sosuisha.presentation.screens.unit;

import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.domain.model.Drill;
import com.sosuisha.domain.model.DrillBook;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.Turn;
import com.sosuisha.domain.model.Unit;
import com.sosuisha.domain.repository.UnitRepository;
import com.sosuisha.domain.service.AudioPlayer;
import com.sosuisha.domain.service.PlaybackListener;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * ViewModel for the unit screen. It holds the units and the selected unit,
 * loads the segments of the selected unit and builds its drills from them,
 * plays the selected unit, and records the time when the playback stopped.
 */
public class UnitViewModel {
    private static final DateTimeFormatter LAST_PLAYED_AT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObservableList<Unit> units = FXCollections.observableArrayList();
    private final ObservableList<Unit> readOnlyUnits =
        FXCollections.unmodifiableObservableList(units);
    private final ReadOnlyObjectWrapper<Optional<Unit>> selectedUnit =
        new ReadOnlyObjectWrapper<>(Optional.empty());
    private final ReadOnlyStringWrapper selectedUnitTitle = new ReadOnlyStringWrapper();
    private final ReadOnlyBooleanWrapper unitSelected = new ReadOnlyBooleanWrapper();
    private final ReadOnlyStringWrapper audioFolderName = new ReadOnlyStringWrapper("");
    private final ReadOnlyObjectWrapper<PlaybackState> playbackState =
        new ReadOnlyObjectWrapper<>(PlaybackState.STOPPED);
    private final ObservableList<Segment> segments = FXCollections.observableArrayList();
    private final ObservableList<Segment> readOnlySegments =
        FXCollections.unmodifiableObservableList(segments);
    private final ObservableList<Drill> drills = FXCollections.observableArrayList();
    private final ObservableList<Drill> readOnlyDrills =
        FXCollections.unmodifiableObservableList(drills);
    private final ObservableList<TurnRow> turnRows = FXCollections.observableArrayList();
    private final ObservableList<TurnRow> readOnlyTurnRows =
        FXCollections.unmodifiableObservableList(turnRows);
    private final ReadOnlyObjectWrapper<Optional<TurnRow>> currentTurnRow =
        new ReadOnlyObjectWrapper<>(Optional.empty());
    private final ReadOnlyStringWrapper positionText = new ReadOnlyStringWrapper("");
    private Duration position = Duration.ZERO;
    private final AudioPlayer player;
    private final UnitRepository repository;
    private final Clock clock;
    private final Function<AudioFile, List<Segment>> segmentLoader;
    private final Executor executor;

    /**
     * Creates the view model.
     *
     * @param units units, in the order shown to the user
     * @param player player that plays the selected unit
     * @param repository database that keeps the records of the units
     * @param clock clock that gives the time when the playback stops
     * @param segmentLoader gives the segments of an audio file; it may take a while
     * @param executor runs the segment loading in the background
     * @throws NullPointerException if any argument is null
     */
    public UnitViewModel(
        AudioPlayer player, UnitRepository repository, Clock clock,
        Function<AudioFile, List<Segment>> segmentLoader, Executor executor) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.segmentLoader =
            Objects.requireNonNull(segmentLoader, "segmentLoader must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        selectedUnitTitle.bind(selectedUnit.map(unit -> unit.map(Unit::title).orElse("")));
        unitSelected.bind(selectedUnit.map(Optional::isPresent));
        segments.addListener(
            (ListChangeListener<Segment>) _ -> drills.setAll(drillsOfSelectedUnit())
        );
        drills.addListener((ListChangeListener<Drill>) _ -> turnRows.setAll(turnRowsOf(drills)));
        turnRows.addListener(
            (ListChangeListener<TurnRow>) _ -> currentTurnRow.set(turnRows.stream().findFirst())
        );
        segments.addListener((ListChangeListener<Segment>) _ -> {
            position = Duration.ZERO;
            updatePositionText();
        });
    }

    /**
     * Returns the row of the current turn: the turn the learner is at, playing
     * or stopped. It is the first turn of the unit until a playback moves on.
     * It is empty while the unit has no turns.
     *
     * @return read-only property of the current turn row
     */
    public ReadOnlyObjectProperty<Optional<TurnRow>> currentTurnRowProperty() {
        return currentTurnRow.getReadOnlyProperty();
    }

    /**
     * Returns the playback position over the length of the selected unit, as
     * {@code mm:ss / mm:ss}. It is an empty string while no unit is selected.
     *
     * @return read-only string property of the position text
     */
    public ReadOnlyStringProperty positionTextProperty() {
        return positionText.getReadOnlyProperty();
    }

    private void updatePositionText() {
        if (segments.isEmpty()) {
            positionText.set("");
            return;
        }
        var last = segments.getLast();
        var total = last.start().plus(last.duration());
        positionText.set(minutesAndSeconds(position) + " / " + minutesAndSeconds(total));
    }

    private static String minutesAndSeconds(Duration duration) {
        var seconds = duration.toSeconds();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private Optional<TurnRow> turnRowAt(Duration position) {
        Optional<TurnRow> playing = Optional.empty();
        for (var row : turnRows) {
            if (startOf(row).compareTo(position) > 0) {
                break;
            }
            playing = Optional.of(row);
        }
        return playing;
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
     * Returns the segments of the selected unit, in time order. The list is
     * empty while no unit is selected or while the segments are being loaded.
     * The list cannot be modified by the caller.
     *
     * @return read-only observable list of the segments
     */
    ObservableList<Segment> getSegments() {
        return readOnlySegments;
    }

    /**
     * Returns the drills of the selected unit, in time order. They are built
     * from the segments whenever the segment list changes, so the list is
     * empty while the segment list is empty. The list cannot be modified by
     * the caller.
     *
     * @return read-only observable list of the drills
     */
    ObservableList<Drill> getDrills() {
        return readOnlyDrills;
    }

    /**
     * Returns the rows of the turn list: every turn of every drill of the
     * selected unit, in drill and turn order. The list follows the drill list
     * and cannot be modified by the caller.
     *
     * @return read-only observable list of the turn rows
     */
    ObservableList<TurnRow> getTurnRows() {
        return readOnlyTurnRows;
    }

    /** The shown numbers count the turns of a drill without its cues. */
    private static List<TurnRow> turnRowsOf(List<Drill> drills) {
        var rows = new ArrayList<TurnRow>();
        for (var drill : drills) {
            var shownNumber = 0;
            for (var turn : drill.turns()) {
                var isCue = turn.role() == Turn.Role.CUE;
                if (!isCue) {
                    shownNumber++;
                }
                rows.add(new TurnRow(drill.number(), turn, isCue ? 0 : shownNumber));
            }
        }
        return List.copyOf(rows);
    }

    /**
     * Selects a unit and starts loading its segments in the background. The
     * playback of the unit selected before is stopped, and the segment list
     * is emptied at once and filled when the loading is done. Selecting the
     * unit that is already selected (as a new row of the list, for example
     * with a new last played time) only takes the row: the playback and the
     * segments stay.
     * When the loading fails, the exception is rethrown on the FX thread and
     * reaches the uncaught exception handler of the application.
     *
     * @param unit unit to select, or null to clear the selection
     */
    public void selectUnit(@Nullable Unit unit) {
        if (unit != null && isSelected(unit)) {
            selectedUnit.set(Optional.of(unit));
            return;
        }
        player.stop(); // the playback belongs to the unit that was selected
        playbackState.set(PlaybackState.STOPPED);
        selectedUnit.set(Optional.ofNullable(unit));
        segments.clear();
        if (unit != null) {
            loadSegments(unit);
        }
    }

    private void loadSegments(Unit unit) {
        var task = new Task<List<Segment>>() {
            @Override
            protected List<Segment> call() {
                return segmentLoader.apply(unit.audioFile());
            }
        };
        // A result of a unit that is no longer selected is dropped.
        task.setOnSucceeded(_ -> {
            if (isSelected(unit)) {
                segments.setAll(task.getValue());
            }
        });
        task.setOnFailed(_ -> {
            throw toRuntimeException(task.getException());
        });
        executor.execute(task);
    }

    private List<Drill> drillsOfSelectedUnit() {
        return selectedUnit.get()
            .map(unit -> DrillBook.drillsOf(segments, unit))
            .orElse(List.of());
    }

    private boolean isSelected(Unit unit) {
        return selectedUnit.get().filter(selected -> isSameUnit(selected, unit)).isPresent();
    }

    private static RuntimeException toRuntimeException(Throwable e) {
        if (e instanceof RuntimeException runtime) { return runtime; }
        if (e instanceof Error error) { throw error; }
        return new IllegalStateException(e);
    }

    /**
     * Returns the title of the selected unit (see {@link Unit#title()}). It is
     * an empty string while no unit is selected.
     *
     * @return read-only string property of the selected unit title
     */
    public ReadOnlyStringProperty selectedUnitTitleProperty() {
        return selectedUnitTitle.getReadOnlyProperty();
    }

    /**
     * Returns the selected unit, as the row of the unit list it was selected
     * from. The row changes when the last played time is recorded.
     *
     * @return the selected unit, or empty if no unit is selected
     */
    public ReadOnlyObjectProperty<Optional<Unit>> selectedUnitProperty() {
        return selectedUnit.getReadOnlyProperty();
    }

    /**
     * Shows a registered audio folder: its units replace the list, and its
     * name is shown as the drill name. The playback stops and the selection
     * is cleared, because they belonged to the folder shown before.
     *
     * @param folder the registered folder
     * @param units units found in the folder
     * @throws NullPointerException if folder or units is null
     */
    public void showFolder(AudioFolder folder, List<Unit> units) {
        Objects.requireNonNull(folder, "folder must not be null");
        Objects.requireNonNull(units, "units must not be null");
        selectUnit(null); // stops the playback of the folder shown before
        this.units.setAll(units);
        audioFolderName.set(folder.name());
    }

    /**
     * Returns the name of the audio folder shown, given by the learner as the
     * drill name.
     *
     * @return read-only property of the name, empty while no folder is shown
     */
    public ReadOnlyStringProperty audioFolderNameProperty() {
        return audioFolderName.getReadOnlyProperty();
    }

    /**
     * Tells whether a unit is selected. The play and stop controls make sense
     * only then.
     *
     * @return read-only boolean property, true while a unit is selected
     */
    public ReadOnlyBooleanProperty unitSelectedProperty() {
        return unitSelected.getReadOnlyProperty();
    }

    /**
     * Plays the selected unit. Does nothing while no unit is selected. When
     * the playback stops, the time is recorded as the last played time of the
     * unit.
     */
    public void play() {
        selectedUnit.get().ifPresent(unit -> playFrom(unit, Duration.ZERO));
    }

    /**
     * Plays the selected unit from the beginning of the turn of the row to
     * the end of the file, as the drill book means it. Does nothing while no
     * unit is selected. When the playback stops, the time is recorded as the
     * last played time of the unit.
     *
     * @param row row of the turn to start from
     * @throws NullPointerException if row is null
     */
    public void playTurn(TurnRow row) {
        Objects.requireNonNull(row, "row must not be null");
        selectedUnit.get().ifPresent(unit -> {
            currentTurnRow.set(Optional.of(row)); // at once, not only when the position arrives
            playFrom(unit, startOf(row));
        });
    }

    private Duration startOf(TurnRow row) {
        return segments.get(row.turn().firstSegmentIndex()).start();
    }

    private void playFrom(Unit unit, Duration start) {
        playbackState.set(PlaybackState.PLAYING);
        player.play(unit.audioFile().path(), start, new PlaybackListener() {
            @Override
            public void positionChanged(Duration position) {
                // A position before the start cannot belong to this playback. The media
                // player reports its old position once before it seeks to the start.
                if (position.compareTo(start) < 0) { return; }
                UnitViewModel.this.position = position;
                updatePositionText();
                turnRowAt(position).ifPresent(row -> currentTurnRow.set(Optional.of(row)));
            }

            @Override
            public void stopped() {
                playbackState.set(PlaybackState.STOPPED);
                recordStop(unit);
            }
        });
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
     * Plays the selected unit when nothing is playing, pauses the playback
     * while it is playing, and goes on with it while it is paused.
     */
    public void playOrPause() {
        switch (playbackState.get()) {
            case PLAYING -> {
                player.pause();
                playbackState.set(PlaybackState.PAUSED);
            }
            case PAUSED -> {
                player.resume();
                playbackState.set(PlaybackState.PLAYING);
            }
            case STOPPED -> playCurrentTurn();
        }
    }

    // The playback goes on from the turn the learner is at; from the beginning
    // of the file while the unit has no turns.
    private void playCurrentTurn() {
        selectedUnit.get().ifPresent(
            unit -> playFrom(
                unit, currentTurnRow.get().map(this::startOf).orElse(Duration.ZERO)
            )
        );
    }

    /**
     * Returns the state of the playback.
     *
     * @return the state
     */
    public ReadOnlyObjectProperty<PlaybackState> playbackStateProperty() {
        return playbackState.getReadOnlyProperty();
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
