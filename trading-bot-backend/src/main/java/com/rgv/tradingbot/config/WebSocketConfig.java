package com.rgv.tradingbot.config;

import com.rgv.tradingbot.websocket.PortfolioWebSocketHandler;
import com.rgv.tradingbot.websocket.PriceWebSocketHandler;
import com.rgv.tradingbot.websocket.TradeSignalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;
/**
 * Registers PriceWebSocketHandler, PortfolioWebSocketHandler, and
 * TradeSignalWebSocketHandler on their respective endpoints.
 *
 * Frontend connects to:
 *   ws://<host>/ws/prices    - live price candle updates
 *   ws://<host>/ws/portfolio - live portfolio summary updates
 *   ws://<host>/ws/signals   - live trade signal updates
 *
 * setAllowedOriginPatterns("*") is permissive for local development -
 * restrict this to your actual frontend origin(s) before deploying.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PriceWebSocketHandler priceWebSocketHandler;
    private final PortfolioWebSocketHandler portfolioWebSocketHandler;
    private final TradeSignalWebSocketHandler tradeSignalWebSocketHandler;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            PriceWebSocketHandler priceWebSocketHandler,
            PortfolioWebSocketHandler portfolioWebSocketHandler,
            TradeSignalWebSocketHandler tradeSignalWebSocketHandler,
            @Value("${trading.frontend.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173}")
            List<String> allowedOrigins) {
        this.priceWebSocketHandler = priceWebSocketHandler;
        this.portfolioWebSocketHandler = portfolioWebSocketHandler;
        this.tradeSignalWebSocketHandler = tradeSignalWebSocketHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(priceWebSocketHandler, "/ws/prices")
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));

        registry.addHandler(portfolioWebSocketHandler, "/ws/portfolio")
                .addInterceptors(new AuthenticatedHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));

        registry.addHandler(tradeSignalWebSocketHandler, "/ws/signals")
                .addInterceptors(new AuthenticatedHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}