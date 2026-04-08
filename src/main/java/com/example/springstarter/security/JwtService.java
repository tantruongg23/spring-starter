package com.example.springstarter.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static com.example.springstarter.domain.enumerate.Role.CUSTOMER;

/**
 * Responsible only for JWT <em>generation</em>.
 * <p>
 * Token <em>validation</em> and claim extraction are delegated entirely to
 * Spring Security's {@code NimbusJwtDecoder}, which is configured as a bean
 * in {@link SecurityConfig} and wired into the OAuth2 Resource Server filter chain.
 * </p>
 */
@Service
@Slf4j
public class JwtService {

    /** Claim name for the user's role inside the access token. */
    public static final String ROLE_CLAIM = "role";

    private final byte[] secretKeyBytes;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.jwt.access-token-expiration}") Duration accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration) {
        this.secretKeyBytes = Base64.getDecoder().decode(secretKey);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * Generate a signed access token containing the user's role claim.
     */
    public String generateAccessToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(CUSTOMER.getAuthority());

        JWTClaimsSet claims = baseClaimsBuilder(userDetails.getUsername(), accessTokenExpiration)
                .claim(ROLE_CLAIM, role)
                .build();

        return sign(claims);
    }

    /**
     * Generate a signed refresh token (subject + expiry only — no role claim).
     */
    public String generateRefreshToken(UserDetails userDetails) {
        JWTClaimsSet claims = baseClaimsBuilder(userDetails.getUsername(), refreshTokenExpiration)
                .build();

        return sign(claims);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration.toMillis();
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration.toMillis();
    }

    // ---- Private helpers ----

    private JWTClaimsSet.Builder baseClaimsBuilder(String subject, Duration ttl) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)));
    }

    private String sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new MACSigner(secretKeyBytes));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}
