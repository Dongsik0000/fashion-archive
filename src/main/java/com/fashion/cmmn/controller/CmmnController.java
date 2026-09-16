package com.fashion.cmmn.controller;

import com.fashion.cmmn.util.Constants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class CmmnController {

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute(Constants.SESSION_LOGIN_ID) != null) {
            return "redirect:/";
        }
        return "login/loginMain";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
