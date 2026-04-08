# JWT Security — Reference Documentation

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [How JWT Authentication Works](#how-jwt-authentication-works)
- [Configuration Reference](#configuration-reference)
- [API Endpoints](#api-endpoints)
- [Authorization & Roles](#authorization--roles)
- [Token Lifecycle](#token-lifecycle)
- [Project Structure](#project-structure)
- [Key Classes Explained](#key-classes-explained)
- [Security Best Practices](#security-best-practices)
- [Testing with cURL/Bruno](#testing-with-curlbruno)

---

## Architecture Overview

```
┌──────────┐        ┌──────────────────────────────────┐       ┌──────────────────┐
│  Client  │──JWT──▷│ BearerTokenAuthenticationFilter  │──────▷│  Controller      │
│ (Bruno)  │        │ (Spring Security built-in)        │       │  (Protected)     │
└──────────┘        └──────────────┬───────────────────┘       └──────────────────┘
                                   │
                        ┌──────────▼──────────┐
                        │   NimbusJwtDecoder   │
                        │ (validate signature  │
                        │  + expiry, no DB)    │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────────┐
                        │ JwtAuthenticationConverter│
                        │ (map "role" claim →       │
                        │  GrantedAuthority)        │
                        └─────────────────────────-┘
```

**Flow:**
1. Client sends `Authorization: Bearer <token>` header
2. `BearerTokenAuthenticationFilter` (Spring Security built-in) extracts the token
3. `NimbusJwtDecoder` verifies the signature and checks expiry — **no database call**
4. `JwtAuthenticationConverter` reads the `role` claim and creates a `GrantedAuthority`
5. `SecurityContextHolder` is populated with the authenticated principal
6. The request proceeds to the controller

**On authentication failure:**
- Missing/invalid/expired token → `JwtAuthenticationEntryPoint` returns **401**
- Insufficient role → `CustomAccessDeniedHandler` returns **403**

> **What changed from the previous implementation:**
> The custom `JwtAuthenticationFilter` (which extended `OncePerRequestFilter` and called `UserDetailsService` on every request) has been removed. Token validation is now fully delegated to Spring Security's OAuth2 Resource Server support — no manual filter, no database lookup per request.

---

## How JWT Authentication Works

### What is a JWT?

A JSON Web Token (JWT) consists of three Base64-encoded parts separated by dots:

```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIn0.abc123...
```

| Part | Contains | Purpose |
|------|----------|---------| 
| **Header** | `{"alg": "HS256"}` | Signing algorithm |
| **Payload** | `{"sub": "admin", "role": "ROLE_ADMIN", "exp": 1234567890}` | User claims |
| **Signature** | HMAC-SHA256(header + payload, secret) | Integrity verification |

### Why Stateless?

Unlike session-based auth (which stores state on the server), JWT is **stateless**:
- The server **never** stores session data in memory
- Every request carries its own authentication proof (the token)
- This makes horizontal scaling trivial — any server instance can validate the token

### Access Token vs Refresh Token

| | Access Token | Refresh Token |
|---|---|---|
| **Purpose** | Authenticate API requests | Obtain new access tokens |
| **Lifetime** | Short (2 minutes default) | Long (7 days) |
| **Contains** | username, role, expiry | username, expiry only |
| **Stored in** | Client memory (JS variable) | HttpOnly cookie or secure storage |
| **Sent with** | Every API request (`Authorization` header) | Only to `/api/auth/refresh` |
| **Persisted on server** | No | Yes (in `refresh_tokens` table) |
| **Validated by** | `NimbusJwtDecoder` (crypto only) | `AuthService` (DB lookup required) |

---

## Configuration Reference

All JWT configuration is in `application.yaml`:

```yaml
security:
  jwt:
    # HMAC-SHA256 signing key (Base64-encoded, minimum 256 bits)
    # In production: use ${JWT_SECRET} environment variable
    secret-key: ${JWT_SECRET:dGhpcy1pcy1hLXZlcnktbG9uZy0yNTYtYml0LXNlY3JldC1rZXktZm9yLWRldi1vbmx5LWNoYW5nZS1pbi1wcm9kdWN0aW9u}

    # Access token lifetime — ISO-8601 duration format
    access-token-expiration: 2m    # 2 minutes

    # Refresh token lifetime — ISO-8601 duration format
    refresh-token-expiration: 7d   # 7 days
```

> **Duration format:** Values use Spring's ISO-8601 duration notation (`2m`, `7d`, `PT15M`, etc.), not raw milliseconds. This is bound to `java.time.Duration` fields in `JwtService`.

### Production Checklist
- [ ] Set `JWT_SECRET` environment variable with a cryptographically random 256-bit key
- [ ] Use HTTPS (never transmit JWTs over plain HTTP)
- [ ] Consider shorter access token expiration for sensitive operations
- [ ] Enable CORS configuration if frontend is on a different domain

### Generating a Production Secret Key
```bash
# Generate a random 256-bit Base64-encoded key
openssl rand -base64 32
```

---

## API Endpoints

### Public Endpoints (No Authentication Required)

#### `POST /api/auth/register`
Register a new user account.

**Request:**
```json
{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securePassword123"
}
```

**Response (201):**
```json
{
    "status": 200,
    "message": "User registered successfully",
    "data": {
        "access_token": "eyJhbGciOiJIUzI1NiJ9...",
        "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
        "token_type": "Bearer",
        "expires_in": 120
    }
}
```

**Validation Rules:**
- `username`: 3-50 characters, unique
- `email`: valid email format, unique
- `password`: minimum 8 characters

---

#### `POST /api/auth/login`
Authenticate with username and password.

**Request:**
```json
{
    "username": "admin",
    "password": "password"
}
```

**Response (200):**
```json
{
    "status": 200,
    "message": "Login successful",
    "data": {
        "access_token": "eyJhbGciOiJIUzI1NiJ9...",
        "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
        "token_type": "Bearer",
        "expires_in": 120
    }
}
```

---

#### `POST /api/auth/refresh`
Get a new access token using a valid refresh token.

**Request:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200):** Same structure as login response with new tokens.

> **Note:** Refresh token rotation is implemented — the old refresh token is revoked and a new one is issued. This protects against token replay attacks.

---

#### `POST /api/auth/logout`
Revoke a refresh token (effectively logging out).

**Request:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200):**
```json
{
    "status": 200,
    "message": "Logged out successfully",
    "data": null
}
```

---

### Protected Endpoints (Authentication Required)

For all protected endpoints, include the access token in the `Authorization` header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## Authorization & Roles

### Roles
| Role | Description | Granted Authority |
|------|-------------|-------------------|
| `CUSTOMER` | Default role for self-registered users | `ROLE_CUSTOMER` |
| `ADMIN` | System administrator | `ROLE_ADMIN` |

The `role` claim is embedded in the access token as a single string (e.g., `"ROLE_ADMIN"`). `JwtAuthenticationConverter` reads this claim and converts it to a `SimpleGrantedAuthority` — no database lookup required.

### Endpoint Access Matrix

| Endpoint | Method | CUSTOMER | ADMIN | Unauthenticated |
|----------|--------|----------|-------|-----------------|
| `/api/auth/**` | POST | ✅ | ✅ | ✅ |
| `/api/products` | GET | ✅ | ✅ | ❌ |
| `/api/products` | POST | ❌ | ✅ | ❌ |
| `/api/products` | PUT/DELETE | ❌ | ✅ | ❌ |
| `/api/users` | POST | ❌ | ✅ | ❌ |
| `/api/users/{id}` | GET | ✅ | ✅ | ❌ |
| `/api/orders/**` | GET/POST | ✅ | ✅ | ❌ |

### How Authorization is Enforced

**Two layers of defense:**

1. **URL-pattern rules** in `SecurityConfig.java`:
   ```java
   .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
   ```

2. **Method-level annotations** on controllers:
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   public ApiResponse<UserDTO> createUser(...) { }
   ```

---

## Token Lifecycle

```
  Register/Login
       │
       ▼
┌──────────────┐     ┌──────────────┐
│ Access Token │     │Refresh Token │
│   (2 min)    │     │   (7 days)   │
└──────┬───────┘     └──────┬───────┘
       │                    │
       ▼                    │
  Use for API calls         │
       │                    │
       ▼                    │
  Token Expires             │
       │                    ▼
       │            POST /api/auth/refresh
       │                    │
       ▼                    ▼
┌──────────────┐     ┌──────────────┐
│ New Access   │     │ New Refresh  │  ← Token Rotation
│ Token        │     │ Token        │    (old one revoked)
└──────────────┘     └──────────────┘
```

### Refresh Token Rotation
When a refresh token is used:
1. The old refresh token is **revoked** (marked as used)
2. A **new** refresh token is issued alongside the new access token
3. If a revoked token is reused (potential attack), **all tokens for that user are deleted**

---

## Project Structure

```
src/main/java/com/example/springstarter/
├── controller/
│   └── AuthController.java              # Register, Login, Refresh, Logout endpoints
├── domain/
│   ├── dto/
│   │   ├── AuthResponseDTO.java         # Token response (access + refresh + expiry)
│   │   ├── LoginRequestDTO.java         # Login payload (username + password)
│   │   ├── RefreshTokenRequestDTO.java  # Refresh/logout payload
│   │   └── RegisterRequestDTO.java      # Registration payload (validated)
│   └── entity/
│       └── RefreshToken.java            # Persisted refresh token entity
├── repository/
│   └── RefreshTokenRepository.java      # Refresh token CRUD + cleanup
├── security/
│   ├── CustomAccessDeniedHandler.java   # 403 JSON response handler
│   ├── JwtAuthenticationEntryPoint.java # 401 JSON response handler (OAuth2-aware)
│   ├── JwtService.java                  # Token generation only (Nimbus JOSE+JWT)
│   ├── SecurityConfig.java              # Central security config + OAuth2 resource server
│   ├── UserDetailsImpl.java             # Spring Security UserDetails wrapper
│   └── UserDetailsServiceImpl.java      # Loads user from DB (login only)
└── service/
    └── AuthService.java             # Business logic: register, login, refresh, logout
```

> **Removed:** `JwtAuthenticationFilter.java` — this class no longer exists. Token validation is handled entirely by Spring Security's `BearerTokenAuthenticationFilter`.

---

## Key Classes Explained

### `JwtService`
Responsible **only for token generation**. Validation and claim extraction are delegated to `NimbusJwtDecoder` (configured in `SecurityConfig`).

| Method | Purpose |
|--------|---------|
| `generateAccessToken(UserDetails)` | Creates a signed JWT with `role` claim, configured expiry |
| `generateRefreshToken(UserDetails)` | Creates a signed JWT with expiry only (no role claim) |
| `getAccessTokenExpiration()` | Returns access token TTL in milliseconds |
| `getRefreshTokenExpiration()` | Returns refresh token TTL in milliseconds |

**Library:** Uses the **Nimbus JOSE+JWT** library (bundled with `spring-boot-starter-oauth2-resource-server`). The old JJWT dependency has been removed.

**Signing:** HMAC-SHA256 (`HS256`) via `MACSigner` with a Base64-decoded secret key from `application.yaml`.

### `SecurityConfig`
The central `@Configuration` class. Key beans:

| Bean | Purpose |
|------|---------|
| `SecurityFilterChain` | Defines URL rules, session policy, and OAuth2 resource server |
| `JwtDecoder` | `NimbusJwtDecoder` that verifies HMAC-SHA256 signature + expiry |
| `JwtAuthenticationConverter` | Maps the `role` JWT claim to a `SimpleGrantedAuthority` |
| `PasswordEncoder` | BCrypt with default strength (10 rounds) |
| `AuthenticationManager` | Used by `AuthService` for login only |
| `DaoAuthenticationProvider` | Connects `UserDetailsService` + `PasswordEncoder` (login only) |

**OAuth2 Resource Server configuration:**
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt
        .decoder(jwtDecoder)
        .jwtAuthenticationConverter(jwtAuthenticationConverter())
    )
    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
)
```

### `JwtAuthenticationEntryPoint`
Returns a structured JSON 401 response. Handles both standard `AuthenticationException` and `OAuth2AuthenticationException` (thrown by the resource server when a token is malformed, expired, or has an invalid signature) — extracting human-readable OAuth2 error descriptions when available.

### `AuthService`
Business logic for the complete auth lifecycle:

| Method | What it does |
|--------|--------------|
| `register()` | Validates uniqueness → BCrypt hash → save user → generate tokens |
| `login()` | Authenticate via `AuthenticationManager` → generate tokens |
| `refreshToken()` | Validate refresh token via `JwtDecoder` → revoke old → issue new pair (rotation) |
| `logout()` | Revoke the refresh token |

---

## Security Best Practices

### What This Implementation Does Right
1. **Stateless sessions** — `SessionCreationPolicy.STATELESS` prevents `HttpSession` creation
2. **BCrypt password hashing** — Passwords are never stored in plaintext
3. **Short-lived access tokens** — 2-minute default limits the exposure window
4. **Refresh token rotation** — Old tokens are revoked on use, preventing replay attacks
5. **Token reuse detection** — If a revoked token is reused, all user tokens are deleted
6. **CSRF disabled** — Appropriate for stateless JWT APIs (CSRF attacks rely on cookies)
7. **Structured error responses** — 401/403 return consistent `ApiResponse` JSON, never HTML
8. **No sensitive data in tokens** — Only `username` and `role` in the payload
9. **No DB call per request** — `NimbusJwtDecoder` validates tokens cryptographically; `UserDetailsService` is only invoked during login

### Production Hardening Checklist
- [ ] **Environment-based secret**: Set `JWT_SECRET` env var (never commit secrets)
- [ ] **HTTPS only**: Configure `server.ssl` or use a reverse proxy (nginx)
- [ ] **CORS**: Configure allowed origins in `SecurityConfig` if serving a frontend
- [ ] **Rate limiting**: Add rate limiting to `/api/auth/**` to prevent brute-force
- [ ] **Account lockout**: Lock accounts after N failed login attempts
- [ ] **Refresh token cleanup**: Schedule a job to delete expired tokens periodically
- [ ] **Audit logging**: Log all auth events (login, logout, failed attempts)
- [ ] **Token blacklisting**: For immediate access token revocation (optional, adds state)

---

## Testing with cURL/Bruno

### 1. Register a New User
```bash
curl -X POST http://localhost:8888/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"mypassword123"}'
```

### 2. Login
```bash
curl -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

### 3. Access Protected Endpoint
```bash
# Replace <TOKEN> with the access_token from login response
curl http://localhost:8888/api/products \
  -H "Authorization: Bearer <TOKEN>"
```

### 4. Access Without Token (Expect 401)
```bash
curl http://localhost:8888/api/products
# Response: {"status":401,"message":"Authentication required. Please provide a valid JWT token."}
```

### 5. Access Admin Endpoint as Customer (Expect 403)
```bash
# Login as customer first, then:
curl -X POST http://localhost:8888/api/products \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test"}'
# Response: {"status":403,"message":"Access denied..."}
```

### 6. Refresh Token
```bash
curl -X POST http://localhost:8888/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

### 7. Logout
```bash
curl -X POST http://localhost:8888/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

---

## Dependencies

```xml
<!-- Spring Security (authentication & authorization framework) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Security OAuth2 Resource Server (bundles Nimbus JOSE+JWT) -->
<!-- Replaces the old JJWT (io.jsonwebtoken) dependency trio -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

> **JJWT removed:** The three `io.jsonwebtoken` artifacts (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) are no longer used. The Nimbus JOSE+JWT library is transitively included via `spring-boot-starter-oauth2-resource-server` and provides the `MACSigner`, `SignedJWT`, and `NimbusJwtDecoder` types used throughout the security layer.

---

## Database Migrations

### V4 — Refresh Tokens Table
Creates `refresh_tokens` table with indexes for fast token lookup and user-based queries.

### V5 — Hash Existing Passwords
Uses PostgreSQL's `pgcrypto` extension to BCrypt-hash existing plaintext passwords in the `users` table, making seed data compatible with `BCryptPasswordEncoder`.
