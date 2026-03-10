package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.security.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String generateToken(String email) {
        return jwtUtil.generateToken(email);
    }
    public String extractUsername(String token) {
        return jwtUtil.extractEmail(token);
    }
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

}