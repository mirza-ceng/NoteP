# NoteP AI Chat Mimarisi

Bu doküman, NoteP uygulamasındaki yapay zeka (AI) sohbet sisteminin mimarisini, veri akışını ve bileşenlerini detaylı bir şekilde açıklar.

---

## 1. GENEL BAKIŞ

NoteP, kullanıcıların notlarını yönetebildiği bir Spring Boot uygulamasıdır. AI chat özelliği, kullanıcının **kendi notlarını bağlam (context) olarak kullanarak** Google Gemini 2.5 Flash modeliyle sohbet etmesini sağlar. Kullanıcı, sohbete hangi notların dahil edileceğini seçebilir; sistem bu notların içeriğini ve ekli dosyalarını AI modeline bağlam olarak iletir.

### Temel Özellikler

- **Çoklu Mesaj (Multi-Turn) Sohbet:** Kullanıcı `conversationId` ile mevcut bir sohbete devam edebilir; AI önceki mesajları hatırlar. Yeni sohbet başlatmak için `conversationId` boş bırakılır.
- **Bağlam Tabanlı Sohbet:** Kullanıcı, sohbete dahil etmek istediği notları `pageIds` listesiyle belirtir.
- **Mesaj Geçmişi Yönetimi:** Her sohbet oturumunda son 20 mesaj AI'a bağlam olarak iletilir (token yönetimi).
- **Çoklu Ortam (Multimodal) Desteği:** Notlara eklenmiş dosyalar (görsel, PDF, metin vb.) AI modeline medya olarak iletilir.
- **Güvenlik Kontrolü:** Kullanıcı yalnızca kendisinin sahibi olduğu veya üyesi olduğu gruba ait notları bağlam olarak kullanabilir. Ayrıca yalnızca kendi diyalog oturumlarına erişebilir.
- **Türkçe Asistan:** Sistem prompt'u, AI'ı Türkçe yanıt veren bir NoteP asistanı olarak yapılandırır.

---

## 2. MİMARİ KATMANLAR

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ChatController (REST)                        │
│        POST /api/chat  (JWT korumalı, multi-turn destekli)           │
│        GET  /api/chat/conversations                                  │
│        GET  /api/chat/conversations/{id}                             │
│        DELETE /api/chat/conversations/{id}                           │
├─────────────────────────────────────────────────────────────────────┤
│                         ChatService (Orkestrasyon)                   │
│              • Kimlik doğrulama (SecurityContext)                    │
│              • Conversation doğrulama / oluşturma (multi-turn)       │
│              • Not erişim kontrolü (sahiplik / grup üyeliği)         │
│              • Mesaj geçmişi yükleme (son 20)                        │
│              • Kullanıcı/AI mesajlarını kaydetme                     │
├─────────────────────────────────────────────────────────────────────┤
│                    IAiService (Arayüz / Soyutlama)                   │
│                              ▲                                       │
│                              │ implements                            │
│                    GeminiServiceImpl (Google Gemini)                 │
│              • System prompt oluşturma                               │
│              • Medya (dosya) yükleme ve MIME tespiti                 │
│              • Geçmiş mesajları Spring AI mesajlarına çevirme        │
│              • Spring AI ChatModel çağrısı                           │
├─────────────────────────────────────────────────────────────────────┤
│      PageRepository / ConversationRepository / ChatMessageRepository │
│        findByIdAndUserOrGroupMember() → JPQL sorgusu                 │
│        findByIdAndUserId() → diyalog sahiplik kontrolü               │
│        findByConversationIdOrderByCreatedDateAsc() → mesaj geçmişi   │
├─────────────────────────────────────────────────────────────────────┤
│                    Google Gemini 2.5 Flash (Dış Servis)              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. BİLEŞENLERİN DETAYLI İNCELEMESİ

### 3.1 ChatController (Sunum Katmanı)

**Dosya:** `src/main/java/com/example/demo/Controllers/ChatController.java`

