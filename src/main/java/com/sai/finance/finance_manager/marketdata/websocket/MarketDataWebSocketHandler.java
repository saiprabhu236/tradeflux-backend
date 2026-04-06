package com.sai.finance.finance_manager.marketdata.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sai.finance.finance_manager.marketdata.service.MarketDataSubscriptionManager;
import com.sai.finance.finance_manager.marketdata.util.SymbolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWebSocketHandler implements WebSocketHandler {

    private final MarketDataWebSocketBroadcaster broadcaster;
    private final WebSocketSessionSubscriptionManager sessionSubscriptionManager;
    private final MarketDataSubscriptionManager subscriptionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.addSession(session);
        log.info("Client connected: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            String payload = message.getPayload().toString();
            log.debug("Received message from {}: {}", session.getId(), payload);

            JsonNode root = objectMapper.readTree(payload);
            String action = root.path("action").asText(null);

            if (action == null) {
                log.warn("Missing action in message from {}", session.getId());
                return;
            }

            action = action.toLowerCase();

            // Collect symbols from "symbol" and/or "symbols"
            Set<String> symbols = new HashSet<>();

            if (root.hasNonNull("symbol")) {
                String symbol = root.get("symbol").asText();
                symbols.add(symbol);
            }

            if (root.has("symbols") && root.get("symbols").isArray()) {
                for (JsonNode node : root.get("symbols")) {
                    if (node.isTextual()) {
                        symbols.add(node.asText());
                    }
                }
            }

            if (symbols.isEmpty()) {
                log.warn("No symbols provided in message from {}", session.getId());
                return;
            }

            // Convert to Yahoo symbols (TCS -> TCS.NS)
            Set<String> yahooSymbols = new HashSet<>();
            for (String s : symbols) {
                String yahoo = SymbolMapper.toYahooSymbol(s);
                if (yahoo != null) {
                    yahooSymbols.add(yahoo);
                }
            }

            if (yahooSymbols.isEmpty()) {
                log.warn("No valid symbols after mapping for session {}", session.getId());
                return;
            }

            if (action.equals("subscribe")) {
                for (String yahoo : yahooSymbols) {
                    sessionSubscriptionManager.subscribe(session.getId(), yahoo);
                    subscriptionManager.subscribe(yahoo); // global
                }
                log.info("Session {} subscribed to {}", session.getId(), yahooSymbols);

            } else if (action.equals("unsubscribe")) {
                for (String yahoo : yahooSymbols) {
                    sessionSubscriptionManager.unsubscribe(session.getId(), yahoo);
                    subscriptionManager.unsubscribe(yahoo); // global
                }
                log.info("Session {} unsubscribed from {}", session.getId(), yahooSymbols);

            } else {
                log.warn("Unknown action '{}' from {}", action, session.getId());
            }

        } catch (Exception e) {
            log.error("Error handling message from {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error for {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.removeSession(session);
        sessionSubscriptionManager.removeSession(session.getId());
        log.info("Client disconnected: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // Keep this as you had it
    public void broadcastPrice(com.sai.finance.finance_manager.marketdata.dto.PriceDto dto) {
        broadcaster.broadcastPrice(dto);
    }
}