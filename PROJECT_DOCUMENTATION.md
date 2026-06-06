# NoteP Project - Complete Documentation

> **Goal:** This file contains everything another LLM needs to know about this project in a single document. Use this to ask questions, generate code, or understand the architecture.

---

## 1. PROJECT OVERVIEW

**NoteP** is a Spring Boot backend application for note-taking and group management. It allows users to register/login, create personal notes, organize notes into password-protected groups, and collaborate with other users.

- **Java Version:** 21
- **Spring Boot Version:** 4.0.3
- **Build Tool:** Maven (with Maven Wrapper: `mvnw` / `mvnw.cmd`)
- **Base Package:** `com.example.demo`
- **Main Class:** `com.example.demo.NotePApplication`
- **Application Name:** NoteP
- **GitHub:** https://github.com/mirza-ceng/NoteP.git

### Tech Stack

| Category            | Technology                                      |
| ------------------- | ----------------------------------------------- |
| Framework           | Spring Boot 4.0.3                               |
| Web Layer           | Spring Web MVC (REST)                           |
| Security            | Spring Security + JWT (jjwt 0.12.5)             |
| Database            | JPA / Hibernate                                 |
| Database Drivers    | MySQL (local), PostgreSQL (production/Render)   |
| Validation          | Hibernate Validator (Bean Validation)           |
| API Documentation   | Springdoc OpenAPI 3.0.3 (Swagger UI)            |
| Testing             | Spring Boot Starter Test                        |

---

## 2. DATABASE INFO

### Dual-Database Setup (Smart Detection)

The application supports both MySQL (local development) and PostgreSQL (production on Render). It picks the correct driver automatically based on the `DB_URL` environment variable.

**application.properties logic:**
```properties
# If DB_URL env var is set → PostgreSQL; otherwise → MySQL (localhost)
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/notep_db?createDatabaseIfNotExist=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:mirza6445}
spring.datasource.driver-class-name=   # Auto-detected from URL
spring.jpa.database-platform=${DB_DIALECT:org.hibernate.dialect.MySQLDialect}
spring.jpa.hibernate.ddl-auto=update
server.port=${PORT:8080}
```

### Tables

| Table Name       | Entity | Description              |
| ---------------- | ------ | ------------------------ |
| `app_user`       | User   | Registered users         |
| `page`           | Page   | Notes/pages              |
| `app_group`      | Group  | Groups (password-protected) |
| `group_members`  | (join) | Many-to-many: User ↔ Group |

---

## 3. ENTITY RELATIONSHIPS (JPA)

### User Entity (`Entities/User.java`)
```java
@Entity @Table(name = "app_user") @DynamicUpdate
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;       // nullable = false
    String surName;    // nullable = false
    String eMail;      // nullable = false, unique = true
    String password;   // nullable = false

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Page> pages = new ArrayList<>();

    @ManyToMany(mappedBy = "members")
    List<Group> groups = new ArrayList<>();
}
```

### Page Entity (`Entities/Page.java`)
```java
@Entity @Table(name = "page") @EntityListeners(AuditingEntityListener.class)
public class Page {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String title;        // nullable = false
    @Lob @Column(columnDefinition = "TEXT")
    String content;
    @CreatedDate LocalDateTime createdDate;     // updatable = false
    @LastModifiedDate LocalDateTime lastUpdateDate;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_id")
    Group group;       // nullable - a page may not be in any group
}
```

### Group Entity (`Entities/Group.java`)
```java
@Entity @Table(name = "app_group")
public class Group {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String name;        // nullable = false
    String password;    // nullable = false (BCrypt-hashed)

    @ManyToMany
    @JoinTable(name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    List<User> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    List<Page> pages = new ArrayList<>();
}
```

### Summary of Relationships

```
User ──OneToMany──→ Page          (A user has many pages)
User ──ManyToMany→ Group          (A user belongs to many groups, a group has many users)
Group ──OneToMany──→ Page         (A group has many pages)
Page ──ManyToOne──→ User          (A page belongs to one user/owner)
Page ──ManyToOne──→ Group         (A page belongs to one group, nullable)
```

