package com.sai.finance.finance_manager.explore.service;

import com.sai.finance.finance_manager.explore.model.ExploreSymbolState;
import com.sai.finance.finance_manager.explore.registry.ExploreStateStore;
import com.sai.finance.finance_manager.explore.registry.ExploreUniverseLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExploreYahooScheduler {

    private final ExploreUniverseLoader universeLoader;
    private final ExploreStateStore stateStore;
    private final ExploreYahooClient yahooClient; // We'll build this in Phase 5

    private Queue<String> queue;

    @Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE)
    public void initQueue() {
        log.info("Initializing Explore Yahoo Scheduler queue...");
        this.queue = new LinkedList<>(universeLoader.getSymbols());

    }

    /**
     * Fetch 1 symbol every 120ms → 500 stocks in ~60 seconds.
     */
    @Scheduled(fixedRate = 120)
    public void fetchNextSymbol() {
        if (queue == null || queue.isEmpty()) return;

        String symbol = queue.poll();
        if (symbol == null) return;

        try {
            ExploreYahooClient.YahooQuote quote = yahooClient.fetchQuote(symbol);

            if (quote != null && quote.lastPrice() > 0) {
                ExploreSymbolState state = stateStore.getOrCreate(symbol);

                state.setAnchorPrice(quote.lastPrice());
                state.setPreviousClose(quote.previousClose());

                state.setVolume(quote.volume());
                state.setAverageVolume(quote.averageVolume());
                state.setFiftyTwoWeekHigh(quote.fiftyTwoWeekHigh());
                state.setFiftyTwoWeekLow(quote.fiftyTwoWeekLow());

                // Initialize tickPrice on first load
                if (state.getTickPrice() == 0) {
                    state.setTickPrice(quote.lastPrice());
                }
            }

        } catch (Exception e) {

        } finally {
            queue.add(symbol); // rotate
        }

    }
}