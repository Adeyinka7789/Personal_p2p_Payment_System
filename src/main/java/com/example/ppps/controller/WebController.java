package com.example.ppps.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // Serves the main landing page when hitting http://localhost:8081/
    @GetMapping("/")
    public String index() {
        // Forward to the static HTML file in resources/static/
        return "forward:/users/index.html";
    }

    // Serves the user login page when hitting http://localhost:8081/login
    @GetMapping("/login")
    public String userLogin() {
        // Forward to static file in resources/static/users/login.html
        return "forward:/users/login.html";
    }

    // Serves the user registration page
    @GetMapping("/register")
    public String userRegister() {
        // Forward to static file in resources/static/users/register.html
        return "forward:/users/register.html";
    }

    // Serves the user dashboard (protected route)
    @GetMapping("/dashboard")
    public String userDashboard() {
        // Forward to static file in resources/static/users/dashboard.html
        return "forward:/users/dashboard.html";
    }

    // Serves the admin login page
    @GetMapping("/admin/login")
    public String adminLogin() {
        // Forward to static file in resources/static/admin/login.html
        return "forward:/admin/login.html";
    }

    // Serves the admin dashboard (protected route)
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        // Forward to static file in resources/static/admin/index.html
        return "forward:/admin/index.html";
    }
}