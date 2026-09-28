package com.rgv.tradingbot.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgv.tradingbot.dto.PortfolioSummaryDTO;
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
 * Pushes live PortfolioSummaryDTO updates to connected clients whenever
 * a trade is executed or funds are deposited/withdrawn. Called by
 * PortfolioService / OrderExecutionService / FundService after any
 * balance-changing operation.
 *
 * Registered against a WebSocket endpoint (e.g. "/ws/portfolio") in
 * WebSocketConfig.
 */
@Slf4j
@Component
public class PortfolioWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public PortfolioWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Portfolio stream client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Portfolio stream client disconnected: {} ({})", session.getId(), status);
    }

    /**
     * Serializes the given payload (typically a PortfolioSummaryDTO) to
     * JSON and sends it to every connected session.
     */
    public void broadcast(Object payload) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (payload instanceof PortfolioSummaryDTO summary
                        && !summary.getPortfolioId().equals(session.getAttributes().get("portfolioId"))) {
                    continue;
                }
                sendSafely(session, message);
            }
        } catch (IOException ex) {
            log.error("Failed to serialize portfolio payload: {}", ex.getMessage(), ex);
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            log.warn("Failed to send portfolio update to session {}: {}", session.getId(), ex.getMessage());
        }
    }
}