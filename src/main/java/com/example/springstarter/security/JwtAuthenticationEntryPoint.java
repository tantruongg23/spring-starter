package com.example.springstarter.security;

import com.example.springstarter.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a structured JSON 401 response for any unauthenticated request.
 *
 * <p>This handler is invoked both by the OAuth2 Resource Server (when a bearer token
 * is missing or invalid) and by the standard Spring Security filter chain.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        String reason = resolveReason(authException);
        log.warn("Unauthorized access attempt to [{} {}]: {}",
                request.getMethod(), request.getRequestURI(), reason);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication required. " + reason
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Extracts a human-readable reason from the exception, including OAuth2 error descriptions.
     */
    private String resolveReason(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauth2Ex
                && oauth2Ex.getError() != null
                && oauth2Ex.getError().getDescription() != null) {
            return oauth2Ex.getError().getDescription();
        }
        return "Please provide a valid JWT token.";
    }
}
