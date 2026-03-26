package com.finsight.user.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DevAuthService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> usersByEmail = new ConcurrentHashMap<>();

    public DevAuthService(
            PasswordEncoder passwordEncoder,
            @Value("${security.auth.dev-user.email}") String devUserEmail,
            @Value("${security.auth.dev-user.password}") String devUserPassword) {
        this.passwordEncoder = passwordEncoder;
        usersByEmail.put(normalizeEmail(devUserEmail), passwordEncoder.encode(devUserPassword));
    }

    public boolean authenticate(String email, String password) {
        String encodedPassword = usersByEmail.get(normalizeEmail(email));
        return encodedPassword != null && passwordEncoder.matches(password, encodedPassword);
    }

    public boolean register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String encodedPassword = passwordEncoder.encode(password);
        return usersByEmail.putIfAbsent(normalizedEmail, encodedPassword) == null;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
