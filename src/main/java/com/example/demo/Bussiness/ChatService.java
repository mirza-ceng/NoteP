/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.DataAccess.PageRepository;
import com.example.demo.DataAccess.UserRepository;
import com.example.demo.DataAccess.GroupRepository;
import com.example.demo.Entities.User;
import com.example.demo.Entities.Page;
import com.example.demo.Entities.Attachment;
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

    public ChatService(PageRepository pageRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            IAiService aiService) {
        this.pageRepository = pageRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;

    }

    private User getAuthanticatedUser() {
        String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEMail(eMail).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
    }

    @Transactional(readOnly = true)
    public String handleChatWithContext(String userMessage, List<Long> pageIds) {
        User u = getAuthanticatedUser();
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
        return aiService.generateResponse(userMessage, contexts);
    };

    
    
}
