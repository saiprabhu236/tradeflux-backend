package com.sai.finance.finance_manager.explore.service;

import com.sai.finance.finance_manager.explore.dto.ChartPointDto;
import com.sai.finance.finance_manager.explore.dto.ExploreStockDto;
import com.sai.finance.finance_manager.explore.dto.SectorOverviewDto;
import com.sai.finance.finance_manager.explore.model.Candle;
import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.explore.registry.SectorUniverseLoader;
import com.sai.finance.finance_manager.marketdata.service.MarketStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorUniverseLoader sectorUniverseLoader;
    private final ExploreStateStore stateStore;
    private final ExploreService exploreService;
    private final ExploreYahooClient exploreYahooClient;
    private final MarketStatusService marketStatusService;

    private ExploreStockDto freezeIfClosed(ExploreStockDto dto) {
        if (!marketStatusService.isMarketOpenNow()) {
            dto.setChange(0);
            dto.setChangePercent(0);
            dto.setPrice(dto.getPreviousClose());
        }
        return dto;
    }

    public List<Map<String, Object>> getAllSectors() {
        return sectorUniverseLoader.getAllSectors().stream()
                .sorted()
                .map(sector -> {
                    List<String> symbols = sectorUniverseLoader.getSymbolsForSector(sector);

                    List<ExploreSymbolState> states = symbols.stream()
                            .map(stateStore::get)
                            .filter(Objects::nonNull)
                            .toList();

                    double avgChange = marketStatusService.isMarketOpenNow()
                            ? states.stream().mapToDouble(ExploreSymbolState::getChangePercent).average().orElse(0.0)
                            : 0.0;

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("sector", sector);
                    dto.put("todayChangePercent", avgChange);
                    dto.put("stockCount", symbols.size());
                    return dto;
                })
                .toList();
    }

    public SectorOverviewDto getSectorOverview(String sector) {
        List<String> symbols = sectorUniverseLoader.getSymbolsForSector(sector);

        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Unknown sector: " + sector);
        }

        List<ExploreSymbolState> states = symbols.stream()
                .map(stateStore::get)
                .filter(Objects::nonNull)
                .toList();

        double avgChange = marketStatusService.isMarketOpenNow()
                ? states.stream().mapToDouble(ExploreSymbolState::getChangePercent).average().orElse(0.0)
                : 0.0;

        List<ExploreStockDto> allStocks = states.stream()
                .map(exploreService::toDto)
                .map(this::freezeIfClosed)
                .toList();

        List<ExploreStockDto> topGainers = states.stream()
                .sorted(Comparator.comparingDouble(ExploreSymbolState::getChangePercent).reversed())
                .limit(20)
                .map(exploreService::toDto)
                .map(this::freezeIfClosed)
                .toList();

        List<ExploreStockDto> topLosers = states.stream()
                .sorted(Comparator.comparingDouble(ExploreSymbolState::getChangePercent))
                .limit(20)
                .map(exploreService::toDto)
                .map(this::freezeIfClosed)
                .toList();

        List<ChartPointDto> chart = generateSyntheticWeeklyIndex(symbols);

        return SectorOverviewDto.builder()
                .sector(sector)
                .todayChangePercent(avgChange)
                .stockCount(symbols.size())
                .chart(chart)
                .topGainers(topGainers)
                .topLosers(topLosers)
                .stocks(allStocks)
                .build();
    }

    private List<ChartPointDto> generateSyntheticWeeklyIndex(List<String> symbols) {
        Map<Long, List<Double>> weeklyValues = new HashMap<>();

        for (String symbol : symbols) {
            try {
                List<Candle> daily = exploreYahooClient.getDailyCandles(symbol, 5);
                if (daily.isEmpty()) continue;

                double base = daily.get(0).close();
                Map<Long, Double> weekly = new TreeMap<>();

                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

                for (Candle c : daily) {
                    cal.setTimeInMillis(c.timestamp());
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    long weekStart = cal.getTimeInMillis();

                    double normalized = (c.close() / base) * 100.0;
                    weekly.put(weekStart, normalized);
                }

                for (var entry : weekly.entrySet()) {
                    weeklyValues
                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }

            } catch (Exception ignored) {}
        }

        return weeklyValues.entrySet().stream()
                .map(e -> {
                    double avg = e.getValue().stream()
                            .mapToDouble(v -> v)
                            .average()
                            .orElse(0.0);

                    return new ChartPointDto(e.getKey(), avg);
                })
                .sorted(Comparator.comparingLong(ChartPointDto::getTimestamp))
                .toList();
    }
}
