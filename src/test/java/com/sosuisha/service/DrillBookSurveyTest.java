package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.DrillBook;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.SegmentDetectionParameters;
import com.sosuisha.domain.model.Unit;

/**
 * Runs the detector and the drill book parser over a real audio folder. It
 * runs only when the system property {@code edh.survey.folder} names the
 * folder, so the regular build does not depend on the drill files.
 */
class DrillBookSurveyTest {
    private static final String FOLDER_PROPERTY = "edh.survey.folder";

    @Test
    @DisplayName("実際の音声フォルダの全ファイルを検出器と解析器で処理しても、例外が出ない（edh.survey.folder が指定されたときだけ実行）")
    void every_file_of_the_real_folder_is_parsed_without_an_exception() throws IOException {
        var folder = System.getProperty(FOLDER_PROPERTY);
        assumeTrue(folder != null, "set -D" + FOLDER_PROPERTY + " to run the survey");
        var decoder = new JavaSoundAudioDecoder();
        var detector = new SegmentDetector(SegmentDetectionParameters.DEFAULT);
        var failures = new ArrayList<String>();

        try (var files = Files.list(Path.of(folder))) {
            for (var file : files.filter(f -> f.toString().endsWith(".mp3")).sorted().toList()) {
                var unit =
                    new Unit(new AudioFile(file, file.getFileName().toString()), Optional.empty());
                var segments = detector.detect(decoder.decode(file));
                try {
                    DrillBook.drillsOf(segments, unit);
                } catch (RuntimeException e) {
                    failures.add(
                        file.getFileName() + ": " + e.getMessage() + " | " + layoutOf(segments)
                    );
                }
            }
        }

        assertEquals(List.of(), failures);
    }

    /** The lengths of the segments in seconds, such as "s1.93 q1.73 s1.25 ...". */
    private static String layoutOf(List<Segment> segments) {
        return segments.stream()
            .map(
                s -> (s.kind() == Segment.Kind.SOUND ? "s" : "q")
                    + String.format("%.2f", s.duration().toMillis() / 1000.0)
            )
            .collect(Collectors.joining(" "));
    }
}
