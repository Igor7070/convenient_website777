package com.example.unl_pos12.controller.messenger;

import com.example.unl_pos12.model.messenger.Message;
import com.example.unl_pos12.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketMessageController {
    @Autowired
    private MessageService messageService;

    @MessageMapping("/sendMessage")  // Обрабатывает сообщения, отправленные на /app/sendMessage
    @SendTo("/topic/messages")       // Отправляет сообщение всем подписчикам на /topic/messages
    public Message sendMessage(Message message) {
        System.out.println("Working method sendMessage...");
        System.out.println("Received message: " + message.getContent() +
                " from user: " + message.getSender().getUsername());
        return messageService.saveMessage(message); // Сохраняем сообщение и отправляем его назад
    }
}
