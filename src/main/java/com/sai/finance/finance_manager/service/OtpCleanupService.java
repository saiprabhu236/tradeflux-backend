package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.repository.OtpRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OtpCleanupService {

    @Autowired
    private OtpRepository otpRepository;

    @Transactional
    @Scheduled(fixedRate = 600000) // every 10 minutes
    public void deleteExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        System.out.println("Expired OTPs cleaned at: " + LocalDateTime.now());
    }
}