package com.sai.finance.finance_manager.marketdata.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.model.SymbolState;
import com.sai.finance.finance_manager.marketdata.service.MarketDataSubscriptionManager;
import com.sai.finance.finance_manager.marketdata.service.SnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWebSocketBroadcaster {

    private final SnapshotService snapshotService;
    private final MarketDataSubscriptionManager subscriptionManager;
    private final ObjectMapper mapper = new ObjectMapper();

    // Active WebSocket sessions
    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("WebSocket disconnected: {}", session.getId());
    }

    /**
     * Broadcast a single PriceDto (used for real delayed price updates)
     */
    public void broadcastPrice(PriceDto dto) {
        try {
            String json = mapper.writeValueAsString(dto);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }

        } catch (Exception e) {
            log.error("Error broadcasting price for {}: {}", dto.getSymbol(), e.getMessage());
        }
    }

    /**
     * Broadcast synthetic ticks ONLY for subscribed symbols.
     * Called by TickEngine every 2 seconds.
     */
    public void broadcastAllTicks() {
        try {
            Set<String> subscribed = subscriptionManager.getSubscribedSymbols();

            if (subscribed.isEmpty() || sessions.isEmpty()) {
                return;
            }

            for (String symbol : subscribed) {

                SymbolState state = snapshotService.getState(symbol);

                TickMessage tick = new TickMessage(
                        symbol.replace(".NS", ""),
                        state.getLastTickPrice(),
                        System.currentTimeMillis()
                );

                String json = mapper.writeValueAsString(tick);
                TextMessage message = new TextMessage(json);

                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error broadcasting ticks", e);
        }
    }

    // Internal tick message format
    private record TickMessage(String symbol, double price, long timestamp) {}
}