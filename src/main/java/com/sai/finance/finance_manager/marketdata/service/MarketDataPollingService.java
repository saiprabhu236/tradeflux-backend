package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataPollingService {

    private final MarketDataSubscriptionManager subscriptionManager;
    private final YahooFinanceClient yahooClient;
    private final SnapshotService snapshotService;
    private final MarketStatusService marketStatusService;

    private final Map<String, Double> basePrices = new ConcurrentHashMap<>();

    // Poll every 30 seconds for REAL prices
    @Scheduled(fixedRate = 30000)
    public void pollBasePrices() {
        try {
            // 1️⃣ Check market status before polling
            if (!marketStatusService.isMarketOpenNow()) {
                log.info("Market is not OPEN → Skipping polling");
                return;
            }

            Set<String> symbols = subscriptionManager.getSubscribedSymbols();
            if (symbols.isEmpty()) {
                return;
            }

            for (String rawSymbol : symbols) {

                String symbol = subscriptionManager.normalizeSymbol(rawSymbol);

                // Fetch REAL delayed price from Yahoo
                PriceDto price = yahooClient.getCurrentPrice(symbol);

                if (price == null) {
                    log.warn("No price data for {}", symbol);
                    continue;
                }

                double realPrice = price.getCurrentPrice();

                // Update tick base
                basePrices.put(symbol, realPrice);

                // Update snapshot (OHLC, previousClose, etc.)
                snapshotService.updateRealPrice(symbol, realPrice);
            }

            snapshotService.cleanupUnsubscribed();

        } catch (Exception e) {
            log.error("Error during base price polling", e);
        }
    }


    public double getBasePrice(String symbol) {
        return basePrices.getOrDefault(symbol, 0.0);
    }
}