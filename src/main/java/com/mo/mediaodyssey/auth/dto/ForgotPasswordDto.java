package com.mo.mediaodyssey.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordDto(
        @NotBlank @Email String email) {
}
