package com.finsight.user.security;

import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
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
    private final String devAdminEmail;
    private final String devAdminPassword;

    public DevAuthService(
            UserCredentialRepository userCredentialRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.auth.dev-user.email:demo@finsight.local}") String devUserEmail,
            @Value("${security.auth.dev-user.password:Passw0rd!123}") String devUserPassword,
            @Value("${security.auth.dev-admin.email:admin@finsight.local}") String devAdminEmail,
            @Value("${security.auth.dev-admin.password:Adm1nP@ss!}") String devAdminPassword) {
        this.userCredentialRepository = userCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.devUserEmail = devUserEmail;
        this.devUserPassword = devUserPassword;
        this.devAdminEmail = devAdminEmail;
        this.devAdminPassword = devAdminPassword;
    }

    @PostConstruct
    @Transactional
    public void bootstrapDevUser() {
        registerWithRole(devUserEmail, devUserPassword, "USER");
        registerWithRole(devAdminEmail, devAdminPassword, "ADMIN");
    }

    @Transactional(readOnly = true)
    public boolean authenticate(String email, String password) {
        return authenticateUser(email, password) != null;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticateUser(String email, String password) {
        UserCredential user = userCredentialRepository.findByEmailAndDeactivatedAtIsNull(normalizeEmail(email))
                .orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return null;
        }
        return new AuthenticatedUser(user.getEmail(), user.getRole());
    }

    @Transactional
    public boolean register(String email, String password) {
        return registerWithRole(email, password, "USER");
    }

    @Transactional
    public boolean registerWithRole(String email, String password, String role) {
        String normalizedEmail = normalizeEmail(email);
        if (userCredentialRepository.existsByEmail(normalizedEmail)) {
            return false;
        }

        UserCredential userCredential = new UserCredential();
        userCredential.setEmail(normalizedEmail);
        userCredential.setPasswordHash(passwordEncoder.encode(password));
        userCredential.setRole(normalizeRole(role));
        userCredentialRepository.save(userCredential);
        return true;
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserCredential user = userCredentialRepository.findByEmailAndDeactivatedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(user);
    }

    @Transactional
    public void resetPasswordWithoutCurrent(String email, String newPassword) {
        UserCredential user = userCredentialRepository.findByEmailAndDeactivatedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userCredentialRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String getRoleForActiveUser(String email) {
        return userCredentialRepository.findByEmailAndDeactivatedAtIsNull(normalizeEmail(email))
                .map(UserCredential::getRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive or missing"));
    }

    @Transactional
    public void setUserActive(String email, boolean active) {
        UserCredential user = userCredentialRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setDeactivatedAt(active ? null : Instant.now());
        userCredentialRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeRole(String role) {
        return role == null ? "USER" : role.trim().toUpperCase();
    }

    public record AuthenticatedUser(String email, String role) {
    }
}
