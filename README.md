# NoteP

NoteP, Spring Boot ile gelistirilmis JWT tabanli bir not ve grup yonetimi backend projesidir. Kullanici kaydi/girisi, kisisel not olusturma, notlari listeleme, guncelleme, silme, notlari sifreli gruplara ekleme ve dosya (attachment) yukleme gibi temel islemleri destekler.

## Ozellikler

- Kullanici kaydi ve girisi
- BCrypt ile parola hashleme
- JWT ile kimlik dogrulama
- Kisisel not olusturma, listeleme, guncelleme ve silme
- Grup olusturma ve sifre ile gruba katilma
- Notlari gruba ekleme ve gruptan cikarma
- Notlara dosya (attachment) ekleme ve silme
- Supabase S3 uyumlu storage ile dosya saklama
- DTO tabanli request/response modeli
- Bean Validation destegi
- Swagger/OpenAPI dokumantasyonu

## Teknolojiler

- Java 21
- Spring Boot 4.0.3
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Spring Data REST
- MySQL / PostgreSQL
- JJWT
- Springdoc OpenAPI / Swagger UI
- Lombok
- AWS SDK S3 (Supabase S3 uyumlu API)
- Maven

## Gereksinimler

- Java 21 veya uzeri
- Maven Wrapper proje icinde mevcut oldugu icin ayrica Maven kurulu olmasi zorunlu degildir
- MySQL veya PostgreSQL Server

## Konfigurasyon

Uygulama varsayilan olarak `src/main/resources/application.properties` dosyasindaki ayarlari kullanir.

Temel ayarlar:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/notep_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=...
server.port=${PORT:8081}
jwt.secretSTR=...
springdoc.enable-data-rest=false

# Supabase S3 Storage
supabase.s3.endpoint=https://<project>.supabase.co/storage/v1/s3
supabase.s3.region=eu-central-1
supabase.s3.access-key=...
supabase.s3.secret-key=...
supabase.s3.bucket-name=notep-attachments

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=15MB
```

Not: Render gibi ortamlarda PostgreSQL kullanilir. O durumda `spring.datasource.url`, `spring.datasource.username` ve `spring.datasource.password` degerleri environment variable olarak verilmelidir.

Guvenlik notu: Gercek ortamda veritabani sifresi ve JWT secret gibi hassas degerleri dogrudan repo icinde tutmak yerine environment variable veya secret manager ile yonetmek daha dogrudur.

## Calistirma

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Uygulama varsayilan olarak su adreste calisir:

```text
http://localhost:8081
```

Not: Root `/` endpoint'i tanimli olmadigi ve guvenlik altinda oldugu icin `http://localhost:8081/` adresinde 403 gorulebilir. API ve Swagger endpointleri kullanilmalidir.

Proje [Render](https://render.com) uzerinde PostgreSQL ile deploy edilmistir. Degisiklikler GitHub'a pushlandiktan sonra otomatik olarak derlenir ve deploy edilir.

## Swagger / OpenAPI

Swagger UI:

```text
http://localhost:8081/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8081/v3/api-docs
```

JWT gerektiren endpointleri Swagger uzerinden test etmek icin:

1. `/api/auth/register` ile kullanici olustur.
2. `/api/auth/login` ile token al.
3. Swagger UI'daki `Authorize` butonuna tikla.
4. Token'i gir.
5. Korumali endpointleri test et.

## Kimlik Dogrulama

Public endpointler:

- `POST /api/auth/register`
- `POST /api/auth/login`

Diger endpointler JWT token gerektirir.

Header formati:

```http
Authorization: Bearer <jwt-token>
```

## API Endpointleri

### Auth

| Method | Endpoint | Aciklama |
| --- | --- | --- |
| POST | `/api/auth/register` | Yeni kullanici kaydi |
| POST | `/api/auth/login` | Kullanici girisi ve JWT token alma |
| PUT | `/api/auth/update` | Giris yapan kullaniciyi guncelleme (sifre) |
| GET | `/api/auth/me` | Giris yapan kullanicinin profil bilgileri |

Register request ornegi:

```json
{
  "name": "Ali",
  "surName": "Yilmaz",
  "eMail": "ali@example.com",
  "password": "123456"
}
```

Login request ornegi:

```json
{
  "eMail": "ali@example.com",
  "password": "123456"
}
```

### Pages

| Method | Endpoint | Aciklama |
| --- | --- | --- |
| POST | `/api/pages/save` | Yeni not olusturma |
| GET | `/api/pages/my-list` | Giris yapan kullanicinin notlarini listeleme |
| PUT | `/api/pages/{id}` | Not guncelleme |
| DELETE | `/api/pages/{id}` | Not silme |
| PUT | `/api/pages/{pageId}/add-to-group/{groupId}` | Notu gruba ekleme |
| PUT | `/api/pages/{pageId}/remove-from-group` | Notu gruptan cikarma |
| POST | `/api/pages/{pageId}/attachments` | Nota dosya yukleme |
| DELETE | `/api/pages/{pageId}/attachments/{attachmentId}` | Notdan dosya silme |

Page request ornegi:

```json
{
  "title": "Ders Notu",
  "content": "Spring Boot calisma notlari"
}
```

### Groups

| Method | Endpoint | Aciklama |
| --- | --- | --- |
| POST | `/api/groups/create` | Yeni grup olusturma |
| GET | `/api/groups/my-groups` | Giris yapan kullanicinin gruplarini listeleme |
| GET | `/api/groups/{id}` | Grup detayini getirme |
| POST | `/api/groups/join` | Sifre ile gruba katilma (groupName + password) |
| DELETE | `/api/groups/{id}` | Gruptan ayrilma |
| GET | `/api/groups/{id}/pages` | Gruptaki tum sayfalari listeleme |
| PUT | `/api/groups/{id}/pages/{pageId}` | Gruptaki bir sayfayi guncelleme |

Group create request ornegi:

```json
{
  "name": "Backend Ekibi",
  "password": "group123"
}
```

Join group request ornegi:

```json
{
  "groupName": "Backend Ekibi",
  "password": "group123"
}
```

## Test

```bash
.\mvnw.cmd test
```

## Proje Yapisi

```text
src/main/java/com/example/demo
  AuthenticationElements/   JWT, login request/response ve filtreler
  Bussiness/                Servis katmani (UserService, PageService, GroupService, IStorageService, SupabaseServiceImpl)
  Controllers/              REST controller siniflari
  DTOs/                     Request/response modelleri ve mapper siniflari
  DataAccess/               Repository arayuzleri
  Entities/                 JPA entity siniflari (User, Page, Group, Attachment)
  ExceptionHandling/        Global exception handler
  SecurityConfig.java       Spring Security filter chain yapilandirmasi
  OpenApiConfig.java        Swagger/OpenAPI yapilandirmasi (Bearer JWT)
  StorageConfig.java        Supabase S3 istemci yapilandirmasi
  NotePApplication.java     Ana uygulama sinifi
```

## Notlar

- `springdoc.enable-data-rest=false` ayari, Springdoc'un Spring Data REST repositorylerini otomatik dokumante etmesini kapatir.
- Swagger endpointleri Spring Security icinde `permitAll()` olarak tanimlanmistir.
- URL'lerde cift slash kullanilmamalidir. Ornegin `//v3/api-docs` Spring Security tarafindan reddedilebilir.
- Dosya yuklemeleri Supabase S3 uyumlu storage uzerinde `pages/{pageId}/` klasorune kaydedilir.
- Dosya boyutu siniri: max 10MB (tek dosya), max 15MB (toplam istek).