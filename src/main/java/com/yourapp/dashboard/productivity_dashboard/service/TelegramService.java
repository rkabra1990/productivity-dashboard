package com.yourapp.dashboard.productivity_dashboard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.Collections;

@Service
public class TelegramService {
    private final String botToken;
    private final String chatId;
    private final RestTemplate rest = new RestTemplate();

    public TelegramService(@Value("${telegram.bot-token}") String botToken,
                           @Value("${telegram.chat-id}") String chatId) {
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public void sendMessage(String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        
        // Set up headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // Create form data
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("chat_id", chatId);
        map.add("text", text);
        map.add("parse_mode", "HTML");
        
        // Create request entity
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        try {
            // Use postForEntity instead of getForEntity to properly handle URL encoding
            rest.postForEntity(url, request, String.class);
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception for debugging
        }
    }
}
