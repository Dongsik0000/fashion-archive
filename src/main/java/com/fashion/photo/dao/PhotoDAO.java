package com.fashion.photo.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PhotoDAO {

    // photo.xml의 namespace + "." — 뒤에 쿼리 id를 붙여 호출한다
    private static final String NS = "com.fashion.photo.dao.PhotoDAO.";

    @Resource(name = "sqlSession")
    private SqlSessionTemplate sqlSession;

    // 사진 목록
    public List<Map<String, Object>> selectPhotoList(String slug) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("slug", slug);
        return sqlSession.selectList(NS + "selectPhotoList", param);
    }

    // 사진 1장
    public Map<String, Object> selectPhoto(long id) {
        return sqlSession.selectOne(NS + "selectPhoto", id);
    }

    // 사진이 속한 카테고리 id 목록
    public List<Integer> selectPhotoCategoryIds(long photoId) {
        return sqlSession.selectList(NS + "selectPhotoCategoryIds", photoId);
    }

    // 사진 등록. 실행 후 param.get("id")에 생성된 id
    public void insertPhoto(Map<String, Object> param) {
        sqlSession.insert(NS + "insertPhoto", param);
    }

    // 영향 행 수 (0이면 없는 id)
    public int updatePhoto(Map<String, Object> param) {
        return sqlSession.update(NS + "updatePhoto", param);
    }

    public int deletePhoto(long id) {
        return sqlSession.delete(NS + "deletePhoto", id);
    }

    public void deletePhotoCategories(long photoId) {
        sqlSession.delete(NS + "deletePhotoCategories", photoId);
    }

    public void insertPhotoCategories(long photoId, List<Integer> categoryIds) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("photoId", photoId);
        param.put("categoryIds", categoryIds);
        sqlSession.insert(NS + "insertPhotoCategories", param);
    }

}
