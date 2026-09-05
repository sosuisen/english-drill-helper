package com.sosuisha.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.sosuisha.domain.exception.AudioDecodeException;
import com.sosuisha.domain.model.PcmAudio;

/**
 * Decodes an audio file to 16-bit PCM with the Java Sound API (see ADR 001).
 * <p>
 * The file is opened through a {@link BufferedInputStream}, not a
 * {@code File}: the mp3 decoder decodes zero bytes, without an error, when it
 * is given a {@code File}. The conversion to PCM passes an explicit
 * {@link AudioFormat} for the same reason.
 */
public class JavaSoundAudioDecoder {
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int BYTES_PER_SAMPLE = SAMPLE_SIZE_IN_BITS / 8;
    private static final boolean BIG_ENDIAN = false;

    /**
     * Decodes the audio file to PCM at the sample rate and the channel count
     * of the file.
     *
     * @param file audio file to decode
     * @return the decoded PCM audio
     * @throws NullPointerException if file is null
     * @throws AudioDecodeException if the file cannot be read or its format is not supported
     */
    public PcmAudio decode(Path file) throws AudioDecodeException {
        Objects.requireNonNull(file, "file must not be null");
        try {
            return decodeOrThrow(file);
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new AudioDecodeException("Could not decode the audio file: " + file, e);
        }
    }

    private static PcmAudio decodeOrThrow(Path file)
        throws IOException, UnsupportedAudioFileException {
        try (
            var in = new BufferedInputStream(Files.newInputStream(file));
            var encoded = AudioSystem.getAudioInputStream(in);
            var pcm = AudioSystem.getAudioInputStream(pcmFormatOf(encoded.getFormat()), encoded)) {
            var format = pcm.getFormat();
            return new PcmAudio(
                (int) format.getSampleRate(), format.getChannels(), toSamples(pcm.readAllBytes())
            );
        }
    }

    private static AudioFormat pcmFormatOf(AudioFormat source) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            source.getSampleRate(),
            SAMPLE_SIZE_IN_BITS,
            source.getChannels(),
            source.getChannels() * BYTES_PER_SAMPLE,
            source.getSampleRate(),
            BIG_ENDIAN
        );
    }

    private static short[] toSamples(byte[] bytes) {
        var samples = new short[bytes.length / BYTES_PER_SAMPLE];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
        return samples;
    }
}
