package com.sai.finance.finance_manager.explore.dto;

import java.util.List;

public class ThemeOverviewDto {

    private String theme;
    private Double todayChangePercent;
    private List<ExploreStockDto> topGainers;
    private List<ExploreStockDto> topLosers;
    private List<ExploreStockDto> stocks;

    public ThemeOverviewDto(String theme,
                            Double todayChangePercent,
                            List<ExploreStockDto> topGainers,
                            List<ExploreStockDto> topLosers,
                            List<ExploreStockDto> stocks) {
        this.theme = theme;
        this.todayChangePercent = todayChangePercent;
        this.topGainers = topGainers;
        this.topLosers = topLosers;
        this.stocks = stocks;
    }

    public String getTheme() {
        return theme;
    }

    public Double getTodayChangePercent() {
        return todayChangePercent;
    }

    public List<ExploreStockDto> getTopGainers() {
        return topGainers;
    }

    public List<ExploreStockDto> getTopLosers() {
        return topLosers;
    }

    public List<ExploreStockDto> getStocks() {
        return stocks;
    }
}

