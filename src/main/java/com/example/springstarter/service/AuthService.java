package com.example.springstarter.service;

import com.example.springstarter.domain.dto.AuthResponseDTO;
import com.example.springstarter.domain.dto.LoginRequestDTO;
import com.example.springstarter.domain.dto.RefreshTokenRequestDTO;
import com.example.springstarter.domain.dto.RegisterRequestDTO;
import com.example.springstarter.domain.entity.RefreshToken;
import com.example.springstarter.domain.entity.User;
import com.example.springstarter.domain.enumerate.Role;
import com.example.springstarter.exception.BusinessException;
import com.example.springstarter.repository.RefreshTokenRepository;
import com.example.springstarter.repository.UserRepository;
import com.example.springstarter.security.JwtService;
import com.example.springstarter.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' is already taken");
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email '" + request.getEmail() + "' is already registered");
        }

        // Create user with hashed password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER) // Default role for self-registration
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        return generateTokenPair(new UserDetailsImpl(user));
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            log.info("User logged in: {}", userDetails.getUsername());

            return generateTokenPair(userDetails);
        } catch (BadCredentialsException e) {
            throw new BusinessException("Invalid username or password");
        }
    }

    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (!storedToken.isUsable()) {
            // If token is revoked, it could be a token reuse attack — revoke all user tokens
            if (storedToken.isRevoked()) {
                log.warn("Refresh token reuse detected for user: {}", storedToken.getUser().getUsername());
                refreshTokenRepository.deleteByUser(storedToken.getUser());
            }
            throw new BusinessException("Refresh token is expired or revoked");
        }

        // Rotate: revoke old token and issue new pair
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UserDetailsImpl userDetails = new UserDetailsImpl(storedToken.getUser());
        log.info("Token refreshed for user: {}", userDetails.getUsername());

        return generateTokenPair(userDetails);
    }

    @Transactional
    public void logout(RefreshTokenRequestDTO request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
        log.info("User logged out: {}", storedToken.getUser().getUsername());
    }

    // ---- Private helpers ----

    private AuthResponseDTO generateTokenPair(UserDetailsImpl userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Persist the refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(userDetails.getUser())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .build();
    }
}
