package com.sosuisha.presentation.screens.drill;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * ViewModel for the drill screen. It holds the audio files of the drills,
 * the question shown to the user, the answer typed by the user, and the
 * feedback to the answer.
 */
public class DrillViewModel {
    private static final String INITIAL_QUESTION = "Type any English sentence and press Check.";
    private static final String EMPTY_ANSWER_FEEDBACK = "Please type an answer.";

    private final ReadOnlyStringWrapper question = new ReadOnlyStringWrapper(INITIAL_QUESTION);
    private final StringProperty answer = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper feedback = new ReadOnlyStringWrapper("");
    private final ObservableList<Path> audioFiles = FXCollections.observableArrayList();
    private final ObservableList<Path> readOnlyAudioFiles =
        FXCollections.unmodifiableObservableList(audioFiles);

    /**
     * Creates the view model.
     *
     * @param audioFiles audio files of the drills, in the order shown to the user
     * @throws NullPointerException if audioFiles is null
     */
    public DrillViewModel(List<Path> audioFiles) {
        Objects.requireNonNull(audioFiles, "audioFiles must not be null");
        this.audioFiles.setAll(audioFiles);
    }

    /**
     * Returns the audio files of the drills, in the order shown to the user.
     * The list cannot be modified by the caller.
     *
     * @return read-only observable list of the audio files
     */
    ObservableList<Path> getAudioFiles() {
        return readOnlyAudioFiles;
    }

    /**
     * Returns the question shown to the user.
     *
     * @return read-only string property of the question
     */
    public ReadOnlyStringProperty questionProperty() {
        return question.getReadOnlyProperty();
    }

    /**
     * Returns the answer typed by the user. The initial value is an empty
     * string.
     *
     * @return string property of the answer
     */
    public StringProperty answerProperty() {
        return answer;
    }

    /**
     * Returns the feedback to the last checked answer. It is an empty string
     * until the answer is checked.
     *
     * @return read-only string property of the feedback
     */
    public ReadOnlyStringProperty feedbackProperty() {
        return feedback.getReadOnlyProperty();
    }

    /**
     * Checks the current answer and updates the feedback. A blank answer gets
     * a feedback that asks the user to type an answer.
     */
    public void check() {
        var text = answer.get();
        if (text == null || text.isBlank()) {
            feedback.set(EMPTY_ANSWER_FEEDBACK);
            return;
        }
        feedback.set("You typed: " + text.strip());
    }

    /**
     * Clears the answer and the feedback.
     */
    public void clear() {
        answer.set("");
        feedback.set("");
    }
}
