package com.sai.finance.finance_manager.repository;

import com.sai.finance.finance_manager.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByEmail(String email);

    void deleteByExpiresAtBefore(LocalDateTime time);
}