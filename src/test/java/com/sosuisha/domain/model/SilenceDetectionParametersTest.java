package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SilenceDetectionParametersTest {
    @Test
    @DisplayName("既定のパラメータは、ウィンドウ幅20ms、しきい値-45 dBFS、最小無音長1秒である（ADR 003）")
    void default_parameters_are_a_20ms_window_a_minus_45_dbfs_threshold_and_a_1s_minimum_silence() {
        var parameters = SilenceDetectionParameters.DEFAULT;

        assertEquals(Duration.ofMillis(20), parameters.windowWidth());
        assertEquals(-45.0, parameters.silenceThresholdDbfs());
        assertEquals(Duration.ofSeconds(1), parameters.minSilenceDuration());
    }
}