---

## 4. LAYERED ARCHITECTURE

```
┌──────────────────────────────────────────────────┐
│                  Controllers                      │  (REST API endpoints)
├──────────────────────────────────────────────────┤
│                 Bussiness (Services)              │  (Business logic)
├──────────────────────────────────────────────────┤
│                   DTOs                           │  (Request/Response objects & Mappers)
├──────────────────────────────────────────────────┤
│                DataAccess (Repositories)          │  (JPA Repositories)
├──────────────────────────────────────────────────┤
│                   Entities                       │  (JPA Entity classes)
└──────────────────────────────────────────────────┘
```

### Package Structure

```
src/main/java/com/example/demo/
├── AuthenticationElements/    ← JWT utilities, login DTOs, auth filter
│   ├── JWTFilter.java         ← OncePerRequestFilter: validates JWT on every request
│   ├── JWTUtil.java           ← Token generation, extraction, validation
│   ├── LoginRequest.java      ← { eMail, password }
│   └── LoginResponse.java     ← { token, message, user (UserResponse) }
│
├── Bussiness/                 ← Service layer (business logic)
│   ├── UserService.java       ← User CRUD, auth, implements UserDetailsService
│   ├── GroupService.java      ← Group CRUD, membership, page management
│   └── PageService.java       ← Page CRUD, group assignment
│
├── Controllers/               ← REST endpoints
│   ├── UserController.java    ← /api/auth/**
│   ├── PageController.java    ← /api/pages/**
│   └── GroupController.java   ← /api/groups/**
│
├── DTOs/                      ← Data Transfer Objects
│   ├── IMapper.java           ← Generic mapper interface: <Response, Entity> → toResponse(Entity)
│   ├── UserRequest.java       ← Register payload
│   ├── UserResponse.java      ← User profile output
│   ├── UserUpdateRequest.java ← Password update payload
│   ├── PageRequest.java       ← Create/update page payload
│   ├── PageResponse.java      ← Page output (includes groupId, ownerId, ownerName)
│   ├── GroupRequest.java      ← Create group payload
│   ├── GroupResponse.java     ← Group output (includes members & pages lists)
│   ├── JoinRequest.java       ← Join group payload (groupName + password)
│   ├── UserMapper.java        ← User ↔ UserResponse/UserRequest
│   ├── PageMapper.java        ← Page ↔ PageResponse/PageRequest
│   └── GroupMapper.java       ← Group ↔ GroupResponse/GroupRequest
│
├── DataAccess/                ← Spring Data JPA Repositories
│   ├── UserRepository.java    ← findByEMail, existsByEMailAndName, updatePasswordByEmail
│   ├── PageRepository.java    ← findByIdAndUserId, findByUserId, findByGroupId, findByUserIdAndGroupId
│   └── GroupRepository.java   ← findByName, findByMembersId, isMember
│
├── Entities/                  ← JPA entities
│   ├── User.java              → table: app_user
│   ├── Page.java              → table: page
│   └── Group.java             → table: app_group
│
├── ExceptionHandling/
│   └── GlobalExceptionHandler.java  ← @ControllerAdvice exception handler
│
├── SecurityConfig.java        ← Spring Security filter chain config
├── OpenApiConfig.java         ← Swagger/OpenAPI config with Bearer JWT auth
└── NotePApplication.java      ← Main class (@SpringBootApplication, @EnableJpaAuditing)
```

---

## 5. SECURITY

### Authentication Flow

1. **Register:** `POST /api/auth/register` → password is BCrypt-hashed → saved to DB
2. **Login:** `POST /api/auth/login` → password verified with BCrypt → JWT token returned as **raw string**
3. **Authenticated Requests:** Every subsequent request must include header: `Authorization: Bearer <token>`

### JWT Details

