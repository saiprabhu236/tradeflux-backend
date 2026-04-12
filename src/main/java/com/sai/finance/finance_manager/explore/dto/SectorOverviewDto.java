package com.sai.finance.finance_manager.explore.dto;

import com.sai.finance.finance_manager.explore.dto.ExploreStockDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SectorOverviewDto {
    private String sector;
    private double todayChangePercent;
    private int stockCount;
    private List<ChartPointDto> chart;          // weekly synthetic index
    private List<ExploreStockDto> topGainers;
    private List<ExploreStockDto> topLosers;
    private List<ExploreStockDto> stocks;
}

