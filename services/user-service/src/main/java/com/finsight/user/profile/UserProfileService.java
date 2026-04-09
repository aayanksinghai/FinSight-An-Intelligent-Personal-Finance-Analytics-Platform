package com.finsight.user.profile;

import com.finsight.user.api.profile.UpdateUserProfileRequest;
import com.finsight.user.api.profile.UserProfileResponse;
import com.finsight.user.api.profile.UserSecurityResponse;
import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserCredentialRepository userCredentialRepository;

    public UserProfileService(UserCredentialRepository userCredentialRepository) {
        this.userCredentialRepository = userCredentialRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        UserCredential user = requireUser(email);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserSecurityResponse getSecurityOverview(String email) {
        UserCredential user = requireUser(email);
        boolean profileConfigured = user.getFullName() != null
                || user.getCity() != null
                || user.getAgeGroup() != null
                || user.getMonthlyIncome() != null;
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isEmpty();
        return new UserSecurityResponse(
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                profileConfigured,
                hasPassword);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateUserProfileRequest request) {
        UserCredential user = requireUser(email);
        user.setFullName(trimToNull(request.fullName()));
        user.setCity(trimToNull(request.city()));
        user.setAgeGroup(trimToNull(request.ageGroup()));
        user.setMonthlyIncome(request.monthlyIncome());
        return toResponse(userCredentialRepository.save(user));
    }

    @Transactional
    public void deleteAccount(String email) {
        long deleted = userCredentialRepository.deleteByEmail(normalizeEmail(email));
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private UserCredential requireUser(String email) {
        return userCredentialRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileResponse toResponse(UserCredential user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getFullName(),
                user.getCity(),
                user.getAgeGroup(),
                user.getMonthlyIncome());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
