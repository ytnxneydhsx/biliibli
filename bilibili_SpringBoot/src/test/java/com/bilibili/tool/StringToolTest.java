package com.bilibili.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringToolTest {

    @Test
    void normalizeOptional_shouldTrimAndConvertBlankToNull() {
        assertEquals("abc", StringTool.normalizeOptional("  abc  "));
        assertNull(StringTool.normalizeOptional("   "));
        assertNull(StringTool.normalizeOptional(null));
    }

    @Test
    void normalizeRequired_shouldThrowWhenBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> StringTool.normalizeRequired("  ", "keyword")
        );
        assertEquals("keyword is required", ex.getMessage());
    }

    @Test
    void isBlank_shouldMatchNormalizedRule() {
        assertTrue(StringTool.isBlank(null));
        assertTrue(StringTool.isBlank(" "));
        assertFalse(StringTool.isBlank("x"));
    }

    @Test
    void trimTrailingSlash_shouldKeepCorePath() {
        assertEquals("http://localhost:8080", StringTool.trimTrailingSlash(" http://localhost:8080/// "));
        assertNull(StringTool.trimTrailingSlash("   "));
    }
}
