package com.mo.mediaodyssey.layout.controllers;

import com.mo.mediaodyssey.auth.repository.UserRepository;
import com.mo.mediaodyssey.shared.model.User;
import com.mo.mediaodyssey.shared.services.CurrentAccountService;
import com.mo.mediaodyssey.shared.services.ObjectStorageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AvatarController {

    /*
     * This controller is for handling avatar related requests
     * Take Request of avatar type change from front end and update in the database.
     * Store it so the application knows which avatar is chosen to use
     * Notice: the avatar type is either "custom" or "default.
     */

    @Value("${storage.public-url}")
    private String storagePublicURL;

    private final UserRepository userRepository;

    private final ObjectStorageService objectStorageService;

    private final CurrentAccountService currentAccountService;

    AvatarController(UserRepository userRepository, ObjectStorageService objectStorageService,
            CurrentAccountService currentAccountService) {
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
        this.currentAccountService = currentAccountService;
    }

    @PostMapping("/user/profile/avatar/selectedType")
    public ResponseEntity<?> selectedAvatarType(@RequestBody Map<String, String> body,
            Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        String selectedAvatarType = body.get("selectedAvatarType");

        User user = currentAccountService.getCurrentAccount(authentication);

        // set the variable in the database and save it.
        user.setSelected_avatar_type(selectedAvatarType);
        userRepository.save(user);
        currentAccountService.refreshPrincipal(authentication, request, response);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/profile/avatar/upload")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
            Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String fileKey = objectStorageService.uploadFile(file);
            String customAvatarURL = storagePublicURL + "/" + fileKey;

            // When user upload a file, assume they will use custom avatar immediately.

            User user = currentAccountService.getCurrentAccount(authentication);

            user.setCustom_avatar_URL(customAvatarURL);
            user.setSelected_avatar_type("custom");

            userRepository.save(user);
            currentAccountService.refreshPrincipal(authentication, request, response);

            return ResponseEntity.ok(Map.of("customAvatarURL", customAvatarURL));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to upload avatar");
        }
    }
}
