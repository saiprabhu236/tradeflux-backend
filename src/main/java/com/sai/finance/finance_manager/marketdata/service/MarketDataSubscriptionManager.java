package com.sai.finance.finance_manager.marketdata.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MarketDataSubscriptionManager {

    private final Set<String> subscribedSymbols =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Normalize symbol:
     * - Uppercase
     * - Remove duplicate .NS
     * - Append .NS if missing
     */
    public String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        symbol = symbol.toUpperCase().trim();

        // Remove accidental double suffix
        if (symbol.endsWith(".NS.NS")) {
            symbol = symbol.replace(".NS.NS", ".NS");
        }

        // Append .NS if missing
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

        String normalized = normalizeSymbol(rawSymbol);

        // Remove all variants to avoid duplicates
        subscribedSymbols.remove(rawSymbol.toUpperCase());
        subscribedSymbols.remove(rawSymbol);
        subscribedSymbols.remove(normalized.replace(".NS", ""));
        subscribedSymbols.remove(normalized + ".NS");

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
        String normalized = normalizeSymbol(rawSymbol);

        if (normalized == null) {
            return false;
        }

        boolean removed = subscribedSymbols.remove(normalized);

        if (removed) {
            log.info("Unsubscribed from {}", normalized);
        } else {
            log.info("Symbol {} was not subscribed", normalized);
        }

        return removed;
    }

    /**
     * Get all active subscriptions (sorted)
     */
    public Set<String> getSubscribedSymbols() {
        return new TreeSet<>(subscribedSymbols);
    }
}