```java
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

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
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id) {
        chatService.deleteConversation(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Görevleri:**
- `POST /api/chat` uç noktasını tanımlar (multi-turn destekli).
- `GET /api/chat/conversations` ile kullanıcının tüm diyalog oturumlarını listeler.
- `GET /api/chat/conversations/{id}` ile bir diyaloğun tüm mesajlarını getirir.
- `DELETE /api/chat/conversations/{id}` ile bir diyaloğu siler (204 No Content).
- CORS tüm kaynaklara açıktır.

### 3.2 ChatRequest (DTO)

**Dosya:** `src/main/java/com/example/demo/DTOs/ChatRequest.java`

```java
public record ChatRequest(
        @NotBlank(message = "Mesaj alanı boş bırakılamaz!!")
        String message,
        List<Long> pageIds,
        Long conversationId
) { }
```

| Alan              | Tip            | Zorunlu | Açıklama                                                     |
| ----------------- | -------------- | ------- | ------------------------------------------------------------ |
| `message`         | `String`       | ✅ Evet | Kullanıcının AI'a gönderdiği mesaj                            |
| `pageIds`         | `List<Long>`   | ❌ Hayır| Sohbete bağlam olarak dahil edilecek not ID'leri              |
| `conversationId`  | `Long`         | ❌ Hayır| Mevcut bir sohbete devam etmek için konuşma oturum ID'si. Boşsa yeni sohbet oluşturulur. |

### 3.3 AI Yanıt DTO'lar

**ChatResponse** (`DTOs/ChatResponse.java`):
```java
public record ChatResponse(
        Long conversationId,
        String response
) { }
```

**ChatMessageResponse** (`DTOs/ChatMessageResponse.java`):
```java
public record ChatMessageResponse(
        Long id,
        Role role,          // USER veya ASSISTANT
        String content,
        LocalDateTime createdDate
) { }
```

**ConversationSummaryResponse** (`DTOs/ConversationSummaryResponse.java`):
```java
public record ConversationSummaryResponse(
        Long id,
        String title,
        LocalDateTime createdDate
) { }
```

### 3.4 ChatService (İş Katmanı / Orkestrasyon)

**Dosya:** `src/main/java/com/example/demo/Bussiness/ChatService.java`

```java
@Service
public class ChatService {

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final IAiService aiService;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatResponse handleChatWithContext(String userMessage, List<Long> pageIds, Long conversationId) {
        // 1. Diyaloğu doğrula veya oluştur
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

        // 2. Not bağlamlarını topla
        List<Map<String, Object>> contexts = new ArrayList<>();
        if (pageIds != null && !pageIds.isEmpty()) {
            for (Long pageId : pageIds) {
                Page page = pageRepository.findByIdAndUserOrGroupMember(pageId, u.getId())
                    .orElseThrow(() -> new SecurityException("Not bulunamadı! Id: " + pageId));
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

        // 3. Son 20 mesajı geçmiş olarak al
        List<ChatMessage> history = chatMessageRepository
            .findByConversationIdOrderByCreatedDateAsc(conversation.getId(), PageRequest.of(0, 20));

        // 4. AI yanıtı üret ve mesajları kaydet
        String aiResponse = aiService.generateResponse(userMessage, contexts, history);
        chatMessageRepository.save(new ChatMessage(Role.USER, userMessage, conversation));
        chatMessageRepository.save(new ChatMessage(Role.ASSISTANT, aiResponse, conversation));
        return new ChatResponse(conversation.getId(), aiResponse);
    }
}
```

**Görevleri:**
1. **Kimlik Doğrulama:** `SecurityContextHolder` üzerinden kimliği doğrulanmış kullanıcıyı bulur.
2. **Oturum Yönetimi:** `conversationId` verilmişse sahiplik kontrolü yapar; verilmemişse yeni `Conversation` oluşturur (başlık = mesajın ilk 30 karakteri + "...").
3. **Erişim Kontrolü:** Her `pageId` için `findByIdAndUserOrGroupMember()` sorgusuyla notun erişilebilirliğini kontrol eder.
4. **Bağlam Oluşturma:** Bağlam paketlerini (title, content, fileUrls) oluşturur.
5. **Geçmiş Yükleme:** Son 20 `ChatMessage`'i kronolojik sırayla çeker.
6. **AI Çağrısı:** `IAiService.generateResponse(userMessage, contexts, history)` metodunu çağırır.
7. **Mesaj Kaydetme:** Kullanıcı mesajını ve AI yanıtını `ChatMessage` olarak veri tabanına sırasıyla kaydeder.
8. **Dönüş:** `ChatResponse(conversationId, response)` döndürür.

### 3.5 IAiService (Arayüz / Soyutlama)

**Dosya:** `src/main/java/com/example/demo/Bussiness/IAiService.java`

```java
public interface IAiService {
    String generateResponse(String userMessage, List<Map<String, Object>> contexts, List<ChatMessage> history);
}
```

Üç parametreli imza, AI sağlayıcısından bağımsız bir soyutlama sağlar. `history` parametresi çoklu mesaj (multi-turn) desteği için eklenmiştir. Gelecekte Gemini yerine başka bir model kullanılmak istenirse yeni bir `IAiService` implementasyonu eklenir.

### 3.6 GeminiServiceImpl (AI Sağlayıcı Implementasyonu)

**Dosya:** `src/main/java/com/example/demo/Bussiness/GeminiServiceImpl.java`

```java
@Service
public class GeminiServiceImpl implements IAiService {

    private final ChatModel chatModel;  // Spring AI ChatModel (Gemini 2.5 Flash)

    @Override
    public String generateResponse(String userMessage, List<Map<String, Object>> contexts, List<ChatMessage> history) {
        // 1. System prompt oluştur
        String systemInstruction = """
            Sen NoteP uygulamasının akıllı asistanısın.
            ...
            Her zaman nazik, net ve Türkçe yanıt ver.
            """;

        // 2. Bağlam metni ve medya listesi oluştur
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
                    for (String urlStr : urls) {
                        try {
                            String mimeType = MimeTypeUtils.IMAGE_PNG_VALUE;
                            if (urlStr.endsWith(".pdf")) mimeType = "application/pdf";
                            else if (urlStr.endsWith(".jpg") || urlStr.endsWith(".jpeg")) mimeType = "image/jpeg";
                            else if (urlStr.endsWith(".webp")) mimeType = "image/webp";
                            else if (urlStr.endsWith(".txt")) mimeType = "text/plain";
                            UrlResource resource = new UrlResource(URI.create(urlStr));
                            mediaList.add(new Media(MimeTypeUtils.parseMimeType(mimeType), resource));
                        } catch (Exception e) {
                            System.out.println("Dosya indirilirken hata oluştu, atlanıyor: " + urlStr);
                        }
                    }
                }
            }
        }

