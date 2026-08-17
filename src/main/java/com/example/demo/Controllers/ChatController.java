/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Controllers;

import com.example.demo.DTOs.ChatRequest;
import com.example.demo.Bussiness.ChatService;
import com.example.demo.DTOs.ChatMessageResponse;
import com.example.demo.DTOs.ChatResponse;
import com.example.demo.DTOs.ConversationSummaryResponse;
import com.example.demo.Entities.ChatMessage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

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
    public ResponseEntity<ChatResponse> chatWithContext(@Valid @RequestBody ChatRequest chatRequest) {
        ChatResponse chatResponse = chatService.handleChatWithContext(chatRequest.message(), chatRequest.pageIds(), chatRequest.conversationId());
        return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryResponse>> getUserConversations() {
        return ResponseEntity.ok(chatService.getUserConversations());
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<List<ChatMessageResponse>> getConversationMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getConversationMessages(id));

    }
    @DeleteMapping("conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id){
    chatService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }

}
