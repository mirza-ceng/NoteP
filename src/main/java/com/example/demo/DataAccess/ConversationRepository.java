/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.DataAccess;

import com.example.demo.Entities.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author 2005m
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    //tüm oturumları yeniden eskiye çağırır
    List<Conversation> findByUserIdOrderByCreatedDateDesc(Long userId);

    // kullanıcının sadece kendi oturumuna erişmesini garanti eder
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);

    boolean existByIdAndUserId(Long id, Long userId);
}
