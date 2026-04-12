package com.sai.finance.finance_manager.explore.util;

public record ExploreStockDto(
        String symbol,
        double price,
        double changePercent
) {}