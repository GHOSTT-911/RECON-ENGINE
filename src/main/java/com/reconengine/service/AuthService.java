package com.reconengine.service;

import com.reconengine.dto.AuthResponse;
import com.reconengine.dto.LoginRequest;
import com.reconengine.dto.RegisterRequest;
import com.reconengine.model.AppUser;
import com.reconengine.repository.AppUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple user accounts: passwords are stored as BCrypt hashes, and a random
 * session token is returned on login. Tokens live in memory (reset on restart).
 */
@Service
public class AuthService {

    public static final String DEMO_EMAIL = "admin@reconengine.com";
    public static final String DEMO_PASSWORD = "admin123";

    private final AppUserRepository users;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, String> tokens = new ConcurrentHashMap<>(); // token -> email

    public AuthService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public void ensureDemoUser() {
        if (!users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            users.save(new AppUser("GHOST", DEMO_EMAIL, encoder.encode(DEMO_PASSWORD)));
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalise(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            return AuthResponse.fail("An account with this email already exists. Please log in.");
        }
        String name = (req.name() == null || req.name().isBlank()) ? email.split("@")[0] : req.name().trim();
        AppUser user = users.save(new AppUser(name, email, encoder.encode(req.password())));
        return AuthResponse.ok(issueToken(user.getEmail()), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Optional<AppUser> user = users.findByEmailIgnoreCase(normalise(req.email()));
        if (user.isEmpty() || !encoder.matches(req.password(), user.get().getPasswordHash())) {
            return AuthResponse.fail("Invalid email or password");
        }
        return AuthResponse.ok(issueToken(user.get().getEmail()), user.get().getName(), user.get().getEmail());
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> userForToken(String token) {
        if (token == null) return Optional.empty();
        String email = tokens.get(token);
        return email == null ? Optional.empty() : users.findByEmailIgnoreCase(email);
    }

    public void logout(String token) {
        if (token != null) tokens.remove(token);
    }

    private String issueToken(String email) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, email);
        return token;
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
