/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

import com.example.demo.DTOs.ChatRequest;
import com.example.demo.Bussiness.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 *
 * @author 2005m
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chatWithContext(@Valid @RequestBody ChatRequest chatRequest) {

        String aiResponse = chatService.handleChatWithContext(chatRequest.message(), chatRequest.pageIds());
        return ResponseEntity.ok(Map.of("response", aiResponse));
    }

}
