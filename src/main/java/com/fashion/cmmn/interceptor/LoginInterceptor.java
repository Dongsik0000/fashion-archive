package com.fashion.cmmn.interceptor;

import com.fashion.cmmn.util.Constants;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// /admin/**, /api/admin/** 에 매핑. 로그인 여부와 Ajax 여부를 판별한다.
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");

        // 교차 출처 폼 전송은 이 헤더를 붙일 수 없다 → CSRF 방어
        if (api && !ajax) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(Constants.SESSION_LOGIN_ID) != null) {
            return true;
        }

        if (ajax) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"sessionExpired\":true}");
        } else {
            // 로그인 후 원래 가려던 화면으로 돌아오도록 경로를 넘긴다 (컨텍스트 경로 제외)
            String next = request.getRequestURI().substring(request.getContextPath().length());
            response.sendRedirect(request.getContextPath() + "/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
        }
        return false;
    }
}
