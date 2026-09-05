package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitTest {
    @Test
    @DisplayName("ファイル名に「Unit 0.」を含むユニットは導入ユニットであり、含まないユニットは導入ユニットではない。Unit 10.x は Unit 0.x ではない")
    void units_whose_file_name_contains_unit_zero_are_introduction_units() {
        assertTrue(unit("001_Unit 0.1.mp3").isIntroduction());
        assertTrue(unit("010_Unit 0.10.mp3").isIntroduction());
        assertFalse(unit("050_Unit 3.3_slow.mp3").isIntroduction());
        assertFalse(unit("100_Unit 10.2.mp3").isIntroduction());
    }

    @Test
    @DisplayName("ユニットの表示名は、ファイル名の先頭の「数字_」と末尾の拡張子を除いた名前である。番号や拡張子がないファイル名はそのまま")
    void the_title_of_a_unit_is_its_file_name_without_the_number_prefix_and_the_extension() {
        assertEquals("Unit 1.1_slow", unit("011_Unit 1.1_slow.mp3").title());
        assertEquals("Unit 0.1", unit("001_Unit 0.1.mp3").title());
        assertEquals("Unit 1.1", unit("Unit 1.1").title());
    }

    private static Unit unit(String fileName) {
        return new Unit(
            new AudioFile(Path.of("units", fileName), "fingerprint-of-" + fileName),
            Optional.empty()
        );
    }
}
