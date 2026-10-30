package com.reconengine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(boolean success, String token, Map<String, Object> user, String message) {

    public static AuthResponse ok(String token, String name, String email) {
        return new AuthResponse(true, token, Map.of("name", name, "email", email), null);
    }

    public static AuthResponse fail(String message) {
        return new AuthResponse(false, null, null, message);
    }
}
