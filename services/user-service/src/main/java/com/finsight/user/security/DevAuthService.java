package com.finsight.user.security;

import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevAuthService {

    private final UserCredentialRepository userCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final String devUserEmail;
    private final String devUserPassword;

    public DevAuthService(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.auth.dev-user.email}") String devUserEmail,
            @Value("${security.auth.dev-user.password}") String devUserPassword) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.devUserEmail = devUserEmail;
        this.devUserPassword = devUserPassword;
    }

    @PostConstruct
    @Transactional
    public void bootstrapDevUser() {
        register(devUserEmail, devUserPassword);
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String email, String password) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .map(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .orElse(false);
    }

    @Transactional
    public boolean register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (userCredentialRepository.existsByEmail(normalizedEmail)) {
            return false;
        }

        UserCredential userCredential = new UserCredential();
        userCredential.setEmail(normalizedEmail);
        userCredential.setPasswordHash(passwordEncoder.encode(password));
        userCredentialRepository.save(userCredential);
        return true;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
