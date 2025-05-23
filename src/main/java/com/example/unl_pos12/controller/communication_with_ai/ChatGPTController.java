package com.example.unl_pos12.controller.communication_with_ai;

import com.example.unl_pos12.model.chat_ai.MessageRequest;
import com.example.unl_pos12.service.OpenAIService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ChatGPTController {
    @Autowired
    private OpenAIService openAIService;

    @GetMapping("/communicating_with_a_advanced_ai_model")
    public String chatGPT(HttpSession session, Model model) {
        // Извлекаем историю чата из сессии или создаем новую, если ее нет
        List<String> chatMessages = (List<String>) session.getAttribute("chatMessagesGpt");
        if (chatMessages == null) {
            chatMessages = new ArrayList<>();
            session.setAttribute("chatMessagesGpt", chatMessages);
        }

        model.addAttribute("title", "Продвинутая модель");
        model.addAttribute("chatMessages", chatMessages);
        return "communication_with_ai/advancedModel";
    }

    @PostMapping("/communicating_with_a_advanced_ai_model")
    public String generateCompletion(@RequestParam(name = "prompt") String prompt,
                                     HttpSession session, Model model) {
        String result = openAIService.generateCompletion(prompt);

        // Извлекаем историю чата из сессии
        List<String> chatMessages = (List<String>) session.getAttribute("chatMessagesGpt");
        chatMessages.add(prompt);
        chatMessages.add(result);
        model.addAttribute("chatMessages", chatMessages);
        return "communication_with_ai/advancedModel";
    }

    @PostMapping("/api/communicating_with_a_advanced_ai_model")
    @ResponseBody
    public ResponseEntity<String> generateApiCompletion(@RequestBody MessageRequest request) {
        String result = openAIService.generateCompletion(request.getPrompt());
        return ResponseEntity.ok(result);
    }
}
