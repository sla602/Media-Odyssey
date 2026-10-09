package com.mo.mediaodyssey.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordDto(
        @NotBlank String token,
        @NotBlank String newPassword) {
}
