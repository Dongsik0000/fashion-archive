package com.fashion.cmmn.service.impl;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.cmmn.service.CmmnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service("cmmnService")
public class CmmnServiceImpl implements CmmnService {

    private static final Logger logger = LoggerFactory.getLogger(CmmnServiceImpl.class);

    @Autowired
    private CmmnDAO cmmnDAO;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${Globals.Fashion.Admin.LoginId}")
    private String adminLoginId;

    @Value("${Globals.Fashion.Admin.PasswordHash}")
    private String adminPasswordHash;

    // 기동 시 users가 비어 있으면 환경변수의 관리자 1명을 넣는다
    @PostConstruct
    public void ensureAdmin() {
        if (cmmnDAO.countUsers() > 0) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("loginId", adminLoginId);
        param.put("passwordHash", adminPasswordHash);
        cmmnDAO.insertUser(param);
        logger.info("초기 관리자 계정 생성: {}", adminLoginId);
    }

    @Override
    public Map<String, Object> login(String loginId, String rawPassword) {
        Map<String, Object> user = cmmnDAO.selectUserByLoginId(loginId);
        if (user == null || !passwordEncoder.matches(rawPassword, (String) user.get("password_hash"))) {
            return null;
        }
        user.remove("password_hash");
        return user;
    }
}
