package com.example.parsing_vacancies.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HuggingFaceChatController {
    private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-Nemo-Instruct-2407";
    //private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/bigscience/bloom";
    //private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/vicgalle/gpt2-alpaca-gpt4";
    //private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/erfanzar/LinguaMatic-GPT4";
    //private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/tiiuae/falcon-mamba-7b";
    //private static final String API_TOKEN = "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";

    @Value("${huggingFace.api.token}")
    private String API_TOKEN;
    private final RestTemplate restTemplate;
    private final List<String> chatMessages = new ArrayList<>();

    public HuggingFaceChatController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/communicating_with_a_primitive_ai_model")
    public String getChat(Model model) {
        model.addAttribute("title", "Примитивная модель");
        model.addAttribute("chatMessages", chatMessages);
        return "primitiveModel";
    }

    @PostMapping("/communicating_with_a_primitive_ai_model")
    public String sendMessage(@RequestParam(name = "message", required = false) String message, Model model) {
        model.addAttribute("title", "Примитивная модель");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode requestBody = mapper.createObjectNode().put("inputs", message);

        HttpEntity<JsonNode> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(HUGGING_FACE_API_URL, request, String.class);

        String responseText = response.getBody();
        chatMessages.add(message);
        chatMessages.add(responseText);
        model.addAttribute("chatMessages", chatMessages);
        return "primitiveModel";
    }
}