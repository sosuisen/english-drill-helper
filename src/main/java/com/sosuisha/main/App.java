package com.sosuisha.main;

import java.io.File;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.sosuisha.domain.exception.UnrecoverableException;
import com.sosuisha.domain.model.AudioFolder;
import com.sosuisha.domain.model.SegmentDetectionParameters;
import com.sosuisha.domain.repository.AudioFolderRepository;
import com.sosuisha.presentation.View;
import com.sosuisha.presentation.WindowManager;
import com.sosuisha.presentation.screens.alert.AlertDialog;
import com.sosuisha.presentation.screens.audiofolder.AudioFolderSettingsView;
import com.sosuisha.presentation.screens.audiofolder.AudioFolderSettingsViewModel;
import com.sosuisha.presentation.screens.unit.UnitView;
import com.sosuisha.presentation.screens.unit.UnitViewModel;
import com.sosuisha.repository.SqliteAudioFolderRepository;
import com.sosuisha.repository.SqliteDatabase;
import com.sosuisha.repository.SqliteSegmentRepository;
import com.sosuisha.repository.SqliteUnitRepository;
import com.sosuisha.service.FileSystemAudioFolderScanner;
import com.sosuisha.service.JavaSoundAudioDecoder;
import com.sosuisha.service.MediaAudioPlayer;
import com.sosuisha.service.SegmentDetector;
import com.sosuisha.service.SegmentLoader;
import com.sosuisha.service.Sha256Fingerprinter;
import com.sosuisha.service.UnitLoader;

import atlantafx.base.theme.NordLight;
import javafx.application.Application;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * JavaFX application of English Drill Player.
 */
public class App extends Application {
    /** The view whose window is shown at startup. */
    static final Class<? extends View> FIRST_VIEW = UnitView.class;

    /**
     * Sets up the application and shows the first window. When no audio folder
     * is registered yet, the dialog that registers one opens over the window.
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
        setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
        var windowManager = new WindowManager();
        var database = new SqliteDatabase(SqliteDatabase.resolveFile());
        var unitRepository = new SqliteUnitRepository(database);
        var folderRepository = new SqliteAudioFolderRepository(database);
        var unitLoader = new UnitLoader(
            new FileSystemAudioFolderScanner(new Sha256Fingerprinter()), unitRepository
        );
        var segmentLoader = new SegmentLoader(
            new JavaSoundAudioDecoder(),
            new SegmentDetector(SegmentDetectionParameters.DEFAULT),
            new SqliteSegmentRepository(database)
        );
        var unitViewModel = new UnitViewModel(
            new MediaAudioPlayer(), unitRepository, Clock.systemDefaultZone(),
            segmentLoader::load, runnable -> Thread.ofVirtual().start(runnable)
        );
        // The folder is scanned on the FX thread; the scan hashes every file and takes a while.
        Consumer<AudioFolder> showFolder =
            folder -> unitViewModel.showFolder(folder, unitLoader.load(folder.path()));
        Runnable openSettings = () -> showAudioFolderDialog(stage, folderRepository, showFolder);
        windowManager.registerView(new UnitView(unitViewModel, openSettings));
        windowManager.showWindow(FIRST_VIEW, stage);

        var registered = folderRepository.findAll();
        if (registered.isEmpty()) {
            openSettings.run();
        } else {
            showFolder.accept(registered.getLast());
        }
    }

    // The dialog is modal to the main window and closes itself once a folder is saved.
    private static void showAudioFolderDialog(
        Stage owner, AudioFolderRepository repository, Consumer<AudioFolder> onSaved) {
        var dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getIcons().addAll(owner.getIcons());
        var viewModel = new AudioFolderSettingsViewModel(repository, folder -> {
            onSaved.accept(folder);
            dialog.close();
        });
        var view = new AudioFolderSettingsView(viewModel, window -> {
            var chooser = new DirectoryChooser();
            chooser.setTitle("音声ファイルのあるフォルダ");
            return Optional.ofNullable(chooser.showDialog(window)).map(File::toPath);
        });
        dialog.setScene(view.getScene());
        dialog.setTitle(view.getTitle());
        dialog.show();
    }
}
