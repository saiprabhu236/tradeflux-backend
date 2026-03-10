package com.sai.finance.finance_manager.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletBalanceResponse {
    private BigDecimal balance;
    private String currency;
}