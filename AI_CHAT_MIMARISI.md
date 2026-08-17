# NoteP AI Chat Mimarisi

Bu doküman, NoteP uygulamasındaki yapay zeka (AI) sohbet sisteminin mimarisini, veri akışını ve bileşenlerini detaylı bir şekilde açıklar.

---

## 1. GENEL BAKIŞ

NoteP, kullanıcıların notlarını yönetebildiği bir Spring Boot uygulamasıdır. AI chat özelliği, kullanıcının **kendi notlarını bağlam (context) olarak kullanarak** Google Gemini 2.5 Flash modeliyle sohbet etmesini sağlar. Kullanıcı, sohbete hangi notların dahil edileceğini seçebilir; sistem bu notların içeriğini ve ekli dosyalarını AI modeline bağlam olarak iletir.

### Temel Özellikler

- **Bağlam Tabanlı Sohbet:** Kullanıcı, sohbete dahil etmek istediği notları `pageIds` listesiyle belirtir.
- **Çoklu Ortam (Multimodal) Desteği:** Notlara eklenmiş dosyalar (görsel, PDF, metin vb.) AI modeline medya olarak iletilir.
- **Güvenlik Kontrolü:** Kullanıcı yalnızca kendisinin sahibi olduğu veya üyesi olduğu gruba ait notları bağlam olarak kullanabilir.
- **Türkçe Asistan:** Sistem prompt'u, AI'ı Türkçe yanıt veren bir NoteP asistanı olarak yapılandırır.

---

## 2. MİMARİ KATMANLAR

```
┌─────────────────────────────────────────────────────────────────────┐
│                        ChatController (REST)                        │
│                    POST /api/chat  (JWT korumalı)                   │
├─────────────────────────────────────────────────────────────────────┤
│                         ChatService (Orkestrasyon)                  │
│              • Kimlik doğrulama (SecurityContext)                   │
│              • Not erişim kontrolü (sahiplik / grup üyeliği)        │
│              • Bağlam paketlerinin oluşturulması                    │
├─────────────────────────────────────────────────────────────────────┤
│                    IAiService (Arayüz / Soyutlama)                  │
│                              ▲                                      │
│                              │ implements                           │
│                    GeminiServiceImpl (Google Gemini)                │
│              • System prompt oluşturma                              │
│              • Medya (dosya) yükleme ve MIME tespiti                │
│              • Spring AI ChatModel çağrısı                          │
├─────────────────────────────────────────────────────────────────────┤
│                    PageRepository (Veri Erişimi)                    │
│        findByIdAndUserOrGroupMember() → JPQL sorgusu                │
├─────────────────────────────────────────────────────────────────────┤
│                    Google Gemini 2.5 Flash (Dış Servis)             │
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
    public ResponseEntity<Map<String, String>> chatWithContext(@Valid @RequestBody ChatRequest chatRequest) {
        String aiResponse = chatService.handleChatWithContext(chatRequest.message(), chatRequest.pageIds());
        return ResponseEntity.ok(Map.of("response", aiResponse));
    }
}
```

**Görevleri:**
- `POST /api/chat` uç noktasını tanımlar.
- Gelen `ChatRequest` DTO'sunu doğrular (`@Valid`).
- İsteği `ChatService`'e iletir ve yanıtı `{ "response": "..." }` formatında döndürür.
- CORS tüm kaynaklara açıktır.

### 3.2 ChatRequest (DTO)

**Dosya:** `src/main/java/com/example/demo/DTOs/ChatRequest.java`

```java
public record ChatRequest(
        @NotBlank(message = "Mesaj alanı bos bırakılamaz!!")
        String message,
        List<Long> pageIds
) { }
```

| Alan      | Tip            | Zorunlu | Açıklama                                   |
| --------- | -------------- | ------- | ------------------------------------------ |
| `message` | `String`       | ✅ Evet | Kullanıcının AI'a gönderdiği mesaj          |
| `pageIds` | `List<Long>`   | ❌ Hayır| Sohbete bağlam olarak dahil edilecek not ID'leri |

### 3.3 ChatService (İş Katmanı / Orkestrasyon)

