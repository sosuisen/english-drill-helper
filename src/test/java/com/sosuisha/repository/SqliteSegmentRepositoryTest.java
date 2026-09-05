package com.sosuisha.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sosuisha.domain.model.Segment;

class SqliteSegmentRepositoryTest {
    private static final String FINGERPRINT =
        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    private static final List<Segment> SEGMENTS = List.of(
        new Segment(Duration.ofMillis(1000), Segment.Kind.SOUND),
        new Segment(Duration.ofMillis(3000), Segment.Kind.SILENCE),
        new Segment(Duration.ofMillis(2500), Segment.Kind.SOUND)
    );

    @Test
    @DisplayName("指紋をキーにセグメントのリストを保存すると、同じ指紋で同じ順序のリストが読み出せる")
    void segments_saved_by_a_fingerprint_are_found_in_the_same_order_by_the_same_fingerprint(
        @TempDir Path folder) {
        var repository = new SqliteSegmentRepository(folder.resolve("drill.db"));

        repository.saveSegments(FINGERPRINT, SEGMENTS);

        assertEquals(Optional.of(SEGMENTS), repository.findSegments(FINGERPRINT));
    }

    @Test
    @DisplayName("保存していない指紋を探すと、空のOptionalになる。キャッシュがない状態を表す")
    void finding_by_a_fingerprint_without_saved_segments_gives_an_empty_optional(
        @TempDir Path folder) {
        var repository = new SqliteSegmentRepository(folder.resolve("drill.db"));

        assertEquals(Optional.empty(), repository.findSegments(FINGERPRINT));
    }

    @Test
    @DisplayName("同じ指紋で保存し直すと、前のセグメントは消えて新しいリストに置き換わる。古い行は残らない")
    void saving_again_by_the_same_fingerprint_replaces_the_segments(@TempDir Path folder) {
        var repository = new SqliteSegmentRepository(folder.resolve("drill.db"));
        repository.saveSegments(FINGERPRINT, SEGMENTS);
        var shorter = List.of(new Segment(Duration.ofMillis(6500), Segment.Kind.SOUND));

        repository.saveSegments(FINGERPRINT, shorter);

        assertEquals(Optional.of(shorter), repository.findSegments(FINGERPRINT));
    }
}
