package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.SegmentDetectionParameters;
import com.sosuisha.domain.repository.NullSegmentRepository;

class SegmentLoaderTest {
    private static final Path WAV = Path.of("src/test/resources/audio/tone-with-silences.wav");
    private static final List<Segment> CACHED_SEGMENTS = List.of(
        new Segment(Duration.ofMillis(1000), Segment.Kind.SOUND),
        new Segment(Duration.ofMillis(3000), Segment.Kind.SILENCE)
    );

    private final JavaSoundAudioDecoder decoder = new JavaSoundAudioDecoder();
    private final SegmentDetector detector =
        new SegmentDetector(SegmentDetectionParameters.DEFAULT);

    @Test
    @DisplayName("リポジトリにセグメントがあれば、それをそのまま返し、ファイルは読まない")
    void segments_found_in_the_repository_are_returned_without_reading_the_file() {
        var repository = new NullSegmentRepository() {
            @Override
            public Optional<List<Segment>> findSegments(String fingerprint) {
                return fingerprint.equals("cached") ? Optional.of(CACHED_SEGMENTS)
                    : Optional.empty();
            }
        };
        var missingFile = new AudioFile(Path.of("missing.mp3"), "cached");
        var loader = new SegmentLoader(decoder, detector, repository);

        var segments = loader.load(missingFile);

        assertEquals(CACHED_SEGMENTS, segments);
    }

    @Test
    @DisplayName("リポジトリになければ、ファイルをデコードして検出し、その結果をリポジトリに保存してから返す")
    void segments_not_in_the_repository_are_detected_from_the_file_and_saved() {
        var savedFingerprint = new AtomicReference<@Nullable String>();
        var savedSegments = new AtomicReference<@Nullable List<Segment>>();
        var repository = new NullSegmentRepository() {
            @Override
            public void saveSegments(String fingerprint, List<Segment> segments) {
                savedFingerprint.set(fingerprint);
                savedSegments.set(segments);
            }
        };
        var wav = new AudioFile(WAV, "fingerprint-of-wav");
        var loader = new SegmentLoader(decoder, detector, repository);

        var segments = loader.load(wav);

        var detected = detector.detect(decoder.decode(WAV));
        assertEquals(detected, segments);
        assertEquals("fingerprint-of-wav", savedFingerprint.get());
        assertEquals(detected, savedSegments.get());
    }
}
