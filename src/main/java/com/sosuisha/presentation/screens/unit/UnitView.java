package com.sosuisha.presentation.screens.unit;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2MZ;

import com.sosuisha.domain.model.Unit;
import com.sosuisha.presentation.View;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import io.github.sosuisen.jfxbuilder.controls.ButtonBuilder;
import io.github.sosuisen.jfxbuilder.controls.LabelBuilder;
import io.github.sosuisen.jfxbuilder.controls.ListViewBuilder;
import io.github.sosuisen.jfxbuilder.controls.TableColumnBuilder;
import io.github.sosuisen.jfxbuilder.controls.TableViewBuilder;
import io.github.sosuisen.jfxbuilder.graphics.HBoxBuilder;
import io.github.sosuisen.jfxbuilder.graphics.SceneBuilder;
import io.github.sosuisen.jfxbuilder.graphics.VBoxBuilder;
import javafx.geometry.Insets;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Priority;
import javafx.stage.StageStyle;

/**
 * View for the unit screen.
 */
public class UnitView implements View {
    private static final String TITLE = "English Drill Helper";
    private static final double WIDTH = 624;
    private static final double HEIGHT = 640;
    private static final double LAST_PLAYED_COLUMN_WIDTH = 130;
    private static final String DRILL_START_CLASS = "drill-start";
    private static final String CUE_CLASS = "cue";

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

    @Override
    @SuppressWarnings("deprecation") // StageStyle.EXTENDED is a preview feature of JavaFX 26.
    public StageStyle stageStyle() {
        return StageStyle.EXTENDED;
    }

    // The header bar merges the title bar with the window and shows the app name.
    @SuppressWarnings("deprecation") // HeaderBar is a preview feature of JavaFX 26 and has no
                                     // builder API.
    private static HeaderBar headerBar() {
        var headerBar = new HeaderBar();
        headerBar.setId("headerBar");
        headerBar
            .setLeft(LabelBuilder.create().text(TITLE).padding(new Insets(0, 0, 0, 10)).build());
        return headerBar;
    }

    private Scene buildSceneGraph() {
        return SceneBuilder
            .withRoot(
                VBoxBuilder
                    .withChildren(
                        headerBar(),
                        HBoxBuilder
                            .withChildren(
                                card(
                                    "unitPane",
                                    TableViewBuilder.<Unit>create()
                                        .id("units")
                                        .addStyleClass(Styles.DENSE)
                                        .addStyleClass(Styles.STRIPED)
                                        .items(viewModel.getUnits())
                                        .addColumns(fileColumn(), lastPlayedColumn())
                                        .columnResizePolicy(
                                            TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
                                        )
                                        .apply(
                                            table -> table.getSelectionModel()
                                                .selectedItemProperty()
                                                .subscribe(viewModel::selectUnit)
                                        )
                                        .build()
                                ),
                                card(
                                    "drillPane",
                                    VBoxBuilder
                                        .withChildren(
                                            LabelBuilder.create()
                                                .id("selectedUnitTitle")
                                                .addStyleClass(Styles.TITLE_3)
                                                .textPropertyApply(
                                                    text -> text
                                                        .bind(viewModel.selectedUnitTitleProperty())
                                                )
                                                .build(),
                                            HBoxBuilder
                                                .withChildren(
                                                    ButtonBuilder.create()
                                                        .id("play")
                                                        .graphic(
                                                            new FontIcon(Material2MZ.PLAY_ARROW)
                                                        )
                                                        .addStyleClass(Styles.BUTTON_ICON)
                                                        .addStyleClass(Styles.ACCENT)
                                                        .onAction(_ -> viewModel.play())
                                                        .apply(this::disableWithoutSelection)
                                                        .build(),
                                                    ButtonBuilder.create()
                                                        .id("stop")
                                                        .graphic(new FontIcon(Material2MZ.STOP))
                                                        .addStyleClass(Styles.BUTTON_ICON)
                                                        .onAction(_ -> viewModel.stop())
                                                        .apply(this::disableWithoutSelection)
                                                        .build()
                                                )
                                                .spacing(10)
                                                .build(),
                                            ListViewBuilder.<TurnRow>create()
                                                .id("turns")
                                                .addStyleClass(Styles.DENSE)
                                                .addStyleClass(Styles.STRIPED)
                                                .items(viewModel.getTurnRows())
                                                .cellFactory(_ -> turnCell())
                                                .vGrowInVBox(Priority.ALWAYS)
                                                .apply(listView -> followPlayingTurn(listView))
                                                .build()
                                        )
                                        .spacing(10)
                                        .build()
                                )
                            )
                            .spacing(10)
                            .padding(new Insets(15))
                            .vGrowInVBox(Priority.ALWAYS)
                            .build()
                    )
                    .build()
            )
            .width(WIDTH)
            .height(HEIGHT)
            .build();
    }

