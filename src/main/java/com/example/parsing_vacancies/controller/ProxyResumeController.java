package com.example.parsing_vacancies.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/proxy")
public class ProxyResumeController {
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/send-resume")
    public ResponseEntity<String> sendResume(@RequestHeader("Authorization") String token,
                                             @RequestBody String jsonBody,
                                             @RequestParam("apiUrl") String apiUrl) {
        System.out.println("apiUrl: " + apiUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        //headers.add("Content-Type", "multipart/form-data");
        headers.add("Content-Type", "application/json");
        // Добавление дополнительных заголовков
        headers.add("Accept", "application/json, text/plain, */*");
        headers.add("Referer", "https://robota.ua/");
        headers.add("Origin", "https://robota.ua/");

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
        return restTemplate.postForEntity(apiUrl, requestEntity, String.class);
    }
}
