package com.sosuisha.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.sosuisha.domain.model.PcmAudio;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.model.SegmentDetectionParameters;

/**
 * Splits PCM audio into sound and silence segments by detecting the silences
 * with the loudness of fixed windows (see ADR 001 and ADR 003). The audio is cut into windows of the
 * window width; the last window may be shorter. A window whose loudness is
 * below the threshold is silent. A run of silent windows that lasts at least
 * the minimum silence is a silence segment; everything else is sound.
 */
public class SegmentDetector {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final SegmentDetectionParameters parameters;

    /**
     * Creates the detector.
     *
     * @param parameters window width, silence threshold, and minimum silence
     * @throws NullPointerException if parameters is null
     */
    public SegmentDetector(SegmentDetectionParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
    }

    /**
     * Detects the segments of the PCM audio, from the beginning to the end.
     * The durations of the segments add up to the length of the audio.
     *
     * @param pcm audio to split
     * @return the segments in time order
     * @throws NullPointerException if pcm is null
     * @throws IllegalArgumentException if pcm has no samples
     */
    public List<Segment> detect(PcmAudio pcm) {
        Objects.requireNonNull(pcm, "pcm must not be null");
        if (pcm.samples().length == 0) {
            throw new IllegalArgumentException("pcm must have samples");
        }
        var minSilenceFrames = framesOf(parameters.minSilenceDuration(), pcm.sampleRate());
        var runs = absorbShortSilence(mergeAdjacent(classifyWindows(pcm)), minSilenceFrames);
        return toSegments(runs, pcm.sampleRate());
    }

    /** Consecutive frames of one kind. */
    private record Run(Segment.Kind kind, long frames) {
    }

    private List<Run> classifyWindows(PcmAudio pcm) {
        var frames = pcm.samples().length / pcm.channels();
        var windowFrames = framesOf(parameters.windowWidth(), pcm.sampleRate());
        var windows = new ArrayList<Run>();
        for (var start = 0L; start < frames; start += windowFrames) {
            var end = Math.min(start + windowFrames, frames);
            windows.add(new Run(kindOfWindow(pcm, start, end), end - start));
        }
        return windows;
    }

    private Segment.Kind kindOfWindow(PcmAudio pcm, long startFrame, long endFrame) {
        var channels = pcm.channels();
        var window = Arrays.copyOfRange(
            pcm.samples(), (int) (startFrame * channels), (int) (endFrame * channels)
        );
        return Loudness.dbfsOf(window) < parameters.silenceThresholdDbfs()
            ? Segment.Kind.SILENCE
            : Segment.Kind.SOUND;
    }

    private static List<Run> mergeAdjacent(List<Run> runs) {
        var merged = new ArrayList<Run>();
        for (var run : runs) {
            if (!merged.isEmpty() && merged.getLast().kind() == run.kind()) {
                var last = merged.removeLast();
                merged.add(new Run(last.kind(), last.frames() + run.frames()));
            } else {
                merged.add(run);
            }
        }
        return merged;
    }

    private static List<Run> absorbShortSilence(List<Run> runs, long minSilenceFrames) {
        var withoutShortSilence = runs.stream()
            .map(
                run -> isShortSilence(run, minSilenceFrames)
                    ? new Run(Segment.Kind.SOUND, run.frames())
                    : run
            )
            .toList();
        return mergeAdjacent(withoutShortSilence);
    }

    private static boolean isShortSilence(Run run, long minSilenceFrames) {
        return run.kind() == Segment.Kind.SILENCE && run.frames() < minSilenceFrames;
    }

    /** Durations are taken from the frame boundaries, so they add up to the total exactly. */
    private static List<Segment> toSegments(List<Run> runs, int sampleRate) {
        var segments = new ArrayList<Segment>();
        var startFrame = 0L;
        for (var run : runs) {
            var endFrame = startFrame + run.frames();
            var start = Duration.ofNanos(nanosAt(startFrame, sampleRate));
            var duration =
                Duration.ofNanos(nanosAt(endFrame, sampleRate) - nanosAt(startFrame, sampleRate));
            segments.add(new Segment(segments.size(), start, duration, run.kind()));
            startFrame = endFrame;
        }
        return List.copyOf(segments);
    }

    private static long nanosAt(long frame, int sampleRate) {
        return frame * NANOS_PER_SECOND / sampleRate;
    }

    private static long framesOf(Duration duration, int sampleRate) {
        return duration.toNanos() * sampleRate / NANOS_PER_SECOND;
    }

    /**
     * Measures the loudness of a window of 16-bit PCM samples (see ADR 003).
     * It is a part of the detector and is exposed to its tests only.
     */
    static final class Loudness {
        private static final double FULL_SCALE = Short.MAX_VALUE;

        private Loudness() {}

        /**
         * Converts the RMS of the samples to dBFS. The samples of all channels
         * are given together in one array.
         *
         * @param samples 16-bit PCM samples of one window, at least one
         * @return the loudness in dBFS, 0 at full scale
         * @throws NullPointerException if samples is null
         * @throws IllegalArgumentException if samples is empty
         */
        static double dbfsOf(short[] samples) {
            Objects.requireNonNull(samples, "samples must not be null");
            if (samples.length == 0) {
                throw new IllegalArgumentException("samples must not be empty");
            }
            var sumOfSquares = 0.0;
            for (var sample : samples) {
                sumOfSquares += (double) sample * sample;
            }
            var rms = Math.sqrt(sumOfSquares / samples.length);
            return 20 * Math.log10(rms / FULL_SCALE);
        }
    }
}
