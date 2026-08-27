package com.sosuisha.presentation.screens.drill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DrillViewModelTest {
    @Test
    @DisplayName("初期状態では、問題文が表示され、回答とフィードバックは空である")
    void the_initial_state_has_a_question_and_an_empty_answer_and_feedback() {
        var viewModel = new DrillViewModel();

        assertEquals(
            "Type any English sentence and press Check.", viewModel.questionProperty().get()
        );
        assertEquals("", viewModel.answerProperty().get());
        assertEquals("", viewModel.feedbackProperty().get());
    }

    @Test
    @DisplayName("回答を入力してチェックすると、入力した回答がフィードバックに表示される")
    void checking_a_typed_answer_shows_the_answer_in_the_feedback() {
        var viewModel = new DrillViewModel();
        viewModel.answerProperty().set("  I have a pen.  ");

        viewModel.check();

        assertEquals("You typed: I have a pen.", viewModel.feedbackProperty().get());
    }

    @Test
    @DisplayName("回答が空のままチェックすると、回答を促すフィードバックが表示される")
    void checking_a_blank_answer_asks_the_user_to_type_an_answer() {
        var viewModel = new DrillViewModel();
        viewModel.answerProperty().set("   ");

        viewModel.check();

        assertEquals("Please type an answer.", viewModel.feedbackProperty().get());
    }

    @Test
    @DisplayName("クリアすると、回答とフィードバックが空になる")
    void clearing_empties_the_answer_and_the_feedback() {
        var viewModel = new DrillViewModel();
        viewModel.answerProperty().set("I have a pen.");
        viewModel.check();

        viewModel.clear();

        assertEquals("", viewModel.answerProperty().get());
        assertEquals("", viewModel.feedbackProperty().get());
    }
}
