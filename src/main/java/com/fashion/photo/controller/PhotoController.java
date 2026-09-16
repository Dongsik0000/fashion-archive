package com.fashion.photo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PhotoController {

    @GetMapping("/")
    public String home() {
        return "/fashion/photo/list";
    }

    @GetMapping("/c/{slug}")
    public String category(@PathVariable String slug, Model model) {
        model.addAttribute("currentSlug", slug);
        return "/fashion/photo/list";
    }

    @GetMapping("/photos/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("photoId", id);
        return "/fashion/photo/detail";
    }

    // 아래 두 화면은 LoginInterceptor(/admin/**)가 로그인 여부를 확인한다

    @GetMapping("/admin/photos/new")
    public String photoNew() {
        return "/fashion/admin/photoForm";
    }

    @GetMapping("/admin/photos/{id}/edit")
    public String photoEdit(@PathVariable long id, Model model) {
        model.addAttribute("photoId", id);
        return "/fashion/admin/photoForm";
    }
}