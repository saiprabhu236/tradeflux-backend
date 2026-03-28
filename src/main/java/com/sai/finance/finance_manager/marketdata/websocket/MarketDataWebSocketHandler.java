package com.sai.finance.finance_manager.marketdata.websocket;

import com.sai.finance.finance_manager.marketdata.dto.PriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataWebSocketHandler implements WebSocketHandler {

    private final MarketDataWebSocketBroadcaster broadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.addSession(session);
        log.info("Client connected: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket error for {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.removeSession(session);
        log.info("Client disconnected: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Step‑6: Allow TickBroadcaster to send synthetic ticks
     */
    public void broadcastPrice(PriceDto dto) {
        broadcaster.broadcastPrice(dto);
    }
}