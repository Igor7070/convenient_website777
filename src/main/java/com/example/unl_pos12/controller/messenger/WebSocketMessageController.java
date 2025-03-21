package com.example.unl_pos12.controller.messenger;

import com.example.unl_pos12.model.messenger.Chat;
import com.example.unl_pos12.model.messenger.Message;
import com.example.unl_pos12.repo.ChatRepository;
import com.example.unl_pos12.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketMessageController {
    @Autowired
    private MessageService messageService;
    @Autowired
    private ChatRepository chatRepository;

    @MessageMapping("/sendMessage/chat/{chatId}")
    @SendTo("/topic/chat/{chatId}/messages")
    public Message sendMessage(@DestinationVariable String chatId, Message message) {
        System.out.println("Working method sendMessage...");
        System.out.println("Received message object: " + message);
        System.out.println("chatId: " + chatId);
        System.out.println("Received message: " + message.getContent() +
                " from user: " + message.getSender().getUsername());
        System.out.println("Message from chat: " + message.getChat());
        System.out.println("Message from chatName: " + message.getChat().getName());
        System.out.println("Message from chat id: " + (message.getChat() != null
                ? message.getChat().getId() : "null"));

        // Убедитесь, что chatId установлен
        if (message.getChat() != null && message.getChat().getId() != null) {
            Chat chat = chatRepository.findById(message.getChat().getId())
                    .orElseThrow(() -> new RuntimeException("Chat not found"));
            message.setChat(chat);
        } else {
            throw new RuntimeException("Chat ID is missing");
        }

        message.setDelivered_status(true); // Устанавливаем статус доставки
        return messageService.saveMessage(message, null); // Замените null на файл, если он есть
    }

    @MessageMapping("/editMessage/{chatId}")
    @SendTo("/topic/chat/{chatId}/messages")
    public Message editMessage(@DestinationVariable String chatId, Message message) {
        System.out.println("Editing message: " + message.getId());
        return messageService.updateMessage(message); // Обновляем сообщение в базе данных
    }

    @MessageMapping("/deleteMessage/{chatId}")
    @SendTo("/topic/chat/{chatId}/messages") // Замените на правильную тему
    public Message deleteMessage(@DestinationVariable String chatId, Long id) {
        messageService.deleteMessage(id);
        Message deletedMessage = new Message();
        deletedMessage.setId(id);
        deletedMessage.setContent("Message deleted");
        return deletedMessage; // Возвращаем сообщение о удалении
    }

    // Обработчик для статуса прочтения...
    @MessageMapping("/readMessage/{chatId}")
    @SendTo("/topic/chat/{chatId}/messages")
    public Message readMessage(@DestinationVariable String chatId, Long messageId) {
        // Обновите статус прочтения сообщения в базе данных, если необходимо
        System.out.println("Marking message as read: " + messageId);
        Message message = messageService.markAsRead(messageId);
        return message;
    }
}
