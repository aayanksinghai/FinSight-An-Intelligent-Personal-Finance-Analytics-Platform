package com.finsight.user.admin;

import com.finsight.user.api.admin.AdminUserResponse;
import com.finsight.user.persistence.UserCredential;
import com.finsight.user.persistence.UserCredentialRepository;
import com.finsight.user.security.AuthSessionService;
import com.finsight.user.security.DevAuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserCredentialRepository userCredentialRepository;
    private final DevAuthService devAuthService;
    private final AuthSessionService authSessionService;

    public AdminUserService(
            UserCredentialRepository userCredentialRepository,
            DevAuthService devAuthService,
            AuthSessionService authSessionService) {
        this.userCredentialRepository = userCredentialRepository;
        this.devAuthService = devAuthService;
        this.authSessionService = authSessionService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "email"));
        return switch (status == null ? "all" : status.toLowerCase()) {
            case "active" -> userCredentialRepository.findAllByDeactivatedAtIsNull(pageable).map(this::toResponse);
            case "inactive" -> userCredentialRepository.findAllByDeactivatedAtIsNotNull(pageable).map(this::toResponse);
            default -> userCredentialRepository.findAll(pageable).map(this::toResponse);
        };
    }

    @Transactional
    public void deactivateUser(String email) {
        devAuthService.setUserActive(email, false);
        authSessionService.revokeRefreshSessionsByEmail(email);
    }

    @Transactional
    public void activateUser(String email) {
        devAuthService.setUserActive(email, true);
    }

    private AdminUserResponse toResponse(UserCredential user) {
        return new AdminUserResponse(
                user.getEmail(),
                user.getRole(),
                user.getDeactivatedAt() == null,
                user.getCreatedAt(),
                user.getDeactivatedAt());
    }
}

