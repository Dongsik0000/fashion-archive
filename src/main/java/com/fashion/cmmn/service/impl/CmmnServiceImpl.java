package com.fashion.cmmn.service.impl;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.cmmn.service.CmmnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("cmmnService")
public class CmmnServiceImpl implements CmmnService {

    @Autowired
    private CmmnDAO cmmnDAO;

    @Override
    public Map<String, Object> selectUserByLoginId(String loginId) {
        return cmmnDAO.selectUserByLoginId(loginId);
    }
}
