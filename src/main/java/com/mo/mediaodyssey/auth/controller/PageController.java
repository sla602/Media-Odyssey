package com.mo.mediaodyssey.auth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.shared.model.User;

@Controller
public class PageController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Splash page for authentication. Users can choose to login or signup.
     * 
     * @return Static page at src/main/resources/static/auth/index.html
     */
    @GetMapping("/auth")
    public String authPage() {
        return "forward:/auth/index.html";
    }

    /**
     * Log in page for authentication.
     * 
     * @return Static page at src/main/resources/static/auth/login/index.html
     */
    @GetMapping("/auth/login")
    public String loginPage() {
        return "forward:/auth/login/index.html";
    }

    /**
     * Sign up page for authentication.
     * 
     * @return Static page at src/main/resources/static/auth/signup/index.html
     */
    @GetMapping("/auth/signup")
    public String registerPage() {
        return "forward:/auth/signup/index.html";
    }

    /**
     * Log out page for authentication.
     *
     * Log out is primarily handled using logic built into Spring Security. See
     * src/main/java/com/mo/mediaodyssey/auth/config/SecurityConfig.java.
     * Handled at /api/auth/logout.
     * 
     * For consistency of all authentication at /auth,
     * we will redirect /auth/logout to /api/auth/logout. GET requests to
     * /api/auth/logout are redirected afterwards to /auth.
     * 
     * @return Redirect to /api/auth/logout
     */
    @GetMapping("/auth/logout")
    public String logoutPage() {
        return "redirect:/api/auth/logout";
    }

    /**
     * Resend email verification token page for authentication.
     * 
     * @return Static page at src/main/resources/static/auth/resend/index.html
     */
    @GetMapping("/auth/resend")
    public String resendPage() {
        return "forward:/auth/resend/index.html";
    }

    /**
     * Display a list of all Users
     * 
     * @return Thymeleaf template at src/main/resources/templates/usersAD.html
     */
    @GetMapping("/admin/users")
    public String adminListUsersPage(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "boardsLayout/admin/user";
    }
}