**Dosya:** `src/main/java/com/example/demo/Bussiness/ChatService.java`

```java
@Service
public class ChatService {

    private final PageRepository pageRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final IAiService aiService;

    @Transactional(readOnly = true)
    public String handleChatWithContext(String userMessage, List<Long> pageIds) {
        User u = getAuthanticatedUser();
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
        return aiService.generateResponse(userMessage, contexts);
    }
}
```

**Görevleri:**
1. **Kimlik Doğrulama:** `SecurityContextHolder` üzerinden giriş yapan kullanıcıyı bulur.
2. **Erişim Kontrolü:** Her `pageId` için `findByIdAndUserOrGroupMember()` sorgusuyla notun erişilebilirliğini kontrol eder.
3. **Bağlam Oluşturma:** Her not için `title`, `content` ve `fileUrls` bilgilerini içeren bir `Map` oluşturur.
4. **Delegasyon:** Oluşturulan bağlam listesini `IAiService.generateResponse()` metoduna iletir.

### 3.4 IAiService (Arayüz / Soyutlama)

**Dosya:** `src/main/java/com/example/demo/Bussiness/IAiService.java`

```java
public interface IAiService {
    String generateResponse(String userMessage, List<Map<String, Object>> contexts);
}
```

Bu arayüz, AI sağlayıcısından bağımsız bir soyutlama sağlar. Gelecekte Gemini yerine başka bir model (OpenAI, Claude vb.) kullanılmak istenirse, yalnızca yeni bir `IAiService` implementasyonu eklenmesi yeterlidir.

### 3.5 GeminiServiceImpl (AI Sağlayıcı Implementasyonu)

**Dosya:** `src/main/java/com/example/demo/Bussiness/GeminiServiceImpl.java`

```java
@Service
public class GeminiServiceImpl implements IAiService {

    private final ChatModel chatModel;  // Spring AI ChatModel (Gemini 2.5 Flash)

    @Override
    public String generateResponse(String userMessage, List<Map<String, Object>> contexts) {
        // 1. System prompt oluştur
        String systemInstruction = """
            Sen NoteP uygulamasının akıllı asistanısın.
            ...
            Her zaman nazik, net ve Türkçe yanıt ver.
            """;

        // 2. Medya listesi ve bağlam metni oluştur
        List<Media> mediaList = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        // 3. Bağlamları işle (not içerikleri + dosya URL'leri)
        for (Map<String, Object> ctx : contexts) {
            contextBuilder.append("Başlık: ").append(ctx.get("title")).append("\n");
            contextBuilder.append("İçerik: ").append(ctx.get("content")).append("\n");

            List<String> urls = (List<String>) ctx.get("fileUrls");
            for (String urlStr : urls) {
                // MIME tipini URL uzantısından tespit et
                String mimeType = MimeTypeUtils.IMAGE_PNG_VALUE;
                if (urlStr.endsWith(".pdf")) mimeType = "application/pdf";
                else if (urlStr.endsWith(".jpg") || urlStr.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (urlStr.endsWith(".webp")) mimeType = "image/webp";
                else if (urlStr.endsWith(".txt")) mimeType = "text/plain";

                UrlResource resource = new UrlResource(URI.create(urlStr));
                mediaList.add(new Media(MimeTypeUtils.parseMimeType(mimeType), resource));
            }
        }

        // 4. System + User mesajlarını oluştur
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(
            systemInstruction + "\nBağlam:\n" + contextBuilder.toString()
        );
        Message systemMessage = systemPromptTemplate.createMessage();

        UserMessage userMediaMessage = new UserMessage(userMessage);
        if (!mediaList.isEmpty()) {
            userMediaMessage.getMedia().addAll(mediaList);
        }

        // 5. Prompt'u oluştur ve Gemini'ye gönder
        Prompt prompt = new Prompt(List.of(systemMessage, userMediaMessage));
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
```

