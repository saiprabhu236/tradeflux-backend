package com.sai.finance.finance_manager.explore.model;

import lombok.*;

@Data
public class ExploreSymbolState {

    private final String symbol;

    // Prices
    private double anchorPrice;
    private double previousClose;
    private double tickPrice;
    private double changePercent;
    private double change;

    // Day metrics
    private double dayHigh;
    private double dayLow;

    // Volume metrics
    private long volume;
    private long averageVolume;

    // 52-week metrics
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;

    // Flags
    private boolean trending;
    private boolean near52WeekHigh;
    private boolean near52WeekLow;

    // Volatility factor
    private double volatility;

    public ExploreSymbolState(String symbol) {
        this.symbol = symbol;
        this.volatility = 0.5 + Math.random() * 1.5;
    }
}