package com.reconengine.dto;

import jakarta.validation.constraints.NotBlank;

public record OverrideRequest(@NotBlank(message = "id is required") String id, String action) {}
