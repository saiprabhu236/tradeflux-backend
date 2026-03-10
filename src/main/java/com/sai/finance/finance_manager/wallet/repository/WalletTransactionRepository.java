package com.sai.finance.finance_manager.wallet.repository;

import com.sai.finance.finance_manager.wallet.model.WalletTransaction;
import com.sai.finance.finance_manager.wallet.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletWalletIdOrderByTimestampDesc(Long walletId);

    Page<WalletTransaction> findByWalletWalletId(Long walletId, Pageable pageable);

    Page<WalletTransaction> findByWalletWalletIdAndTransactionType(
            Long walletId,
            TransactionType type,
            Pageable pageable
    );

    Page<WalletTransaction> findByWalletWalletIdAndTimestampBetween(
            Long walletId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    Page<WalletTransaction> findByWalletWalletIdAndTransactionTypeAndTimestampBetween(
            Long walletId,
            TransactionType type,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}