- **Library:** `io.jsonwebtoken` (jjwt) 0.12.5
- **Algorithm:** HMAC-SHA256 (via `Keys.hmacShaKeyFor`)
- **Secret Key:** From `application.properties` → `jwt.secretSTR`
- **Expiration:** 5 hours from generation
- **Claims:** `subject` = user's email (eMail)
- **Token in response:** `login` endpoint returns the token as a **raw string** (not JSON), not as a JSON object.

### Security Configuration (`SecurityConfig.java`)

```java
http
  .csrf(csrf -> csrf.disable())
  .httpBasic(httpBasic -> httpBasic.disable())
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
    .requestMatchers("/api/auth/**").permitAll()   // register + login are public
    .anyRequest().authenticated()                   // everything else needs JWT
  )
  .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

### JWT Filter (`JWTFilter.java`)

- Extends `OncePerRequestFilter`
- Reads `Authorization` header (expects `Bearer <token>` format)
- Extracts email from token using `JWTUtil.extractEMail()`
- Validates token with `JWTUtil.validateToken()`
- Sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
- On invalid/expired token → Spring Security returns 401/403 automatically

### BCryptPasswordEncoder

- Defined as a `@Bean` in `SecurityConfig`
- Used in `UserService.register()` to hash passwords
- Used in `UserService.logIn()` to verify passwords via `passwordEncoder.matches()`
- Used in `GroupService` to hash and verify group passwords

### How Authentication Works in Services

Each service method that needs authentication calls:
```java
private User getAuthanticatedUser() {
    String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByEMail(eMail)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
}
```

This retrieves the currently authenticated user from the security context (set by JWTFilter).

---

## 6. COMPLETE API ENDPOINTS REFERENCE

### 6.1 Auth Endpoints (`/api/auth/`)

| Method | Endpoint          | Auth  | Description                          |
| ------ | ----------------- | ----- | ------------------------------------ |
| POST   | `/api/auth/register` | ❌ No  | Register a new user                  |
| POST   | `/api/auth/login`    | ❌ No  | Login, returns JWT token (raw string)|
| PUT    | `/api/auth/update`   | ✅ Yes | Update current user's password       |
| GET    | `/api/auth/me`       | ✅ Yes | Get current user's profile           |

#### POST `/api/auth/register`
```json
// Request
{
  "name": "Ali",
  "surName": "Yilmaz",
  "eMail": "ali@example.com",
  "password": "123456"
}
// Success Response (200)
{ "message": "Kullanıcı oluşturuldu." }
// Error (400)
{ "error": "This User Already Existed.", "status": 400, "path": "/api/auth/register" }
```

#### POST `/api/auth/login`
```json
// Request
{
  "eMail": "ali@example.com",
  "password": "123456"
}
// Success Response (200) - IMPORTANT: Returns raw string, NOT JSON
<eyJhbGciOiJIUzI1NiJ9...>   (the JWT token as plain text)
// Error (400)
{ "error": "Hatalı şifre ya da e-posta!", "status": 400, "path": "/api/auth/login" }
```

#### PUT `/api/auth/update`
```json
// Request (only password can be updated)
{ "password": "newpassword123" }
// Success Response (200)
{ "message": "Kullanıcı güncelleme başarılı." }
```

#### GET `/api/auth/me`
```json
// Success Response (200)
{
  "id": 1,
  "name": "Ali",
  "surName": "Yilmaz",
  "eMail": "ali@example.com"
}
```

### 6.2 Page Endpoints (`/api/pages/`) - All require JWT

| Method | Endpoint                                    | Description                          |
| ------ | ------------------------------------------- | ------------------------------------ |
| POST   | `/api/pages/save`                           | Create a new page (note)             |
| GET    | `/api/pages/my-list`                        | List current user's pages            |
| PUT    | `/api/pages/{id}`                           | Update a page (only owner)           |
| DELETE | `/api/pages/{id}`                           | Delete a page (only owner)           |
| PUT    | `/api/pages/{pageId}/add-to-group/{groupId}` | Add page to a group                  |
| PUT    | `/api/pages/{pageId}/remove-from-group`     | Remove page from its group           |

#### POST `/api/pages/save`
```json
// Request
{
  "title": "Ders Notu",
  "content": "Spring Boot çalışma notları"
}
// Success Response (200)
{ "message": "Not oluşturma başarılı." }
```

#### GET `/api/pages/my-list`
```json
// Success Response (200)
[
  {
    "id": 1,
    "title": "Ders Notu",
    "content": "Spring Boot çalışma notları",
    "groupId": null,
    "ownerId": 1,
    "ownerName": "Ali"
  }
]
```

#### PUT `/api/pages/{id}`
```json
// Request
{
  "title": "Yeni Başlık",
  "content": "Güncellenmiş içerik"
}
// Success Response (200)
{ "message": "Update başarılı." }
// Error (400) - if page not found or not owned by user
{ "error": "Page not found ", "status": 400, "path": "/api/pages/1" }
```

#### DELETE `/api/pages/{id}`
```json
// Success Response (200)
{ "message": "Silme işlemi başarılı." }
// Error (400)
{ "error": "GÜVENLİK İHLALİ: Başkasına ait bir notu silemezsiniz!", "status": 400, "path": "/api/pages/1" }
```

#### PUT `/api/pages/{pageId}/add-to-group/{groupId}`
```json
// No request body
// Success Response (200)
{ "message": "Not başarıyla gruba dahil edildi." }
// Possible errors:
// "Page is not exist or You are not owner." (400)
// "Group is not found." (400)
// "You are not member of this group!" (400)
```

#### PUT `/api/pages/{pageId}/remove-from-group`
```json
// No request body
// Success Response (200)
{ "message": "Not gruptan çıkarıldı." }
// Possible errors:
// "Page doesn't have a group" (400)
// "You are not member of this group!" (400)
```

### 6.3 Group Endpoints (`/api/groups/`) - All require JWT

| Method | Endpoint                              | Description                              |
| ------ | ------------------------------------- | ---------------------------------------- |
| POST   | `/api/groups/create`                  | Create a new group                       |
| GET    | `/api/groups/my-groups`               | List current user's groups               |
| GET    | `/api/groups/{id}`                    | Get group details (must be member)       |
| POST   | `/api/groups/join`                    | Join a group with password               |
| DELETE | `/api/groups/{id}`                    | Leave (exit) a group                     |
| GET    | `/api/groups/{id}/pages`              | List all pages in a group                |
| PUT    | `/api/groups/{id}/pages/{pageId}`     | Update a page inside a group             |

#### POST `/api/groups/create`
```json
// Request
{
  "name": "Backend Ekibi",
  "password": "group123"
}
// The creator automatically becomes a member
// Success Response (200)
{ "message": "Grup Olusturma Basarılı!" }
// Error (400)
{ "error": "Grup ismi kullanılmış!", "status": 400, "path": "/api/groups/create" }
```

#### GET `/api/groups/my-groups`
```json
// Success Response (200)
[
  {
    "id": 1,
    "name": "Backend Ekibi",
    "members": [
      {"id": 1, "name": "Ali", "surName": "Yilmaz", "eMail": "ali@example.com"}
    ],
    "pages": []
  }
]
```
Note: This only returns groups the current user is a member of. Pages list may be empty.

#### GET `/api/groups/{id}`
```json
// Success Response (200)
{
  "id": 1,
  "name": "Backend Ekibi",
  "members": [
    {"id": 1, "name": "Ali", "surName": "Yilmaz", "eMail": "ali@example.com"},
    {"id": 2, "name": "Ayşe", "surName": "Kaya", "eMail": "ayse@example.com"}
  ],
  "pages": [
    {
      "id": 1,
      "title": "Ders Notu",
      "content": "Spring Boot notları",
      "groupId": 1,
      "ownerId": 1,
      "ownerName": "Ali"
    }
  ]
}
// Error (400)
{ "error": "GUVENLIK IHLALI:Uyesı olmadıgınız bır grubun ıcerıgını goremezsınız!", "status": 400, "path": "/api/groups/2" }
```

#### POST `/api/groups/join`
```json
// Request
{
  "groupName": "Backend Ekibi",
  "password": "group123"
}
// Success Response (200)
{ "message": "Gruba Katılım Basarılı!" }
// Possible errors:
// "Grup Bulunamadı!" (400)
// "KULLANICI ZATEN GRUBA UYE!" (400)
// "YANLIS SIFRE GIRDINIZ!" (400)
```

#### DELETE `/api/groups/{id}` (Leave Group)
```json
// No request body - removes current user from the group
// Success Response (200)
{ "message": "Ayrılma işlemi başarılı." }
// Possible errors:
// "This group doesn't exist!" (400)
// "You are not member" (400)
```

#### PUT `/api/groups/{id}/pages/{pageId}`
```json
// Request (same as PageRequest)
{
  "title": "Güncellenmiş Başlık",
  "content": "Güncellenmiş içerik"
}
// Success Response (200)
{ "message": "Update başarılı." }
// Error (400)
{ "error": "You are not member of this group!", "status": 400, "path": "/api/groups/1/pages/5" }
```
This endpoint allows any **group member** (not just the page owner) to update a page that is part of a group.

---

## 7. VALIDATION RULES

| DTO                  | Field     | Rules                                |
| -------------------- | --------- | ------------------------------------ |
| **UserRequest**      | name      | @NotBlank, max 50                    |
|                      | surName   | @NotBlank, max 50                    |
|                      | eMail     | @NotBlank, @Email, max 100           |
|                      | password  | @NotBlank, min 6, max 100            |
| **UserUpdateRequest**| password  | @NotBlank, min 6, max 100            |
| **PageRequest**      | title     | @NotBlank, max 100                   |
|                      | content   | @NotBlank, max 10000                 |
| **GroupRequest**     | name      | @NotBlank                            |
|                      | password  | @NotBlank                            |
| **JoinRequest**      | groupName | @NotBlank                            |
|                      | password  | @NotBlank                            |

---

## 8. EXCEPTION HANDLING

Handled by `GlobalExceptionHandler.java` (a `@ControllerAdvice` class):

| Exception               | HTTP Status | Response Format                                  |
| ----------------------- | ----------- | ------------------------------------------------ |
| RuntimeException        | 400         | `{ "error": "<message>", "status": 400, "path": "..." }` |
| IllegalArgumentException | 400         | `{ "error": "<message>", "status": 400, "path": "..." }` |
| Exception (generic)     | 500         | `{ "error": "Internal Server Error", "status": 500, "path": "..." }` |
| No/invalid JWT token    | 401/403     | Handled by Spring Security (not custom handler) |

---

## 9. SWAGGER / OPENAPI

- **URL:** `http://localhost:8080/swagger-ui.html`
- **JSON Spec:** `http://localhost:8080/v3/api-docs`
- **Config file:** `OpenApiConfig.java`
- **Auth method:** Bearer JWT (configured via SecurityScheme in OpenApiConfig)
- **Security:** Swagger endpoints are `permitAll()` in SecurityConfig

