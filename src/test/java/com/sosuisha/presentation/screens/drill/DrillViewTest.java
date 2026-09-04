package com.sosuisha.presentation.screens.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testfx.api.FxAssert.verifyThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.control.TextInputControlMatchers;

import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
class DrillViewTest {
    private DrillViewModel viewModel;

    @Start
    void setup(Stage stage) {
        viewModel = new DrillViewModel(List.of());
        var view = new DrillView(viewModel);
        stage.setScene(view.getScene());
        stage.setTitle(view.getTitle());
        stage.show();
    }

    @Test
    @DisplayName("画面に問題文が表示される")
    void the_screen_shows_the_question() {
        verifyThat("#question", LabeledMatchers.hasText(viewModel.questionProperty().get()));
    }

    @Test
    @DisplayName("回答欄に入力してCheckボタンを押すと、フィードバックが表示される")
    void typing_an_answer_and_clicking_check_shows_the_feedback(FxRobot robot) {
        robot.clickOn("#answer").write("I have a pen.");

        robot.clickOn("#check");

        assertEquals("You typed: I have a pen.", viewModel.feedbackProperty().get());
        verifyThat("#feedback", LabeledMatchers.hasText("You typed: I have a pen."));
    }

    @Test
    @DisplayName("Clearボタンを押すと、回答欄とフィードバックが空になる")
    void clicking_clear_empties_the_answer_field_and_the_feedback(FxRobot robot) {
        robot.clickOn("#answer").write("I have a pen.");
        robot.clickOn("#check");

        robot.clickOn("#clear");

        verifyThat("#answer", TextInputControlMatchers.hasText(""));
        verifyThat("#feedback", LabeledMatchers.hasText(""));
    }
}
