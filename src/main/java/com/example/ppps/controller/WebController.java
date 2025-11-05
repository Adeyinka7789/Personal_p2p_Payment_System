// REVIEW: full code review requested

package com.example.ppps.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/")
    public String index() {
        return "forward:/users/index.html";
    }

    @GetMapping("/login")
    public String userLogin() {
        return "forward:/users/login.html";
    }

    @GetMapping("/register")
    public String userRegister() {
        return "forward:/users/register.html";
    }

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "forward:/users/dashboard.html";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "forward:/admin/login.html";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "forward:/admin/index.html";
    }
}