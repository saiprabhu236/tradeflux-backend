package com.sai.finance.finance_manager.wallet.dto;

import com.sai.finance.finance_manager.wallet.model.TransactionType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTransactionRequest {
    private TransactionType transactionType;   // CREDIT or DEBIT
    private BigDecimal amount;
    private String description;
    private String category;
}