**Görevleri:**
1. **System Prompt:** AI'ın rolünü, davranışını ve dilini belirler (Türkçe, nazik, NoteP asistanı).
2. **Bağlam Metni:** Seçilen notların başlık ve içeriklerini metin olarak prompt'a ekler.
3. **Medya Yükleme:** Notlara ekli dosyaların URL'lerinden `UrlResource` oluşturur ve MIME tipine göre `Media` nesnelerine dönüştürür.
4. **Prompt Oluşturma:** System ve User mesajlarını birleştirerek `Prompt` nesnesi oluşturur.
5. **Model Çağrısı:** `chatModel.call(prompt)` ile Gemini 2.5 Flash'a istek gönderir ve yanıt metnini döndürür.

---

## 4. VERİ AKIŞI (SEQUENCE DIAGRAM)

```
Kullanıcı                    ChatController          ChatService              PageRepository          GeminiServiceImpl          Gemini API
    │                              │                       │                        │                        │                        │
    │  POST /api/chat              │                       │                        │                        │                        │
    │  {message, pageIds}          │                       │                        │                        │                        │
    │─────────────────────────────>│                       │                        │                        │                        │
    │                              │  handleChatWithContext │                        │                        │                        │
    │                              │──────────────────────>│                        │                        │                        │
    │                              │                       │  getAuthanticatedUser() │                        │                        │
    │                              │                       │───────────────────────>│                        │                        │
    │                              │                       │  User (SecurityContext) │                        │                        │
    │                              │                       │<───────────────────────│                        │                        │
    │                              │                       │                        │                        │                        │
    │                              │                       │  findByIdAndUserOrGroupMember(pageId, userId)   │                        │
    │                              │                       │────────────────────────────────────────────────>│                        │
    │                              │                       │                        │  Page (erişim kontrolü)  │                        │
    │                              │                       │<────────────────────────────────────────────────│                        │
    │                              │                       │                        │                        │                        │
    │                              │                       │  Bağlam Map'leri oluştur (title, content, fileUrls)                        │
    │                              │                       │                        │                        │                        │
    │                              │                       │  generateResponse(message, contexts)            │                        │
    │                              │                       │────────────────────────────────────────────────>│                        │
    │                              │                       │                        │                        │  System + User Prompt  │
    │                              │                       │                        │                        │───────────────────────>│
    │                              │                       │                        │                        │  AI Yanıtı             │
    │                              │                       │                        │                        │<───────────────────────│
    │                              │                       │  AI Yanıtı (String)     │                        │                        │
    │                              │                       │<────────────────────────────────────────────────│                        │
    │                              │  { "response": "..." }│                        │                        │                        │
    │                              │<──────────────────────│                        │                        │                        │
    │  { "response": "..." }       │                       │                        │                        │                        │
    │<─────────────────────────────│                       │                        │                        │                        │
```

---

## 5. GÜVENLİK MİMARİSİ

### 5.1 Kimlik Doğrulama

- Tüm `/api/chat` istekleri **JWT token** gerektirir (`SecurityConfig`).
- `JWTFilter`, her istekte `Authorization: Bearer <token>` başlığını doğrular.
- `ChatService.getAuthanticatedUser()` metodu, `SecurityContextHolder`'dan e-posta adresini alır ve kullanıcıyı veritabanından yükler.

### 5.2 Not Erişim Kontrolü

`PageRepository.findByIdAndUserOrGroupMember()` metodu, kullanıcının bir nota erişim hakkı olup olmadığını kontrol eder:

```java
@Query("SELECT p FROM Page p LEFT JOIN p.group g LEFT JOIN g.members m "
     + "WHERE p.id = :pageId AND (p.user.id = :userId OR m.id = :userId)")
Optional<Page> findByIdAndUserOrGroupMember(@Param("pageId") Long pageId, @Param("userId") Long userId);
```

**Erişim Koşulları:**
- Kullanıcı notun **sahibi** ise (`p.user.id = :userId`), VEYA
- Kullanıcı, notun ait olduğu **grubun üyesi** ise (`m.id = :userId`)

Bu sorgu sayesinde, kullanıcı yalnızca kendi notlarını veya üyesi olduğu gruplardaki notları AI'a bağlam olarak verebilir. Erişim hakkı yoksa `SecurityException` fırlatılır.

---

## 6. PROMPT MÜHENDİSLİĞİ

### 6.1 System Prompt

