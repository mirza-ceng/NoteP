/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.Entities.ChatMessage;
import java.util.List;
import java.util.Map;

/**
 *
 * @author 2005m
 */
public interface IAiService {

    /**
     * Kullanıcı mesajını ve seçilen notların bağlamını alarak LLM'den yanıt
     * üretir.
     *
     * @param userMessage Kullanıcının chat ekranına yazdığı soru
     * @param contexts Seçilen notların başlık, içerik ve dosya URL'lerini
     * içeren paket listesi
     * @return Yapay zekanın ürettiği metin yanıtı
     */
    String generateResponse(String userMessage, List<Map<String, Object>> contexts,List<ChatMessage> history);

}
