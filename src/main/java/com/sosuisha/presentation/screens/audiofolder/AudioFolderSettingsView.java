package com.sosuisha.presentation.screens.audiofolder;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.sosuisha.presentation.View;

import atlantafx.base.theme.Styles;
import io.github.sosuisen.jfxbuilder.controls.ButtonBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.TextFieldBuilder;
import io.github.sosuisen.jfxbuilder.graphics.HBoxBuilder;
import io.github.sosuisen.jfxbuilder.graphics.SceneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.VBoxBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * Dialog that registers the folder of the drill audio files: the learner adds
 * a folder, gives it a drill name, and saves.
 */
public class AudioFolderSettingsView implements View {
    private static final String TITLE = "音声フォルダの登録";
    private static final String GUIDE = "音声ファイルのあるフォルダを追加してください";
    private static final double GAP = 8;
    private static final double PADDING = 16;
    private static final double WIDTH = 480;

    private final AudioFolderSettingsViewModel viewModel;
    private final Function<Window, Optional<Path>> folderChooser;
    private final Scene scene;

    /**
     * Creates the view.
     *
     * @param viewModel view model of the dialog
     * @param folderChooser lets the learner choose a folder, given the window
     *        of the dialog; empty when the learner cancels
     * @throws NullPointerException if viewModel or folderChooser is null
     */
    public AudioFolderSettingsView(
        AudioFolderSettingsViewModel viewModel, Function<Window, Optional<Path>> folderChooser) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel must not be null");
        this.folderChooser =
            Objects.requireNonNull(folderChooser, "folderChooser must not be null");
        this.scene = buildSceneGraph();
    }

    @Override
    public Scene getScene() {
        return scene;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    private Scene buildSceneGraph() {
        return SceneBuilder
            .withRoot(
                VBoxBuilder
                    .withChildren(
                        HBoxBuilder
                            .withChildren(
                                LabelBuilder.create().id("guide").text(GUIDE).build(),
                                ButtonBuilder.create()
                                    .id("add")
                                    .text("追加")
                                    .onAction(_ -> chooseFolder())
                                    .build()
                            )
                            .spacing(GAP)
                            .alignment(Pos.CENTER_LEFT)
                            .build(),
                        LabelBuilder.create().text("ドリル名").addStyleClass(Styles.TEXT_MUTED).build(),
                        TextFieldBuilder.create()
                            .id("name")
                            .textPropertyApply(
                                text -> text.bindBidirectional(viewModel.nameProperty())
                            )
                            .build(),
                        LabelBuilder.create()
                            .text("ドリルの場所")
                            .addStyleClass(Styles.TEXT_MUTED)
                            .build(),
                        LabelBuilder.create()
                            .id("location")
                            .textPropertyApply(
                                text -> text.bind(viewModel.locationTextProperty())
                            )
                            .build(),
                        HBoxBuilder
                            .withChildren(
                                ButtonBuilder.create()
                                    .id("save")
                                    .text("保存")
                                    .addStyleClass(Styles.ACCENT)
                                    .disablePropertyApply(
                                        disable -> disable.bind(viewModel.canSaveProperty().not())
                                    )
                                    .onAction(_ -> viewModel.save())
                                    .build()
                            )
                            .alignment(Pos.CENTER_RIGHT)
                            .build()
                    )
                    .spacing(GAP)
                    .padding(new Insets(PADDING))
                    .prefWidth(WIDTH)
                    .build()
            )
            .build();
    }

    // The chooser gets the window of the dialog so that it opens over it.
    private void chooseFolder() {
        folderChooser.apply(scene.getWindow()).ifPresent(viewModel::chooseFolder);
    }
}
