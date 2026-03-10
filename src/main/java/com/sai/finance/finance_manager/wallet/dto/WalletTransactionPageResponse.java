package com.sai.finance.finance_manager.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WalletTransactionPageResponse {
    private List<WalletTransactionResponse> transactions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}