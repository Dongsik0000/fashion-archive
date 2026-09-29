package com.fashion.cmmn.controller;

import com.fashion.cmmn.service.CmmnService;
import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.LoginAttemptLimiter;
import com.fashion.cmmn.util.RequestUtil;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class CmmnApiController {

    private static final Logger logger = LoggerFactory.getLogger(CmmnApiController.class);

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX_BYTES = 72;   // BCrypt 입력 한도. 넘는 부분은 비교에 쓰이지 않는다

    @Resource(name = "cmmnService")
    private CmmnService cmmnService;

    @Autowired
    private PasswordEncoder passwordEncoder;   // context-datasource.xml의 BCrypt 빈

    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    @Value("${Globals.Fashion.SignupCode}")
    private String signupCode;

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

    // 회원가입. 가입 코드(FASHION_SIGNUP_CODE)를 아는 사람만. 코드를 5번 틀리면 같은 IP는 5분 차단
    @ResponseBody
    @PostMapping("/api/signup")
    public Response signup(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        try {
            // 로그인과 같은 방식(앞뒤 공백 제거)으로 읽어야 가입한 비밀번호로 로그인된다
            String loginId = Validation.requireLoginId(param.get("loginId"));
            String password = Validation.requireText(param.get("password"), "비밀번호", 200);
            if (password.length() < PASSWORD_MIN || password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
                throw new IllegalArgumentException("비밀번호는 8자 이상, 72바이트 이하로 입력해주세요. (영문 72자, 한글 24자까지)");
            }
            if (!password.equals(Validation.requireText(param.get("passwordConfirm"), "비밀번호 확인", 200))) {
                throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
            }

            String attemptKey = "signup|" + RequestUtil.clientIp(request);
            if (!loginAttemptLimiter.tryAcquire(attemptKey)) {
                return Response.of(Constants.FAIL, "가입 코드를 여러 번 잘못 입력했습니다. 5분 뒤에 다시 시도해주세요.", null);
            }
            // 코드 검사를 중복 검사보다 먼저: 코드가 없는 사람이 아이디 존재 여부를 알아낼 수 없게
            if (!isSignupCodeValid(param.get("signupCode"))) {
                return Response.of(Constants.FAIL, "가입 코드가 올바르지 않습니다.", null);
            }
            loginAttemptLimiter.reset(attemptKey);

            Map<String, Object> user = new HashMap<String, Object>();
            user.put("loginId", loginId);
            user.put("passwordHash", passwordEncoder.encode(password));
            try {
                cmmnService.insertUser(user);
            } catch (DuplicateKeyException e) {
                return Response.of(Constants.FAIL, "이미 사용 중인 아이디입니다.", null);
            }
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("회원가입 처리 중 오류", e);
            return Response.of(Constants.FAIL, "가입 처리 중 오류가 발생했습니다.", null);
        }
    }

    // FASHION_SIGNUP_CODE가 비어 있으면 가입 불가. 비교는 상수 시간
    private boolean isSignupCodeValid(Object value) {
        String code = value == null ? "" : value.toString().trim();
        if (signupCode == null || signupCode.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(signupCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
    }
}