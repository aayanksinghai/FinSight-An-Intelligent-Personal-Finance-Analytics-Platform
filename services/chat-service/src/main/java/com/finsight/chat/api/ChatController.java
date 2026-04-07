package com.finsight.chat.api;

import com.finsight.chat.model.*;
import com.finsight.chat.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Send a message and get a data-grounded AI response.
     * The JWT is forwarded to downstream services.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {

        String bearerToken = httpRequest.getHeader("Authorization");
        String ownerEmail = jwt.getSubject();
        ChatResponse response = chatService.handleMessage(ownerEmail, bearerToken, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get full chat history for a specific session.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessageDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String sessionId) {

        return ResponseEntity.ok(chatService.getHistory(jwt.getSubject(), sessionId));
    }

    /**
     * List all sessions for the authenticated user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> getSessions(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getSessions(jwt.getSubject()));
    }

    /**
     * Rate a single AI response as helpful or not helpful.
     */
    @PostMapping("/rate")
    public ResponseEntity<Void> rateMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RatingRequest request) {

        chatService.rateMessage(jwt.getSubject(), request);
        return ResponseEntity.ok().build();
    }
}
