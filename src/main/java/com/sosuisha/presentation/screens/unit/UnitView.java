package com.sosuisha.presentation.screens.unit;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.sosuisha.domain.model.Unit;
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
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;

/**
 * View for the unit screen.
 */
public class UnitView implements View {
    private static final String TITLE = "English Drill Helper";
    private static final double WIDTH = 480;
    private static final double HEIGHT = 240;

    private final UnitViewModel viewModel;
    private final Scene scene;

    /**
     * Creates the view.
     *
     * @param viewModel view model of the unit screen
     * @throws NullPointerException if viewModel is null
     */
    public UnitView(UnitViewModel viewModel) {
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
                        ListViewBuilder.<Unit>create()
                            .id("units")
                            .items(viewModel.getUnits())
                            .cellFactory(_ -> unitCell())
                            .hGrowInHBox(Priority.ALWAYS)
                            .apply(
                                listView -> listView.getSelectionModel()
                                    .selectedItemProperty()
                                    .subscribe(viewModel::selectUnit)
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
                                    .build(),
                                ListViewBuilder.<TurnRow>create()
                                    .id("turns")
                                    .items(viewModel.getTurnRows())
                                    .cellFactory(_ -> turnCell())
                                    .vGrowInVBox(Priority.ALWAYS)
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

    private static ListCell<TurnRow> turnCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(@Nullable TurnRow item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : item.label());
            }
        };
    }

    private ListCell<Unit> unitCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(@Nullable Unit item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : cellTextOf(item));
            }
        };
    }

    private String cellTextOf(Unit unit) {
        var lastPlayedAt = viewModel.lastPlayedAtTextOf(unit);
        return lastPlayedAt.isEmpty() ? unit.fileName() : unit.fileName() + "  " + lastPlayedAt;
    }
}
