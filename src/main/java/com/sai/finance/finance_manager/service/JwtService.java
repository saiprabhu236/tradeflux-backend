package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // -------------------------
    // TOKEN GENERATION
    // -------------------------
    public String generateAccessToken(String email) {
        return jwtUtil.generateAccessToken(email);
    }

    public String generateRefreshToken(String email) {
        return jwtUtil.generateRefreshToken(email);
    }

    // -------------------------
    // CLAIMS
    // -------------------------
    public String extractUsername(String token) {
        return jwtUtil.extractEmail(token);
    }

    public Claims extractAllClaims(String token) {
        return jwtUtil.extractAllClaims(token);
    }

    // -------------------------
    // VALIDATION
    // -------------------------
    public boolean validateAccessToken(String token) {
        return jwtUtil.validateAccessToken(token);
    }

    public boolean validateRefreshToken(String token) {
        return jwtUtil.validateRefreshToken(token);
    }

    public boolean isTokenExpired(String token) {
        return jwtUtil.isTokenExpired(token);
    }
}