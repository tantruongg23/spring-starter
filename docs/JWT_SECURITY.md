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
┌──────────┐        ┌─────────────────────┐       ┌──────────────────┐
│  Client  │──JWT──▷│ JwtAuthFilter       │──────▷│  Controller      │
│ (Bruno)  │        │ (OncePerRequestFilter│       │  (Protected)     │
└──────────┘        └──────────┬──────────┘       └──────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │    JwtService       │
                    │ (parse/validate/    │
                    │  generate tokens)   │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │ UserDetailsService  │
                    │ (load user from DB) │
                    └─────────────────────┘
```

**Flow:**
1. Client sends `Authorization: Bearer <token>` header
2. `JwtAuthenticationFilter` intercepts the request
3. `JwtService` validates the token (signature, expiry, claims)
4. `UserDetailsServiceImpl` loads the user from PostgreSQL
5. `SecurityContextHolder` is populated with the authenticated user
6. The request proceeds to the controller

**On authentication failure:**
- Missing/invalid token → `JwtAuthenticationEntryPoint` returns **401**
- Insufficient role → `CustomAccessDeniedHandler` returns **403**

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
| **Lifetime** | Short (15 minutes) | Long (7 days) |
| **Contains** | username, role, expiry | username, expiry only |
| **Stored in** | Client memory (JS variable) | HttpOnly cookie or secure storage |
| **Sent with** | Every API request (`Authorization` header) | Only to `/api/auth/refresh` |
| **Persisted on server** | No | Yes (in `refresh_tokens` table) |

---

## Configuration Reference

All JWT configuration is in `application.yaml`:

```yaml
security:
  jwt:
    # HMAC-SHA256 signing key (Base64-encoded, minimum 256 bits)
    # In production: use ${JWT_SECRET} environment variable
    secret-key: ${JWT_SECRET:dGhpcy1pcy1hLXZlcnktbG9uZy1...}

    # Access token lifetime in milliseconds (default: 15 minutes)
    access-token-expiration: 900000

    # Refresh token lifetime in milliseconds (default: 7 days)
    refresh-token-expiration: 604800000
```

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
        "expires_in": 900
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
        "expires_in": 900
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
│  (15 min)    │     │   (7 days)   │
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
│   └── AuthController.java           # Register, Login, Refresh, Logout endpoints
├── domain/
│   ├── dto/
│   │   ├── AuthResponseDTO.java      # Token response (access + refresh + expiry)
│   │   ├── LoginRequestDTO.java      # Login payload (username + password)
│   │   ├── RefreshTokenRequestDTO.java # Refresh/logout payload
│   │   └── RegisterRequestDTO.java   # Registration payload (validated)
│   └── entity/
│       └── RefreshToken.java         # Persisted refresh token entity
├── repository/
│   └── RefreshTokenRepository.java   # Refresh token CRUD + cleanup
├── security/
│   ├── CustomAccessDeniedHandler.java    # 403 JSON response handler
│   ├── JwtAuthenticationEntryPoint.java  # 401 JSON response handler
│   ├── JwtAuthenticationFilter.java      # Extracts & validates JWT from header
│   ├── JwtService.java                   # Token generation, parsing, validation
│   ├── SecurityConfig.java               # Central security configuration
│   ├── UserDetailsImpl.java              # Spring Security UserDetails wrapper
│   └── UserDetailsServiceImpl.java       # Loads user from DB for auth
└── service/
    └── AuthService.java              # Business logic: register, login, refresh, logout
```

---

## Key Classes Explained

### `JwtService`
The **core JWT utility**. All token operations go through here.

| Method | Purpose |
|--------|---------|
| `generateAccessToken(UserDetails)` | Creates a signed JWT with role claim, 15min expiry |
| `generateRefreshToken(UserDetails)` | Creates a signed JWT with 7-day expiry |
| `extractUsername(token)` | Parses the `sub` (subject) claim |
| `isTokenValid(token, UserDetails)` | Validates signature + expiry + username match |

**Signing:** Uses HMAC-SHA256 (`HS256`) with a Base64-decoded secret key via the JJWT library.

### `JwtAuthenticationFilter`
Extends `OncePerRequestFilter` — runs **once per HTTP request**.

1. Checks for `Authorization: Bearer <token>` header
2. If present, extracts and validates the JWT via `JwtService`
3. On success: populates `SecurityContextHolder` with the authenticated user
4. On failure: does nothing (lets `JwtAuthenticationEntryPoint` handle it)

### `SecurityConfig`
The central `@Configuration` class. Key beans:

| Bean | Purpose |
|------|---------|
| `SecurityFilterChain` | Defines URL rules, session policy, filter chain |
| `PasswordEncoder` | BCrypt with default strength (10 rounds) |
| `AuthenticationManager` | Used by `AuthService` for login authentication |
| `DaoAuthenticationProvider` | Connects `UserDetailsService` + `PasswordEncoder` |

### `AuthService`
Business logic for the complete auth lifecycle:

| Method | What it does |
|--------|--------------|
| `register()` | Validates uniqueness → BCrypt hash → save user → generate tokens |
| `login()` | Authenticate via `AuthenticationManager` → generate tokens |
| `refreshToken()` | Validate refresh token → revoke old → issue new pair (rotation) |
| `logout()` | Revoke the refresh token |

---

## Security Best Practices

### What This Implementation Does Right
1. **Stateless sessions** — `SessionCreationPolicy.STATELESS` prevents HttpSession creation
2. **BCrypt password hashing** — Passwords are never stored in plaintext
3. **Short-lived access tokens** — 15 minutes limits exposure window
4. **Refresh token rotation** — Old tokens are revoked on use, preventing replay attacks
5. **Token reuse detection** — If a revoked token is reused, all user tokens are deleted
6. **CSRF disabled** — Appropriate for stateless JWT APIs (CSRF attacks rely on cookies)
7. **Structured error responses** — 401/403 return consistent `ApiResponse` JSON, never HTML
8. **No sensitive data in tokens** — Only `username` and `role` in the payload

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
# Response: {"status":401,"message":"Authentication required..."}
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

## Dependencies Added

```xml
<!-- Spring Security (authentication & authorization framework) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JJWT — Java JWT library for token creation and parsing -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

## Database Migrations

### V4 — Refresh Tokens Table
Creates `refresh_tokens` table with indexes for fast token lookup and user-based queries.

### V5 — Hash Existing Passwords
Uses PostgreSQL's `pgcrypto` extension to BCrypt-hash existing plaintext passwords in the `users` table, making seed data compatible with `BCryptPasswordEncoder`.