```text
Sen NoteP uygulamasının akıllı asistanısın.
Sana kullanıcıya ait notların içerikleri ve bu notlara eklenmiş olan dosyaların internet adresleri (URL) sağlanacaktır.

Eğer kullanıcı ekteki dosyalarla ilgili bir soru sorarsa:
1. Sana sağlanan URL stringlerini incele (dosya adı, uzantısı veya link yapısından anlam çıkarmaya çalış).
2. Eğer link bir dosya indirme bağlantısıysa (attachment) veya içeriği doğrudan göremiyorsan, kullanıcıya dürüstçe içeriği göremediğini söyle AMA linkteki dosya adını ve türünü belirterek mantıklı bir yönlendirme yap. Uydurma, ama direkt 'Sadece URL görüyorum' diyerek de kestirip atma.

Her zaman nazik, net ve Türkçe yanıt ver.
```

**Prompt Tasarım İlkeleri:**
- **Rol Tanımı:** AI'ın NoteP asistanı olduğu belirtilir.
- **Bağlam Açıklaması:** AI'a hangi verilerin sağlanacağı açıklanır.
- **Dosya Yönlendirme Kuralı:** Dosya içeriği görülemiyorsa dürüst olma ve dosya adından yönlendirme yapma talimatı verilir.
- **Dil Kuralı:** Her zaman Türkçe yanıt verilmesi zorunlu kılınır.

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

Notlara eklenen dosyalar, URL'lerinden `UrlResource` olarak yüklenir ve `Media` nesnelerine dönüştürülür. Desteklenen MIME tipleri:

| Uzantı      | MIME Tipi          |
| ----------- | ------------------ |
| `.png`      | `image/png`        |
| `.jpg/.jpeg`| `image/jpeg`       |
| `.webp`     | `image/webp`       |
| `.pdf`      | `application/pdf`  |
| `.txt`      | `text/plain`       |

---

## 7. TEKNOLOJİ YIĞINI

| Bileşen                | Teknoloji                                    |
| ---------------------- | -------------------------------------------- |
| AI Modeli              | Google Gemini 2.5 Flash                      |
| AI Entegrasyonu        | Spring AI 1.1.0-M3 (`ChatModel`)             |
| HTTP Katmanı           | Spring Web MVC (REST)                        |
| Güvenlik               | Spring Security + JWT (jjwt 0.12.5)          |
| Veri Erişimi           | Spring Data JPA / Hibernate                  |
| Doğrulama              | Hibernate Validator (Bean Validation)        |
| API Dokümantasyonu     | Springdoc OpenAPI 2.6.0 (Swagger UI)         |
| Dosya Depolama         | Supabase S3 (AWS SDK S2 v2)                  |

---

## 8. GENİŞLETİLEBİLİRLİK

### 8.1 Farklı AI Sağlayıcısına Geçiş

`IAiService` arayüzü sayesinde yeni bir sağlayıcı eklemek kolaydır:

```java
@Service
public class OpenAiServiceImpl implements IAiService {
    @Override
    public String generateResponse(String userMessage, List<Map<String, Object>> contexts) {
        // OpenAI implementasyonu
    }
}
```

### 8.2 Sohbet Geçmişi (Multi-turn) Desteği

Mevcut mimari **tek mesajlık (single-turn)** yapıdadır. Her istek bağımsızdır ve önceki mesajlar hatırlanmaz. **Çoklu mesaj (multi-turn)** desteği, kullanıcının AI ile **bağlamı korunan, kesintisiz bir sohbet** yürütmesini sağlar. Bu, notlar üzerinde tartışma, karşılaştırma veya derinlemesine analiz gerektiren senaryolarda kritik bir geliştirmedir.

#### 8.2.1 Tasarım Hedefleri

| Hedef | Açıklama |
| ----- | -------- |
| **Kalıcı Diyalog** | Kullanıcı aynı diyalog içinde birden fazla soru sorabilir, AI önceki cevapları hatırlar |
| **Not Bağlamı İzolasyonu** | Her diyalog, kullanıcının seçtiği notlarla ilişkilendirilir ve diğer diyaloglardan bağımsız çalışır |
| **Ölçeklenebilirlik** | Token limitini aşmamak için geçmiş mesajlar makul uzunlukta (örn. son 20 mesaj) saklanır |
| **Gizlilik** | Yalnızca kullanıcının kendi diyalogları ve erişebildiği notlar bağlam olarak kullanılabilir |

