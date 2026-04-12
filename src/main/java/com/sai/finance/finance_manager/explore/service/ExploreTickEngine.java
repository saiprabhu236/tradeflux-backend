package com.sai.finance.finance_manager.explore.service;

import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.marketdata.service.MarketStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExploreTickEngine {

    private final ExploreStateStore stateStore;
    private final MarketStatusService marketStatusService;


    /**
     * Runs every 1 second → generates synthetic ticks for all 500 stocks.
     */
    @Scheduled(fixedRate = 1000)
    public void generateTicks() {

        for (ExploreSymbolState state : stateStore.getAll()) {

            double anchor = state.getAnchorPrice();
            if (anchor <= 0) continue; // skip until Yahoo loads first price

            double volatility = state.getVolatility(); // 0.5% to 2%
            double maxMove = anchor * (volatility / 100.0);

            // random movement between -maxMove and +maxMove
            double delta = (Math.random() - 0.5) * 2 * maxMove;

            double newTick = state.getTickPrice() + delta;

            // update tick price
            state.setTickPrice(newTick);

            // update changePercent
            double prevClose = state.getPreviousClose();
            if (prevClose > 0) {
                double changePct = ((newTick - prevClose) / prevClose) * 100.0;
                state.setChangePercent(changePct);
            }
            if (Math.random() < 0.001) {

            }
        }
    }
}