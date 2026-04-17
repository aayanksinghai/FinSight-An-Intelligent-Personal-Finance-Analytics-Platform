package com.finsight.notification.api;

import com.finsight.notification.domain.Notification;
import com.finsight.notification.domain.NotificationPreference;
import com.finsight.notification.domain.NotificationPreferenceId;
import com.finsight.notification.persistence.NotificationPreferenceRepository;
import com.finsight.notification.persistence.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    // All supported notification types
    private static final Set<String> ALL_TYPES = Set.of(
            "BUDGET_WARNING", "BUDGET_EXCEEDED", "ANOMALY_DETECTED",
            "STRESS_SCORE_CHANGE", "FORECAST_UPDATE", "ANNOUNCEMENT"
    );

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationController(NotificationRepository notificationRepository,
                                   NotificationPreferenceRepository preferenceRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ── Inbox ───────────────────────────────────────────────────────────────

    @GetMapping
    public List<Notification> getAll(@AuthenticationPrincipal Jwt jwt) {
        return notificationRepository.findByOwnerEmailInOrderByCreatedAtDesc(List.of(jwt.getSubject(), "ALL"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        long count = notificationRepository.countByOwnerEmailInAndIsRead(List.of(jwt.getSubject(), "ALL"), false);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getOwnerEmail().equals(jwt.getSubject()) || n.getOwnerEmail().equals("ALL")) {
                n.markAsRead();
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationRepository.markAllAsReadForUser(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getOwnerEmail().equals(jwt.getSubject())) {
                notificationRepository.delete(n);
            }
        });
        return ResponseEntity.noContent().build();
    }

    // ── Preferences ─────────────────────────────────────────────────────────

    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Boolean>> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        List<NotificationPreference> saved = preferenceRepository.findByOwnerEmail(email);
        Map<String, Boolean> savedMap = saved.stream()
                .collect(Collectors.toMap(NotificationPreference::getType, NotificationPreference::isEnabled));

        // Return all types, defaulting to true if not explicitly set
        Map<String, Boolean> result = ALL_TYPES.stream()
                .collect(Collectors.toMap(t -> t, t -> savedMap.getOrDefault(t, true)));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<Void> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Boolean> updates) {
        String email = jwt.getSubject();
        updates.forEach((type, enabled) -> {
            if (!ALL_TYPES.contains(type)) return;
            NotificationPreference pref = preferenceRepository
                    .findById(new NotificationPreferenceId(email, type))
                    .orElse(new NotificationPreference(email, type, true));
            pref.setEnabled(enabled);
            preferenceRepository.save(pref);
        });
        return ResponseEntity.noContent().build();
    }

    // ── Admin Broadcast ──────────────────────────────────────────────────────

    @PostMapping("/admin/broadcast")
    @Transactional
    public ResponseEntity<Map<String, Object>> broadcast(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {

        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        String title = body.getOrDefault("title", "Announcement");
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }

        // Persist for every user who has at least one notification (known users) + push via topic broadcast
        // For simplicity, we store one shared notification and broadcast to all via /topic/announcements
        Notification announcement = new Notification("ALL", "ANNOUNCEMENT", title, message);
        notificationRepository.save(announcement);

        // Broadcast to all subscribed clients
        messagingTemplate.convertAndSend("/topic/announcements",
                Map.of("id", announcement.getId(), "title", title, "message", message,
                        "type", "ANNOUNCEMENT", "createdAt", announcement.getCreatedAt()));

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "notificationId", announcement.getId()
        ));
    }
}
