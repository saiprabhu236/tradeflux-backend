package com.sai.finance.finance_manager.explore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ExploreStockDto {

    private String symbol;
    private double price;
    private double changePercent;

    private double change;
    private double previousClose;
    private double dayHigh;
    private double dayLow;

    private long volume;
    private long averageVolume;

    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;

    private boolean trending;
    private boolean near52WeekHigh;
    private boolean near52WeekLow;

    private String detailUrl;
}