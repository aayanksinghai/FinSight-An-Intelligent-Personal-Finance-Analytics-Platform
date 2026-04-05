package com.finsight.notification.api;

import com.finsight.notification.domain.Notification;
import com.finsight.notification.persistence.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificationRepository.findByOwnerEmailOrderByCreatedAtDesc(jwt.getSubject()));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getOwnerEmail().equals(jwt.getSubject())) {
                n.markAsRead();
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationRepository.markAllAsReadByOwnerEmail(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
