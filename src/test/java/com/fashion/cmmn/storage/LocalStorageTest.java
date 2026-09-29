package com.fashion.cmmn.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageTest {

    @Test
    void storedPath_resolvesUserFolder() {
        assertEquals(Paths.get("/data/fashion-uploads", "ds_kim1", "a.jpg"),
                LocalStorage.storedPath("/data/fashion-uploads", "ds_kim1/a.jpg"));
    }

    // 삭제 대상은 "{아이디}/{파일}" 형식만. 업로드 폴더를 벗어날 수 있는 이름은 null
    @Test
    void storedPath_rejectsUnsafeNames() {
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "a.jpg"));
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "../a.jpg"));
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "ds_kim1/../a.jpg"));
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "ds_kim1/x/a.jpg"));
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "ds_kim1/"));
        assertNull(LocalStorage.storedPath("/data/fashion-uploads", "Admin/a.jpg"));
    }
}
