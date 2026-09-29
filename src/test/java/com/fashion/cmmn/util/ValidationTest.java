package com.fashion.cmmn.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    @Test
    void requireText_trimsAndReturns() {
        assertEquals("아우터", Validation.requireText("  아우터 ", "아이템", 30));
    }

    @Test
    void requireText_rejectsEmptyAndTooLong() {
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("   ", "제목", 100));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText(null, "제목", 100));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("a".repeat(101), "제목", 100));
    }

    @Test
    void optionalText_emptyBecomesNull() {
        assertNull(Validation.optionalText("  ", "메모", 10));
        assertEquals("메모", Validation.optionalText(" 메모 ", "메모", 10));
    }

    @Test
    void requireHttpUrl_acceptsHttpAndHttps() {
        assertEquals("https://musinsa.com/app/goods/1", Validation.requireHttpUrl(" https://musinsa.com/app/goods/1 "));
        assertEquals("http://example.com", Validation.requireHttpUrl("http://example.com"));
    }

    @Test
    void requireHttpUrl_rejectsOtherSchemesAndGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("ftp://example.com/a"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("musinsa.com"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl("http://"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireHttpUrl(""));
    }

    @Test
    void imageExtension_detectsByMagicBytes() {
        assertEquals("jpg", Validation.imageExtension(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals("png", Validation.imageExtension(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        assertEquals("webp", Validation.imageExtension(new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'}));
    }

    @Test
    void imageExtension_rejectsOthers() {
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(new byte[]{'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(new byte[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> Validation.imageExtension(null));
    }

    @Test
    void isLoginId_allowsLowercaseDigitsUnderscoreHyphen() {
        assertTrue(Validation.isLoginId("ds_kim1"));
        assertTrue(Validation.isLoginId("a-b"));
        assertTrue(Validation.isLoginId("a".repeat(30)));
    }

    // 폴더 이름으로 쓰이므로 경로 문자, 대문자, 공백, 한글과 길이 밖의 값은 막는다
    @Test
    void isLoginId_rejectsUnsafeOrOutOfRange() {
        assertFalse(Validation.isLoginId(null));
        assertFalse(Validation.isLoginId("ab"));
        assertFalse(Validation.isLoginId("a".repeat(31)));
        assertFalse(Validation.isLoginId(".."));
        assertFalse(Validation.isLoginId("../etc"));
        assertFalse(Validation.isLoginId("a/b"));
        assertFalse(Validation.isLoginId("a.b"));
        assertFalse(Validation.isLoginId("Admin"));
        assertFalse(Validation.isLoginId("ds kim"));
        assertFalse(Validation.isLoginId("사용자"));
    }

    @Test
    void requireLoginId_trimsAndRejectsInvalid() {
        assertEquals("ds_kim1", Validation.requireLoginId(" ds_kim1 "));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireLoginId("DS_KIM1"));
    }
}
