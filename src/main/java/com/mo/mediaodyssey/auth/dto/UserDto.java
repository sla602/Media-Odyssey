package com.mo.mediaodyssey.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserDto(
                @NotBlank @Email String email,
                @NotBlank String password) {
}
