package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.model.RefreshToken;
import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.RefreshTokenRepository;
import com.sai.finance.finance_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final long REFRESH_TOKEN_VALIDITY_DAYS = 30;

    // -------------------------
    // CREATE NEW REFRESH TOKEN
    // -------------------------
    public RefreshToken createRefreshToken(User user) {

        // Revoke all old tokens for this user
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()) && !rt.isRevoked())
                .forEach(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    // -------------------------
    // VALIDATE REFRESH TOKEN
    // -------------------------
    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token is revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    // -------------------------
    // ROTATE TOKEN (REVOKE OLD + CREATE NEW)
    // -------------------------
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {

        // Revoke old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Create new token
        return createRefreshToken(oldToken.getUser());
    }
}