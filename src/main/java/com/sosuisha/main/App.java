package com.sosuisha.main;

import java.util.Objects;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.alert.AlertDialog;
import com.sosuisha.presentation.screens.drill.DrillView;
import com.sosuisha.presentation.screens.drill.DrillViewModel;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application of English Drill Helper.
 */
public class App extends Application {
    /** Change this constant during development to open another window first. */
    static final Class<? extends View> FIRST_VIEW = DrillView.class;

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
        windowManager.registerView(new DrillView(new DrillViewModel()));
        windowManager.showWindow(FIRST_VIEW, stage);
    }
}
