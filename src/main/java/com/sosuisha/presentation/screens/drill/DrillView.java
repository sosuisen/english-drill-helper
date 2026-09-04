package com.sosuisha.presentation.screens.drill;

import java.nio.file.Path;
import java.util.Objects;

import com.sosuisha.presentation.View;

import atlantafx.base.theme.Styles;
import io.github.sosuisen.jfxbuilder.controls.ButtonBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.ListViewBuilder;
import io.github.sosuisen.jfxbuilder.graphics.HBoxBuilder;
import io.github.sosuisen.jfxbuilder.graphics.SceneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.VBoxBuilder;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;

/**
 * View for the drill screen.
 */
public class DrillView implements View {
    private static final String TITLE = "English Drill Helper";
    private static final double WIDTH = 480;
    private static final double HEIGHT = 240;

    private final DrillViewModel viewModel;
    private final Scene scene;

    /**
     * Creates the view.
     *
     * @param viewModel view model of the drill screen
     * @throws NullPointerException if viewModel is null
     */
    public DrillView(DrillViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel must not be null");
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
                HBoxBuilder
                    .withChildren(
                        ListViewBuilder.<Path>create()
                            .id("audioFiles")
                            .items(viewModel.getAudioFiles())
                            .hGrowInHBox(Priority.ALWAYS)
                            .apply(
                                listView -> listView.getSelectionModel()
                                    .selectedItemProperty()
                                    .subscribe(viewModel::selectAudioFile)
                            )
                            .build(),
                        VBoxBuilder
                            .withChildren(
                                LabelBuilder.create()
                                    .id("selectedFileName")
                                    .textPropertyApply(
                                        text -> text.bind(viewModel.selectedFileNameProperty())
                                    )
                                    .build(),
                                HBoxBuilder
                                    .withChildren(
                                        ButtonBuilder.create()
                                            .id("play")
                                            .text("Play")
                                            .addStyleClass(Styles.ACCENT)
                                            .onAction(_ -> viewModel.play())
                                            .build(),
                                        ButtonBuilder.create()
                                            .id("stop")
                                            .text("Stop")
                                            .onAction(_ -> viewModel.stop())
                                            .build()
                                    )
                                    .spacing(10)
                                    .build()
                            )
                            .spacing(10)
                            .build()
                    )
                    .spacing(10)
                    .padding(new Insets(15))
                    .build()
            )
            .width(WIDTH)
            .height(HEIGHT)
            .build();
    }
}
