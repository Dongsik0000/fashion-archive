package com.fashion.photo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class LinkDAO {

    private static final String NS = "com.fashion.photo.dao.LinkDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    // 사진의 제품 링크 목록. 등록 순서
    public List<Map<String, Object>> selectLinkList(long photoId) {
        return sqlSession.selectList(NS + "selectLinkList", photoId);
    }

    public void insertLink(Map<String, Object> param) {
        sqlSession.insert(NS + "insertLink", param);
    }

    // 영향 행 수 (0이면 없는 id)
    public int updateLink(Map<String, Object> param) {
        return sqlSession.update(NS + "updateLink", param);
    }

    public int deleteLink(long id) {
        return sqlSession.delete(NS + "deleteLink", id);
    }
}