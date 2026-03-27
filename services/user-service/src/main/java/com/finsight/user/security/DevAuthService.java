package com.finsight.user.security;

import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserCredential user = userCredentialRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(user);
    }

    @Transactional
    public void resetPasswordWithoutCurrent(String email, String newPassword) {
        UserCredential user = userCredentialRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
