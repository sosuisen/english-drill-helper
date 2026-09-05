package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaSoundAudioDecoderTest {
    private static final Path WAV = Path.of("src/test/resources/audio/tone-with-silences.wav");

    @Test
    @DisplayName("WAVファイルをデコードすると、元のサンプルレートとチャンネル数のPCMが得られる")
    void decoding_a_wav_file_gives_pcm_with_the_original_sample_rate_and_channels()
        throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(WAV);

        assertEquals(44100, pcm.sampleRate());
        assertEquals(1, pcm.channels());
    }
}
