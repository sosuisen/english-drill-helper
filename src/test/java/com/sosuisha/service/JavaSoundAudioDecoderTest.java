package com.sosuisha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JavaSoundAudioDecoderTest {
    // テスト用音声の仕様（helpers/generate-test-audio.sh で生成）
    // 440Hzのトーン、振幅はフルスケールの半分（16384）、44.1kHz、モノラル、10.5秒
    // 0.0〜1.0秒 有音
    // 1.0〜4.0秒 無音（3.0秒、シャドーイング用のポーズ）
    // 4.0〜5.0秒 有音
    // 5.0〜5.5秒 無音（0.5秒、文中のポーズ。無視されるべき）
    // 5.5〜6.5秒 有音
    // 6.5〜9.5秒 無音（3.0秒、シャドーイング用のポーズ）
    // 9.5〜10.5秒 有音
    // MP3 は WAV を ffmpeg で 128kbps に変換したもの
    private static final Path WAV = Path.of("src/test/resources/audio/tone-with-silences.wav");
    private static final Path MP3 = Path.of("src/test/resources/audio/tone-with-silences.mp3");
    private static final int SAMPLE_RATE = 44100;
    private static final double DURATION_SECONDS = 10.5;
    private static final int HALF_OF_FULL_SCALE = 16384;

    @Test
    @DisplayName("WAVファイルをデコードすると、元のサンプルレートとチャンネル数のPCMが得られる")
    void decoding_a_wav_file_gives_pcm_with_the_original_sample_rate_and_channels()
        throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(WAV);

        assertEquals(SAMPLE_RATE, pcm.sampleRate());
        assertEquals(1, pcm.channels());
    }

    @Test
    @DisplayName("WAVファイルをデコードすると、サンプル数がファイルの長さに一致する（10.5秒 × 44100）")
    void decoding_a_wav_file_gives_as_many_samples_as_the_length_of_the_file() throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(WAV);

        assertEquals(secondsToIndex(DURATION_SECONDS), pcm.samples().length);
    }

    @Test
    @DisplayName("デコードしたPCMのサンプル値は時間軸と対応する。2.5秒（無音）のサンプルはゼロで、0.4〜0.6秒（有音）の最大振幅はフルスケールの半分である")
    void samples_of_the_decoded_pcm_follow_the_time_axis_silence_is_zero_and_sound_is_at_half_of_full_scale()
        throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(WAV);

        assertEquals(0, pcm.samples()[secondsToIndex(2.5)]);
        assertEquals(
            HALF_OF_FULL_SCALE,
            maxAmplitude(pcm.samples(), secondsToIndex(0.4), secondsToIndex(0.6)), 1
        );
    }

    @Test
    @DisplayName("mp3ファイルをデコードすると、WAVと同じサンプルレートとチャンネル数のPCMが得られる")
    void decoding_an_mp3_file_gives_pcm_with_the_same_sample_rate_and_channels_as_the_wav()
        throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(MP3);

        assertEquals(SAMPLE_RATE, pcm.sampleRate());
        assertEquals(1, pcm.channels());
    }

    @Test
    @DisplayName("mp3ファイルをデコードしたPCMはファイル全体を含む。サンプル数は10.5秒分か、エンコーダ遅延の分（0.1秒未満）だけ多い")
    void decoding_an_mp3_file_gives_the_whole_file_with_at_most_the_encoder_delay_in_extra_samples()
        throws Exception {
        var pcm = new JavaSoundAudioDecoder().decode(MP3);

        var expected = secondsToIndex(DURATION_SECONDS);
        var actual = pcm.samples().length;
        assertTrue(
            actual >= expected && actual < expected + secondsToIndex(0.1),
            "samples: " + actual + ", expected: " + expected
        );
    }

    private static int secondsToIndex(double seconds) {
        return (int) (seconds * SAMPLE_RATE);
    }

    private static int maxAmplitude(short[] samples, int from, int to) {
        var max = 0;
        for (var i = from; i < to; i++) {
            max = Math.max(max, Math.abs(samples[i]));
        }
        return max;
    }
}
