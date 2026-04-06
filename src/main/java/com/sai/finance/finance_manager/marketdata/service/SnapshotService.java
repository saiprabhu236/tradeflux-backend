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

    private final Map<String, SymbolState> symbolStateMap = new ConcurrentHashMap<>();

    public SymbolState getState(String rawSymbol) {
        String symbol = subscriptionManager.normalizeSymbol(rawSymbol);
        return symbolStateMap.computeIfAbsent(symbol, s -> new SymbolState());
    }

    public void removeState(String rawSymbol) {
        String symbol = subscriptionManager.normalizeSymbol(rawSymbol);
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
    public void updateRealPrice(String rawSymbol, double price) {
        String symbol = subscriptionManager.normalizeSymbol(rawSymbol);

        SymbolState state = getState(symbol);
        LocalDate today = LocalDate.now();

        // Daily reset
        if (!today.equals(state.getLastResetDate())) {
            log.info("Daily reset for {}", symbol);

            state.setOpen(price);
            state.setHigh(price);
            state.setLow(price);
            state.setClose(price);
            state.setVolume(0);
            state.setPreviousClose(state.getClose());
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

        // Real price tracking
        state.setLastRealPrice(price);
        state.setLastRealUpdateTime(System.currentTimeMillis());

        // Tick base
        state.setLastTickPrice(price);
        // Update previous close ONLY when first real price of the day arrives
        if (state.getPreviousClose() == 0) {
            state.setPreviousClose(price);
        }

        // Update change & changePercent
        double prev = state.getPreviousClose();
        if (prev > 0) {
            double change = price - prev;
            double changePercent = (change / prev) * 100.0;

            state.setChange(change);
            state.setChangePercent(changePercent);
        }
    }

    public void ensureSymbolTracked(String rawSymbol) {
        getState(rawSymbol);
    }

    public void cleanupUnsubscribed() {
        Set<String> subscribed = subscriptionManager.getSubscribedSymbols();
        symbolStateMap.keySet().removeIf(s -> !subscribed.contains(s));
    }

    public void updateTickPrice(String rawSymbol, double tickPrice) {
        String symbol = subscriptionManager.normalizeSymbol(rawSymbol);
        SymbolState state = getState(symbol);

        state.setLastTickPrice(tickPrice);

        // Update intraday OHLC based on tick
        if (state.getOpen() == 0) {
            state.setOpen(tickPrice);
            state.setHigh(tickPrice);
            state.setLow(tickPrice);
        }

        state.setHigh(Math.max(state.getHigh(), tickPrice));
        state.setLow(Math.min(state.getLow(), tickPrice));
        state.setClose(tickPrice);
        // Update change & changePercent based on tick price
        double prev = state.getPreviousClose();
        if (prev > 0) {
            double change = tickPrice - prev;
            double changePercent = (change / prev) * 100.0;

            state.setChange(change);
            state.setChangePercent(changePercent);
        }
    }

}