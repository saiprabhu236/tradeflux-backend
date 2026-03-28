package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.dto.SearchResultDto;
import com.sai.finance.finance_manager.marketdata.dto.CandleDto;
import com.sai.finance.finance_manager.marketdata.dto.StockMetricsDto;
import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import com.sai.finance.finance_manager.marketdata.util.SymbolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final SnapshotService snapshotService;
    private final MarketDataSubscriptionManager subscriptionManager;
    private final NseSymbolStore symbolStore;
    private final YahooFinanceClient yahooFinanceClient;

    private static final Map<String, String[]> PERIOD_MAP = Map.of(
            "1d",  new String[]{"1d", "1m"},
            "5d",  new String[]{"5d", "15m"},
            "1mo", new String[]{"1mo", "1d"},
            "6mo", new String[]{"6mo", "1d"},
            "1y",  new String[]{"1y", "1d"},
            "5y",  new String[]{"5y", "1wk"},
            "max", new String[]{"max", "1mo"}
    );

    // SEARCH
    public List<SearchResultDto> searchStocks(String query) {
        log.info("Searching for: {}", query);
        symbolStore.search(query).forEach(s -> log.info("Match: {} - {}", s.symbol(), s.name()));
        return symbolStore.search(query).stream()
                .map(s -> new SearchResultDto(s.symbol(), s.name()))
                .toList();
    }

    // CURRENT PRICE (synthetic tick + real)
    public PriceDto getCurrentPrice(String symbol) {
        snapshotService.ensureSymbolTracked(symbol);

        SymbolState state = snapshotService.getState(symbol);

        double real = state.getLastRealPrice();
        double tick = state.getLastTickPrice() > 0 ? state.getLastTickPrice() : real;

        double change = tick - real;
        double changePct = real != 0 ? (change / real) * 100 : 0;

        return new PriceDto(
                symbol.replace(".NS", ""),
                tick,
                real,
                change,
                changePct
        );
    }

    // HISTORICAL DATA
    public List<CandleDto> getHistoricalData(String symbol, String period) {
        String yahooSymbol = SymbolMapper.toYahooSymbol(symbol);
        validateSymbol(yahooSymbol);

        String[] mapped = PERIOD_MAP.getOrDefault(period, new String[]{"1d", "5m"});
        String range = mapped[0];
        String interval = mapped[1];

        return yahooFinanceClient.fetchHistorical(yahooSymbol, range, interval);
    }

    // METRICS (snapshot only)
    public StockMetricsDto getStockMetrics(String symbol) {

        //String normalized = subscriptionManager.normalizeSymbol(symbol);
        String yahooSymbol = SymbolMapper.toYahooSymbol(symbol);
        return yahooFinanceClient.fetchMetrics(yahooSymbol);
    }

    // SUBSCRIPTIONS
    public boolean subscribe(String symbol) {
        snapshotService.ensureSymbolTracked(symbol);
        return subscriptionManager.subscribe(symbol);
    }

    public boolean unsubscribe(String symbol) {
        return subscriptionManager.unsubscribe(symbol);
    }

    public Set<String> getActiveSubscriptions() {
        return subscriptionManager.getSubscribedSymbols();
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        if (!symbol.endsWith(".NS")) {
            throw new IllegalArgumentException("Symbol must be normalized (e.g., TCS.NS)");
        }
    }
}