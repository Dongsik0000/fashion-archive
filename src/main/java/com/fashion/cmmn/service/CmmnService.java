package com.fashion.cmmn.service;

import java.util.Map;

public interface CmmnService {

    // Login
    Map<String, Object> selectUserByLoginId(String loginId);

    // Signup
    void insertUser(Map<String, Object> param);

}