### How to test with Swagger:
1. Register a user via `/api/auth/register`
2. Login via `/api/auth/login` → copy the token (raw string)
3. Click "Authorize" button in Swagger UI → paste token
4. Test any protected endpoint

---

## 10. CORS

- **Controllers:** CORS is enabled on all controllers via `@CrossOrigin` or `@CrossOrigin(origins = "*")`
- This allows frontend applications on different origins to access the API.

---

## 11. DEPLOYMENT

### Docker (Multi-stage build)
```dockerfile
# Build stage: maven:3.9-eclipse-temurin-21
# Runtime stage: eclipse-temurin:21-jre
# Final artifact: /app/app.jar (from target/app.jar)
```

### Production (Render)
- Deployed on Render.com with PostgreSQL
- Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `jwt.secretSTR`
- Automatic deploy from GitHub on push

### Local Run
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### Build & Package
```bash
.\mvnw.cmd clean package -DskipTests
# Output: target/app.jar
```

### Test
```bash
.\mvnw.cmd test
```

---

## 12. KEY BUSINESS LOGIC NOTES

### User Registration
- Password is BCrypt-hashed before saving to DB
- Duplicate check: `userRepository.existsByEMailAndName()`
- If user exists with same email AND name → throws "This User Already Existed."

### Login
- Finds user by email → throws "User not found" if missing
- Verifies password with BCrypt `passwordEncoder.matches()`
- If password matches → generates JWT token (5-hour expiry) with email as subject
- Returns token as **raw string** (not JSON object!)

