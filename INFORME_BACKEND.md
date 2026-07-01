# INFORME TÉCNICO — Backend Gahramheit

> Fecha: Junio 2026  
> Objetivo: Responder las 4 preguntas sobre la arquitectura, seguridad, lógica de negocio y estado del Wrapped.

---

## Índice

1. [Procesamiento del Token y Capa de Seguridad](#1-procesamiento-del-token-y-capa-de-seguridad)
2. [Manejo de JWT, Interceptores, Refresh Tokens y Errores Globales](#2-manejo-de-jwt-interceptores-refresh-tokens-y-errores-globales)
3. [Lógica de Negocio — Services y Repositories](#3-lógica-de-negocio--services-y-repositories)
4. [Gahramheit Wrapped — Estado de Implementación](#4-gahramheit-wrapped--estado-de-implementación)

---

## 1. Procesamiento del Token y Capa de Seguridad

### 1.1 Arquitectura de Seguridad

```
┌─────────────────────────────────────────────────────────────┐
│                     SecurityConfig.java                      │
│  • Stateless (sin sesión HTTP)                               │
│  • CORS abierto (todos los orígenes, métodos, headers)       │
│  • CSRF deshabilitado                                        │
│  • Filtro JWT antes de UsernamePasswordAuthenticationFilter  │
│  • Rutas públicas: auth, GET animes/users/genres/comments    │
│  • Rutas protegidas: todo lo demás (anyRequest().authenticated)│
│  • @EnableMethodSecurity para @PreAuthorize en controladores │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                  JwtAuthenticationFilter.java                │
│  (OncePerRequestFilter — se ejecuta en CADA request HTTP)   │
│                                                              │
│  1. extractToken(request) → header "Authorization: Bearer X" │
│  2. jwtUtils.validateToken(token)  → ¿firma válida?         │
│  3. jwtUtils.getUsernameFromToken(token) → extrae "sub"     │
│  4. jwtUtils.getRoleFromToken(token)   → extrae "role"      │
│  5. Crea UsernamePasswordAuthenticationToken(username, null, │
│     [ROLE_role]) y lo setea en SecurityContextHolder         │
│  6. filterChain.doFilter() → NUNCA bloquea por sí mismo     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              SecurityConfig.authorizeHttpRequests()          │
│  • Si la ruta es pública → permite el paso                  │
│  • Si la ruta es protegida y NO hay autenticación → 401     │
│  • Si la ruta tiene @PreAuthorize y el rol no coincide → 403│
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Generación del Token

**Archivo:** `src/main/java/com/example/gahramheit/security/JwtUtils.java`

```java
public String generateToken(Long userId, String username, Role role) {
    return Jwts.builder()
            .setSubject(username)              // "sub" claim
            .claim("userId", userId)            // claim personalizado
            .claim("role", role.name())         // claim personalizado
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)                      // HMAC-SHA (clave Base64 desde env)
            .compact();
}
```

**¿Dónde se invoca?**

| Lugar | Archivo | Línea | Disparador |
|-------|---------|-------|------------|
| Login | `AuthService.java` | 39 | `POST /api/auth/login` |
| Register | `AuthService.java` | 71 | `POST /api/auth/register` |

### 1.3 Validación del Token

**Archivo:** `JwtUtils.java:50-57`

```java
public boolean validateToken(String token) {
    try {
        getClaims(token);    // parseClaimsJws → verifica firma y expiración
        return true;
    } catch (JwtException | IllegalArgumentException e) {
        return false;        // NO lanza excepción, solo retorna false
    }
}
```

La validación **nunca lanza excepción**. Si el token es inválido, el `JwtAuthenticationFilter` simplemente **no setea autenticación** en el `SecurityContextHolder`. La protección real la da `SecurityConfig` que rechaza requests no autenticados a rutas protegidas.

### 1.4 Entry Point de Autenticación

**Archivo:** `JwtAuthenticationEntryPoint.java`

```java
response.setStatus(401);
response.getWriter().write("{\"error\": \"No autorizado. Token inválido o faltante.\"}");
```

Se activa cuando Spring Security detecta que un request a ruta protegida no tiene autenticación válida en el `SecurityContext`.

### 1.5 UserDetailsService

**Archivo:** `UserDetailsServiceImpl.java`

- Soporta login **por username o por email**
- Busca primero por username; si no encuentra, busca por email
- Retorna `User` de Spring Security con autoridad `ROLE_USER`, `ROLE_MODERATOR` o `ROLE_ADMIN`

### 1.6 Excepciones de Seguridad

| Excepción | Archivo | HTTP Status | ¿Se usa actualmente? |
|-----------|---------|-------------|---------------------|
| `UnauthorizedTokenException` | `exception/UnauthorizedTokenException.java` | 401 | ❌ Definida pero **nunca lanzada** |
| `AccessDeniedException` | Propia del proyecto (en `exception/`) | 403 | ✅ Usada en `UserAnimeListService`, `ReviewService`, `UserService` |

---

## 2. Manejo de JWT, Interceptores, Refresh Tokens y Errores Globales

### 2.1 Evaluación vs Requisito

> **Requisito original:** *"Maneja tokens JWT almacenados de manera segura (sin localStorage en artifacts, pero usando métodos seguros en implementación real). Los interceptores manejan autenticación, refresh tokens, y errores globales"*

| Aspecto | Estado | Archivos involucrados |
|---------|--------|----------------------|
| **JWT almacenados seguramente** | ✅ **El backend cumple** | El backend nunca almacena tokens; solo los genera y valida. El almacenamiento compete al frontend. |
| **Interceptores de autenticación** | ✅ **Implementado** | `JwtAuthenticationFilter.java` — intercepta cada request, extrae y valida token Bearer |
| **Refresh tokens** | ❌ **NO implementado** | No existe endpoint `/api/auth/refresh`, ni entidad `RefreshToken`, ni lógica de rotación |
| **Errores globales** | ✅ **Implementado** | `GlobalExceptionHandler.java` con `@RestControllerAdvice` — 6 handlers específicos + catch-all 500 |

### 2.2 ¿Dónde ocurre cada proceso?

#### Interceptor de autenticación (trazabilidad)

```
Request HTTP entrante
    │
    ▼
SecurityFilterChain (SecurityConfig.java:46-54)
    │
    ├─ ¿Ruta pública? → permitAll (auth, GET animes/users/genres/comments)
    │
    └─ ¿Ruta protegida? → JwtAuthenticationFilter (línea 55)
            │
            ├─ ¿Token válido? → SecurityContextHolder.setAuthentication()
            │                     → Pasa al Controller
            │
            └─ ¿Token inválido/ausente? → No setea autenticación
                                           → SecurityConfig.anyRequest().authenticated()
                                           → JwtAuthenticationEntryPoint.commence() → 401
```

#### Errores globales

```
Cualquier excepción lanzada en Controller/Service/Repository
    │
    ▼
GlobalExceptionHandler (@RestControllerAdvice)
    │
    ├─ InvalidDataException          → 400 Bad Request
    ├─ MethodArgumentNotValidException → 400 Validation Error
    ├─ UnauthorizedTokenException    → 401 (handler existe pero nunca se ejecuta)
    ├─ AccessDeniedException         → 403 Forbidden
    ├─ ResourceNotFoundException     → 404 Not Found
    ├─ DuplicateResourceException    → 409 Conflict
    └─ Exception (catch-all)         → 500 Internal Server Error
         │
         ▼
    ErrorResponse DTO: { timestamp, status, error, message, path }
```

#### Refresh tokens

> **NO IMPLEMENTADO.** No existe:
> - Endpoint `/api/auth/refresh`
> - Entidad `RefreshToken` en BD
> - Repositorio de refresh tokens
> - Lógica de rotación o expiración de refresh tokens
> - Método en `JwtUtils` para generar refresh tokens

### 2.3 Resumen de endpoints de autenticación existentes

| Método | Endpoint | Archivo | ¿Requiere auth? |
|--------|----------|---------|----------------|
| `POST` | `/api/auth/login` | `AuthController.java:22` | ❌ Público |
| `POST` | `/api/auth/register` | `AuthController.java:27` | ❌ Público |
| `POST` | `/api/auth/refresh` | ❌ **No existe** | — |

---

## 3. Lógica de Negocio — Services y Repositories

### 3.1 Arquitectura General

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌────────────┐
│  Controllers  │────▶│   Services   │────▶│  Repositories │────▶│ PostgreSQL │
│ (@RestController)│   │   (@Service) │     │ (JpaRepository)│     │  (JPA/Hibernate)│
│  @PreAuthorize │    │ @Transactional│    │               │     │            │
└──────────────┘     └──────────────┘     └──────────────┘     └────────────┘
                            │
                    ┌───────┴───────┐
                    │    JwtUtils    │
                    │  (JWT engine)  │
                    └───────────────┘
```

### 3.2 Services (11 archivos)

---

#### `AuthService.java`

**Ubicación:** `service/AuthService.java`  
**Dependencias:** `UserRepository`, `AuthenticationManager`, `PasswordEncoder`, `JwtUtils`, `ApplicationEventPublisher`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `login` | `AuthResDTO login(UserLoginReqDTO)` | Autentica credenciales y devuelve JWT | `AuthenticationManager.authenticate()` → busca `User` por username → `JwtUtils.generateToken()` → construye `AuthResDTO` con token + userInfo |
| `register` | `AuthResDTO register(UserRegisterReqDTO)` | Crea usuario y devuelve JWT | Verifica duplicados de username y email → hashea password con BCrypt → crea `User` con `Role.USER` → `userRepository.save()` → publica `UserRegisteredEvent` (email async) → genera JWT igual que login |

---

#### `UserService.java`

**Ubicación:** `service/UserService.java`  
**Dependencias:** `UserRepository`, `UserAnimeListRepository`, `ModelMapper`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `getUserById` | `UserProfileResDTO getUserById(Long)` | Perfil completo del usuario | Busca User → `buildProfile()` que suma episodios (sum de `currentEpisode`), cuenta COMPLETED, calcula rango |
| `getUserByUsername` | `UserResponseDTO getUserByUsername(String)` | Info básica del usuario | `findByUsername()` → `ModelMapper.map()` a DTO |
| `getUserProfile` | `UserProfileResDTO getUserProfile(Long)` | Alias de getById | Misma lógica que getById |
| `getUserRecap` | `UserRecapResDTO getUserRecap(Long, Integer)` | Resumen anual del usuario | Filtra `userAnimeListRepository.findByUser_Id()` → `buildRecapDto()` cuenta episodios totales, calcula tiempo (`episodios × 24`), asigna insignia según completados |
| `updateUser` | `UserUpdateDTO updateUser(Long, UserUpdateDTO)` | Actualizar perfil | Verifica ownership desde `SecurityContextHolder` (self o admin) → `Optional.ofNullable().ifPresent()` para actualizar solo campos no nulos |
| `deleteUser` | `void deleteUser(Long)` | Eliminar usuario | Verifica existencia → `deleteById()` |
| `updateUserRole` | `UserResponseDTO updateUserRole(Long, Role)` | Cambiar rol (admin) | Busca User → `setRole()` → save → mapea a DTO |

**Métodos privados:**

| Método | Qué hace |
|--------|----------|
| `buildProfile(User)` | Construye perfil con: episodios vistos (sumatoria), animes completados (count COMPLETED), rango, logros placeholder "0/6" |
| `buildRecapDto(List<UserAnimeList>, Integer)` | Construye recap con: total episodios, tiempo total (ep × 24 min), género favorito "Sin datos" (hardcodeado), anime mejor calificado "Sin datos" (hardcodeado), insignia (≥10 completados = "Completador Serial", sino "Principiante") |
| `calculateRango(long)` | < 5 → "Nuevo en el Mundo Anime", 5-14 → "Otaku en Formación", 15-29 → "Otaku Experimentado", ≥ 30 → "Dios del Anime" |

---

#### `AnimeService.java`

**Ubicación:** `service/AnimeService.java`  
**Dependencias:** `AnimeRepository`, `ModelMapper`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `getAnimeCatalog` | `Page<AnimeDTO> getAnimeCatalog(int, int)` | Catálogo paginado | `PageRequest.of()` → `findAll()` → `.map(this::toCardDto)` |
| `searchAnimesByTitle` | `List<AnimeDTO> searchAnimesByTitle(String)` | Búsqueda por título | `findByTitleContainingIgnoreCase()` (SQL `LIKE '%keyword%'` case-insensitive) |
| `getAnimeDetails` | `AnimeDetailResDTO getAnimeDetails(Long)` | Detalle completo | `findById()` → ModelMapper a DTO → setea lista de nombres de géneros → split `voiceActors` por coma |
| `animeFiltradoPorNombre` | `Page<AnimeDTO> animeFiltradoPorNombre(String, int, int)` | Por género (paginado) | `findByGenres_NameIgnoreCase()` |
| `getAnimeCatalog` (overloaded) | `Page<AnimeDTO> getAnimeCatalog(String, String, int, int)` | Super filtro combinado | Normaliza keyword ("" si null, trim) y genre (null si vacío) → `findWithFilters()` JPQL: `DISTINCT a LEFT JOIN a.genres g WHERE LOWER(a.title) LIKE... AND (:genre IS NULL OR g.name = :genre)` |

---

#### `ReviewService.java`

**Ubicación:** `service/ReviewService.java`  
**Dependencias:** `ReviewRepository`, `AnimeRepository`, `UserRepository`, `ModelMapper`, `ApplicationEventPublisher`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `createReview` | `ReviewResDTO createReview(Long, ReviewCreateReqDTO)` | Crear reseña | Valida User + Anime existen → construye `Review` con score, comment, timestamp → `save()` → publica `AnimeReviewedEvent` |
| `getReviewsByAnime` | `List<ReviewResDTO> getReviewsByAnime(Long)` | Reseñas de un anime | Verifica anime existe → `findByAnime_Id()` → stream → map a DTO con username |
| `getReviewById` | `ReviewResDTO getReviewById(Long)` | Reseña individual | `findById()` → toDto |
| `deleteReview` | `void deleteReview(Long)` | Eliminar con control de acceso | Obtiene `Authentication` del contexto → verifica si es owner (dueño de la review) O moderador/admin (ROLE_MODERATOR o ROLE_ADMIN) → delete |

---

#### `CommentService.java`

**Ubicación:** `service/CommentService.java`  
**Dependencias:** `CommentRepository`, `UserRepository`, `AnimeRepository`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `getRootComments` | `Page<CommentResDTO> getRootComments(Long, int, int)` | Comentarios raíz paginados | `findByAnime_IdAndParentIdIsNull()` → map a DTO con flag `hasReplies` (usa `countByParentId()`) |
| `getReplies` | `List<CommentResDTO> getReplies(Long)` | Respuestas a comentario | Valida que comentario padre no sea a su vez reply (máx 1 nivel de anidamiento) → `findByParentId()` |
| `createComment` | `CommentResDTO createComment(Long, Long, CommentCreateReqDTO)` | Crear comentario | Si tiene `parentId`, valida que exista y que sea raíz (no reply anidado) → construye `Comment` con content, likes/dislikes en 0 → save |
| `likeComment` | `void likeComment(Long)` | Like | `findById()` → increments `likesCount` +1 → save |
| `dislikeComment` | `void dislikeComment(Long)` | Dislike | `findById()` → increments `dislikesCount` +1 → save |
| `deleteComment` | `void deleteComment(Long, Long)` | Eliminar | Valida ownership por userId → delete |
| `editComment` | `CommentResDTO editComment(Long, Long, CommentCreateReqDTO)` | Editar | Valida ownership → actualiza content y refreshed timestamp → save |

---

#### `UserAnimeListService.java`

**Ubicación:** `service/UserAnimeListService.java`  
**Dependencias:** `UserAnimeListRepository`, `UserRepository`, `AnimeRepository`

| Método | Firma | Qué hace | Cómo lo hace |
|--------|-------|----------|-------------|
| `getUserList` | `List<UserAnimeListResDTO> getUserList(Long)` | Lista del usuario | Verifica usuario existe → `findByUser_Id()` → map a DTO con animeId, title, imageUrl, status, currentEpisode, total episodes |
| `updateAnimeInList` | `UserAnimeListResDTO updateAnimeInList(Long, UpdateUserAnimeListReqDTO)` | Upsert en lista | `verifyOwnership()` → busca User + Anime → busca por clave compuesta `UserAnimeListId`; si no existe, crea nueva entrada vacía → setea status y currentEpisode → save |
| `removeFromList` | `void removeFromList(Long, Long)` | Eliminar entrada | `verifyOwnership()` → busca por `UserAnimeListId` → delete |
| `verifyOwnership` (privado) | `void verifyOwnership(Long)` | Validación de permisos | Obtiene `Authentication` del `SecurityContextHolder` → busca User actual → si no es admin y no coincide userId, lanza `AccessDeniedException` |

---

#### `GenreService.java`

**Ubicación:** `service/GenreService.java`  
**Dependencias:** `GenreRepository`, `ModelMapper`

| Método | Firma | Qué hace |
|--------|-------|----------|
| `getAllGenres` | `List<GenreDTO> getAllGenres()` | findAll → stream → map a DTO |
| `getGenreById` | `GenreDTO getGenreById(Long)` | findById → map |
| `createGenre` | `GenreDTO createGenre(GenreDTO)` | Verifica duplicado por nombre → save → map |
| `updateGenre` | `GenreDTO updateGenre(Long, GenreDTO)` | findById → setName → save |
| `deleteGenre` | `void deleteGenre(Long)` | findById → delete |

---

#### `EpisodeService.java`

**Ubicación:** `service/EpisodeService.java`  
**Dependencias:** `EpisodeRepository`, `AnimeRepository`, `ModelMapper`

| Método | Firma | Qué hace |
|--------|-------|----------|
| `getEpisodesByAnime` | `List<EpisodeDTO> getEpisodesByAnime(Long)` | Verifica anime existe → `findByAnime_IdOrderByEpisodeNumberAsc()` |
| `getEpisodeById` | `EpisodeDTO getEpisodeById(Long)` | findById → toDto (incluye animeId) |
| `createEpisode` | `EpisodeDTO createEpisode(Long, EpisodeDTO)` | Busca Anime → crea Episode con episodeNumber y title → save |

---

#### `EmailService.java`

**Ubicación:** `service/EmailService.java`  
**Dependencias:** `JavaMailSender`

| Método | Firma | Qué hace |
|--------|-------|----------|
| `sendWelcomeEmail` | `void sendWelcomeEmail(String, String)` | Construye MIME message con HTML template de bienvenida → `mailSender.send()` → atrapa todas las excepciones (MessagingException, Exception) sin propagar, solo loguea |

---

#### `AIService.java`

**Ubicación:** `service/AIService.java`

| Método | Firma | Qué hace |
|--------|-------|----------|
| `generateOtakuProfile` | `String generateOtakuProfile(String, String, Double)` | Genera texto de personalidad según avgScore (≥9: generoso, ≥7: equilibrado, <7: crítico) → retorna string formateado con username y topGenre |

---

#### `DataPopulatorService.java`

**Ubicación:** `service/DataPopulatorService.java`  
**Dependencias:** `AnimeRepository`, `GenreRepository`, `EpisodeRepository`, `ReviewRepository`, `UserRepository`, `PlatformTransactionManager`, `RestTemplate`

| Método | Firma | Qué hace |
|--------|-------|----------|
| `syncTopAnime` | `void syncTopAnime(int)` | Bucle principal: seedea 3 usuarios de prueba → itera páginas de Jikan API `/top/anime` → por cada anime llama a `processSingleAnime()` |
| `processSingleAnime` | `void processSingleAnime(AnimeData, List<User>)` | Si ya existe o tiene ≥1000 episodios, salta → abre transacción propia → guarda Anime, asocia Géneros (M:N), obtiene y guarda Staff/Director, obtiene y guarda Episodios, obtiene Reviews y asigna round-robin a usuarios seed |
| `fetchStaffAndCast` | `void fetchStaffAndCast(Anime)` | Llama a `/anime/{id}/staff` (directores) y `/anime/{id}/characters` (actores de voz) |
| `fetchEpisodes` | `void fetchEpisodes(Anime)` | Llama a `/anime/{id}/episodes` |
| `fetchReviews` | `void fetchReviews(Anime, List<User>)` | Llama a `/anime/{id}/reviews`, asigna cada review a un usuario seed rotatorio |
| `sleepToAvoidRateLimit` | `void sleepToAvoidRateLimit()` | Sleep de 1250ms entre cada llamada a Jikan para respetar rate limit |

---

### 3.3 Repositories (8 interfaces)

| Repositorio | Entidad | Llave primaria | Métodos query |
|-------------|---------|----------------|---------------|
| `UserRepository` | `User` | `Long` (ID auto) | `findByUsername(String)`, `findByEmail(String)` |
| `AnimeRepository` | `Anime` | `Long` (ID manual desde Jikan) | `findByTitleContainingIgnoreCase(String)`, `getMostWatchedGenreByUser(Long)` (native SQL), `findByStudioIsNullOrReleaseYearIsNull()`, `findByGenres_NameIgnoreCase(String, Pageable)`, `findWithFilters(String, String, Pageable)` (JPQL) |
| `GenreRepository` | `Genre` | `Long` (ID auto) | `findByName(String)` |
| `ReviewRepository` | `Review` | `Long` (ID auto) | `findByAnime_Id(Long)`, `getAverageScoreByUser(Long)` (JPQL con AVG) |
| `CommentRepository` | `Comment` | `Long` (ID auto) | `findByAnime_IdAndParentIdIsNull(Long, Pageable)`, `findByParentId(Long)`, `countByParentId(Long)` |
| `EpisodeRepository` | `Episode` | `Long` (ID auto) | `findByAnime_IdOrderByEpisodeNumberAsc(Long)` |
| `UserAnimeListRepository` | `UserAnimeList` | `UserAnimeListId` (compuesta: userId + animeId) | `findByUser_Id(Long)` |
| `UserRecapRepository` | `UserRecap` | `Long` (ID auto) | `findByUserIdAndYear(Long, Integer)`, `existsByUserIdAndYear(Long, Integer)` |

### 3.4 Entities (11 clases)

| Entidad | Tabla | Relaciones clave |
|---------|-------|------------------|
| `User` | `users` | `@OneToMany` → Review, Comment, UserAnimeList |
| `Anime` | `animes` | `@ManyToMany` → Genre; `@OneToMany` → Episode, Review, Comment, UserAnimeList |
| `Genre` | `genres` | `@ManyToMany(mappedBy)` ← Anime |
| `Review` | `reviews` | `@ManyToOne` → User, Anime |
| `Comment` | `comments` | `@ManyToOne` → User, Anime; `parentId` (auto-referencia para anidamiento) |
| `Episode` | `episodes` | `@ManyToOne` → Anime |
| `UserAnimeList` | `user_anime_list` | `@EmbeddedId` (userId + animeId); `@ManyToOne` → User, Anime |
| `UserRecap` | `user_recaps` | `@ManyToOne` → User |
| `Role` | (Enum) | USER, MODERATOR, ADMIN |
| `Status` | (Enum) | WATCHING, COMPLETED, DROPPED |

---

## 4. Gahramheit Wrapped — Estado de Implementación

### 4.1 Contexto

El **"Gahramheit Wrapped"** es una función estilo **Spotify Wrapped** que muestra al usuario un resumen completo de su actividad en la plataforma: estadísticas anuales, logros desbloqueados, badges, género favorito, top animes, mensaje personalizado generado por IA, etc.

### 4.2 ¿Qué ya existe?

| Componente | Archivo | Estado | Detalle |
|------------|---------|--------|---------|
| **Entidad UserRecap** | `entity/UserRecap.java` | ✅ Creada | Columnas: `id`, `user` (FK), `recap_year`, `total_genres_rated`, `top_genre`, `top5_animes` (TEXT), `average_score`, `ai_personalized_message` (TEXT) |
| **Repositorio UserRecap** | `repository/UserRecapRepository.java` | ✅ Creado | `findByUserIdAndYear(Long, Integer)`, `existsByUserIdAndYear(Long, Integer)` |
| **DTO UserRecapResDTO** | `dto/UserRecapResDTO.java` | ✅ Creado | Campos: `anio`, `totalEpisodiosVistos`, `tiempoTotalMinutos`, `generoFavorito`, `animeMejorCalificado` (inner class TopAnime: id, title, score), `insigniaDestacadaAnual` |
| **DTO AchievementResDTO** | `dto/AchievementResDTO.java` | ✅ Creado | Campos: `id`, `name`, `description`, `isUnlocked`, `unlockedAt` |
| **Endpoint GET /recap** | `controller/UserController.java:38-44` | ✅ Creado | `GET /api/users/{id}/recap?year=2024` con `@PreAuthorize("hasAnyRole('USER','MODERATOR','ADMIN')")` |
| **Lógica buildRecapDto** | `service/UserService.java:133-156` | ⚠️ Placeholder | Calcula datos en caliente desde `UserAnimeList`, NO usa `UserRecapRepository` |
| **AIService** | `service/AIService.java` | ✅ Creado | `generateOtakuProfile(username, topGenre, avgScore)` genera texto según score |
| **Native query top genre** | `repository/AnimeRepository.java:17-25` | ✅ Creada | SQL nativo `getMostWatchedGenreByUser(Long)` con JOIN a `user_anime_list`, `anime_genre`, `genres` |
| **Logros en perfil** | `dto/UserProfileResDTO.java` | ⚠️ Placeholder | Campo `logrosDesbloqueados` con valor fijo `"0/6"` |
| **Query avg score** | `repository/ReviewRepository.java:16` | ✅ Creada | `getAverageScoreByUser(Long)` con `AVG(score)` |

### 4.3 ¿Qué falta o está incompleto?

| # | Aspecto | Problema | Severidad |
|---|---------|----------|-----------|
| 1 | **UserRecapRepository no se usa** | `UserService.getUserRecap()` computa datos en caliente desde `UserAnimeList` y devuelve hardcoded values. El repositorio `UserRecapRepository` existe pero **nunca se inyecta en ningún service**. | 🔴 Crítico |
| 2 | **DTO desalineado con Entity** | `UserRecapResDTO` NO tiene campos para `top5Animes`, `averageScore`, `aiPersonalizedMessage` que sí están en la entidad. A su vez, la entidad NO tiene `insigniaDestacadaAnual`, `totalEpisodiosVistos`, `tiempoTotalMinutos` que sí están en el DTO. | 🔴 Crítico |
| 3 | **Sistema de logros** | `AchievementResDTO` existe pero **no hay**: entidad `Achievement`/`Badge` en BD, repositorio, lógica de desbloqueo, ni servicio. Solo placeholder `"0/6"`. | 🔴 Crítico |
| 4 | **Datos hardcodeados** | `generoFavorito = "Sin datos"`, `animeMejorCalificado = new TopAnime(0L, "Sin datos", 0)` a pesar de que existe `getMostWatchedGenreByUser()` en `AnimeRepository`. | 🟡 Medio |
| 5 | **Insignias estáticas** | `insigniaDestacadaAnual` es un if-else hardcodeado (≥10 completados = "Completador Serial", sino "Principiante"). No hay sistema de badges configurable. | 🟡 Medio |
| 6 | **Tiempo total arbitrario** | `tiempoTotalMinutos = episodios * 24` — promedio fijo de 24 min por episodio. No considera duración real del anime. | 🟢 Bajo |
| 7 | **Sin job programado** | No hay scheduler (`@Scheduled`) que genere recaps periódicamente (anualmente o bajo demanda). | 🟡 Medio |
| 8 | **Sin endpoint dedicado Wrapped** | El recap está dentro de `UserController`. No hay un controlador o servicio independiente para el Wrapped. | 🟢 Bajo |

### 4.4 Base de Datos

Las tablas del Wrapped **deben estar en la misma base de datos PostgreSQL** principal. No se necesita una base separada.

#### Ya existe:
- `user_recaps` — tabla creada pero no utilizada activamente

#### Se necesitaría crear:
- `achievements` / `badges` — tabla de definición de logros (nombre, descripción, condición, imagen)
- `user_achievements` / `user_badges` — tabla de logros desbloqueados por usuario (userId, achievementId, unlockedAt)

### 4.5 Diagrama de Estado Actual del Wrapped

```
┌────────────────────────────────────────────────────────────┐
│                   GAHRAMHEIT WRAPPED                        │
│                       Estado Actual                         │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌──────────────────┐               │
│  │ UserRecap Entity │    │ UserRecapResDTO  │               │
│  │ (tabla user_recaps)│   │ (respuesta API)  │               │
│  ├─────────────────┤    ├──────────────────┤               │
│  │ ✅ id           │    │ ✅ anio           │               │
│  │ ✅ user (FK)    │    │ ✅ totalEpisodios  │               │
│  │ ✅ recap_year   │    │ ✅ tiempoTotalMin  │               │
│  │ ✅ totalGenres  │    │ ✅ generoFavorito  │               │
│  │ ✅ topGenre     │    │ ✅ animeMejorCalif  │               │
│  │ ✅ top5Animes   │    │ ✅ insigniaAnual   │               │
│  │ ✅ averageScore │    │ ❌ top5Animes      │ ← NO EXISTE  │
│  │ ✅ aiMessage    │    │ ❌ averageScore    │ ← NO EXISTE  │
│  └─────────────────┘    │ ❌ aiMessage       │ ← NO EXISTE  │
│        ⬆⬇               └──────────────────┘               │
│     NO CONECTADO         ⬆                                  │
│                          │                                  │
│        ╔════════════════╧══════════════════╗                │
│        ║  UserService.getUserRecap()        ║                │
│        ║  (compute en caliente desde        ║                │
│        ║   UserAnimeList, datos hardcodeados)║                │
│        ╚══════════════════════════════════════╝                │
│                                                             │
│  ┌──────────────────────────┐                               │
│  │ AchievementResDTO        │  ← existe pero sin entidad    │
│  │ Sistema de Logros        │  ← NO IMPLEMENTADO            │
│  └──────────────────────────┘                               │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### 4.6 Resumen General del Wrapped

```
╔══════════════════════════════════════════════════════════════╗
║               GAHRAMHEIT WRAPPED — STATUS REPORT             ║
╠══════════════════════════════════════════════════════════════╣
║                                                            ║
║  ✅ Entidad UserRecap       — CREADA                        ║
║  ✅ Repositorio UserRecap   — CREADO                        ║
║  ✅ DTO UserRecapResDTO     — CREADO                        ║
║  ✅ DTO AchievementResDTO   — CREADO                        ║
║  ✅ Endpoint GET /recap     — CREADO                        ║
║  ✅ AIService.generateProfile — CREADO                      ║
║  ✅ Native query top genre  — CREADA                        ║
║  ✅ Native query avg score  — CREADA                        ║
║                                                            ║
║  ⚠️  UserRecap sin uso       — REPOSITORIO NUNCA INYECTADO  ║
║  ⚠️  DTO vs Entity           — CAMPOS DESALINEADOS          ║
║  ⚠️  Datos hardcodeados     — GÉNERO Y TOP ANIME FIJOS     ║
║                                                            ║
║  ❌ Sistema de Logros       — NO IMPLEMENTADO               ║
║  ❌ Tabla achievements      — NO CREADA                     ║
║  ❌ Job programado          — NO IMPLEMENTADO               ║
║  ❌ Endpoint dedicado       — NO CREADO                     ║
║                                                            ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Anexo: Mapa Completo de Archivos del Backend

```
src/main/java/com/example/gahramheit/
│
├── GahramheitApplication.java           ← Entry point @SpringBootApplication @EnableAsync
├── config/
│   ├── ModelMapperConfig.java           ← Bean ModelMapper con ambiguityIgnored
│   └── SecurityConfig.java              ← SecurityFilterChain, CORS, CSRF, rutas, filtros
├── controller/
│   ├── AdminController.java             ← syncTopAnime (población desde Jikan API)
│   ├── AnimeController.java             ← CRUD animes, búsqueda, filtros
│   ├── AuthController.java              ← POST /login, POST /register
│   ├── CommentController.java           ← CRUD comentarios, likes, dislikes
│   ├── EpisodeController.java           ← Episodios por anime
│   ├── GenreController.java             ← CRUD géneros
│   ├── ReviewController.java            ← CRUD reseñas
│   ├── UserAnimeListController.java     ← Lista personal de anime
│   └── UserController.java              ← Perfil, recap, update, delete, roles
├── dto/                                 ← 24 DTOs (requests y responses)
├── entity/                              ← 9 entidades + 2 enums + 1 ID compuesto
├── event/
│   ├── AnimeReviewedEvent.java          ← Evento: anime reseñado
│   └── UserRegisteredEvent.java         ← Evento: usuario registrado
├── exception/
│   ├── AccessDeniedException.java       ← 403
│   ├── DuplicateResourceException.java  ← 409
│   ├── GlobalExceptionHandler.java      ← @RestControllerAdvice (6 handlers + catch-all)
│   ├── InvalidDataException.java        ← 400
│   ├── ResourceNotFoundException.java   ← 404
│   └── UnauthorizedTokenException.java  ← 401 (definida pero no usada)
├── listener/
│   └── AsyncSystemListener.java         ← @Async @EventListener (email, recálculo stats)
├── repository/                          ← 8 interfaces JpaRepository
├── security/
│   ├── JwtAuthenticationEntryPoint.java ← 401 JSON
│   ├── JwtAuthenticationFilter.java     ← Interceptor Bearer token
│   ├── JwtUtils.java                    ← Generación, validación, claims
│   └── UserDetailsServiceImpl.java      ← Carga por username o email
└── service/                             ← 11 servicios (@Service + @Transactional)
```

---

*Fin del informe.*
