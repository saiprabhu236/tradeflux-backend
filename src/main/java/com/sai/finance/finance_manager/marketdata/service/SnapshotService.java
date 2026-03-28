package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Log4j2
public class SnapshotService {

    private final MarketDataSubscriptionManager subscriptionManager;

    // Only store states for actively subscribed symbols
    private final Map<String, SymbolState> symbolStateMap = new ConcurrentHashMap<>();

    public SymbolState getState(String symbol) {
        return symbolStateMap.computeIfAbsent(symbol, s -> new SymbolState());
    }

    public void removeState(String symbol) {
        symbolStateMap.remove(symbol);
    }

    public Map<String, SymbolState> getSubscribedStates() {
        Map<String, SymbolState> result = new ConcurrentHashMap<>();
        for (String symbol : subscriptionManager.getSubscribedSymbols()) {
            result.put(symbol, getState(symbol));
        }
        return result;
    }

    /**
     * Called by polling service whenever a new REAL price is fetched.
     */
    public void updateRealPrice(String symbol, double price) {
        log.info("Metrics update for {} → {}", symbol, price);

        SymbolState state = getState(symbol);

        LocalDate today = LocalDate.now();

        // Daily reset
        if (!today.equals(state.getLastResetDate())) {
            state.setOpen(price);
            state.setHigh(price);
            state.setLow(price);
            state.setClose(price);
            state.setVolume(0);
            state.setLastResetDate(today);
        }

        // First price of the day
        if (state.getOpen() == 0) {
            state.setOpen(price);
            state.setHigh(price);
            state.setLow(price);
        }

        // Intraday metrics
        state.setClose(price);
        state.setHigh(Math.max(state.getHigh(), price));
        state.setLow(Math.min(state.getLow(), price));

        // Real price + tick base
        state.setLastRealPrice(price);
        state.setLastRealUpdateTime(System.currentTimeMillis());
        state.setLastTickPrice(price);
    }

    public void ensureSymbolTracked(String symbol) {
        getState(symbol);
    }

    /**
     * Cleanup for unsubscribed symbols (optional, can be called periodically if you like).
     */
    public void cleanupUnsubscribed() {
        Set<String> subscribed = subscriptionManager.getSubscribedSymbols();
        symbolStateMap.keySet().removeIf(s -> !subscribed.contains(s));
    }
}