### Page Ownership & Security
- Users can only update/delete their own pages (`pageRepository.findByIdAndUserId()`)
- Users can only add their own pages to groups
- Delete check: verifies `page.getUser().geteMail().equals(currentUserEMail)`

### Group Membership & Security
- When creating a group, the creator is automatically added as a member
- Group passwords are BCrypt-hashed
- Group details/pages can only be viewed by **members** of the group
- To join a group, user must provide the correct password
- Any group member can update pages belonging to the group (checked via `groupRepository.isMember()`) - **this means ANY group member can edit ANY page in the group**
- Users can leave a group via `DELETE /api/groups/{id}` which:
  - Finds the group by ID
  - Checks if the current user is a member
  - Removes the user from the group's members list
  - Removes the group from the user's groups list (bidirectional relationship maintenance)
  - Saves the group


### Page ↔ Group Relationship
- A page can be in at most one group (or none)
- Adding to group: sets `page.setGroup(group)` and adds page to `group.getPages()`
- Removing from group: sets `page.setGroup(null)` and removes page from `group.getPages()`
- If page is already in a group, adding it to another group just replaces the reference

### Auditing
- `@EnableJpaAuditing` in main application class
- `@EntityListeners(AuditingEntityListener.class)` on Page entity
- `@CreatedDate` on `createdDate` field (auto-set on creation)
- `@LastModifiedDate` on `lastUpdateDate` field (auto-set on update)
- In `PageService.updatePage()`, `lastUpdateDate` is also manually set via `pageMapper.updateEntityWithResponse()`

