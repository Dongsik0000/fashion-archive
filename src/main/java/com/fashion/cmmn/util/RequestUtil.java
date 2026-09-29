package com.fashion.cmmn.util;

import javax.servlet.http.HttpServletRequest;

public final class RequestUtil {

    private static final int IP_MAX_LENGTH = 45; // IPv6 문자열 최대 길이

    private RequestUtil() {
    }

    // 클라이언트 IP. 서버에서 Tomcat은 127.0.0.1에만 열려 있고 nginx가 X-Real-IP를 넣는다.
    // 그래서 루프백에서 온 요청일 때만 X-Real-IP를 믿는다.
    public static String clientIp(HttpServletRequest request) {
        return clientIp(request.getRemoteAddr(), request.getHeader("X-Real-IP"));
    }

    static String clientIp(String remoteAddr, String realIpHeader) {
        boolean loopback = "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || "::1".equals(remoteAddr);
        if (loopback && realIpHeader != null) {
            String realIp = realIpHeader.trim();
            if (!realIp.isEmpty() && realIp.length() <= IP_MAX_LENGTH) {
                return realIp;
            }
        }
        return remoteAddr;
    }
}
