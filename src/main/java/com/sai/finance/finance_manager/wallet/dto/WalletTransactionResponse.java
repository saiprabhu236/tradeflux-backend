package com.sai.finance.finance_manager.wallet.dto;

import com.sai.finance.finance_manager.wallet.model.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletTransactionResponse {
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private String category;
    private String referenceId;
    private BigDecimal balanceAfterTransaction;
    private LocalDateTime timestamp;
}