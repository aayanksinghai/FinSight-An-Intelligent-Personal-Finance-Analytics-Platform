package com.finsight.user.api.profile;

import com.finsight.user.profile.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userProfileService.getProfile(jwt.getSubject()));
    }

    @GetMapping("/security")
    public ResponseEntity<UserSecurityResponse> getSecurityOverview(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userProfileService.getSecurityOverview(jwt.getSubject()));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(jwt.getSubject(), request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        userProfileService.deleteAccount(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
