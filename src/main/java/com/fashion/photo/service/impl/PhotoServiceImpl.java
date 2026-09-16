package com.fashion.photo.service.impl;

import com.fashion.photo.dao.LinkDAO;
import com.fashion.photo.dao.PhotoDAO;
import com.fashion.photo.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("photoService")
public class PhotoServiceImpl implements PhotoService {

    @Autowired
    private PhotoDAO photoDAO;

    @Autowired
    private LinkDAO linkDAO;

    @Override
    public List<Map<String, Object>> selectPhotoList(String slug) {
        return photoDAO.selectPhotoList(slug);
    }

    @Override
    public Map<String, Object> selectPhoto(long id) {
        return photoDAO.selectPhoto(id);
    }

    @Override
    public List<Integer> selectPhotoCategoryIds(long photoId) {
        return photoDAO.selectPhotoCategoryIds(photoId);
    }

    @Override
    public List<Map<String, Object>> selectLinkList(long photoId) {
        return linkDAO.selectLinkList(photoId);
    }

    @Override
    public void insertPhoto(Map<String, Object> param) {
        photoDAO.insertPhoto(param);
    }

    @Override
    public int updatePhoto(Map<String, Object> param) {
        return photoDAO.updatePhoto(param);
    }

    @Override
    public int deletePhoto(long id) {
        return photoDAO.deletePhoto(id);
    }

    @Override
    public void deletePhotoCategories(long photoId) {
        photoDAO.deletePhotoCategories(photoId);
    }

    @Override
    public void insertPhotoCategories(long photoId, List<Integer> categoryIds) {
        photoDAO.insertPhotoCategories(photoId, categoryIds);
    }

    @Override
    public void insertLink(Map<String, Object> param) {
        linkDAO.insertLink(param);
    }

    @Override
    public int updateLink(Map<String, Object> param) {
        return linkDAO.updateLink(param);
    }

    @Override
    public int deleteLink(long id) {
        return linkDAO.deleteLink(id);
    }
}