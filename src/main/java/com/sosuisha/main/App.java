package com.sosuisha.main;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.alert.AlertDialog;
import com.sosuisha.presentation.screens.unit.UnitView;
import com.sosuisha.presentation.screens.unit.UnitViewModel;
import com.sosuisha.service.UnitLoader;
import com.sosuisha.service.JavaSoundAudioDecoder;
import com.sosuisha.service.SegmentLoader;
import com.sosuisha.service.SegmentDetector;
import com.sosuisha.repository.SqliteDatabase;
import com.sosuisha.repository.SqliteSegmentRepository;
import com.sosuisha.domain.model.SegmentDetectionParameters;
import com.sosuisha.service.FileSystemAudioFolderScanner;
import com.sosuisha.repository.SqliteUnitRepository;
import com.sosuisha.service.MediaAudioPlayer;
import com.sosuisha.service.Sha256Fingerprinter;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application of English Drill Helper.
 */
public class App extends Application {
    /** Change this constant during development to open another window first. */
    static final Class<? extends View> FIRST_VIEW = UnitView.class;
    /** Fixed folder that holds the drill audio files. */
    static final Path AUDIO_FOLDER = Path.of("D:\\Dropbox\\英語のハノン_210407");

    /**
     * Composition root that wires the dependencies and shows the first window.
     *
     * @param stage the primary stage for this application
     * @throws NullPointerException if stage is null
     */
    @Override
    public void start(Stage stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        Thread.currentThread().setUncaughtExceptionHandler((_, e) -> {
            if (e instanceof UnrecoverableException unrecoverable) {
                AlertDialog.showError(unrecoverable.getMessage());
            } else {
                e.printStackTrace();
                AlertDialog.showUnexpectedError(e);
            }
        });
        setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        var windowManager = new WindowManager();
        var database = new SqliteDatabase(SqliteDatabase.resolveFile());
        var repository = new SqliteUnitRepository(database);
        var scanner = new FileSystemAudioFolderScanner(new Sha256Fingerprinter());
        var units = new UnitLoader(scanner, repository).load(AUDIO_FOLDER);
        var segmentLoader = new SegmentLoader(
            new JavaSoundAudioDecoder(),
            new SegmentDetector(SegmentDetectionParameters.DEFAULT),
            new SqliteSegmentRepository(database)
        );
        var unitViewModel =
            new UnitViewModel(
                units, new MediaAudioPlayer(), repository, Clock.systemDefaultZone(),
                segmentLoader::load, runnable -> Thread.ofVirtual().start(runnable)
            );
        windowManager.registerView(new UnitView(unitViewModel));
        windowManager.showWindow(FIRST_VIEW, stage);
    }
}
