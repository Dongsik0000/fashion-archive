package com.fashion.cmmn.controller;

import com.fashion.cmmn.service.CmmnService;
import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class CmmnApiController {

    private static final Logger logger = LoggerFactory.getLogger(CmmnApiController.class);

    @Resource(name = "cmmnService")
    private CmmnService cmmnService;

    @Autowired
    private PasswordEncoder passwordEncoder;   // context-datasource.xml의 BCrypt 빈

    // 로그인
    @ResponseBody
    @PostMapping("/api/login")
    public Response login(@RequestBody Map<String, Object> param, HttpSession session) {
        String code;
        String message = null;

        try {
            // 1) 파라미터. JS가 보낸 JSON 키 = JSP input의 name
            String loginId = Validation.requireText(param.get("loginId"), "아이디", 50);
            String password = Validation.requireText(param.get("password"), "비밀번호", 200);

            // 2) 조회
            Map<String, Object> user = cmmnService.selectUserByLoginId(loginId);

            // 3) 비교. 계정이 없을 때와 비밀번호가 틀렸을 때 메시지를 같게 해서 계정 존재 여부를 노출하지 않는다
            if (user == null || !passwordEncoder.matches(password, (String) user.get("password_hash"))) {
                code = Constants.LOGIN_FAIL;
                message = "아이디 또는 비밀번호를 확인해주세요.";
            } else {
                // 4) 세션 저장. 인터셉터와 레이아웃 JSP가 이 키를 본다
                session.setAttribute(Constants.SESSION_LOGIN_ID, user.get("login_id"));
                session.setAttribute(Constants.SESSION_USER_ID, ((Number) user.get("id")).longValue());
                code = Constants.SUCCESS;
            }
        } catch (IllegalArgumentException e) {
            code = Constants.FAIL;
            message = e.getMessage();              // Validation이 만든 사용자용 문구
        } catch (Exception e) {
            logger.error("로그인 처리 중 오류", e);
            code = Constants.FAIL;
            message = "로그인 처리 중 오류가 발생했습니다.";
        }

        return Response.of(code, message, null);
    }
}