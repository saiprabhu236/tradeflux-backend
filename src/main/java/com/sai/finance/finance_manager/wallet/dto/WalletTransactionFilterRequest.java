package com.sai.finance.finance_manager.wallet.dto;

import com.sai.finance.finance_manager.wallet.model.TransactionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WalletTransactionFilterRequest {
    private TransactionType type;
    private LocalDateTime start;
    private LocalDateTime end;
    private int page;
    private int size;
}