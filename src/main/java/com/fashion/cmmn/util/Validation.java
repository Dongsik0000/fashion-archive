package com.fashion.cmmn.util;

import java.net.URI;
import java.net.URISyntaxException;

// 입력 검증. 실패 시 사용자에게 그대로 보여줄 메시지를 담은 IllegalArgumentException을 던진다
public final class Validation {

    private Validation() {
    }

    public static String requireText(Object value, String field, int max) {
        String s = value == null ? "" : value.toString().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(field + "을(를) 입력해주세요.");
        }
        if (s.length() > max) {
            throw new IllegalArgumentException(field + "은(는) " + max + "자 이하로 입력해주세요.");
        }
        return s;
    }

    public static String optionalText(Object value, String field, int max) {
        String s = value == null ? "" : value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        return requireText(s, field, max);
    }

    public static String requireHttpUrl(Object value) {
        String s = requireText(value, "URL", 2000);
        try {
            URI uri = new URI(s);
            String scheme = uri.getScheme();
            boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!http || uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new IllegalArgumentException("http:// 또는 https://로 시작하는 URL을 입력해주세요.");
            }
            return s;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("올바른 URL이 아닙니다.");
        }
    }

    // 확장자가 아니라 파일 앞 12바이트로 형식을 판별한다
    public static String imageExtension(byte[] head) {
        if (head != null && head.length >= 12) {
            if ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if ((head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
                return "png";
            }
            if (head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                return "webp";
            }
        }
        throw new IllegalArgumentException("jpg, png, webp 이미지만 등록할 수 있습니다.");
    }
}
