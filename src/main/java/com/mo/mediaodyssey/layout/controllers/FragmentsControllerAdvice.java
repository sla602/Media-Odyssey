package com.mo.mediaodyssey.layout.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;

@ControllerAdvice
public class FragmentsControllerAdvice {

    @Autowired
    private CurrentAccountService currentAccountService;

    /*
     * This Controller works like a global controller
     ** Purpose: Get the current logged in user's details (username, avatar_path,
     * ...)
     * and send the object user to all thymeleaf pages, so the header/sidebar (which
     * exists in all pages)
     * can get the object user instead of calling for user in every controllers.
     */

    @ModelAttribute("user")
    public User getCurrentUser(Authentication authentication) {
        return currentAccountService.getCurrentAccount(authentication);
    }
}
