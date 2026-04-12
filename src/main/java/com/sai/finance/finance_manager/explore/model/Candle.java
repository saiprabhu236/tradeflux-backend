package com.sai.finance.finance_manager.explore.model;

public record Candle(
        long timestamp,
        double open,
        double high,
        double low,
        double close,
        long volume
) {}