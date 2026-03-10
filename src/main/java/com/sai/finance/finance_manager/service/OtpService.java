package com.sai.finance.finance_manager.service;

import com.sai.finance.finance_manager.model.Otp;
import com.sai.finance.finance_manager.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpRepository otpRepository;
    private final Map<String, Long> otpTimestamps = new HashMap<>();

    // Generate + save + send OTP with cooldown + expiry
    public String generateAndSendOtp(String email) {

        LocalDateTime now = LocalDateTime.now();

        // Check if OTP already exists for this email
        Optional<Otp> existingOtpOpt = otpRepository.findByEmail(email);

        if (existingOtpOpt.isPresent()) {
            Otp existingOtp = existingOtpOpt.get();

            // 1-minute cooldown based on createdAt
            LocalDateTime lastSent = existingOtp.getCreatedAt();
            LocalDateTime oneMinuteLater = lastSent.plusMinutes(1);

            if (now.isBefore(oneMinuteLater)) {
                long secondsLeft = Duration.between(now, oneMinuteLater).getSeconds();
                return "Please wait " + secondsLeft + " seconds before requesting a new OTP.";
            }

            // Delete old OTP before creating a new one
            otpRepository.delete(existingOtp);
        }


        // Generate new 6-digit OTP
        String otpCode = String.valueOf((int) (Math.random() * 900000) + 100000);

        // 2-minute expiry
        LocalDateTime expiresAt = now.plusMinutes(2);

        // Save OTP in DB
        Otp otpEntity = new Otp(email, otpCode, now, expiresAt);
        otpRepository.save(otpEntity);

        // Send email
        sendOtpEmail(email, otpCode);

        return "OTP sent to " + email;
    }

    // Verify OTP (DB-based)
    public boolean verifyOtp(String email, String otp) {

        Optional<Otp> otpOpt = otpRepository.findByEmail(email);

        if (otpOpt.isEmpty()) {
            return false; // no OTP for this email
        }

        Otp otpEntity = otpOpt.get();

        // Check expiry
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            return false; // expired
        }

        // Check match
        if (!otpEntity.getOtp().equals(otp)) {
            return false; // invalid
        }

        // Valid OTP → delete it
        otpRepository.delete(otpEntity);
        return true;
    }

    // Send email
    private void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp + "\n\nThis OTP is valid for 2 minutes.");
        mailSender.send(message);
    }
}