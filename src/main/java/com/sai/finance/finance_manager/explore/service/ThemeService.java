package com.sai.finance.finance_manager.explore.service;

import com.sai.finance.finance_manager.explore.dto.ExploreStockDto;
import com.sai.finance.finance_manager.explore.dto.ThemeOverviewDto;
import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.explore.registry.ThemeUniverseLoader;
import com.sai.finance.finance_manager.marketdata.service.MarketStatusService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ThemeService {

    private final ThemeUniverseLoader themeUniverseLoader;
    private final ExploreStateStore exploreStateStore;
    private final MarketStatusService marketStatusService;

    public ThemeService(ThemeUniverseLoader themeUniverseLoader,
                        ExploreStateStore exploreStateStore,
                        MarketStatusService marketStatusService) {
        this.themeUniverseLoader = themeUniverseLoader;
        this.exploreStateStore = exploreStateStore;
        this.marketStatusService = marketStatusService;
    }

    private ExploreStockDto freezeIfClosed(ExploreStockDto dto) {
        if (!marketStatusService.isMarketOpenNow()) {
            dto.setChange(0);
            dto.setChangePercent(0);
            dto.setPrice(dto.getPreviousClose());
        }
        return dto;
    }

    public List<String> getAllThemes() {
        return themeUniverseLoader.getAllThemes()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public Optional<ThemeOverviewDto> getThemeOverview(String theme) {
        if (!themeUniverseLoader.themeExists(theme)) {
            return Optional.empty();
        }

        List<String> symbols = themeUniverseLoader.getSymbolsForTheme(theme);

        List<ExploreStockDto> stocks = symbols.stream()
                .map(this::toExploreStockDto)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::freezeIfClosed)
                .collect(Collectors.toList());

        if (stocks.isEmpty()) {
            return Optional.empty();
        }

        double todayChangePercent = marketStatusService.isMarketOpenNow()
                ? computeAverageChangePercent(stocks)
                : 0.0;

        List<ExploreStockDto> sortedByChange = stocks.stream()
                .sorted(Comparator.comparingDouble(ExploreStockDto::getChangePercent).reversed())
                .collect(Collectors.toList());

        List<ExploreStockDto> topGainers = sortedByChange.stream()
                .limit(5)
                .map(this::freezeIfClosed)
                .collect(Collectors.toList());

        List<ExploreStockDto> topLosers = sortedByChange.stream()
                .sorted(Comparator.comparingDouble(ExploreStockDto::getChangePercent))
                .limit(5)
                .map(this::freezeIfClosed)
                .collect(Collectors.toList());

        ThemeOverviewDto dto = new ThemeOverviewDto(
                theme,
                todayChangePercent,
                topGainers,
                topLosers,
                stocks
        );

        return Optional.of(dto);
    }

    private Optional<ExploreStockDto> toExploreStockDto(String symbol) {
        ExploreSymbolState s = exploreStateStore.get(symbol);
        if (s == null) {
            return Optional.empty();
        }

        ExploreStockDto dto = ExploreStockDto.builder()
                .symbol(symbol)
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

        return Optional.of(dto);
    }

    private double computeAverageChangePercent(List<ExploreStockDto> stocks) {
        return stocks.stream()
                .mapToDouble(ExploreStockDto::getChangePercent)
                .average()
                .orElse(0.0);
    }
}
