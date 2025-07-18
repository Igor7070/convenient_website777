package com.example.unl_pos12.service;

import com.example.unl_pos12.model.messenger.User;
import com.example.unl_pos12.model.messenger.UserStatusMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {
    @Autowired
    private UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotification(Long userId, Long recipientId, String content, Long chatId) {
        try {
            User user = userService.getUserById(userId);
            String userName = user.getUsername();
            String destination = "/topic/notifications/" + recipientId;
            // Используем заглушку только если content null или пустой
            String notificationContent = (content == null || content.isEmpty()) ? "New audio message" : content;
            String notificationMessage = String.format("New message: %s from:" +
                    " %s (chatId: %d)", notificationContent, userName, chatId);
            messagingTemplate.convertAndSend(destination, notificationMessage);
            System.out.println("Notification sent to user ID: " + recipientId + ", message: " + notificationMessage);
        } catch (Exception e) {
            System.out.println("Error in method sendNotification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendUserStatusUpdate(Long userId, boolean online) {
        try {
            String username = userService.getUsername(userId);
            messagingTemplate.convertAndSend("/topic/users/status",
                    new UserStatusMessage(userId, username, online));
            System.out.println("User status update sent: userId=" + userId + ", online=" + online);
        } catch (Exception e) {
            System.out.println("Error in method sendUserStatusUpdate: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
