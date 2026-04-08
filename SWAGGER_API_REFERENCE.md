# Swagger API Implementation Reference

This document explains how Swagger (OpenAPI) is integrated in this project and how to use it with JWT authentication.

## 1) What was added

Swagger support is implemented with Springdoc.

### Dependency

Added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>
```

### OpenAPI configuration

Created `src/main/java/com/example/springstarter/config/OpenApiConfig.java` with:
- API metadata (`title`, `version`, `description`)
- JWT bearer security scheme named `bearerAuth`
- Global security requirement so secured endpoints show lock icons in Swagger UI

### Security whitelist for docs endpoints

Updated `src/main/java/com/example/springstarter/security/SecurityConfig.java` to allow unauthenticated access to:
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### Springdoc properties

Updated `src/main/resources/application.yaml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## 2) How to run and open Swagger UI

Start the application, then open:
- Swagger UI: `http://localhost:8888/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8888/v3/api-docs`

> The project currently uses port `8888` from `application.yaml`.

## 3) Using JWT in Swagger UI

Because APIs are secured by JWT, use these steps in Swagger UI:

1. Call `POST /api/auth/login` with valid credentials.
2. Copy `data.access_token` from the response payload.
3. Click **Authorize** in Swagger UI.
4. Enter token as: `Bearer <access_token>`
5. Execute secured endpoints.

Example login body:

```json
{
  "username": "admin",
  "password": "password123"
}
```

## 4) Key implementation files

- `pom.xml`
- `src/main/java/com/example/springstarter/config/OpenApiConfig.java`
- `src/main/java/com/example/springstarter/security/SecurityConfig.java`
- `src/main/resources/application.yaml`

## 5) Optional improvements

For cleaner Swagger docs, annotate controllers/DTOs with:
- `@Tag`
- `@Operation`
- `@ApiResponses`
- `@Schema`

This is optional; Swagger UI already works with Spring MVC mappings.

## 6) Troubleshooting

- `401` on Swagger endpoints: verify docs paths are whitelisted in `SecurityConfig`.
- Swagger UI not loading: confirm app is running on port `8888` and open `/swagger-ui.html`.
- Auth works in Postman but not Swagger: ensure token is entered with `Bearer ` prefix.
- Build does not start in terminal due to Java setup: set `JAVA_HOME` correctly before running Maven.

