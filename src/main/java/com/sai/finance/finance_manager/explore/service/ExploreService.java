package com.sai.finance.finance_manager.explore.service;

import com.sai.finance.finance_manager.explore.dto.ExploreStockDto;
import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.marketdata.service.MarketStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final ExploreStateStore stateStore;
    private final MarketStatusService marketStatusService;

    // Freeze logic
    private ExploreStockDto freezeIfClosed(ExploreStockDto dto) {
        if (!marketStatusService.isMarketOpenNow()) {
            dto.setChange(0);
            dto.setChangePercent(0);
            dto.setPrice(dto.getPreviousClose());
        }
        return dto;
    }

    public List<ExploreStockDto> getTopGainers(int limit) {
        return stateStore.getAll().stream()
                .filter(s -> s.getPreviousClose() > 0)
                .sorted(Comparator.comparing(ExploreSymbolState::getChangePercent).reversed())
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }

    public List<ExploreStockDto> getTopLosers(int limit) {
        return stateStore.getAll().stream()
                .filter(s -> s.getPreviousClose() > 0)
                .sorted(Comparator.comparing(ExploreSymbolState::getChangePercent))
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }

    public ExploreStockDto toDto(ExploreSymbolState s) {
        return ExploreStockDto.builder()
                .symbol(s.getSymbol())
                .price(s.getTickPrice())
                .changePercent(s.getChangePercent())
                .change(s.getChange())
                .previousClose(s.getPreviousClose())
                .dayHigh(s.getDayHigh())
                .dayLow(s.getDayLow())
                .volume(s.getVolume())
                .averageVolume(s.getAverageVolume())
                .fiftyTwoWeekHigh(s.getFiftyTwoWeekHigh())
                .fiftyTwoWeekLow(s.getFiftyTwoWeekLow())
                .trending(s.isTrending())
                .near52WeekHigh(s.isNear52WeekHigh())
                .near52WeekLow(s.isNear52WeekLow())
                .detailUrl("/market/price?symbol=" + s.getSymbol())
                .build();
    }

    public List<ExploreStockDto> getMostActive(int limit) {
        return stateStore.getAll().stream()
                .sorted(Comparator.comparingLong(ExploreSymbolState::getVolume).reversed())
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }

    public List<ExploreStockDto> getTrending(int limit) {
        return stateStore.getAll().stream()
                .filter(s -> s.getAverageVolume() > 0)
                .sorted((a, b) -> {
                    double spikeA = a.getVolume() / (double) a.getAverageVolume();
                    double spikeB = b.getVolume() / (double) b.getAverageVolume();
                    return Double.compare(spikeB, spikeA);
                })
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }

    public List<ExploreStockDto> get52WeekHigh(int limit) {
        return stateStore.getAll().stream()
                .filter(s -> s.getTickPrice() >= s.getFiftyTwoWeekHigh())
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }

    public List<ExploreStockDto> get52WeekLow(int limit) {
        return stateStore.getAll().stream()
                .filter(s -> s.getTickPrice() <= s.getFiftyTwoWeekLow())
                .limit(limit)
                .map(this::toDto)
                .map(this::freezeIfClosed)
                .toList();
    }
}
