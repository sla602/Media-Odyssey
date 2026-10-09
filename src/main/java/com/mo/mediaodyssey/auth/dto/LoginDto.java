package com.mo.mediaodyssey.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank @Email String email,
        @NotBlank String password,
        Boolean rememberMe) {

    public boolean rememberMeRequested() {
        return Boolean.TRUE.equals(rememberMe);
    }
}
