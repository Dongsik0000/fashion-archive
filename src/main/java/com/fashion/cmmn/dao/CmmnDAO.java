package com.fashion.cmmn.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class CmmnDAO {

    private static final String NS = "com.fashion.cmmn.dao.CmmnDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    public List<Map<String, Object>> selectCategoryList() {
        return sqlSession.selectList(NS + "selectCategoryList");
    }

    public Map<String, Object> selectUserByLoginId(String loginId) {
        return sqlSession.selectOne(NS + "selectUserByLoginId", loginId);
    }
}
