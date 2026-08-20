/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.Bussiness;

import com.example.demo.DTOs.ChatMessageResponse;
import com.example.demo.Entities.ChatMessage;
import com.example.demo.Entities.Role;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 *
 * @author 2005m
 */
@Service
public class GeminiServiceImpl implements IAiService {

    private final ChatModel chatModel;

    public GeminiServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateResponse(String userMessage, List<Map<String, Object>> contexts, List<ChatMessage> history) {
        String systemInstruction = """
    Sen NoteP uygulamasının akıllı asistanısın.
 Kullanıcıya uygun biçimde cevap ver
    """;
 /*  Sana kullanıcıya ait notların içerikleri ve bu notlara eklenmiş olan dosyaların internet adresleri (URL) sağlanacaktır.
    
    Eğer kullanıcı ekteki dosyalarla ilgili bir soru sorarsa:
    1. Sana sağlanan URL stringlerini incele (dosya adı, uzantısı veya link yapısından anlam çıkarmaya çalış).
    2. Eğer link bir dosya indirme bağlantısıysa (attachment) veya içeriği doğrudan göremiyorsan, kullanıcıya dürüstçe içeriği göremediğini söyle AMA linkteki dosya adını (örneğin 'dummy.pdf' veya 'ISO_C_Logo.png') ve türünü belirterek mantıklı bir yönlendirme yap. Uydurma, ama direkt 'Sadece URL görüyorum' diyerek de kestirip atma.
    
    Her zaman nazik, net ve Türkçe yanıt ver.*/
        List<Media> mediaList = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        if (contexts == null || contexts.isEmpty()) {
            contextBuilder.append("Kullanıcı şu an herhangi bir not seçmedi. Genel bir sohbet yürütüyorsun.");
        } else {

            for (int i = 0; i < contexts.size(); i++) {
                Map<String, Object> ctx = contexts.get(i);

                contextBuilder.append(String.format("\n--- NOT %d ---\n", i + 1));
                contextBuilder.append("Başlık: ").append(ctx.get("title")).append("\n");
                contextBuilder.append("İçerik: ").append(ctx.get("content")).append("\n");

                List<String> urls = (List<String>) ctx.get("fileUrls");
                if (urls != null && !urls.isEmpty()) {
                    // contextBuilder.append("Ekli Dosya Adresleri :\n");
                    for (String urlStr : urls) {
                        try {
                            String mimeType = MimeTypeUtils.IMAGE_PNG_VALUE;
                            if (urlStr.endsWith(".pdf")) {
                                mimeType = "application/pdf";
                            } else if (urlStr.endsWith(".jpg") || urlStr.endsWith(".jpeg")) {
                                mimeType = "image/jpeg";
                            } else if (urlStr.endsWith(".webp")) {
                                mimeType = "image/webp";
                            } else if (urlStr.endsWith(".txt")) {
                                mimeType = "text/plain";
                            }
                            UrlResource resource = new UrlResource(URI.create(urlStr));

                            mediaList.add(new Media(MimeTypeUtils.parseMimeType(mimeType), resource));

                        } catch (Exception e) {
                            System.out.println("Dosya indirilirken hata oluştu, atlanıyor: " + urlStr);
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                        }

                    }

                    //
                }

            }

        }

        List<Message> messages = new ArrayList<>();
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction + "\nBağlam:\n" + contextBuilder.toString());
        Message systemMessage = systemPromptTemplate.createMessage();
        messages.add(systemMessage);

        if (history != null && !history.isEmpty()) {
            for (ChatMessage chatMessage : history) {
                if (chatMessage.getRole() == Role.USER) {
                    messages.add(new UserMessage(chatMessage.getContent()));
                } else if (chatMessage.getRole() == Role.ASSISTANT) {
                    messages.add(new AssistantMessage(chatMessage.getContent()));
                }
            }
        }

        UserMessage userMediaMessage = new UserMessage(userMessage);
        if (!mediaList.isEmpty()) {
            userMediaMessage.getMedia().addAll(mediaList);
        }
        messages.add(userMediaMessage);

        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }

}
