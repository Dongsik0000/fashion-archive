package com.fashion.cmmn.controller;

import com.fashion.cmmn.dao.CmmnDAO;
import com.fashion.photo.controller.PhotoController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

// 화면 컨트롤러에만 적용해 API 호출마다 카테고리를 조회하지 않게 한다
@ControllerAdvice(assignableTypes = PhotoController.class)
public class GlobalModelAdvice {

    @Autowired
    private CmmnDAO cmmnDAO;

    @ModelAttribute("categories")
    public List<Map<String, Object>> categories() {
        return cmmnDAO.selectCategoryList();
    }
}
