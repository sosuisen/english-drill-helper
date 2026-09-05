package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.model.PcmAudio;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.SegmentDetectionParameters;

class SegmentDetectorTest {
    // 合成PCMの仕様: 44.1kHz、モノラル。有音は振幅がフルスケールの半分（-6 dBFS）、無音はゼロ
    private static final int SAMPLE_RATE = 44100;
    private static final short HALF_OF_FULL_SCALE = 16384;

    private final SegmentDetector detector =
        new SegmentDetector(SegmentDetectionParameters.DEFAULT);

    @Test
    @DisplayName("全体が有音のPCM（3秒）は、区間長3秒の有音セグメント1つになる")
    void pcm_of_sound_only_is_one_sound_segment_of_the_whole_length() {
        var pcm = pcm(sound(3.0));

        var segments = detector.detect(pcm);

        assertEquals(List.of(new Segment(Duration.ofSeconds(3), Segment.Kind.SOUND)), segments);
    }

    @Test
    @DisplayName("有音1秒・無音3秒・有音1秒のPCMは、有音1秒・無音3秒・有音1秒の3セグメントになる")
    void sound_silence_sound_becomes_three_segments_with_the_lengths_of_the_parts() {
        var pcm = pcm(sound(1.0), silence(3.0), sound(1.0));

        var segments = detector.detect(pcm);

        assertEquals(
            List.of(
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND),
                new Segment(Duration.ofSeconds(3), Segment.Kind.SILENCE),
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND)
            ),
            segments
        );
    }

    @Test
    @DisplayName("有音1秒・無音0.5秒・有音1秒のPCMは、区間長2.5秒の有音セグメント1つになる。最小無音長未満の無音（文中のポーズ）は有音に含まれる")
    void silence_shorter_than_the_minimum_is_absorbed_into_the_surrounding_sound() {
        var pcm = pcm(sound(1.0), silence(0.5), sound(1.0));

        var segments = detector.detect(pcm);

        assertEquals(
            List.of(new Segment(Duration.ofMillis(2500), Segment.Kind.SOUND)), segments
        );
    }

    @Test
    @DisplayName("無音がちょうど最小無音長（1秒）のPCMは、その無音が無音セグメントになる。最小無音長の境界は「以上」である")
    void silence_exactly_as_long_as_the_minimum_is_a_silence_segment() {
        var pcm = pcm(sound(1.0), silence(1.0), sound(1.0));

        var segments = detector.detect(pcm);

        assertEquals(
            List.of(
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND),
                new Segment(Duration.ofSeconds(1), Segment.Kind.SILENCE),
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND)
            ),
            segments
        );
    }

    @Test
    @DisplayName("無音3秒・有音1秒のPCMは、先頭が無音セグメントになる。無音で始まるファイルを扱える")
    void pcm_that_starts_with_silence_begins_with_a_silence_segment() {
        var pcm = pcm(silence(3.0), sound(1.0));

        var segments = detector.detect(pcm);

        assertEquals(
            List.of(
                new Segment(Duration.ofSeconds(3), Segment.Kind.SILENCE),
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND)
            ),
            segments
        );
    }

    @Test
    @DisplayName("有音1秒・無音3秒のPCMは、末尾が無音セグメントになる。無音で終わるファイルを扱える")
    void pcm_that_ends_with_silence_ends_with_a_silence_segment() {
        var pcm = pcm(sound(1.0), silence(3.0));

        var segments = detector.detect(pcm);

        assertEquals(
            List.of(
                new Segment(Duration.ofSeconds(1), Segment.Kind.SOUND),
                new Segment(Duration.ofSeconds(3), Segment.Kind.SILENCE)
            ),
            segments
        );
    }

    @Test
    @DisplayName("PCMの長さがウィンドウ幅で割り切れないとき（1.01秒）も、セグメントの区間長の合計はPCMの長さに一致する。端数のサンプルは失われない")
    void durations_of_the_segments_add_up_to_the_length_of_the_pcm_even_with_a_partial_last_window() {
        var pcm = pcm(sound(1.01));

        var segments = detector.detect(pcm);

        var total = segments.stream().map(Segment::duration).reduce(Duration.ZERO, Duration::plus);
        assertEquals(Duration.ofMillis(1010), total);
        assertEquals(
            List.of(new Segment(Duration.ofMillis(1010), Segment.Kind.SOUND)), segments
        );
    }

    @Test
    @DisplayName("音量がしきい値ちょうどのウィンドウは有音、それより小さいウィンドウは無音として扱う。しきい値の境界は「未満」である")
    void window_exactly_at_the_threshold_is_sound_and_a_quieter_window_is_silence() {
        var thresholdDbfs = Loudness.dbfsOf(constant(0.02, HALF_OF_FULL_SCALE));
        var parameters = new SegmentDetectionParameters(
            Duration.ofMillis(20), thresholdDbfs, Duration.ofSeconds(1)
        );
        var detectorAtThreshold = new SegmentDetector(parameters);

        var atThreshold = detectorAtThreshold.detect(pcm(constant(3.0, HALF_OF_FULL_SCALE)));
        var quieter =
            detectorAtThreshold.detect(pcm(constant(3.0, (short) (HALF_OF_FULL_SCALE / 2))));

        assertEquals(List.of(new Segment(Duration.ofSeconds(3), Segment.Kind.SOUND)), atThreshold);
        assertEquals(List.of(new Segment(Duration.ofSeconds(3), Segment.Kind.SILENCE)), quieter);
    }

    @Test
    @DisplayName("全体が無音のPCM（3秒）は、区間長3秒の無音セグメント1つになる")
    void pcm_of_silence_only_is_one_silence_segment_of_the_whole_length() {
        var pcm = pcm(silence(3.0));

        var segments = detector.detect(pcm);

        assertEquals(List.of(new Segment(Duration.ofSeconds(3), Segment.Kind.SILENCE)), segments);
    }

    @Test
    @DisplayName("サンプルが空のPCMは検出できず、IllegalArgumentExceptionになる")
    void pcm_without_samples_cannot_be_detected() {
        var empty = new PcmAudio(SAMPLE_RATE, 1, new short[0]);

        assertThrows(IllegalArgumentException.class, () -> detector.detect(empty));
    }

    // テスト用音声（src/test/resources/audio、helpers/generate-test-audio.sh で生成）の仕様
    // 440Hzのトーン、振幅はフルスケールの半分、44.1kHz、モノラル、10.5秒
    // 有音1.0秒・無音3.0秒・有音1.0秒・無音0.5秒（文中のポーズ）・有音1.0秒・無音3.0秒・有音1.0秒
    private static final Path WAV = Path.of("src/test/resources/audio/tone-with-silences.wav");
    private static final Path MP3 = Path.of("src/test/resources/audio/tone-with-silences.mp3");
    private static final List<Segment.Kind> EXPECTED_KINDS = List.of(
        Segment.Kind.SOUND, Segment.Kind.SILENCE, Segment.Kind.SOUND, Segment.Kind.SILENCE,
        Segment.Kind.SOUND
    );
    private static final List<Duration> EXPECTED_DURATIONS = List.of(
        Duration.ofMillis(1000), Duration.ofMillis(3000), Duration.ofMillis(2500),
        Duration.ofMillis(3000), Duration.ofMillis(1000)
    );

    @Test
    @DisplayName("WAVを既定パラメータで検出すると、有音1.0秒・無音3.0秒・有音2.5秒・無音3.0秒・有音1.0秒の5セグメントになる。0.5秒の文中ポーズは有音に吸収される。誤差はウィンドウ幅（20ms）以内")
    void wav_with_default_parameters_gives_five_segments_and_the_short_pause_is_absorbed() {
        var pcm = new JavaSoundAudioDecoder().decode(WAV);

        var segments = detector.detect(pcm);

        assertSegmentsClose(EXPECTED_KINDS, EXPECTED_DURATIONS, segments, Duration.ofMillis(20));
    }

    @Test
    @DisplayName("mp3を既定パラメータで検出しても、WAVと同じ5セグメントになる。エンコーダ遅延の分だけ先頭の有音が長くなるので、誤差は0.1秒以内")
    void mp3_with_default_parameters_gives_the_same_five_segments_within_the_encoder_delay() {
        var pcm = new JavaSoundAudioDecoder().decode(MP3);

        var segments = detector.detect(pcm);

        assertSegmentsClose(EXPECTED_KINDS, EXPECTED_DURATIONS, segments, Duration.ofMillis(100));
    }

    private static void assertSegmentsClose(
        List<Segment.Kind> expectedKinds, List<Duration> expectedDurations,
        List<Segment> actual, Duration tolerance) {
        assertEquals(expectedKinds, actual.stream().map(Segment::kind).toList());
        for (var i = 0; i < expectedDurations.size(); i++) {
            var difference = expectedDurations.get(i).minus(actual.get(i).duration()).abs();
            assertTrue(
                difference.compareTo(tolerance) <= 0,
                "segment " + i + ": expected " + expectedDurations.get(i) + " but was "
                    + actual.get(i).duration()
            );
        }
    }

    private static PcmAudio pcm(short[]... parts) {
        var length = Arrays.stream(parts).mapToInt(part -> part.length).sum();
        var samples = new short[length];
        var offset = 0;
        for (var part : parts) {
            System.arraycopy(part, 0, samples, offset, part.length);
            offset += part.length;
        }
        return new PcmAudio(SAMPLE_RATE, 1, samples);
    }

    private static short[] silence(double seconds) {
        return new short[(int) (seconds * SAMPLE_RATE)];
    }

    private static short[] sound(double seconds) {
        return constant(seconds, HALF_OF_FULL_SCALE);
    }

    private static short[] constant(double seconds, short amplitude) {
        var samples = new short[(int) (seconds * SAMPLE_RATE)];
        Arrays.fill(samples, amplitude);
        return samples;
    }
}
