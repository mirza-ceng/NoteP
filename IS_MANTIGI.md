# NoteP Projesi - İş Mantığı Dokümantasyonu

> **Amaç:** Bu doküman, NoteP uygulamasının iş mantığını (business logic) uçtan uca açıklar: kullanıcı yönetimi, not yönetimi, grup yönetimi, dosya ekleme ve yapay zeka sohbet sistemi. Her modülün kuralları, akışları, güvenlik kontrolleri ve istisna durumları detaylandırılmıştır.

---

## İÇİNDEKİLER

1. [Uygulamanın Temel Amacı](#1-uygulamanın-temel-amacı)
2. [Temel İş Kuralları (Özet)](#2-temel-iş-kuralları-özet)
3. [Kullanıcı Yönetimi İş Mantığı](#3-kullanıcı-yönetimi-iş-mantığı)
4. [Not (Page) Yönetimi İş Mantığı](#4-not-page-yönetimi-iş-mantığı)
5. [Grup Yönetimi İş Mantığı](#5-grup-yönetimi-iş-mantığı)
6. [Dosya Ekleme (Attachment) İş Mantığı](#6-dosya-ekleme-attachment-iş-mantığı)
7. [AI Sohbet Sistemi İş Mantığı](#7-ai-sohbet-sistemi-iş-mantığı)
8. [Erişim Kontrolü ve Güvenlik Modeli](#8-erişim-kontrolü-ve-güvenlik-modeli)
9. [İş Akışları (Flowcharts)](#9-iş-akışları-flowcharts)
10. [İş Kuralları Matrisi](#10-iş-kuralları-matrisi)
11. [Bilinen İş Mantığı Sorunları](#11-bilinen-iş-mantığı-sorunları)
12. [Özet](#12-özet)
13. [Mantık Hataları Özet Listesi](#13-mantık-hataları-özet-listesi)

---

## 1. UYGULAMANIN TEMEL AMACI

**NoteP**, kullanıcıların dijital notlarını yönetmesini, bu notları **parola korumalı gruplar** halinde başkalarıyla paylaşmasını ve notlarını **bağlam olarak kullanarak** yapay zeka asistanı (Google Gemini) ile sohbet etmesini sağlayan bir Spring Boot backend uygulamasıdır.

### Temel Kullanıcı Rolleri

| Rol | Tanım |
| --- | ----- |
| **Not Sahibi (Owner)** | Bir notu oluşturan kullanıcı. Not üzerinde tam yetkiye sahiptir (güncelleme, silme, gruba ekleme, dosya yükleme). |
| **Grup Üyesi (Member)** | Bir gruba katılmış kullanıcı. Grubun notlarını görüntüleyebilir ve (mevcut tasarımda) kendi notlarını grup uç noktası üzerinden güncelleyebilir. |
| **Kullanıcı (Authenticated User)** | Sisteme kayıtlı ve JWT ile kimliği doğrulanmış kullanıcı. |

---

## 2. TEMEL İŞ KURALLARI (ÖZET)

| # | Kural | Açıklama |
| - | ----- | -------- |
| K1 | **Kayıt benzersizliği:** | E-posta **VE** isim aynı olan ikinci bir kullanıcı kaydedilemez. |
| K2 | **Parola güvenliği:** | Tüm parolalar (kullanıcı ve grup) BCrypt ile hash'lenerek saklanır. Düz metin asla veritabanına yazılmaz. |
| K3 | **Kimlik doğrulama:** | Korumalı tüm uç noktalar `Authorization: Bearer <token>` başlığı ile JWT token gerektirir. |
| K4 | **Not sahipliği:** | Bir notu yalnızca **sahibi** güncelleyebilir, silebilir, gruba ekleyebilir/çıkarabilir veya dosya yükleyebilir. |
| K5 | **Grup erişimi:** | Bir grubun içeriğini yalnızca **üyeler** görüntüleyebilir. Üye olmayanlar güvenlik ihlali hatası alır. |
| K6 | **Gruba katılım:** | Bir gruba katılabilmek için **grup adı** ve **doğru parola** gereklidir. |
| K7 | **Grup oluşturma:** | Grubu oluşturan kullanıcı otomatik olarak grubun ilk üyesi olur. |
| K8 | **Not-grup ilişkisi:** | Bir not **en fazla bir gruba** ait olabilir (veya hiçbir gruba ait olmayabilir). |
| K9 | **AI bağlam erişimi:** | AI sohbetinde bağlam olarak yalnızca kullanıcının **sahip olduğu** veya **üyesi olduğu gruba ait** notlar kullanılabilir. |
| K10 | **Sohbet sahipliği:** | Bir sohbet oturumuna (conversation) yalnızca **sahibi** erişebilir, görüntüleyebilir ve silebilir. |
| K11 | **Sohbet bağlamı:** | Aynı sohbete devam edilirken geçmişte seçilen sayfalar, yeni istekte `pageIds` gönderilmezse AI'a iletilmez (bağlam her istekte yeniden belirlenir). |
| K12 | **Not silme:** | Bir not silinirken Supabase'deki tüm ekli dosyaları da silinir. |
| K13 | **Grup üyesi güncelleme yetkisi:** | Bir grup üyesi, **sahibi olduğu ve belirtilen gruba ait** bir notu, üyesi olduğu grup üzerinden güncelleyebilir (`findByIdAndUserIdAndGroupId` ile doğrulanır). |
| K14 | **Grup parolası:** | Grup parolası değiştirilemez; görüntülenemez; yalnızca BCrypt ile doğrulanır. |
| K15 | **Kullanıcı profili:** | Kullanıcı yalnızca **parolasını** güncelleyebilir. İsim, soyisim ve e-posta değiştirilemez. |

---

## 3. KULLANICI YÖNETİMİ İŞ MANTIĞI

### 3.1 Kayıt Olma (Register)

**Uç Nokta:** `POST /api/auth/register` *(herkese açık)*

**İş Akışı:**

1. Kullanıcı `UserRequest` gönderir: `{ name, surName, eMail, password }`
2. **Benzersizlik kontrolü:** `existsByEMailAndName()` ile e-posta **ve** isim aynı olan kullanıcı var mı kontrol edilir:
   - Varsa → `"This User Already Existed."` hatası fırlatılır (400).
3. Parola **BCrypt** ile hash'lenir.
4. Kullanıcı veritabanına kaydedilir.
5. Başarılı yanıt: `{ "message": "Kullanıcı oluşturuldu." }`

**Önemli:** Benzersizlik kontrolü yalnızca e-posta değil, **e-posta + isim** kombinasyonu üzerinden yapılır. (Veritabanında e-posta sütunu `unique = true` olduğu için aynı e-posta ile farklı isimde kayıt olmaya çalışmak veritabanı hatasına yol açabilir — bkz. Bilinen Sorunlar.)

### 3.2 Giriş Yapma (Login)

**Uç Nokta:** `POST /api/auth/login` *(herkese açık)*

**İş Akışı:**

1. Kullanıcı `LoginRequest` gönderir: `{ eMail, password }`
2. E-posta ile kullanıcı aranır:
   - Bulunamazsa → `"User not found"` hatası (400).
3. Parola `passwordEncoder.matches()` ile doğrulanır:
   - **Eşleşmezse** → `"Hatalı şifre ya da e-posta!"` hatası (400).
   - **Eşleşirse** → JWT token üretilir (subject = e-posta, geçerlilik: 5 saat).
4. Yanıt olarak **yalnızca ham JWT token** (düz metin, JSON değil) döner.

**İş Kuralı:** Login yanıtı JSON nesnesi değil, ham string tokendır. Frontend bu token'ı olduğu gibi saklamalı ve `Authorization: Bearer <token>` olarak göndermelidir.

### 3.3 Profil Görüntüleme

**Uç Nokta:** `GET /api/auth/me` *(JWT gerekli)*

- `SecurityContextHolder` üzerinden e-posta alınır.
- Kullanıcı veritabanından yüklenir.
- `UserResponse` döner: `{ id, name, surName, eMail }`

### 3.4 Parola Güncelleme

**Uç Nokta:** `PUT /api/auth/update` *(JWT gerekli)*

**İş Akışı:**

1. Kullanıcı `UserUpdateRequest` gönderir: `{ password }`
2. Yeni parola BCrypt ile hash'lenir.
3. `updatePasswordByEmail()` sorgusu ile e-posta bazlı doğrudan veritabanı güncellemesi yapılır.
4. Başarılı yanıt: `{ "message": "Kullanıcı güncelleme başarılı." }`

**Kısıtlar:** Yalnızca parola güncellenir. İsim, soyisim ve e-posta değiştirilemez.

---

## 4. NOT (PAGE) YÖNETİMİ İŞ MANTIĞI

### 4.1 Not Oluşturma

**Uç Nokta:** `POST /api/pages/save` *(JWT gerekli)*

**İş Akışı:**

1. Kullanıcı `PageRequest` gönderir: `{ title, content }`
2. Kimliği doğrulanmış kullanıcı bulunur.
3. `Page` nesnesi oluşturulur; sahibi (`user`) otomatik olarak giriş yapan kullanıcıya atanır.
4. Not kaydedilir.
5. Başarılı yanıt: `{ "message": "Not oluşturma başarılı." }`

**Doğrulama Kuralları:** `title` boş olamaz (max 100 karakter), `content` boş olamaz (max 10000 karakter).

### 4.2 Notları Listeleme

**Uç Nokta:** `GET /api/pages/my-list` *(JWT gerekli)*

- Yalnızca **giriş yapan kullanıcının sahip olduğu** notlar döner.
- Grup içindeki notlar da dahil olmak üzere kullanıcının tüm notları listelenir.

### 4.3 Not Güncelleme

**Uç Nokta:** `PUT /api/pages/{id}` *(JWT gerekli)*

**İş Kuralları:**

- `findByIdAndUserId()` sorgusu kullanılır → yalnızca notun **sahibi** güncelleyebilir.
- Not bulunamazsa veya kullanıcı sahibi değilse → `"Page not found"` hatası (400).
- `PageMapper.updateEntityWithResponse()` yalnızca `title`, `content` ve `lastUpdateDate` alanlarını günceller.
- Notun sahibi (`user`), grubu (`group`) ve oluşturma tarihi (`createdDate`) **değiştirilmez**.

### 4.4 Not Silme

**Uç Nokta:** `DELETE /api/pages/{id}` *(JWT gerekli)*

**İş Akışı:**

1. Not ID ile bulunur; bulunamazsa → `"Page not found with id: <email>"` hatası.
2. **Sahiplik kontrolü:** `page.getUser().geteMail()` ile giriş yapan kullanıcının e-postası karşılaştırılır.
   - **Eşleşmezse** → `"GÜVENLİK İHLALİ: Başkasına ait bir notu silemezsiniz!"` hatası (400).
3. Eşleşirse → notun tüm ekli dosyaları Supabase'den silinir (`storageService.deleteFile()`).
4. Not veritabanından silinir.
5. Başarılı yanıt: `{ "message": "Silme işlemi başarılı." }`

### 4.5 Notu Gruba Ekleme

**Uç Nokta:** `PUT /api/pages/{pageId}/add-to-group/{groupId}` *(JWT gerekli)*

**İş Kuralları:**

| Koşul | Sonuç |
| ----- | ----- |
| Not bulunamazsa veya kullanıcı sahibi değilse | `"Page is not exist or You are not owner."` hatası |
| Grup bulunamazsa | `"Group is not found."` hatası |
| Kullanıcı grubun üyesi değilse | `"You are not member of this group!"` hatası |
| Tüm kontroller geçerse | Not grup ilişkisi kurulur |

**İş Mantığı Detayı:**
- `page.setGroup(group)` ile notun grubu atanır.
- `group.getPages().add(page)` ile grubun not listesine eklenir (çift yönlü ilişki).
- Not zaten başka bir gruptaysa, grup referansı **üzerine yazılır** (bir not en fazla bir gruba ait olabilir).

### 4.6 Notu Gruptan Çıkarma

**Uç Nokta:** `PUT /api/pages/{pageId}/remove-from-group` *(JWT gerekli)*

**İş Kuralları:**

| Koşul | Sonuç |
| ----- | ----- |
| Not bulunamazsa veya kullanıcı sahibi değilse | `"Page is not exist or You are not owner."` hatası |
| Not hiçbir gruba ait değilse | `"Page doesn't have a group"` hatası |
| Kullanıcı grubun üyesi değilse | `"You are not member of this group!"` hatası |
| Tüm kontroller geçerse | Not grup ilişkisi kaldırılır |

### 4.7 Gruptaki Notu Güncelleme (Grup Üyesi Yetkisi)

**Uç Nokta:** `PUT /api/groups/{id}/pages/{pageId}` *(JWT gerekli)*

**İş Kuralları:**

1. `existsByIdAndMembersId(groupId, userId)` ile kullanıcının grup üyeliği kontrol edilir.
   - Üye değilse → `"You are not member of this group!"` hatası.
2. **Sahiplik + grup aitliği kontrolü:** Üye ise not, `findByIdAndUserIdAndGroupId(pageId, userId, groupId)` ile doğrulanır — **not hem kullanıcının sahibi olduğu hem de belirtilen gruba ait olmalıdır**.
   - Değilse → `"Page not found or you are not owner!"` hatası.
3. `PageMapper.updateEntityWithResponse()` ile not güncellenir.

---

## 5. GRUP YÖNETİMİ İŞ MANTIĞI

### 5.1 Grup Oluşturma

**Uç Nokta:** `POST /api/groups/create` *(JWT gerekli)*

**İş Akışı:**

1. `groupRepository.findByName()` ile grup adının benzersizliği kontrol edilir:
   - Zaten varsa → `"Grup ismi kullanılmış!"` hatası.
2. Grup parolası BCrypt ile hash'lenir.
3. **Oluşturan kullanıcı otomatik olarak üye listesine eklenir.**
4. Grup kaydedilir.
5. Başarılı yanıt: `{ "message": "Grup Olusturma Basarılı!" }`

### 5.2 Grupları Listeleme

**Uç Nokta:** `GET /api/groups/my-groups` *(JWT gerekli)*

- `findByMembersId()` ile kullanıcının **üyesi olduğu** gruplar döner.
- Her grup yanıtı üyeleri ve notları (pages) ile birlikte döner.

### 5.3 Grup Detayını Görüntüleme

**Uç Nokta:** `GET /api/groups/{id}` *(JWT gerekli)*

**İş Kuralları:**

1. Grup ID ile bulunur; bulunamazsa → `"Grup Bulunamadı!"` hatası.
2. **Üyelik kontrolü:** Kullanıcının ID'si grubun üye listesinde var mı kontrol edilir.
   - **Üye değilse** → `"GUVENLIK IHLALI:Uyesı olmadıgınız bır grubun ıcerıgını goremezsınız!"` hatası.
3. Üye ise grup detayı (üyeler + notlar) döner.

### 5.4 Gruba Katılma

**Uç Nokta:** `POST /api/groups/join` *(JWT gerekli)*

**İş Akışı:**

1. `JoinRequest` gönderilir: `{ groupName, password }` — **Not:** ID değil, **grup adı** kullanılır.
2. Grup adı ile bulunur; bulunamazsa → `"Grup Bulunamadı!"` hatası.
3. **Zaten üyelik kontrolü:** Kullanıcı zaten üyeyse → `"KULLANICI ZATEN GRUBA UYE!"` hatası.
4. **Parola doğrulama:** `passwordEncoder.matches()` ile parola karşılaştırılır:
   - **Yanlışsa** → `"YANLIS SIFRE GIRDINIZ!"` hatası.
   - **Doğruysa** → kullanıcı üye listesine eklenir.
5. Başarılı yanıt: `{ "message": "Gruba Katılım Basarılı!" }`

### 5.5 Gruptan Ayrılma

**Uç Nokta:** `DELETE /api/groups/{id}` *(JWT gerekli)*

**İş Akışı:**

1. Grup ID ile bulunur; bulunamazsa → `"This group doesn't exist!"` hatası.
2. **Üyelik kontrolü** yapılır:
   - Üye değilse → `"You are not member"` hatası.
3. Üye ise:
   - Kullanıcı grubun üye listesinden çıkarılır.
   - Grup, kullanıcının `groups` listesinden çıkarılır (çift yönlü ilişki bakımı).
4. Grup kaydedilir.
5. Başarılı yanıt: `{ "message": "Ayrılma işlemi başarılı." }`

**Not:** Gruptan ayrılmak, gruba ait notları **silmez** veya kullanıcının sahipliğini değiştirmez. Notlar sahiplerinde kalır.

---

## 6. DOSYA EKLEME (ATTACHMENT) İŞ MANTIĞI

### 6.1 Dosya Yükleme

**Uç Nokta:** `POST /api/pages/{pageId}/attachments` *(JWT gerekli, multipart/form-data)*

**İş Akışı:**

1. Kullanıcı `pageId` ile nota erişmeye çalışır.
2. `findByIdAndUserId()` ile **sahiplik kontrolü** yapılır:
   - Not bulunamazsa veya kullanıcı sahibi değilse → `"Not bulunamadı veya bu işlem için yetkiniz yok."` hatası.
3. Dosya **Supabase S3'e** yüklenir:
   - Klasör yapısı: `pages/{pageId}/`
   - Dosya adı: `UUID + orijinal uzantı` (ör. `pages/5/a3f2c1b4-....pdf`)
   - Depolama: `notep-attachments` bucket (S3 uyumlu API)
4. Veritabanına `Attachment` kaydı eklenir: `{ fileName, fileUrl, fileType, fileSize, createdTime }`
5. `AttachmentResponse` döner.

**Sınırlamalar:**
- Maksimum dosya boyutu: **10MB/dosya**
- Maksimum istek boyutu: **15MB/istek**
- Dosya URL'si: `https://<proje-url>/storage/v1/object/public/notep-attachments/pages/{pageId}/{uuid}.{uzantı}`

### 6.2 Dosya Silme

**Uç Nokta:** `DELETE /api/pages/{pageId}/attachments/{attachmentId}` *(JWT gerekli)*

**İş Akışı:**

1. **Sahiplik kontrolü:** Not sayfasının sahibi kontrol edilir → yetkisizse `"Not bulunamadı veya bu işlem için yetkiniz yok."` hatası.
2. Ek dosyası ID ile bulunur; bulunamazsa → `"Dosya Bulunamadı."` hatası.
3. **Not-ek ilişkisi kontrolü:** Ek, belirtilen nota ait değilse → `"Bu dosya belirtilen nota ait değil!"` hatası.
4. Dosya **Supabase S3'ten** silinir.
5. Veritabanı kaydı silinir.
6. Başarılı yanıt: `{ "message": "Dosya Silme Basarili" }`

---

## 7. AI SOHBET SİSTEMİ İŞ MANTIĞI

### 7.1 Temel Kavramlar

| Terim | Açıklama |
| ----- | -------- |
| **Conversation** | Çoklu mesaj (multi-turn) sohbet oturumu. Kullanıcıya aittir. |
| **ChatMessage** | Tek bir sohbet mesajı. `Role.USER` (kullanıcı) veya `Role.ASSISTANT` (AI) olabilir. |
| **Bağlam (Context)** | AI'a iletilen not içerikleri: `title`, `content`, `fileUrls`. |
| **Geçmiş (History)** | Sohbetin son 20 mesajı, AI'a bağlam olarak iletilir. |

### 7.2 Sohbet Gönderme

**Uç Nokta:** `POST /api/chat` *(JWT gerekli)*

**İstek Formatı:**
```json
{
  "message": "Kullanıcı mesajı",
  "pageIds": [1, 2, 3],      // opsiyonel
  "conversationId": 1        // opsiyonel
}
```

**İş Akışı (Adım Adım):**

1. **Kimlik Doğrulama:** `SecurityContextHolder` üzerinden kullanıcı bulunur.

2. **Oturum Yönetimi:**
   - `conversationId` **verilmişse**:
     - `findByIdAndUserId(conversationId, userId)` ile sahiplik doğrulanır.
     - Kullanıcının kendi oturumu değilse → `"Geçersiz oturum veya yetkisiz erişim! ID: <id>"` hatası.
   - `conversationId` **verilmemişse** (null):
     - **Yeni Conversation** oluşturulur.
     - Başlık: Mesajın ilk 30 karakteri + `"..."` (mesaj 30 karakterden uzunsa).
     - Örn: `"Bu notlar hakkında ne düşünüyorsun?"` → başlık `"Bu notlar hakkında ne düşünüyorsun?"`

3. **Notları Conversation'a Bağlama:**
   - `pageRepository.findAllById(pageIds)` ile seçilen notlar çekilir.
   - `conversation.setPages(pages)` ile notlar `Conversation.pages` ilişkisine kaydedilir (ManyToMany).

4. **Not Bağlamlarını Toplama:**
   - `pageIds` boş veya null ise → bağlam yok (genel sohbet).
   - `pageIds` dolu ise her not için:
     - `findByIdAndUserOrGroupMember(pageId, userId)` ile erişim kontrolü yapılır.
     - **Erişim koşulları:**
       - Notun sahibi kullanıcıysa **VEYA**
       - Notun ait olduğu grubun üyesiyse.
     - Erişim yoksa → `"Not bulunamadı! Id: <pageId>"` hatası.
     - Erişim varsa bağlam paketi oluşturulur:
       ```java
       { "title": "...", "content": "...", "fileUrls": ["...", "..."] }
       ```

5. **Mesaj Geçmişini Yükleme:**
   - Sohbetin **son 20 mesajı** kronolojik sırayla (en eskiden en yeniye) çekilir.
   - Token yönetimi için 20 mesaj sınırı uygulanır.

6. **AI Yanıtı Üretme:**
   - `IAiService.generateResponse(userMessage, contexts, history)` çağrılır.
   - GeminiServiceImpl:
     - **System Prompt** oluşturur: AI'a NoteP asistanı rolü verilir, Türkçe yanıt vermesi istenir.
     - **Bağlam metni** oluşturur: Seçilen notlar `--- NOT 1 ---` formatında prompt'a eklenir.
     - **Medya dosyaları** yükler: Notlara ekli dosyaların URL'lerinden `UrlResource` ile `Media` nesneleri oluşturulur (görsel, PDF, metin vb.).
     - **Geçmiş mesajları** Spring AI mesajlarına dönüştürür: `ChatMessage` → `UserMessage`/`AssistantMessage`.
     - **Prompt** oluşturur: `SystemMessage` + geçmiş mesajlar + mevcut kullanıcı mesajı + medyalar.
     - `ChatModel.call(prompt)` ile Gemini 2.5 Flash'a gönderir.

7. **Mesajları Kaydetme:**
   - Kullanıcı mesajı `Role.USER` olarak kaydedilir.
   - AI yanıtı `Role.ASSISTANT` olarak kaydedilir.

8. **Yanıt:**
   ```json
   { "conversationId": 1, "response": "AI yanıt metni" }
   ```

### 7.3 Sohbet Listeleme

**Uç Nokta:** `GET /api/chat/conversations` *(JWT gerekli)*

- Kullanıcının **tüm** sohbet oturumları, `createdDate` **yeniden eskiye** sıralı döner.
- Her kayıt: `{ id, title, createdDate }`

### 7.4 Sohbet Mesajlarını Görüntüleme

**Uç Nokta:** `GET /api/chat/conversations/{id}` *(JWT gerekli)*

1. `existByIdAndUserId(conversationId, userId)` ile sahiplik kontrolü yapılır.
   - Sohbet yoksa veya kullanıcının değilse → `"Oturum bulunamadı veya yetkisiz erişim!"` hatası.
2. Mesajlar `createdDate` **eskiden yeniye** sıralı döner.
3. Her mesaj: `{ id, role, content, createdDate }`

### 7.5 Sohbet Silme

**Uç Nokta:** `DELETE /api/chat/conversations/{id}` *(JWT gerekli)*

1. `findByIdAndUserId(conversationId, userId)` ile sahiplik kontrolü yapılır.
   - Sohbet yoksa veya kullanıcının değilse → `"Oturum bulunamadı veya yetkisiz erişim!"` hatası.
2. Conversation silinir.
3. **Mesajlar otomatik silinir** (`orphanRemoval = true` sayesinde).
4. Yanıt: **204 No Content** (boş gövde).

### 7.6 AI Sohbetin Kritik İş Mantığı Detayları

| Detay | Açıklama |
| ----- | -------- |
| **Yeni sohbet başlığı** | Mesajın ilk 30 karakteri + "..." |
| **Geçmiş sınırı** | Son 20 mesaj AI'a iletilir (token yönetimi) |
| **Bağlam notu erişimi** | Sahip VEYA grup üyesi |
| **Sohbet erişimi** | Yalnızca sahibi |
| **Medya formatları** | PNG, JPEG, WebP, PDF, TXT (diğerleri varsayılan olarak PNG MIME ile denenir) |
| **Dil** | AI Türkçe yanıt verir |
| **Bağlam formatı** | `--- NOT n ---`, `Başlık:`, `İçerik:`, `Ekli Dosya Adresleri:` |
| **Dosya yükleme hatası** | Dosya indirilemezse atlanır, sohbet devam eder (kritik hata değil) |

---

## 8. ERİŞİM KONTROLÜ VE GÜVENLİK MODELİ

### 8.1 Kimlik Doğrulama (Authentication)

```
İstek → JWTFilter → Authorization: Bearer <token> → Token doğrula
     → SecurityContextHolder'a kullanıcı e-postası yaz → Servis katmanı kullanıcıyı bulur
```

- **JWT süresi:** 5 saat
- **JWT içeriği:** subject = e-posta
- **Hedef:** Tüm `/api/auth/**` hariç tüm uç noktalar JWT gerektirir

### 8.2 Yetkilendirme Matrisi (Authorization)

| İşlem | Not Sahibi | Grup Üyesi | Diğer Kullanıcı |
| ----- | ---------- | ---------- | --------------- |
| Notu güncelle (kendi notu) | ✅ | — | ❌ |
| Notu sil | ✅ | ❌ | ❌ |
| Notu gruba ekle | ✅ | ❌ | ❌ |
| Notu gruptan çıkar | ✅ | ❌ | ❌ |
| Nota dosya yükle | ✅ | ❌ | ❌ |
| Nota ait dosyayı sil | ✅ | ❌ | ❌ |
| Grup detayını görüntüle | — | ✅ | ❌ |
| Grup notlarını listele | — | ✅ (üyelik kontrolü ile) | ❌ |
| Gruptaki bir notu güncelle (grup üzerinden) | ✅ (kendi notu + grup üyesiyse) | ✅ (yalnızca kendi notu + grup üyesiyse) | ❌ |
| Gruba katıl (ad + parola) | — | Mevcut üye: ❌ | ✅ (doğru parolayla) |
| AI sohbette notu bağlam olarak kullan | ✅ | ✅ | ❌ |
| Sohbet oturumunu görüntüle/sil | — | — | Yalnızca sahibi |

### 8.3 Güvenlik Kontrolü Uygulanan Temel Sorgular

| Sorgu | Kullanım Amacı |
| ----- | -------------- |
| `findByIdAndUserId(pageId, userId)` | Not sahipliği kontrolü (güncelle, sil, gruba ekle, dosya işlemleri) |
| `findByIdAndUserIdAndGroupId(pageId, userId, groupId)` | Not sahipliği + grup aitliği kontrolü (grup üzerinden notu güncelleme) |
| `findByIdAndUserOrGroupMember(pageId, userId)` | AI bağlam erişimi: sahip VEYA grup üyesi (JPQL LEFT JOIN ile) |
| `existsByIdAndMembersId(groupId, userId)` | Grup üyeliği kontrolü (gruptaki notu güncelleme) |
| `findByIdAndUserId(conversationId, userId)` | Sohbet sahipliği kontrolü (devam et, görüntüle, sil) |
| `findByMembersId(userId)` | Kullanıcının üyesi olduğu grupları listele |

---

## 9. İŞ AKIŞLARI (FLOWCHARTS)

### 9.1 Kullanıcı Kayıt Akışı

```
Başla
  │
  ▼
UserRequest al (name, surName, eMail, password)
  │
  ▼
existsByEMailAndName() kontrolü
  │
  ├── Evet → "This User Already Existed." hatası → Bitiş
  │
  └── Hayır
        │
        ▼
Parolayı BCrypt ile hash'le
        │
        ▼
Kullanıcıyı kaydet → "Kullanıcı oluşturuldu." → Bitiş
```

### 9.2 Not Ekleme Akışı

```
Başla
  │
  ▼
JWT ile kimliği doğrulanmış kullanıcıyı bul
  │
  ▼
PageRequest al (title, content)
  │
  ▼
Page nesnesi oluştur → user = giriş yapan kullanıcı
  │
  ▼
pageRepository.save(page)
  │
  ▼
"Not oluşturma başarılı." → Bitiş
```

### 9.3 Notu Gruba Ekleme Akışı

```
Başla
  │
  ▼
Notu bul (findByIdAndUserId)
  │
  ├── Yok veya sahibi değil → "Page is not exist or You are not owner." → Bitiş
  │
  ▼
Grubu bul (findById)
  │
  ├── Yok → "Group is not found." → Bitiş
  │
  ▼
Kullanıcı grubun üyesi mi? (getMembers().contains(u))
  │
  ├── Hayır → "You are not member of this group!" → Bitiş
  │
  └── Evet
        │
        ▼
page.setGroup(group) → group.getPages().add(page)
        │
        ▼
pageRepository.save(page) + groupRepository.save(group)
        │
        ▼
"Not başarıyla gruba dahil edildi." → Bitiş
```

### 9.4 AI Sohbet Akışı

```
Başla
  │
  ▼
ChatRequest al (message, pageIds?, conversationId?)
  │
  ▼
Kimliği doğrulanmış kullanıcıyı bul
  │
  ▼
conversationId null mu?
  │
  ├── Evet → Yeni Conversation oluştur (başlık = ilk 30 karakter + "...")
  │
  └── Hayır → findByIdAndUserId ile sahipliği doğrula
                │
                └── Yok → "Geçersiz oturum veya yetkisiz erişim!" → Bitiş
  │
  ▼
pageIds boş mu?
  │
  ├── Evet → Bağlam yok (genel sohbet)
  │
  └── Hayır → Her pageId için findByIdAndUserOrGroupMember ile erişim kontrolü
                │
                └── Yok → "Not bulunamadı! Id: <id>" → Bitiş
  │
  ▼
Son 20 mesajı geçmiş olarak çek
  │
  ▼
aiService.generateResponse(message, contexts, history)
  │
  ▼
Kullanıcı mesajını (USER) + AI yanıtını (ASSISTANT) kaydet
  │
  ▼
ChatResponse(conversationId, response) dön → Bitiş
```

### 9.5 Grup Üyesi → Gruptaki Notu Güncelleme Akışı

```
Başla
  │
  ▼
existsByIdAndMembersId(groupId, userId) ile üyelik kontrolü
  │
  ├── Hayır → "You are not member of this group!" → Bitiş
  │
  ▼
findByIdAndUserIdAndGroupId(pageId, userId, groupId) ile sahiplik + grup aitliği kontrolü
  │
  ├── Yok → "Page not found or you are not owner!" → Bitiş
  │
  ▼
pageMapper.updateEntityWithResponse() ile güncelle
  │
  ▼
pageRepository.save() → "Update başarılı." → Bitiş
```

---

## 10. İŞ KURALLARI MATRİSİ

### 10.1 Kullanıcı İşlemleri

| İşlem | Giriş Gerekli | Sahiplik | Ekstra Koşullar |
| ----- | ------------- | -------- | --------------- |
| Register | ❌ | — | E-posta + isim benzersiz |
| Login | ❌ | — | Parola doğrulama |
| Profil görüntüle | ✅ | — | — |
| Parola güncelle | ✅ | — | Yalnızca parola |

### 10.2 Not İşlemleri

| İşlem | Sahip Yetkisi | Grup Üyesi Yetkisi | Not |
| ----- | ------------- | ------------------ | --- |
| Oluştur | ✅ | — | — |
| Listele (kendi) | ✅ | — | Yalnızca sahibin notları |
| Güncelle | ✅ | ✅ (kendi notları, üyesi olduğu grup üzerinden) | Notun gruba aitliği `findByIdAndUserIdAndGroupId` ile doğrulanıyor |
| Sil | ✅ | ❌ | Silmede ek dosyalar da Silinir |
| Gruba ekle | ✅ | ❌ | Not en fazla 1 grupta |
| Gruptan çıkar | ✅ | ❌ | — |
| Dosya yükle | ✅ | ❌ | 10MB/dosya, 15MB/istek |
| Dosya sil | ✅ | ❌ | — |

### 10.3 Grup İşlemleri

| İşlem | Koşullar |
| ----- | -------- |
| Oluştur | Ad benzersiz; oluşturan otomatik üye |
| Katıl | Ad + doğru parola; zaten üye olamaz |
| Ayrıl | Üye olmalı |
| Detay görüntüle | Üye olmalı |
| Notları listele | Üyelik kontrolü ile sayfaları döner (`existsByIdAndMembersId`) |
| Gruptaki notu güncelle | Grup üyesi + not sahibi olmalı |

### 10.4 Sohbet İşlemleri

| İşlem | Koşullar |
| ----- | -------- |
| Sohbet başlat/ devam et | Sohbet sahibi olmalı; bağlam notları sahip veya grup üyesi olmalı |
| Sohbetleri listele | Yalnızca kendi sohbetleri |
| Mesaj geçmişini görüntüle | Sohbet sahibi olmalı |
| Sohbeti sil | Sohbet sahibi olmalı; mesajlar da silinir |

---

## 11. BİLİNEN İŞ MANTIĞI SORUNLARI

Proje kodunda tespit edilen iş mantığı açıkları ve riskler:

### ~~11.1 Grup Uç Noktası Üzerinden Gruba Ait Olmayan Not Güncellenebiliyor~~ ✅ DÜZELTİLDİ

- **Durum:** `PUT /api/groups/{id}/pages/{pageId}` uç noktası artık `pageRepository.findByIdAndUserIdAndGroupId(pageId, userId, groupId)` sorgusunu kullanıyor. Notun **hem sahibi hem de belirtilen gruba ait** olması doğrulanıyor.
- **Çözüm:** `PageRepository.findByIdAndUserIdAndGroupId()` metodu eklendi; `GroupService.updatePageOfGroup()` bu metodu kullanıyor.
- **Yeni hata mesajı:** `"Page not found or you are not owner!"`

### ~~11.2 Grup Notlarını Listeleme Üyelik Kontrolü Yapmıyor~~ ✅ DÜZELTİLDİ

- **Durum:** `GET /api/groups/{id}/pages` uç noktası (`GroupService.getPages()`) artık `groupRepository.existsByIdAndMembersId(id, userId)` ile **üyelik kontrolü** yapıyor.
- **Çözüm:** Üye olmayan kullanıcılar `"You are not member of this group!"` hatası alıyor.

### 11.3 E-posta Benzersizlik Kontrolü Tutarsız

- **Durum:** Kayıt sırasında benzersizlik kontrolü `existsByEMailAndName()` (e-posta + isim) ile yapılıyor, ancak veritabanında `eMail` sütunu `unique = true` olduğu için aynı e-posta ile farklı isimde kayıt olmaya çalışmak `DataIntegrityViolationException` hatasına yol açabilir.
- **Öneri:** Benzersizlik kontrolü yalnızca e-posta üzerinden yapılmalı.

### ~~11.4 Grup Üyeliği Doğrulaması Yalnızca Güncelleme Akışını Korur~~ ✅ DÜZELTİLDİ

- **Durum:** Hem `PUT /api/groups/{id}/pages/{pageId}` hem de `GET /api/groups/{id}/pages` uç noktaları artık `existsByIdAndMembersId()` ile üyelik kontrolü yapıyor. Gruptan ayrılan kullanıcılar bu uç noktaları **kullanamaz**.
- **Çözüm:** `GroupService.getPages()` içine üyelik kontrolü eklendi (M2 düzeltmesiyle birlikte).

### 11.5 Kullanıcı Profili Güncellenemiyor

- **Durum:** Kullanıcı yalnızca parola güncelleyebiliyor; isim, soyisim ve e-posta değiştirilemiyor. E-posta değiştirilemediği için kullanıcılar hesap bilgilerini güncelleyemiyor.

### ~~11.6 Yeni Sohbette Bağlam Notları İletilmiyor~~ ✅ DÜZELTİLDİ

- **Durum:** `ChatService.handleChatWithContext()` artık `conversation.setPages(pages)` ile seçilen notları `Conversation.pages` ilişkisine kaydediyor.
- **Çözüm:** `pageRepository.findAllById(pageIds)` ile notlar çekilip `conversation.setPages(pages)` çağrılıyor.
- **Not:** `findAllById()` erişim kontrolü yapmadan tüm notları çeker; ancak bağlam oluşturma aşamasında `findByIdAndUserOrGroupMember()` ile erişim kontrolü ayrıca yapılmaktadır.

### 11.7 Medya MIME Tipi Tahmini

- **Durum:** Bilinmeyen uzantılı dosyalar (`GeminiServiceImpl`) varsayılan olarak `image/png` MIME tipiyle gönderiliyor.
- **Risk:** PDF dışındaki belgeler (örn. `.docx`, `.xlsx`) yanlış MIME tipiyle iletilir ve AI tarafından doğru işlenmeyebilir.

### 11.8 `GROUP_PAGES` Tablo Birleşimi (Bilinen TODO)

- **Durum:** Proje dokümantasyonundaki TODO listesinde "Page-Group çoktan çoğa ilişki" planlanmış ancak henüz uygulanmamıştır. Şu an bir not **tek bir gruba** ait olabilir.

---

## 12. ÖZET

NoteP'in iş mantığı üç ana kavram etrafında döner:

1. **Sahiplik (Ownership):** Notlar, dosyalar ve sohbet oturumları sahiplerine aittir. Çoğu güvenlik kontrolü sahiplik üzerine kuruludur (`findByIdAndUserId`).
2. **Üyelik (Membership):** Gruplar, parola korumalı topluluklardır. Grup içeriğine erişim üyelikle belirlenir. Grup üyeleri, gruba ait notları bağlam olarak AI sohbetinde kullanabilir.
3. **Bağlam Tabanlı AI Sohbeti (Context-Aware AI Chat):** Kullanıcı, notlarını seçerek Gemini ile çoklu mesaj sohbeti yapar. Erişim kontrolü veritabanı seviyesinde JPQL sorgusuyla sağlanır.

Bu üç kavram; kayıt/giriş akışları, not CRUD işlemleri, grup yönetimi, dosya depolama ve AI entegrasyonu olmak üzere beş ana modülde hayata geçirilmiştir.

---

## 13. MANTIK HATALARI ÖZET LİSTESİ

> Aşağıdaki liste, kod incelemesinde tespit edilen **tüm mantık hatalarını** tek bir yerde özetler. Detaylar için ilgili bölümlere bakınız.

| # | Öncelik | Dosya / Sınıf | Mantık Hatası | Durum |
| - | ------- | ------------- | ------------- | ----- |
| ~~M1~~ | ~~🔴 Yüksek~~ | `GroupService.updatePageOfGroup()` | ~~Grup üyeliği + not sahipliği doğrulanıyor ama notun o gruba ait olduğu kontrol edilmiyor~~ | ✅ **DÜZELTİLDİ** — Artık `findByIdAndUserIdAndGroupId(pageId, userId, groupId)` kullanılıyor; notun hem sahibi hem de belirtilen gruba ait olması doğrulanıyor. |
| ~~M2~~ | ~~🔴 Yüksek~~ | `GroupService.getPages()` | ~~Üyelik kontrolü yapılmadan grup notları listeleniyor~~ | ✅ **DÜZELTİLDİ** — Artık `existsByIdAndMembersId(id, userId)` ile üyelik kontrolü yapılıyor; üye olmayanlar `"You are not member of this group!"` hatası alıyor. |

| M3 | 🟠 Orta | `UserService.register()` / `UserRepository.existsByEMailAndName()` | E-posta benzersizlik kontrolü tutarsız | ❌ **AÇIK** — Kod `eMail + name` kombinasyonunu kontrol eder ancak DB'de `eMail` sütunu `unique = true`. Aynı e-posta ile farklı isimde kayıt denemesi `DataIntegrityViolationException` fırlatır. (Bölüm 11.3) |

| ~~M4~~ | ~~🟠 Orta~~ | `ChatService.handleChatWithContext()` / `Conversation` | ~~Yeni sohbette `Conversation.pages` (ManyToMany) ilişkisi hiçbir yerde set edilmiyor~~ | ✅ **DÜZELTİLDİ** — Artık `conversation.setPages(pages)` ile seçilen notlar `Conversation.pages` ilişkisine kaydediliyor. (Bölüm 11.6) |

| M5 | 🟠 Orta | `ChatService.handleChatWithContext()` | Yeni sohbet oluşturulduğunda `ChatResponse`'ta **yanlış conversationId dönebiliyor** | ❌ **AÇIK** — Koddaki `return new ChatResponse(conversationId, aiResponse)` ifadesi parametre olarak gelen `conversationId`'yi (null olabilir) döndürür; yeni oluşturulan sohbetin gerçek ID'si (`conversation.getId()`) yerine. Bu, yeni sohbette `conversationId: null` dönmesine yol açar. |

| M6 | 🟡 Düşük | `PageService.deleteById()` | Hata mesajında **yanlış değişken kullanılıyor** | ❌ **AÇIK** — `"Page not found with id: " + eMail` — hata mesajında not ID'si yerine kullanıcının e-postası yazılıyor. (Bölüm 4.4) |
| M7 | 🟡 Düşük | `GroupService.joinGroup()` | Gruba katılma başarılı olduğunda **group kaydedilmiyor** açıkça | ❌ **AÇIK** — `group.getMembers().add(u)` yapılıyor ancak `groupRepository.save()` çağrılmıyor (`@Transactional` sayesinde JPA dirty-checking ile kaydedilir, ancak işlem açık değilse risk vardır). |
| M8 | 🟡 Düşük | `GeminiServiceImpl.generateResponse()` | Bilinmeyen dosya uzantıları **varsayılan olarak `image/png`** MIME tipiyle gönderiliyor | ❌ **AÇIK** — `.docx`, `.xlsx` vb. dosyalar yanlış MIME tipiyle AI'a iletilir ve doğru işlenmeyebilir. (Bölüm 11.7) |
| M9 | 🟡 Düşük | `PageService.addToGroup()` | Not zaten bir gruptaysa, yeni gruba eklenirken **eski gruptan çıkarılmıyor** | ❌ **AÇIK** — `page.setGroup(newGroup)` yapılır ama eski grubun `pages` listesinden not silinmez. Çift yönlü ilişki tutarsız kalır. (Bölüm 4.5) |
| M10 | 🟡 Düşük | `GroupService.leaveFromGroup()` | Gruptan ayrılma işleminde **kullanıcının `groups` listesi** `u.getGroups().remove(group)` ile düzeltiliyor | ❌ **AÇIK** — `User.groups` ilişkisi Lazy yüklüyse (default ManyToMany Lazy), bu erişim `LazyInitializationException` fırlatabilir. |
| M11 | 🟡 Düşük | `PageService.removeFromGroup()` | Notu gruptan çıkarırken **üyelik kontrolünden önce** notun bulunması gerekir; ancak gruptan çıkaran kullanıcı notun sahibi olmalı | ❌ **AÇIK** — Grup üyeleri (sahip olmayanlar) bu uç noktayı kullanamaz — mantıksal olarak grup üyesi olan herkesin, gruba ait bir notu gruptan çıkarabilmesi beklenebilir. |
| M12 | 🟡 Düşük | `ChatService.getConversationMessages()` | Kullanıcı, sohbet mesajlarını görüntülemeden **önce `sahipliği doğruluyor** ama `Conversation` yüklenmiyor | ❌ **AÇIK** — `existByIdAndUserId()` sadece boolean döner; mesaj listesi yüklenirken konuşmanın gerçekten kullanıcıya ait olduğu veritabanı düzeyinde doğrulanmıyor. |

### Durum Dağılımı

- ✅ **Düzeltildi (3):** M1, M2, M4 — Güvenlik açıkları ve veri tutarlılığı sorunları kapatıldı.
- 🟠 **Orta (2):** M3, M5 — Veri tutarlılığı ve işlevsellik sorunları (açık).
- 🟡 **Düşük (7):** M6–M12 — Hata mesajı, ilişki bakımı ve iyileştirme önerileri (açık).

### Kalan En Kritik 3 Düzeltme Önerisi

1. **M5:** `return new ChatResponse(conversation.getId(), aiResponse)` şeklinde düzeltilmeli — böylece yeni sohbette gerçek conversation ID döner.
2. **M3:** Benzersizlik kontrolü yalnızca e-posta üzerinden yapılmalı (`existsByEMail()`).
3. **M6:** `deleteById()` hata mesajındaki `eMail` değişkeni not ID'si ile değiştirilmeli.

---

*Bu doküman, NoteP projesinin mevcut kaynak kodundan (service, controller, entity ve repository katmanları) çıkarılmıştır. İş mantığında yapılacak herhangi bir değişiklikte bu dokümanın güncellenmesi önerilir.*
