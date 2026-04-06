package com.sai.finance.finance_manager.marketdata.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import com.sai.finance.finance_manager.marketdata.dto.TickMessageDto;
import com.sai.finance.finance_manager.marketdata.mapper.MarketDataMapper;
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
    private final WebSocketSessionSubscriptionManager sessionSubscriptionManager;
    private final ObjectMapper objectMapper;
    private final MarketDataMapper marketDataMapper;

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
            String json = objectMapper.writeValueAsString(dto);
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
     * Called by TickEngine every N seconds.
     */
    public void broadcastAllTicks() {
        try {
            Set<String> globallySubscribed = subscriptionManager.getSubscribedSymbols();

            if (globallySubscribed.isEmpty() || sessions.isEmpty()) {
                return;
            }

            for (String symbol : globallySubscribed) {

                SymbolState state = snapshotService.getState(symbol);
                if (state == null) {
                    continue;
                }

                TickMessageDto dto = marketDataMapper.toTickMessage(symbol, state);
                String json = objectMapper.writeValueAsString(dto);
                TextMessage message = new TextMessage(json);

                for (WebSocketSession session : sessions) {
                    if (!session.isOpen()) continue;

                    if (sessionSubscriptionManager.isSessionSubscribed(session.getId(), symbol)) {
                        session.sendMessage(message);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error broadcasting ticks", e);
        }
    }
}