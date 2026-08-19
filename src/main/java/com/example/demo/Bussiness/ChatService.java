/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.DTOs.ChatMessageResponse;
import com.example.demo.DTOs.ChatResponse;
import com.example.demo.DTOs.ConversationSummaryResponse;
import com.example.demo.DataAccess.ChatMessageRepository;
import com.example.demo.DataAccess.ConversationRepository;
import com.example.demo.DataAccess.PageRepository;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.DataAccess.GroupRepository;
import com.example.demo.Entities.User;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.Attachment;
import com.example.demo.Entities.ChatMessage;
import com.example.demo.Entities.Conversation;
import com.example.demo.Entities.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;

/**
 *
 * @author 2005m
 */
@Service
public class ChatService {

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final IAiService aiService;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(PageRepository pageRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            IAiService aiService,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository) {
        this.pageRepository = pageRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;

    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    @Transactional
    public ChatResponse handleChatWithContext(String userMessage, List<Long> pageIds, Long conversationId) {
        //Oturumu doğrula veya oluştur
        User u = getAuthanticatedUser();
        Conversation conversation;
        if (conversationId != null) {
            conversation = conversationRepository.findByIdAndUserId(conversationId, u.getId())
                    .orElseThrow(() -> new SecurityException("Geçersiz oturum veya yetkisiz erişim! ID: " + conversationId));
        } else {
            String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            conversation = new Conversation(u, title);
            
            conversation = conversationRepository.save(conversation);
        }
        List<Page> pages=pageRepository.findAllById(pageIds);
        conversation.setPages(pages);

        List<Map<String, Object>> contexts = new ArrayList<>();
        if (pageIds != null && !pageIds.isEmpty()) {
            for (Long pageId : pageIds) {
                Page page = pageRepository.findByIdAndUserOrGroupMember(pageId, u.getId()).orElseThrow(
                        () -> new SecurityException("Not bulunamadı! Id: " + pageId)
                );

                Map<String, Object> context = new HashMap<>();
                context.put("title", page.getTitle());
                context.put("content", page.getContent());

                List<String> fileUrls = new ArrayList<>();
                if (page.getAttachments() != null) {
                    for (Attachment attachment : page.getAttachments()) {
                        fileUrls.add(attachment.getFileUrl());
                    }
                }
                context.put("fileUrls", fileUrls);
                contexts.add(context);

            }

        }

        //son 20 mesajı çek
        List<ChatMessage> history = chatMessageRepository.findByConversationIdOrderByCreatedDateAsc(conversation.getId(), org.springframework.data.domain.PageRequest.of(0, 20));

        String aiResponse = aiService.generateResponse(userMessage, contexts, history);
        chatMessageRepository.save(new ChatMessage(Role.USER, userMessage, conversation));
        chatMessageRepository.save(new ChatMessage(Role.ASSISTANT, aiResponse, conversation));
        return new ChatResponse(conversationId, aiResponse);

    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations() {
        User u = getAuthanticatedUser();
        return conversationRepository.findByUserIdOrderByCreatedDateDesc(u.getId())
                .stream().map(
                        c -> new ConversationSummaryResponse(c.getId(), c.getTitle(), c.getCreatedDate())
                ).toList();

    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationMessages(Long conversationId) {
        User u = getAuthanticatedUser();
        if (!conversationRepository.existByIdAndUserId(conversationId, u.getId())) {
            throw new SecurityException("Oturum bulunamadı veya yetkisiz erişim!");
        }

        return chatMessageRepository.findByConversationIdOrderByCreatedDateAsc(conversationId)
                .stream().map(
                        m -> new ChatMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedDate())
                ).toList();
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        User u = getAuthanticatedUser();
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, u.getId())
                .orElseThrow(() -> new SecurityException("Oturum bulunamadı veya yetkisiz erişim!"));
        conversationRepository.delete(conversation);

    }
}
