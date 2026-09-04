package com.sosuisha.main;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

import com.sosuisha.domain.exception.AudioFolderScanException;
import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.alert.AlertDialog;
import com.sosuisha.presentation.screens.drill.DrillView;
import com.sosuisha.presentation.screens.drill.DrillViewModel;
import com.sosuisha.service.FileSystemAudioFolderScanner;
import com.sosuisha.repository.SqliteDrillRepository;
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
    static final Class<? extends View> FIRST_VIEW = DrillView.class;
    /** Fixed folder that holds the drill audio files. */
    static final Path AUDIO_FOLDER = Path.of("D:\\Dropbox\\英語のハノン_210407");
    /** SQLite file that keeps the records of the drills. */
    static final Path DRILL_DB =
        Path.of(System.getProperty("user.home"), ".english-drill-helper", "drill.db");

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
        var fingerprinter = new Sha256Fingerprinter();
        var audioFiles = new FileSystemAudioFolderScanner().scan(AUDIO_FOLDER)
            .stream()
            .map(path -> new AudioFile(path, fingerprintOf(fingerprinter, path)))
            .toList();
        var repository = new SqliteDrillRepository(DRILL_DB);
        var drillViewModel = new DrillViewModel(
            audioFiles, new MediaAudioPlayer(), repository, Clock.systemUTC()
        );
        windowManager.registerView(new DrillView(drillViewModel));
        windowManager.showWindow(FIRST_VIEW, stage);
    }

    private static String fingerprintOf(Sha256Fingerprinter fingerprinter, Path path) {
        try {
            return fingerprinter.fingerprint(path);
        } catch (IOException e) {
            throw new AudioFolderScanException("Cannot read the audio file: " + path, e);
        }
    }
}