#### 8.2.2 Veri Modeli (Yeni Entity'ler)

```java
// Conversation.java - Bir kullanıcının diyalog oturumu
@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title = "Yeni Sohbet";   // İlk mesaja göre otomatik oluşturulabilir

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdDate ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "conversation_pages",
        joinColumns = @JoinColumn(name = "conversation_id"),
        inverseJoinColumns = @JoinColumn(name = "page_id"))
    private List<Page> pages = new ArrayList<>();
}
```

```java
// ChatMessage.java — Tek bir diyalog mesajı
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;   // USER veya ASSISTANT

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
}

public enum Role {
    USER,
    ASSISTANT
}
```

#### 8.2.3 Repository'lar

```java
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByCreatedDateDesc(Long userId);
    Optional<Conversation> findByIdAndUserId(Long id, Long userId);
}

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTopNByConversationIdOrderByCreatedDateAsc(Long conversationId, int limit);
    // Son 20 mesajı kronolojik sırayla getirir
}
```

#### 8.2.4 API Tasarımı

`ChatRequest`'e `conversationId` alanı eklenir:

```json
// POST /api/chat — Çoklu mesaj isteği
{
  "message": "Peki bu notla ilgili örnek çalışma var mı?",
  "pageIds": [1, 2],
  "conversationId": 42
}
```

| Method | Endpoint                        | Auth | Açıklama                     |
| ------ | ------------------------------- | ---- | ---------------------------- |
| GET    | `/api/chat/conversations`       | ✅   | Kullanıcının tüm diyaloglarını listeler |
| GET    | `/api/chat/conversations/{id}`  | ✅   | Diyaloğun tüm mesaj geçmişini getirir |
| DELETE | `/api/chat/conversations/{id}`  | ✅   | Diyaloğu siler               |

#### 8.2.5 ChatService Güncellenmiş Akışı

```java
@Transactional
public String handleChatWithContext(String userMessage, List<Long> pageIds, Long conversationId) {
    User u = getAuthanticatedUser();

    // 1. Diyaloğu al veya oluştur
    Conversation conversation;
    if (conversationId != null) {
        conversation = conversationRepository.findByIdAndUserId(conversationId, u.getId())
            .orElseThrow(() -> new SecurityException("Diyalog bulunamadı!"));
    } else {
        conversation = new Conversation();
        conversation.setUser(u);
        conversation = conversationRepository.save(conversation);
    }

    // 2. Notları doğrula ve bağlamı oluştur (mevcut mantık)
    List<Map<String, Object>> contexts = buildContexts(pageIds, u);

    // 3. Geçmiş mesajları son 20 olarak yükle
    List<ChatMessage> history = messageRepository
        .findTopNByConversationIdOrderByCreatedDateAsc(conversation.getId(), 20);

    // 4. AI'dan yanıt üret
    String aiResponse = aiService.generateResponse(userMessage, contexts, history);

    // 5. Kullanıcı mesajını ve AI yanıtını kaydet
    saveMessage(conversation, Role.USER, userMessage);
    saveMessage(conversation, Role.ASSISTANT, aiResponse);

    return aiResponse;
}
```

#### 8.2.6 GeminiServiceImpl Güncellemesi

`IAiService` arayüzü genişletilir ve geçmiş mesajla destek eklenir:

```java
public interface IAiService {
    String generateResponse(String userMessage,
                            List<Map<String, Object>> contexts,
                            List<ChatMessage> history);  // null ise eski davranış
}
```

