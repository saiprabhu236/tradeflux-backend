package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import com.sai.finance.finance_manager.marketdata.websocket.MarketDataWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickEngine {

    private final SnapshotService snapshotService;
    private final MarketDataSubscriptionManager subscriptionManager;
    private final MarketDataWebSocketBroadcaster broadcaster;
    private final Random random = new Random();

    @Scheduled(fixedRate = 2000) // every 2 seconds
    public void generateTicks() {

        // Only generate ticks for actively subscribed symbols
        Set<String> subscribedSymbols = subscriptionManager.getSubscribedSymbols();

        if (subscribedSymbols.isEmpty()) {
            return; // No active stocks → no ticks
        }

        for (String symbol : subscribedSymbols) {

            SymbolState state = snapshotService.getState(symbol);

            double base = state.getLastRealPrice();
            if (base <= 0) continue;

            double lastTick = state.getLastTickPrice() == 0.0
                    ? base
                    : state.getLastTickPrice();

            double maxDelta = base * 0.002; // 0.2%
            double delta = (random.nextDouble() * 2 - 1) * maxDelta;

            double newTick = lastTick + delta;

            state.setLastTickPrice(newTick);
        }

        // Broadcast ticks only for subscribed symbols
        broadcaster.broadcastAllTicks();
    }
}