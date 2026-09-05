package com.sosuisha.service;

import java.util.List;
import java.util.Objects;

import com.sosuisha.domain.exception.AudioDecodeException;
import com.sosuisha.domain.exception.RepositoryException;
import com.sosuisha.domain.model.AudioFile;
import com.sosuisha.domain.model.Segment;
import com.sosuisha.domain.repository.SegmentRepository;

/**
 * Gives the segments of an audio file, from the cache when they are there,
 * and otherwise by decoding the file, detecting the segments, and saving
 * them to the cache.
 */
public class SegmentLoader {
    private final JavaSoundAudioDecoder decoder;
    private final SegmentDetector detector;
    private final SegmentRepository repository;

    /**
     * Creates the loader.
     *
     * @param decoder decodes the audio file to PCM
     * @param detector splits the PCM into segments
     * @param repository cache of the segments, keyed by fingerprint
     * @throws NullPointerException if any argument is null
     */
    public SegmentLoader(
        JavaSoundAudioDecoder decoder, SegmentDetector detector, SegmentRepository repository) {
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
        this.detector = Objects.requireNonNull(detector, "detector must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    /**
     * Loads the segments of the audio file. The file is read only when its
     * segments are not in the cache yet.
     *
     * @param audioFile audio file whose segments are loaded
     * @return the segments in time order
     * @throws NullPointerException if audioFile is null
     * @throws AudioDecodeException if the file must be read and cannot be decoded
     * @throws RepositoryException if the cache cannot be read or written
     */
    public List<Segment> load(AudioFile audioFile)
        throws AudioDecodeException, RepositoryException {
        Objects.requireNonNull(audioFile, "audioFile must not be null");
        return repository.findSegments(audioFile.fingerprint())
            .orElseGet(() -> detectAndSave(audioFile));
    }

    private List<Segment> detectAndSave(AudioFile audioFile) {
        var segments = detector.detect(decoder.decode(audioFile.path()));
        repository.saveSegments(audioFile.fingerprint(), segments);
        return segments;
    }
}
