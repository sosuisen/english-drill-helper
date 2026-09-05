package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoudnessTest {
    @Test
    @DisplayName("全サンプルがフルスケールのウィンドウは、0 dBFSである")
    void window_of_full_scale_samples_is_zero_dbfs() {
        var samples = new short[882];
        Arrays.fill(samples, Short.MAX_VALUE);

        assertEquals(0.0, SegmentDetector.Loudness.dbfsOf(samples), 1e-9);
    }

    @Test
    @DisplayName("全サンプルがゼロのウィンドウ（デジタル無音）は、負の無限大dBFSである")
    void window_of_zero_samples_is_negative_infinity_dbfs() {
        var samples = new short[882];

        assertEquals(Double.NEGATIVE_INFINITY, SegmentDetector.Loudness.dbfsOf(samples));
    }

    @Test
    @DisplayName("振幅がフルスケールの半分で正負に振れるウィンドウは、約-6.02 dBFSである")
    void window_swinging_at_half_of_full_scale_is_about_minus_six_dbfs() {
        var samples = new short[882];
        for (var i = 0; i < samples.length; i++) {
            samples[i] = (short) (i % 2 == 0 ? 16384 : -16384);
        }

        assertEquals(-6.02, SegmentDetector.Loudness.dbfsOf(samples), 0.01);
    }

    @Test
    @DisplayName("空のウィンドウの音量は求められない")
    void loudness_of_an_empty_window_cannot_be_measured() {
        assertThrows(
            IllegalArgumentException.class, () -> SegmentDetector.Loudness.dbfsOf(new short[0])
        );
    }
}