### Data Flow for Creating a Page
1. Controller receives `PageRequest` (title + content)
2. Service creates `new Page(title, content)` (not using mapper)
3. Sets `page.setUser(authenticatedUser)` - **Note:** `page.setUser(null)` is called first then overridden
4. Saves via `pageRepository.save(page)`

---

## 13. MAPPER ARCHITECTURE

```java
// Generic interface
interface IMapper<Response, Entity> {
    Response toResponse(Entity e);
}
```

Each mapper implements this interface. Additional methods exist per mapper:

| Mapper          | Methods                                                                 |
| --------------- | ----------------------------------------------------------------------- |
| **UserMapper**  | `toResponse(User)`, `toRequest(User)`, `toResponseList(List<User>)`, `toEntity(UserRequest)` |
| **PageMapper**  | `toResponse(Page)`, `toResponseList(List<Page>)`, `toEntity(PageRequest)`, `updateEntityWithResponse(Page, PageRequest)` |
| **GroupMapper** | `toResponse(Group)`, `toResponseList(List<Group>)`, `toEntity(GroupRequest)` (uses UserMapper and PageMapper internally) |

Key note about `PageMapper.updateEntityWithResponse()`:
- Only updates `content`, `title`, and `lastUpdateDate`
- Used in both `PageService.updatePage()` and `GroupService.updatePageOfGroup()`

---

## 14. IMPORTANT CODE SNIPPETS

### How to get current authenticated user in any service:
```java
String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
User user = userRepository.findByEMail(eMail)
    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + eMail));
```

### How to get current user's email (no DB query needed):
```java
String eMail = SecurityContextHolder.getContext().getAuthentication().getName();
```

### How JWT is generated:
```java
// JWTUtil.java
return Jwts.builder()
    .subject(eMail)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 5)) // 5 hours
    .signWith(secretKey)
    .compact();
```

