package com.sosuisha.main;

import java.nio.file.Path;
import java.util.Objects;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.alert.AlertDialog;
import com.sosuisha.presentation.screens.drill.DrillView;
import com.sosuisha.presentation.screens.drill.DrillViewModel;
import com.sosuisha.service.FileSystemAudioFolderScanner;
import com.sosuisha.service.MediaAudioPlayer;

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
        var audioFiles = new FileSystemAudioFolderScanner().scan(AUDIO_FOLDER);
        windowManager
            .registerView(new DrillView(new DrillViewModel(audioFiles, new MediaAudioPlayer())));
        windowManager.showWindow(FIRST_VIEW, stage);
    }
}
