package com.mo.mediaodyssey.auth.controller;

import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mo.mediaodyssey.auth.config.AuthRoutes;
import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.shared.model.User;

/**
 * Controller for handling authentication-related pages.
 * 
 * This controller provides endpoints for rendering authentication-related pages
 * such as login, signup, email verification, password reset, and admin user
 * management. Based on the user's authentication status, it either redirects to
 * the home page or serves the appropriate page.
 */
@Controller
public class PageController {

    @Autowired
    private CurrentAccountService currentAccountService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Splash page for authentication. Users can choose to login or signup.
     *
     * @return Redirect to / for authenticated users. Otherwise, static page at
     *         src/main/resources/static/auth/index.html
     */
    @GetMapping(AuthRoutes.Page.AUTH_PAGE_ROOT)
    public String authPage(Authentication authentication) {
        if (currentAccountService.isAuthenticated(authentication)) {
            return "redirect:/";
        } else {
            return "forward:/auth/index.html";
        }
    }

    /**
     * Log in page for authentication.
     *
     * @return Redirect to / for authenticated users. Otherwise, static page at
     *         src/main/resources/static/auth/login/index.html
     */
    @GetMapping(AuthRoutes.Page.LOGIN)
    public String loginPage(Authentication authentication) {
        if (currentAccountService.isAuthenticated(authentication)) {
            return "redirect:/";
        } else {
            return "forward:/auth/login/index.html";
        }
    }

    /**
     * Sign up page for authentication.
     *
     * @return Redirect to / for authenticated users. Otherwise, static page at
     *         src/main/resources/static/auth/signup/index.html
     */
    @GetMapping(AuthRoutes.Page.SIGNUP)
    public String registerPage(Authentication authentication) {
        if (currentAccountService.isAuthenticated(authentication)) {
            return "redirect:/";
        } else {
            return "forward:/auth/signup/index.html";
        }
    }

    /**
     * Log out page for authentication.
     *
     * Log out is primarily handled using logic built into Spring Security. See
     * src/main/java/com/mo/mediaodyssey/auth/config/SecurityConfig.java.
     *
     * For consistency of all authentication at AuthRoutes.Page.AUTH_PAGE_ROOT, we
     * will redirect AuthRoutes.Page.LOGOUT to AuthRoutes.Api.LOGOUT. GET requests
     * to AuthRoutes.Api.LOGOUT are redirected afterwards to
     * AuthRoutes.Page.AUTH_PAGE_ROOT.
     *
     * @return Redirect to the logout API endpoint
     */
    @GetMapping(AuthRoutes.Page.LOGOUT)
    public String logoutPage() {
        return "redirect:" + AuthRoutes.Api.LOGOUT;
    }

    /**
     * Complete email verification token page for authentication.
     *
     * @return Static page at src/main/resources/static/auth/verify/index.html
     */
    @GetMapping(AuthRoutes.Page.VERIFY)
    public String verifyPage() {
        return "forward:/auth/verify/index.html";
    }

    /**
     * Resend email verification token page for authentication.
     *
     * @return Static page at src/main/resources/static/auth/resend/index.html
     */
    @GetMapping(AuthRoutes.Page.RESEND)
    public String resendPage() {
        return "forward:/auth/resend/index.html";
    }

    /**
     * Forgot password page for authentication.
     *
     * @return Redirect to / for authenticated users. Otherwise, static page at
     *         src/main/resources/static/auth/forgot/index.html
     */
    @GetMapping(AuthRoutes.Page.FORGOT)
    public String forgotPasswordPage(Authentication authentication) {
        if (currentAccountService.isAuthenticated(authentication)) {
            return "redirect:/";
        } else {
            return "forward:/auth/forgot/index.html";
        }
    }

    /**
     * Password reset completion page for authentication.
     *
     * @return Static page at src/main/resources/static/auth/reset/index.html
     */
    @GetMapping(AuthRoutes.Page.RESET)
    public String resetPasswordPage() {
        return "forward:/auth/reset/index.html";
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
