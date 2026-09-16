package com.fashion.photo.service;

import java.util.List;
import java.util.Map;

public interface PhotoService {

    List<Map<String, Object>> selectPhotoList(String slug);

    Map<String, Object> selectPhoto(long id);

    List<Integer> selectPhotoCategoryIds(long photoId);

    List<Map<String, Object>> selectLinkList(long photoId);

    void insertPhoto(Map<String, Object> param);

    int updatePhoto(Map<String, Object> param);

    int deletePhoto(long id);

    void deletePhotoCategories(long photoId);

    void insertPhotoCategories(long photoId, List<Integer> categoryIds);

    void insertLink(Map<String, Object> param);

    int updateLink(Map<String, Object> param);

    int deleteLink(long id);
}