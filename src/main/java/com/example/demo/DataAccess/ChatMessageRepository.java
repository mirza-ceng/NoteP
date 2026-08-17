/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.ChatMessage;
import com.example.demo.Entities.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author 2005m
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    //bir oturumun son N mesajını çeker
   // @Query("SELECT  m FROM ChatMessage m WHERE m.conversation_id= :conversationId ORDER BY m.createdDate ASC")
    List<ChatMessage> findByConversationIdOrderByCreatedDateAsc(@Param("conversationId") Long conversationId, Pageable pageable);

    //tüm mesaj geçmişi
    List<ChatMessage> findByConversationIdOrderByCreatedDateAsc(Long conversationId);
}