### How JWT is validated:
```java
// JWTFilter.java
if (jwtUtil.validateToken(jwt, eMail)) {
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        eMail, null, new ArrayList<>()
    );
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
}
```

---

## 15. RESPONSE FORMATS SUMMARY

### Success Responses
- **Simple message:** `{ "message": "...Turkish text..." }`
- **Single object:** Direct JSON object (e.g., UserResponse, GroupResponse)
- **List:** JSON array of objects
- **Login:** **Raw string** (the JWT token itself, NOT wrapped in JSON!)

### Error Responses
All errors follow this pattern (except Spring Security errors):
```json
{
  "error": "Error message text",
  "status": 400,
  "path": "/api/endpoint/path"
}
```

### Common Error Messages (Turkish)
- "Kullanıcı oluşturuldu." → User created successfully
- "Kullanıcı güncelleme başarılı." → User update successful
- "Not oluşturma başarılı." → Note created successfully
- "Not başarıyla gruba dahil edildi." → Note successfully added to group
- "Not gruptan çıkarıldı." → Note removed from group
- "Silme işlemi başarılı." → Deletion successful
- "Grup Olusturma Basarılı!" → Group creation successful
- "Gruba Katılım Basarılı!" → Successfully joined group
- "This User Already Existed." → Duplicate user (English)
- "Hatalı şifre ya da e-posta!" → Wrong password or email
- "User not found" → User not found
- "Grup ismi kullanılmış!" → Group name already taken
- "Grup Bulunamadı!" → Group not found
- "KULLANICI ZATEN GRUBA UYE!" → User already a member
- "YANLIS SIFRE GIRDINIZ!" → Wrong password
- "GÜVENLİK İHLALİ: Başkasına ait bir notu silemezsiniz!" → Security violation: cannot delete another user's note
- "GUVENLIK IHLALI:Uyesı olmadıgınız bır grubun ıcerıgını goremezsınız!" → Security violation: cannot view non-member group content
- "You are not member of this group!" → Not a group member
- "Page is not exist or You are not owner." → Page not found or not owned
- "Page doesn't have a group" → Page not associated with any group

---

## 16. APPLICATION PROPERTIES REFERENCE

```properties
spring.application.name=NoteP
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/notep_db?createDatabaseIfNotExist=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:mirza6445}
spring.datasource.driver-class-name=
spring.jpa.database-platform=${DB_DIALECT:org.hibernate.dialect.MySQLDialect}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
server.port=${PORT:8080}
jwt.secretSTR=230316056S215M457R154Z262251962517m\u0131rza
logging.level.org.hibernate.orm.jdbc.bind=TRACE
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
springdoc.enable-data-rest=false
```

---

## 17. TO-DO LIST (from project_schema.puml)
- [ ] When user profile info is updated, pages and group components belonging to that user should also be updated (user not found with email error)
- [ ] Add note to group and remove note from group (partially done)
- [ ] getpagesofgroup endpoint was added
- [ ] Review id-parameterized endpoints
- [ ] Fix duplicate variables (name, email) in repository operations

---

## 18. FRONTEND INTEGRATION NOTES

For a frontend developer building against this API:

1. **Base URL (local):** `http://localhost:8080`
2. **Always use `Content-Type: application/json` header**
3. **Login flow:** `POST /api/auth/login` → response is a **raw string** (the JWT), store it as-is
4. **Auth header:** `Authorization: Bearer <token>` for all protected endpoints
5. **Login response is NOT JSON** - it's the token plain text. Parse accordingly.
6. **Swagger UI:** Available at `/swagger-ui.html` for testing
7. **CORS:** Enabled (`@CrossOrigin`) on all controllers
8. **Error handling:** All business errors return HTTP 400 with JSON body containing `error`, `status`, `path`
9. **Validation errors:** Returned as HTTP 400 (handled by Spring)

---

*Document generated for LLM consumption - contains complete project architecture, all endpoints, business logic, security details, and integration notes.*