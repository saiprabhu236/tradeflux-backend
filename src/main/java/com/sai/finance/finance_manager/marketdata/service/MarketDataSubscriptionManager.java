package com.sai.finance.finance_manager.marketdata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MarketDataSubscriptionManager {

    // Thread-safe set of subscribed symbols
    private final Set<String> subscribedSymbols =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Normalize symbol:
     * - Convert to uppercase
     * - Append .NS if missing
     */
    public String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        symbol = symbol.toUpperCase();

        if (!symbol.endsWith(".NS")) {
            symbol = symbol + ".NS";
        }

        return symbol;
    }

    /**
     * Subscribe to a symbol
     */
    public boolean subscribe(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            log.warn("Attempted to subscribe with null/blank symbol");
            return false;
        }

        // Normalize first
        String normalized = normalizeSymbol(rawSymbol);

        // Remove raw symbol if it exists (prevents RELIANCE + RELIANCE.NS duplicates)
        subscribedSymbols.remove(rawSymbol.toUpperCase());

        // Add only the normalized symbol
        boolean added = subscribedSymbols.add(normalized);

        if (added) {
            log.info("Subscribed to {}", normalized);
        } else {
            log.info("Already subscribed to {}", normalized);
        }

        return added;
    }

    /**
     * Unsubscribe from a symbol
     */
    public boolean unsubscribe(String rawSymbol) {
        String symbol = normalizeSymbol(rawSymbol);

        if (symbol == null) {
            return false;
        }

        boolean removed = subscribedSymbols.remove(symbol);

        if (removed) {
            log.info("Unsubscribed from {}", symbol);
        } else {
            log.info("Symbol {} was not subscribed", symbol);
        }

        return removed;
    }

    /**
     * Get all active subscribed symbols
     */
    public Set<String> getSubscribedSymbols() {
        return subscribedSymbols;
    }
}