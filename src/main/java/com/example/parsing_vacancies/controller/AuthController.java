package com.example.parsing_vacancies.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String login() {
        // Перенаправление на Google для аутентификации
        System.out.println("Autorization");
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/oauth2/callback")
    public String oauth2Callback(OAuth2AuthenticationToken authentication) {
        // Обработка данных пользователя после успешной аутентификации
        System.out.println("Success autorization");
        System.out.println("User email: " + authentication.getPrincipal().getAttribute("email"));
        return "redirect:/convenient_job_search"; // Перенаправление на домашнюю страницу
    }
}