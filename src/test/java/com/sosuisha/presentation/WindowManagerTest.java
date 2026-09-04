package com.sosuisha.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import com.sosuisha.domain.service.NullAudioPlayer;
import com.sosuisha.presentation.screens.drill.DrillView;
import com.sosuisha.presentation.screens.drill.DrillViewModel;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

@ExtendWith(ApplicationExtension.class)
class WindowManagerTest {
    @Test
    @DisplayName("登録したDrillViewをクラス指定で取得できる")
    void returns_registered_drill_view_by_its_class() {
        var windowManager = new WindowManager();
        var view =
            new DrillView(new DrillViewModel(List.of(), new NullAudioPlayer(), Clock.systemUTC()));

        windowManager.registerView(view);

        assertSame(view, windowManager.getView(DrillView.class));
    }

    @Test
    @DisplayName("未登録のViewを取得しようとすると、IllegalArgumentExceptionが投げられる")
    void getting_an_unregistered_view_throws_illegal_argument_exception() {
        var windowManager = new WindowManager();

        assertThrows(IllegalArgumentException.class, () -> windowManager.getView(DrillView.class));
    }

    @Test
    @DisplayName("showWindowすると、DrillViewのウィンドウが通常のタイトルバー付き（DECORATED）で表示される")
    void show_window_displays_the_drill_view_window_with_the_decorated_style(FxRobot robot) {
        var windowManager = new WindowManager();
        var view =
            new DrillView(new DrillViewModel(List.of(), new NullAudioPlayer(), Clock.systemUTC()));
        windowManager.registerView(view);

        robot.interact(() -> windowManager.showWindow(DrillView.class, new Stage()));

        var window = (Stage) robot.window("English Drill Helper");
        assertTrue(window.isShowing());
        assertSame(view.getScene(), window.getScene());
        assertEquals(StageStyle.DECORATED, window.getStyle());
    }
}
