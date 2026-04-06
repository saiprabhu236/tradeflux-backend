package com.sai.finance.finance_manager.marketdata.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionSubscriptionManager {

    // sessionId -> set of Yahoo symbols (e.g., TCS.NS)
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    public void subscribe(String sessionId, String symbol) {
        sessionSubscriptions
                .computeIfAbsent(sessionId, id -> ConcurrentHashMap.newKeySet())
                .add(symbol);
    }

    public void subscribeMany(String sessionId, Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return;
        sessionSubscriptions
                .computeIfAbsent(sessionId, id -> ConcurrentHashMap.newKeySet())
                .addAll(symbols);
    }

    public void unsubscribe(String sessionId, String symbol) {
        Set<String> set = sessionSubscriptions.get(sessionId);
        if (set != null) {
            set.remove(symbol);
            if (set.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
    }

    public void unsubscribeMany(String sessionId, Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return;
        Set<String> set = sessionSubscriptions.get(sessionId);
        if (set != null) {
            set.removeAll(symbols);
            if (set.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
    }

    public void removeSession(String sessionId) {
        sessionSubscriptions.remove(sessionId);
    }

    public boolean isSessionSubscribed(String sessionId, String symbol) {
        Set<String> set = sessionSubscriptions.get(sessionId);
        return set != null && set.contains(symbol);
    }
}