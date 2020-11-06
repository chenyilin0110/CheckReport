package com.smb.checkreport.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @RequestMapping("/")
    public String onluUrl(HttpServletRequest request, Model model) {
        return "redirect:/manager";
    }

    // manager web
    @RequestMapping("/manager")
    public String manager(HttpServletRequest request, Model model) {
        logger.info(">>> [" + request.getSession().getId() + "] "  + " - Login successful, start to redirect");
        return "opAdmin";
    }
}