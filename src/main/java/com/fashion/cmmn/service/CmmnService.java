package com.fashion.cmmn.service;

import java.util.Map;

public interface CmmnService {

    // Login
    Map<String, Object> selectUserByLoginId(String loginId);

}
