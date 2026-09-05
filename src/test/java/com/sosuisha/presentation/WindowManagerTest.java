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

import com.sosuisha.domain.repository.NullUnitRepository;
import com.sosuisha.domain.service.NullAudioPlayer;
import com.sosuisha.presentation.screens.unit.UnitView;
import com.sosuisha.presentation.screens.unit.UnitViewModel;

import javafx.stage.Stage;
import javafx.stage.StageStyle;

@ExtendWith(ApplicationExtension.class)
class WindowManagerTest {
    @Test
    @DisplayName("登録したUnitViewをクラス指定で取得できる")
    void returns_registered_unit_view_by_its_class() {
        var windowManager = new WindowManager();
        var view =
            new UnitView(
                new UnitViewModel(
                    List.of(), new NullAudioPlayer(), new NullUnitRepository(), Clock.systemUTC(),
                    _ -> List.of(), Runnable::run
                )
            );

        windowManager.registerView(view);

        assertSame(view, windowManager.getView(UnitView.class));
    }

    @Test
    @DisplayName("未登録のViewを取得しようとすると、IllegalArgumentExceptionが投げられる")
    void getting_an_unregistered_view_throws_illegal_argument_exception() {
        var windowManager = new WindowManager();

        assertThrows(IllegalArgumentException.class, () -> windowManager.getView(UnitView.class));
    }

    @Test
    @DisplayName("showWindowすると、UnitViewのウィンドウが通常のタイトルバー付き（DECORATED）で表示される")
    void show_window_displays_the_unit_view_window_with_the_decorated_style(FxRobot robot) {
        var windowManager = new WindowManager();
        var view =
            new UnitView(
                new UnitViewModel(
                    List.of(), new NullAudioPlayer(), new NullUnitRepository(), Clock.systemUTC(),
                    _ -> List.of(), Runnable::run
                )
            );
        windowManager.registerView(view);

        robot.interact(() -> windowManager.showWindow(UnitView.class, new Stage()));

        var window = (Stage) robot.window("English Drill Helper");
        assertTrue(window.isShowing());
        assertSame(view.getScene(), window.getScene());
        assertEquals(StageStyle.DECORATED, window.getStyle());
    }
}
