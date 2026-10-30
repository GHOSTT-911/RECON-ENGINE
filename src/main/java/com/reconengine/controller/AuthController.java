package com.reconengine.controller;

import com.reconengine.dto.AuthResponse;
import com.reconengine.dto.LoginRequest;
import com.reconengine.dto.RegisterRequest;
import com.reconengine.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Send header "Authorization: Bearer <token>". */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return authService.userForToken(bearer(auth))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("name", u.getName(), "email", u.getEmail())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.fail("Not logged in or session expired")));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        authService.logout(bearer(auth));
        return Map.of("success", true);
    }

    private static String bearer(String header) {
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }
}
