package com.sai.finance.finance_manager.marketdata.service;

import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import com.sai.finance.finance_manager.marketdata.websocket.MarketDataWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Log4j2
public class TickEngine {

    private final SnapshotService snapshotService;
    private final MarketDataSubscriptionManager subscriptionManager;

    private final Random random = new Random();

    @Scheduled(fixedRate = 1000)
    public void generateTicks() {

        Set<String> symbols = subscriptionManager.getSubscribedSymbols();
        if (symbols.isEmpty()) return;

        for (String symbol : symbols) {
            SymbolState state = snapshotService.getState(symbol);

            double real = state.getLastRealPrice();
            if (real <= 0) continue;

            double tick = state.getLastTickPrice();
            if (tick <= 0) tick = real;

            double volatility = getVolatilityForTime();
            double momentum = getMomentumFactor();
            double meanReversion = (real - tick) * 0.02;

            double randomMove = (random.nextDouble() - 0.5) * volatility;

            double newTick = tick + randomMove + meanReversion + momentum;

            newTick = clamp(newTick, real * 0.90, real * 1.10);

            snapshotService.updateTickPrice(symbol, newTick);

            double spread = calculateSpread(volatility);
            double bid = newTick - spread / 2;
            double ask = newTick + spread / 2;

            state.setBidPrice(bid);
            state.setAskPrice(ask);
            state.setSpread(spread);

            state.setLastTickTime(System.currentTimeMillis());
        }
    }

    private double getVolatilityForTime() {
        LocalTime now = LocalTime.now();

        if (now.isBefore(LocalTime.of(9, 30))) return 0.012;
        if (now.isBefore(LocalTime.of(11, 0))) return 0.008;
        if (now.isBefore(LocalTime.of(14, 0))) return 0.004;
        if (now.isBefore(LocalTime.of(15, 0))) return 0.007;

        return 0.015;
    }

    private double getMomentumFactor() {
        return (random.nextDouble() - 0.5) * 0.003;
    }

    private double calculateSpread(double volatility) {
        return volatility * (0.5 + random.nextDouble());
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}