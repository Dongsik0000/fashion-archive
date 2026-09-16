package com.fashion.photo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PhotoController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "최근 사진");
        return "/fashion/photo/list";
    }
}