```java
@Override
public String generateResponse(String userMessage,
                               List<Map<String, Object>> contexts,
                               List<ChatMessage> history) {

    List<Message> messages = new ArrayList<>();

    // 1. System mesajı (bağlam metniyle)
    messages.add(systemPromptTemplate.createMessage());

    // 2. Geçmiş mesajları kronoloji sırayla ekle
    if (history != null) {
        for (ChatMessage m : history) {
            if (m.getRole() == Role.USER) {
                messages.add(new UserMessage(m.getContent()));
            } else {
                messages.add(new AssistantMessage(m.getContent()));
            }
        }
    }

    // 3. Güncel kullanıcı mesajı (medyalarla)
    UserMessage current = new UserMessage(userMessage);
    if (!mediaList.isEmpty()) current.getMedia().addAll(mediaList);
    messages.add(current);

    // 4. Model çağrısı
    Prompt prompt = new Prompt(messages);
    return chatModel.call(prompt).getResult().getOutput().getText();
}
```

Bu sayede Gemini, yalnızca son mesajı değil, **diyaloğun tam bağlamını** görerek tutarlı ve bağlam odaklı yanıt verir.

#### 8.2.7 Token (Bağlam Penceresi) Yönetimi

Gemini 2.5 Flash'ın token limiti sınırlı olduğundan geçmiş mesajlar dikkatli yönetilmelidir:

| Yaklaşım | Açıklama |
| -------- | -------- |
| **Son N Mesaj** | Yalnızca son 20 mesajı gönder; daha eskiyi at. Uygulaması basit. |
| **Özetleme** | Diyalog uzun sürede AI'a "Şimdiye kadar olanı özetle" mesajı gönder, özeti geçmişe ekle. |
| **Kayan Pencere** | `TokenUsage` ölçümüyle toplam token takibi yap, sınır yaklaşınca eski mesajları kırp. |
| **Not + Mesaj Yönetimi** | Not içerikleri uzunsa `pageIds` yalnızca diyaloğun başlangıcında gönder, sonraki mesajlarda mesaj geçmişini kullan. |

#### 8.2.8 Frontend Yansımaları

Frontend tarafında:

1. `conversationId` değerini tarayıcı hafızasında sakla, her istekte gönder.
2. Sayfa açılışında `GET /api/chat/conversations/{id}` ile geçmişi yükle ve ekranda listele.
3. "Yeni Sohbet" butonuna basıldığında `conversationId: null` ile yeni oturum başlat.
4. Diyalog listesinde başlık, önizleme ve tarih göster.

#### 8.2.9 Uygulama Adımları (Checklist)

1. **Entity Katmanı:** `Conversation` ve `ChatMessage` entity'lerini oluştur.
2. **Repository Katmanı:** `ConversationRepository` ve `ChatMessageRepository` yaz.
3. **DTO Katmanı:** `ChatRequest`'e `conversationId` ekle; `ConversationResponse` oluştur.
4. **Service Katmanı:** `ConversationService` oluştur veya `ChatService` içine entegre et.
5. **Arayüz Katmanı:** `IAiService` imzasına `history` parametresi ekle.
6. **AI Katmanı:** `GeminiServiceImpl`'de geçmiş mesaj döngüsü ekle.
7. **Controller Katmanı:** Yeni conversation uç noktaları tanımla.
8. **Token Yönetimi:** Kayan pencere veya özetleme stratejisi uygula.
9. **Test:** Çoklu konuşma, yeni konuşma, yetkisiz erişim ve token limiti senaryolarının test edildiğinden emin ol.

### 8.3 Akış (Streaming) Desteği

`ChatModel.stream()` metodu kullanılarak yanıtlar parça parça (token token) istemciye iletilebilir. Bu, kullanıcı deneyimini önemli ölçüde iyileştirir.

---

## 9. ÖZET

NoteP AI chat mimarisi, **katmanlı bir yapı** üzerine kurulmuştur:

1. **ChatController** REST isteğini alır ve doğrular.
2. **ChatService** kimlik doğrulaması yapar, not erişimini kontrol eder ve bağlam paketlerini oluşturur.
3. **IAiService** arayüzü, AI sağlayıcısından bağımsız bir soyutlama sağlar.
4. **GeminiServiceImpl** system prompt'u, bağlam metnini ve medya dosyalarını birleştirerek Google Gemini 2.5 Flash'a gönderir.
5. **PageRepository** JPQL sorgusuyla not erişim kontrolünü veritabanı seviyesinde gerçekleştirir.

Bu mimari; **güvenlik**, **genişletilebilirlik** ve **çoklu ortam desteği** açısından sağlam bir temel sunar.