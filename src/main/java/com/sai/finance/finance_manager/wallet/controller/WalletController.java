package com.sai.finance.finance_manager.wallet.controller;

import com.sai.finance.finance_manager.model.User;
import com.sai.finance.finance_manager.repository.UserRepository;
import com.sai.finance.finance_manager.service.JwtService;
import com.sai.finance.finance_manager.wallet.dto.*;
import com.sai.finance.finance_manager.wallet.model.WalletTransaction;
import com.sai.finance.finance_manager.wallet.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final WalletService walletService;

    public WalletController(JwtService jwtService,
                            UserRepository userRepository,
                            WalletService walletService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    // ---------------------------------------------------------
    // Extract userId from JWT token
    // ---------------------------------------------------------
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // ---------------------------------------------------------
    // 1. Get Wallet Balance
    // ---------------------------------------------------------
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        BigDecimal balance = walletService.getCurrentBalance(userId);

        WalletBalanceResponse response = WalletBalanceResponse.builder()
                .balance(balance)
                .currency("INR")
                .build();

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // Convert Entity → DTO
    // ---------------------------------------------------------
    private WalletTransactionResponse toDto(WalletTransaction txn) {
        return WalletTransactionResponse.builder()
                .amount(txn.getAmount())
                .transactionType(txn.getTransactionType())
                .description(txn.getDescription())
                .category(txn.getCategory())
                .referenceId(txn.getReferenceId())
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .timestamp(txn.getTimestamp())
                .build();
    }

    // ---------------------------------------------------------
    // 2. Get All Transactions (DESC)
    // ---------------------------------------------------------
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@RequestHeader("Authorization") String authHeader) {

        Long userId = getUserIdFromToken(authHeader);

        List<WalletTransaction> txns = walletService.getAllTransactions(userId);

        List<WalletTransactionResponse> response = txns.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // 3. Add Transaction (CREDIT / DEBIT)
    // ---------------------------------------------------------
    @PostMapping("/transaction")
    public ResponseEntity<?> addTransaction(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody WalletTransactionRequest request) {

        Long userId = getUserIdFromToken(authHeader);

        WalletTransaction txn = walletService.addTransaction(userId, request);

        WalletTransactionResponse response = WalletTransactionResponse.builder()
                .amount(txn.getAmount())
                .transactionType(txn.getTransactionType())
                .description(txn.getDescription())
                .category(txn.getCategory())
                .referenceId(txn.getReferenceId())
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .timestamp(txn.getTimestamp())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/filter")
    public ResponseEntity<?> filterTransactions(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody WalletTransactionFilterRequest request) {

        Long userId = getUserIdFromToken(authHeader);

        Page<WalletTransaction> pageResult = walletService.filterTransactions(
                userId,
                request.getType(),
                request.getStart(),
                request.getEnd(),
                request.getPage(),
                request.getSize()
        );

        List<WalletTransactionResponse> dtos = pageResult.getContent().stream()
                .map(this::toDto)
                .toList();

        WalletTransactionPageResponse response = WalletTransactionPageResponse.builder()
                .transactions(dtos)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }
}