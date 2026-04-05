package com.hotel.websocket;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(String username, String type, String message, Object data) {
        Notification notification = build(type, message, data);
        // Send to user-specific topic using user ID concept
        messagingTemplate.convertAndSend("/topic/user." + username, notification);
        log.debug("Notification sent to /topic/user.{}: {}", username, type);
    }

    public void sendToAdmins(String type, String message, Object data) {
        Notification notification = build(type, message, data);
        messagingTemplate.convertAndSend("/topic/admins", notification);
        log.debug("Admin notification sent: {}", type);
    }

    public void broadcastRoomUpdate(Object roomData) {
        messagingTemplate.convertAndSend("/topic/rooms", roomData);
    }

    public void broadcastBookingUpdate(Object bookingData) {
        messagingTemplate.convertAndSend("/topic/bookings", bookingData);
        log.debug("Booking update broadcast");
    }

    private Notification build(String type, String message, Object data) {
        return Notification.builder()
                .type(type)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Notification {
        private String type;
        private String message;
        private Object data;
        private LocalDateTime timestamp;
    }
}
