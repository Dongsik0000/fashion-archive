package com.fashion.cmmn.service;

import java.util.Map;

public interface CmmnService {

    // 성공 시 사용자 정보(id, loginId, role), 실패 시 null
    Map<String, Object> login(String loginId, String rawPassword);
}