    // Each pane is a card so that the two lists read as separate blocks.
    private static Card card(String id, Node body) {
        var card = new Card();
        card.setId(id);
        card.setBody(body);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // Playing and stopping make sense only while a unit is selected.
    private void disableWithoutSelection(Button button) {
        button.disableProperty().bind(viewModel.unitSelectedProperty().not());
    }

    // The first row of a drill gets a line above it as the boundary of the drill,
    // and a cue is shown in a muted color. The style classes mark what the cell is.
    private static void styleTurnCell(ListCell<TurnRow> cell, @Nullable TurnRow row) {
        cell.getStyleClass().removeAll(DRILL_START_CLASS, CUE_CLASS);
        var style = new StringBuilder();
        if (row != null && row.startsDrill()) {
            cell.getStyleClass().add(DRILL_START_CLASS);
            style.append(
                "-fx-border-color: -color-accent-muted transparent transparent transparent; "
            )
                .append("-fx-border-width: 2 0 0 0; ");
        }
        if (row != null && row.isCue()) {
            cell.getStyleClass().add(CUE_CLASS);
            style.append("-fx-text-fill: -color-accent-muted; ");
        }
        cell.setStyle(style.toString());
    }

    // The selected row follows the playing turn; selecting a row never plays it.
    // The list scrolls so that the playing row stays in view (see TurnListScroll).
    private void followPlayingTurn(ListView<TurnRow> listView) {
        viewModel.playingTurnRowProperty().subscribe(row -> row.ifPresentOrElse(playing -> {
            listView.getSelectionModel().select(playing);
            TurnListScroll
                .firstIndexToShow(
                    listView.getItems().indexOf(playing), firstVisibleIndexOf(listView)
                )
                .ifPresent(listView::scrollTo);
        }, listView.getSelectionModel()::clearSelection));
    }

    private static int firstVisibleIndexOf(ListView<TurnRow> listView) {
        var flow = (VirtualFlow<?>) listView.lookup(".virtual-flow");
        if (flow == null || flow.getFirstVisibleCell() == null) { return 0; }
        return flow.getFirstVisibleCell().getIndex();
    }

    // A click on a row plays its turn. Selection changes do not, because the
    // selection also follows the playback position.
    private ListCell<TurnRow> turnCell() {
        var cell = new ListCell<TurnRow>() {
            @Override
            protected void updateItem(@Nullable TurnRow item, boolean empty) {
                super.updateItem(item, empty);
                var row = empty ? null : item;
                setText(row == null ? null : row.label());
                styleTurnCell(this, row);
            }
        };
        cell.setOnMouseClicked(_ -> {
            if (!cell.isEmpty()) {
                viewModel.playTurn(cell.getItem());
            }
        });
        return cell;
    }

    private static TableColumn<Unit, String> fileColumn() {
        return TableColumnBuilder.<Unit, String>create("File")
            .cellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().fileName()))
            .build();
    }

    // The date and time take a fixed width; the file name gets the rest.
    private TableColumn<Unit, String> lastPlayedColumn() {
        return TableColumnBuilder.<Unit, String>create("Last played")
            .cellValueFactory(
                row -> new ReadOnlyStringWrapper(viewModel.lastPlayedAtTextOf(row.getValue()))
            )
            .minWidth(LAST_PLAYED_COLUMN_WIDTH)
            .prefWidth(LAST_PLAYED_COLUMN_WIDTH)
            .maxWidth(LAST_PLAYED_COLUMN_WIDTH)
            .build();
    }
}
