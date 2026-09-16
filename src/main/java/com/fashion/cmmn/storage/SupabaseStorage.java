package com.fashion.cmmn.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Supabase Storage REST. 업로드: POST /storage/v1/object/{bucket}/{path}, 공개 URL: /storage/v1/object/public/{bucket}/{path}
@Component
public class SupabaseStorage {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseStorage.class);
    private static final Map<String, String> CONTENT_TYPES = new HashMap<String, String>();

    static {
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("webp", "image/webp");
    }

    @Value("${Globals.Fashion.Supabase.Url}")
    private String baseUrl;

    @Value("${Globals.Fashion.Supabase.ServiceKey}")
    private String serviceKey;

    @Value("${Globals.Fashion.Supabase.Bucket}")
    private String bucket;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // 업로드 후 공개 URL 반환. 실패하면 IOException
    public String upload(byte[] bytes, String ext) throws IOException {
        if (baseUrl == null || baseUrl.isEmpty() || serviceKey == null || serviceKey.isEmpty()) {
            throw new IllegalArgumentException("사진 저장소(Supabase)가 설정되지 않았습니다. FASHION_SUPABASE_URL / FASHION_SUPABASE_SERVICE_KEY를 확인하세요.");
        }
        String path = UUID.randomUUID() + "." + ext;
        HttpRequest request = authorized(objectUrl(path))
                .header("Content-Type", CONTENT_TYPES.get(ext))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        HttpResponse<String> res = send(request);
        if (res.statusCode() / 100 != 2) {
            throw new IOException("Storage upload failed: HTTP " + res.statusCode() + " " + res.body());
        }
        return baseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }

    // 실패해도 예외를 던지지 않는다 (고아 파일은 허용, DB 불일치는 불허)
    public void delete(String publicUrl) {
        String marker = "/object/public/" + bucket + "/";
        int idx = publicUrl == null ? -1 : publicUrl.indexOf(marker);
        if (idx < 0) {
            logger.warn("Storage delete skipped, unexpected url: {}", publicUrl);
            return;
        }
        String path = publicUrl.substring(idx + marker.length());
        try {
            HttpResponse<String> res = send(authorized(objectUrl(path)).DELETE().build());
            if (res.statusCode() / 100 != 2) {
                logger.warn("Storage delete failed: HTTP {} {} ({})", res.statusCode(), res.body(), path);
            }
        } catch (IOException e) {
            logger.warn("Storage delete failed: {}", path, e);
        }
    }

    private String objectUrl(String path) {
        return baseUrl + "/storage/v1/object/" + bucket + "/" + path;
    }

    private HttpRequest.Builder authorized(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Storage request interrupted", e);
        }
    }
}
