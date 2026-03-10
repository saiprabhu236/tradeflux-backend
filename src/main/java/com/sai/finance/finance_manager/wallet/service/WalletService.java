package com.sai.finance.finance_manager.wallet.service;

import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.wallet.dto.WalletTransactionRequest;
import com.sai.finance.finance_manager.wallet.model.*;
import com.sai.finance.finance_manager.wallet.repository.WalletRepository;
import com.sai.finance.finance_manager.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ---------------------------------------------------------
    // 1. Create wallet for new user with initial 500000 credit
    // ---------------------------------------------------------
    public Wallet createWalletForUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(new BigDecimal("500000"))
                .lockedBalance(BigDecimal.ZERO)
                .currency("INR")
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        // Create initial credit transaction
        createTransaction(
                savedWallet,
                TransactionType.CREDIT,
                new BigDecimal("500000"),
                "Initial Virtual Balance",
                "INITIAL_CREDIT",
                "INIT"
        );

        return savedWallet;
    }

    // ---------------------------------------------------------
    // 2. Get current wallet balance (user sees only their own)
    // ---------------------------------------------------------
    public BigDecimal getCurrentBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return wallet.getBalance();
    }

    // ---------------------------------------------------------
    // 3. Add a transaction (credit or debit)
    // ---------------------------------------------------------
    public WalletTransaction createTransaction(
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            String description,
            String category,
            String referenceId
    ) {

        BigDecimal newBalance;

        if (type == TransactionType.CREDIT) {
            newBalance = wallet.getBalance().add(amount);
        } else {
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            newBalance = wallet.getBalance().subtract(amount);
        }

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(type)
                .amount(amount)
                .description(description)
                .category(category)
                .referenceId(referenceId)
                .balanceAfterTransaction(newBalance)
                .timestamp(LocalDateTime.now())
                .build();

        return transactionRepository.save(txn);
    }

    // ---------------------------------------------------------
    // 4. Get all transactions (sorted DESC)
    // ---------------------------------------------------------
    public List<WalletTransaction> getAllTransactions(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return transactionRepository.findByWalletWalletIdOrderByTimestampDesc(wallet.getWalletId());
    }

    // ---------------------------------------------------------
    // 5. Filter transactions with pagination
    // ---------------------------------------------------------
    public Page<WalletTransaction> filterTransactions(
            Long userId,
            TransactionType type,
            LocalDateTime start,
            LocalDateTime end,
            int page,
            int size
    ) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Pageable pageable = PageRequest.of(page, size);

        if (type != null && start != null && end != null) {
            return transactionRepository
                    .findByWalletWalletIdAndTransactionTypeAndTimestampBetween(
                            wallet.getWalletId(), type, start, end, pageable);
        }

        if (type != null) {
            return transactionRepository
                    .findByWalletWalletIdAndTransactionType(
                            wallet.getWalletId(), type, pageable);
        }

        if (start != null && end != null) {
            return transactionRepository
                    .findByWalletWalletIdAndTimestampBetween(
                            wallet.getWalletId(), start, end, pageable);
        }

        return transactionRepository.findByWalletWalletId(wallet.getWalletId(), pageable);
    }

    public WalletTransaction addTransaction(Long userId, WalletTransactionRequest request) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return createTransaction(
                wallet,
                request.getTransactionType(),
                request.getAmount(),
                request.getDescription(),
                request.getCategory(),
                "REF-" + System.currentTimeMillis()   // auto referenceId
        );
    }
}