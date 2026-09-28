package com.rgv.tradingbot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Pushes live market price updates (PriceCandle) to all connected clients.
 * Called by MarketDataStreamService.broadcast(Object) whenever a new
 * candle is fetched.
 *
 * Registered against a WebSocket endpoint (e.g. "/ws/prices") in
 * WebSocketConfig.
 */
@Slf4j
@Component
public class PriceWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public PriceWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Price stream client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Price stream client disconnected: {} ({})", session.getId(), status);
    }

    /**
     * Serializes the given payload (typically a PriceCandle) to JSON and
     * sends it to every currently connected session. Dead/closed sessions
     * are dropped silently and cleaned up on their next close event.
     */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                sendSafely(session, message);
            }
        } catch (IOException ex) {
            log.error("Failed to serialize price payload: {}", ex.getMessage(), ex);
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            log.warn("Failed to send price update to session {}: {}", session.getId(), ex.getMessage());
        }
    }
}