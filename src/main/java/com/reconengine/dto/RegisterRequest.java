package com.reconengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        String name,
        @NotBlank(message = "email is required") @Email(message = "email is not valid") String email,
        @NotBlank(message = "password is required")
        @Size(min = 4, max = 100, message = "password must be at least 4 characters") String password) {}
