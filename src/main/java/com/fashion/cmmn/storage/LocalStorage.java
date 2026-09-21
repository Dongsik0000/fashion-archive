package com.fashion.cmmn.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

// 로컬 디스크 저장. 업로드: {uploadDir}/{file} 쓰기, 공개 URL: {baseUrl}/{file} (nginx가 정적 파일로 서빙)
@Component
public class LocalStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorage.class);

    @Value("${Globals.Fashion.Upload.Dir}")
    private String uploadDir;

    @Value("${Globals.Fashion.Upload.BaseUrl}")
    private String baseUrl;

    // 업로드 후 공개 URL 반환. 실패하면 IOException
    public String upload(byte[] bytes, String ext) throws IOException {
        if (uploadDir == null || uploadDir.isEmpty() || baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("사진 저장소가 설정되지 않았습니다. FASHION_UPLOAD_DIR / FASHION_UPLOAD_BASE_URL을 확인하세요.");
        }
        String name = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), bytes, StandardOpenOption.CREATE_NEW);
        return baseUrl + "/" + name;
    }

    // 실패해도 예외를 던지지 않는다 (고아 파일은 허용, DB 불일치는 불허)
    public void delete(String publicUrl) {
        String marker = (baseUrl == null || baseUrl.isEmpty()) ? null : baseUrl + "/";
        int idx = (publicUrl == null || marker == null) ? -1 : publicUrl.indexOf(marker);
        if (idx < 0) {
            logger.warn("Storage delete skipped, unexpected url: {}", publicUrl);
            return;
        }
        String name = publicUrl.substring(idx + marker.length());
        if (name.contains("/") || name.contains("..")) {
            logger.warn("Storage delete skipped, unsafe file name: {}", name);
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(uploadDir, name));
        } catch (IOException e) {
            logger.warn("Storage delete failed: {}", name, e);
        }
    }
}
