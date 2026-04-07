package com.mo.mediaodyssey.dev.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import com.mo.mediaodyssey.auth.dto.UserDto;
import com.mo.mediaodyssey.dev.dto.DevAccountApiResponse;
import com.mo.mediaodyssey.dev.services.DevUserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account creation logic for developmental use only.
 */
@RestController
@RequestMapping("/api/dev/auth")
public class DevAccountController {

    @Value("${dev.mode.enabled:FALSE}")
    private String devMode;

    @Value("${dev.email.suffix:}")
    private String email;

    @Autowired
    private DevUserService devUserService;

    /**
     * Logic to create an Admin account and skip email verification.
     * 
     * For developmental use only. Must be enabled via environment variable. Once
     * enabled, access is unrestricted.
     * 
     * Email address and password is randomly assigned. $EMAIL_FROM is combined with
     * the randomly generated UUID. Password uses a different UUID.
     * 
     * @return "OK", Email, and Password in body upon success
     */
    @GetMapping("/createAdminAccount")
    @Transactional
    public ResponseEntity<DevAccountApiResponse> createAdminAccount() {
        return createDevAccount("ROLE_ADMIN", "DEV_AUTH_ADMIN_CREATED", "Admin account created successfully.");
    }

    /**
     * Logic to create an User account and skip email verification.
     * 
     * For developmental use only. Must be enabled via environment variable. Once
     * enabled, access is unrestricted.
     * 
     * Email address and password is randomly assigned. $EMAIL_FROM is combined with
     * the randomly generated UUID. Password uses a different UUID.
     * 
     * @return "OK", Email, and Password in body upon success
     */
    @GetMapping("/createUserAccount")
    @Transactional
    public ResponseEntity<DevAccountApiResponse> createUserAccount() {
        return createDevAccount("ROLE_USER", "DEV_AUTH_USER_CREATED", "User account created successfully.");
    }

    private ResponseEntity<DevAccountApiResponse> createDevAccount(
            String role,
            String successStatus,
            String successMessage) {
        if (!isDevModeEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(DevAccountApiResponse.error("DEV_AUTH_ACCOUNT_CREATION_DISABLED",
                            "Developmental account creation API endpoint is disabled."));
        } else {

            String email = UUID.randomUUID().toString() + this.email;
            String password = UUID.randomUUID().toString();

            UserDto dto = new UserDto(email, password);
            if ("ROLE_ADMIN".equals(role)) {
                devUserService.createAdminAccount(dto);
            } else {
                devUserService.createUserAccount(dto);
            }

            return ResponseEntity.ok(DevAccountApiResponse.success(successStatus, successMessage, email, password));
        }
    }

    private boolean isDevModeEnabled() {
        return "TRUE".equalsIgnoreCase(devMode);
    }
}
