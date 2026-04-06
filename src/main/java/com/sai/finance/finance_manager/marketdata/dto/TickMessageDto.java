package com.sai.finance.finance_manager.marketdata.dto;

import lombok.*;

@Data
@NoArgsConstructor
public class TickMessageDto {

    private String symbol;

    private double tickPrice;
    private double realPrice;

    private double open;
    private double high;
    private double low;
    private double close;
    private double previousClose;

    private double change;
    private double changePercent;

    private long volume;

    private long timestamp;

    private double bidPrice;
    private double askPrice;
    private double spread;

    // getters + setters
}