        // 3. System + User mesajları oluştur
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction + "\nBağlam:\n" + contextBuilder.toString());
        Message systemMessage = systemPromptTemplate.createMessage();

        // 4. Geçmiş mesajları Spring AI mesajlarına çevir
        List<Message> messages = new ArrayList<>();
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

        // 5. Güncel kullanıcı mesajı (medyalarla)
        UserMessage userMediaMessage = new UserMessage(userMessage);
        if (!mediaList.isEmpty()) {
            userMediaMessage.getMedia().addAll(mediaList);
        }
        messages.add(userMediaMessage);

        // 6. Modeli çağır
        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
```

**Görevleri:**
1. **System Prompt:** AI'ın rolünü, davranışını ve dilini belirler.
2. **Bağlam Metni:** Seçilen notların başlık ve içeriklerini prompt'a ekler.
3. **Medya Yükleme:** Notlara ekli dosyaların URL'lerinden `UrlResource` ile `Media` oluşturur.
4. **Geçmiş Mesaj Dönüştürme:** `ChatMessage` (DB) kayıtlarını Spring AI `UserMessage` / `AssistantMessage` nesnelerine dönüştürür.
5. **Prompt Oluşturma:** System + geçmiş + güncel mesaj birleştirerek `Prompt` oluşturur.
6. **Model Çağrısı:** `ChatModel.call(prompt)` ile Gemini 2.5 Flash'a istek gönderir.

---

## 4. VERİ MODELİ (Multi-Turn)

### 4.1 Conversation (Diyalog Oturumu)

**Dosya:** `src/main/java/com/example/demo/Entities/Conversation.java`

```java
@Entity @Table(name = "conversation")
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) String title = "Yeni Sohbet";
    @CreationTimestamp @Column(name = "created_date", updatable = false) LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) User user;
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC") List<ChatMessage> messages = new ArrayList<>();
    @ManyToMany @JoinTable(name = "conversation_pages",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "page_id")) List<Page> pages = new ArrayList<>();
}
```

### 4.2 ChatMessage (Mesaj)

**Dosya:** `src/main/java/com/example/demo/Entities/ChatMessage.java`

```java
@Entity @Table(name = "chat_message")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Role role;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") String content;
    @CreationTimestamp @Column(name = "created_date", updatable = false) LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false)
    Conversation conversation;
}
```

### 4.3 Role Enum

**Dosya:** `src/main/java/com/example/demo/Entities/Role.java`

```java
public enum Role {
    USER,
    ASSISTANT
}
```

---

## 5. GÜVENLİK MİMARİSİ

### 5.1 Kimlik Doğrulama

- Tüm `/api/chat/**` istekleri **JWT token** gerektirir (`SecurityConfig`).
- `JWTFilter`, her istekte `Authorization: Bearer <token>` başlığını doğrular.
- `ChatService.getAuthanticatedUser()` metodu, `SecurityContextHolder`'dan e-posta adresini alır ve kullanıcıyı veritabanından yükler.

### 5.2 Diyalog Erişim Kontrolü

**`ConversationRepository.findByIdAndUserId()` metodu**, kullanıcının yalnızca kendi diyaloglarına erişmesini sağlar:

```java
Optional<Conversation> findByIdAndUserId(Long id, Long userId);
```

Bu sorgu şu amaçlarla kullanılır:
- `POST /api/chat` → mevcut diyaloğu doğrulamak (aynı kullanıcının diyaloğu olmalı)
- `GET /api/chat/conversations/{id}` → mesaj geçmişini getirmeden önce sahiplik kontrolü
- `DELETE /api/chat/conversations/{id}` → silme işleminden önce sahiplik kontrolü

### 5.3 Not Erişim Kontrolü

`PageRepository.findByIdAndUserOrGroupMember()` metodu, kullanıcının bir nota erişim hakkı olup olmadığını kontrol eder:

```java
@Query("SELECT p FROM Page p LEFT JOIN p.group g LEFT JOIN g.members m "
       + "WHERE p.id = :pageId AND (p.user.id = :userId OR m.id = :userId)")
Optional<Page> findByIdAndUserOrGroupMember(@Param("pageId") Long pageId, @Param("userId") Long userId);
```

**Erişim Koşulları:**
- Kullanıcı notun **sahibi** ise (`p.user.id = :userId`), VEYA
- Kullanıcı, notun ait olduğu **grubun üyesi** ise (`m.id = :userId`)

---

## 6. PROMPT MÜHENDİSLİĞİ

### 6.1 System Prompt

```text
Sen NoteP uygulamasının akıllı asistanısın.
Sana kullanıcıya ait notların içerikleri ve bu notlara eklenmiş olan dosyaların internet adresleri (URL) sağlanacaktır.

Eğer kullanıcı ekteki dosyalarla ilgili bir soru sorarsa:
1. Sana sağlanan URL stringlerini incele (dosya adı, uzantısı veya link yapısından anlam çıkarmaya çalış).
2. Eğer link bir dosya indirme bağlantısıysa (attachment) veya içeriği doğrudan göremiyorsan, kullanıcıya dürüstçe içeriği göremediğini söyle AMA linkteki dosya adını (örneğin 'dummy.pdf' veya 'ISO_C_Logo.png') ve türünü belirterek mantıklı bir yönlendirme yap. Uydurma, ama direkt 'Sadece URL görüyorum' diyerek de kestirip atma.

Her zaman nazik, net ve Türkçe yanıt ver.
```

### 6.2 Bağlam Formatı

```text
Bağlam:
--- NOT 1 ---
Başlık: Ders Notu
İçerik: Spring Boot çalışma notları...

--- NOT 2 ---
Başlık: Algoritma Notları
İçerik: Big-O notasyonu...
```

### 6.3 Medya (Dosya) Desteği

Notlara eklenen dosyalar, URL'lerinden `UrlResource` olarak yüklenir ve `Media` nesnelerine dönüştürülür. Desteklenen MIME türleri:

| Uzantı      | MIME Tipi          |
| ----------- | ------------------ |
| `.png`      | `image/png`        |
| `.jpg/.jpeg`| `image/jpeg`       |
| `.webp`     | `image/webp`       |
| `.pdf`      | `application/pdf`  |
| `.txt`      | `text/plain`       |

### 6.4 Geçmiş Mesaj (Multi-Turn) Prompt Yapısı

Gemini'ye gönderilen mesaj listesi:

```
1. SystemMessage  (rol + bağlam)
2. UserMessage    (önceki kullanıcı mesajı)      ← history[0]
3. AssistantMessage (önceki AI yanıtı)            ← history[1]
4. UserMessage    (bir önceki kullanıcı mesajı)  ← history[2]
5. AssistantMessage (bir önceki AI yanıtı)        ← history[3]
...
N. UserMessage    (mevcut kullanıcı mesajı + medyalar)  ← son mesaj
```

---

## 7. VERİ AKIŞI (SEQUENCE DIAGRAM)

```
Kullanıcı                    ChatController          ChatService              ConversationRepository       ChatMessageRepository       IAiService/Gemini          Gemini API
    │                              │                       │                        │                        │                        │                        │
    │  POST /api/chat              │                       │                        │                        │                        │                        │
    │  {message, pageIds, convId}  │                       │                        │                        │                        │                        │
    │─────────────────────────────>│                       │                        │                        │                        │                        │
    │                              │  handleChatWithContext │                        │                        │                        │                        │
    │                              │──────────────────────>│                        │                        │                        │                        │
    │                              │                       │  getAuthanticatedUser() │                        │                        │                        │
    │                              │                       │───────────────────────>│                        │                        │                        │
    │                              │                       │  User (SecurityContext) │                        │                        │                        │
    │                              │                       │<──────────────────────>│                        │                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  findByIdAndUserId(convId, userId)  (convId varsa) │                        │                        │
    │                              │                       │─────────────────────────────────────────────────────>│                        │                        │
    │                              │                       │  Conversation (sahiplik doğrulama)                   │                        │                        │
    │                              │                       │<─────────────────────────────────────────────────────│                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  Yeni Conversation oluştur (convId yoksa)           │                        │                        │
    │                              │                       │───────────────────────────────────────>│         │                        │                        │
    │                              │                       │<───────────────────────────────────────│         │                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  findByIdAndUserOrGroupMember(pageId, userId)      │                        │                        │
    │                              │                       │────────────────────────────────────────────────────>│                        │                        │
    │                              │                       │  Page (erişim kontrolü)                              │                        │                        │
    │                              │                       │<────────────────────────────────────────────────────│                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  Bağlam Map'leri oluştur (title, content, fileUrls)   │                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  findByConversationIdOrderByCreatedDateAsc(convId, top 20) │                  │                        │
    │                              │                       │───────────────────────────────────────────────────────>│                        │                        │
    │                              │                       │  history (son 20 mesaj)                              │                        │                        │
    │                              │                       │<───────────────────────────────────────────────────────│                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  generateResponse(message, contexts, history)                            │                        │
    │                              │                       │─────────────────────────────────────────────────────────────────────────────>│                        │
    │                              │                       │                        │                        │                        │  System + User + geçmiş │
    │                              │                       │                        │                        │                        │────────────────────────>│
    │                              │                       │                        │                        │                        │  AI Yanıtı              │
    │                              │                       │                        │                        │                        │<────────────────────────│
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  save(USER message)     │                        │                        │                        │
    │                              │                       │───────────────────────────────────────────────>│         │                        │                        │
    │                              │                       │  save(ASSISTANT message)│                        │                        │                        │
    │                              │                       │───────────────────────────────────────────────>│         │                        │                        │
    │                              │                       │                        │                        │                        │                        │
    │                              │                       │  AI Yanıtı + conversationId  │                  │                        │                        │
    │                              │                       │<───────────────────────────────────────────────────────────────│                        │                        │
    │                              │  ChatResponse(conversationId, response)   │                        │                        │                        │
    │                              │<───────────────────────│                        │                        │                        │                        │
    │  ChatResponse               │                       │                        │                        │                        │                        │
    │<─────────────────────────────│                       │                        │                        │                        │                        │
```

---

## 8. API UÇ NOKTALARI

| Method | Endpoint                        | Auth | Açıklama                                             |
| ------ | ------------------------------- | ---- | ----------------------------------------------------- |
| POST   | `/api/chat`                     | ✅   | AI ile sohbet (yeni veya mevcut conversationId ile)    |
| GET    | `/api/chat/conversations`       | ✅   | Kullanıcının tüm diyalog oturumlarını listeler (yeniden eskiye) |
| GET    | `/api/chat/conversations/{id}`  | ✅   | Diyaloğun tüm mesaj geçmişini (kronolojik) getirir   |
| DELETE | `/api/chat/conversations/{id}`  | ✅   | Diyaloğu siler (204 No Content), mesajlar da silinir  |

### Örnek İstekler

**Yeni sohbet:**
```json
POST /api/chat
{
  "message": "Bu notlar hakkında ne düşünüyorsun?",
  "pageIds": [1, 2, 3]
}
```
```json
{
  "conversationId": 1,
  "response": "Notlarınızı inceledim..."
}
```

**Mevcut sohbete devam:**
```json
POST /api/chat
{
  "message": "Peki ikinci notta ne anlatılmış?",
  "pageIds": [1, 2, 3],
  "conversationId": 1
}
```
```json
{
  "conversationId": 1,
  "response": "İkinci notta..."
}
```

---

## 9. TEKNOLOJİ YIĞINI

| Bileşen                | Teknoloji                                    |
| ---------------------- | -------------------------------------------- |
| AI Modeli              | Google Gemini 2.5 Flash                      |
| AI Entegrasyonu        | Spring AI 1.1.0-M3 (`ChatModel`)             |
| HTTP Katmanı           | Spring Web MVC (REST)                        |
| Güvenlik               | Spring Security + JWT (jjwt 0.12.5)          |
| Veri Erişimi           | Spring Data JPA / Hibernate                  |
| Doğrulama              | Hibernate Validator (Bean Validation)        |
| API Dokümantasyonu     | Springdoc OpenAPI 2.6.0 (Swagger UI)         |
| Dosya Depolama         | Supabase S3 (AWS SDK v2)                     |

---

## 10. GENİŞLETİLEBİLİRLİK

### 10.1 Farklı AI Sağlayıcısına Geçiş

`IAiService` arayüzü sayesinde yeni sağlayıcı eklemek kolaydır:

```java
@Service
public class OpenAiServiceImpl implements IAiService {
    @Override
    public String generateResponse(String userMessage, List<Map<String, Object>> contexts, List<ChatMessage> history) {
        // OpenAI uygulaması
    }
}
```

### 10.2 Akış (Streaming) Desteği

`ChatModel.stream()` metodu kullanılarak yanıtlar parça parça (token token) istemciye iletilebilir.

---

## 11. ÖZET

NoteP AI chat mimarisi, **katmanlı bir yapı** üzerine kurulmuştur:

1. **ChatController** REST isteğini alır, doğrular ve diyalog/conversation uç noktalarını yönetir.
2. **ChatService** kimlik doğrulaması yapar, diyalog oturumunu doğrular/oluşturur, not erişimini kontrol eder, mesaj geçmişini yükler ve bağlam paketlerini oluşturur.
3. **IAiService** arayüzü, AI sağlayıcısından bağımsız bir soyutlama sağlar.
4. **GeminiServiceImpl** system prompt'u, bağlam metnini, medya dosyalarını ve geçmiş mesajları birleştirerek Google Gemini 2.5 Flash'a gönderir.
5. **ConversationRepository / ChatMessageRepository** multi-turn sohbet geçmişinin saklanması ve sahiplik kontrolü işlemlerini yönetir.
6. **PageRepository** JPQL sorgusuyla not erişim kontrolünü veritabanı seviyesinde gerçekleştirir.

Bu mimari; **güvenlik**, **genişletilebilirlik**, **çoklu ortam desteği** ve **bağlamlı çoklu mesaj sohbeti** açısından sağlam bir temel sunar.