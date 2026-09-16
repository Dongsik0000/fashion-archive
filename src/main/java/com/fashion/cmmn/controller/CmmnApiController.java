package com.fashion.cmmn.controller;

import com.fashion.cmmn.service.CmmnService;
import com.fashion.cmmn.util.Constants;
import com.fashion.cmmn.util.Response;
import com.fashion.cmmn.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @ResponseBody
    @PostMapping("/api/login")
    public Response login(@RequestBody Map<String, Object> param, HttpSession session) {
        try {
            String loginId = Validation.requireText(param.get("loginId"), "아이디", 50);
            String password = Validation.requireText(param.get("password"), "비밀번호", 200);

            Map<String, Object> user = cmmnService.login(loginId, password);
            if (user == null) {
                return Response.of(Constants.LOGIN_FAIL, "아이디 또는 비밀번호를 확인해주세요.", null);
            }
            session.setAttribute(Constants.SESSION_LOGIN_ID, user.get("login_id"));
            session.setAttribute(Constants.SESSION_USER_ID, ((Number) user.get("id")).longValue());
            return Response.of(Constants.SUCCESS);
        } catch (IllegalArgumentException e) {
            return Response.of(Constants.FAIL, e.getMessage(), null);
        } catch (Exception e) {
            logger.error("로그인 처리 중 오류", e);
            return Response.of(Constants.FAIL, "로그인 처리 중 오류가 발생했습니다.", null);
        }
    }
}
