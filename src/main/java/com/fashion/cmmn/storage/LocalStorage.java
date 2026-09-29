package com.fashion.cmmn.storage;

import com.fashion.cmmn.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

// 로컬 디스크 저장. 업로드: {uploadDir}/{loginId}/{file} 쓰기, 공개 URL: {baseUrl}/{loginId}/{file} (nginx가 정적 파일로 서빙)
@Component
public class LocalStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorage.class);

    @Value("${Globals.Fashion.Upload.Dir}")
    private String uploadDir;

    @Value("${Globals.Fashion.Upload.BaseUrl}")
    private String baseUrl;

    // 사용자 폴더에 업로드 후 공개 URL 반환. 폴더는 첫 업로드 때 만든다. 실패하면 IOException
    public String upload(String loginId, byte[] bytes, String ext) throws IOException {
        if (uploadDir == null || uploadDir.isEmpty() || baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("사진 저장소가 설정되지 않았습니다. FASHION_UPLOAD_DIR / FASHION_UPLOAD_BASE_URL을 확인하세요.");
        }
        // 가입 때 검증하지만, 규칙 이전에 만든 계정도 있으므로 폴더를 만들기 전에 다시 확인한다
        if (!Validation.isLoginId(loginId)) {
            throw new IllegalArgumentException("이 계정의 아이디로는 사진 폴더를 만들 수 없습니다. 관리자에게 문의해주세요.");
        }
        String name = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(uploadDir, loginId);
        Files.createDirectories(dir);
        // Tomcat 기본 umask(0027)로 생기는 0750 폴더·0640 파일은 nginx(www-data)가 읽지 못한다
        Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwxr-xr-x"));
        Path file = dir.resolve(name);
        Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        return baseUrl + "/" + loginId + "/" + name;
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
        Path file = storedPath(uploadDir, name);
        if (file == null) {
            logger.warn("Storage delete skipped, unsafe file name: {}", name);
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.warn("Storage delete failed: {}", name, e);
        }
    }

    // "{loginId}/{file}" → 디스크 경로. 형식이 다르거나 경로를 벗어날 수 있는 이름이면 null
    static Path storedPath(String uploadDir, String name) {
        int slash = name.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String folder = name.substring(0, slash);
        String file = name.substring(slash + 1);
        if (!Validation.isLoginId(folder) || file.isEmpty() || file.contains("/") || file.contains("..")) {
            return null;
        }
        return Paths.get(uploadDir, folder, file);
    }
}
