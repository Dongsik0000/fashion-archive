package com.fashion.cmmn.service;

import java.util.Map;

public interface CmmnService {

    // 성공 시 사용자 정보(id, login_id, role. 조회 결과 Map의 키는 컬럼명 그대로), 실패 시 null
    Map<String, Object> login(String loginId, String rawPassword);
}
