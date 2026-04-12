package com.sai.finance.finance_manager.marketdata.model;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class SymbolState {

    private double lastRealPrice;
    private double lastTickPrice;
    private long lastRealUpdateTime;
    private long lastTickTime;
    // Intraday metrics
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume; // simulated or 0

    private double previousClose;

    // 52-week metrics (optional, simulated or fetched once per day)
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;

    private double change;
    private double changePercent;

    private double bidPrice;
    private double askPrice;
    private double spread;
    // For daily reset
    private LocalDate lastResetDate = LocalDate.now();

}
