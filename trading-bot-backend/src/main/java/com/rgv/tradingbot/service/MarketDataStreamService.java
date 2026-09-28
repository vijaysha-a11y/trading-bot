package com.rgv.tradingbot.service;

import com.rgv.tradingbot.model.PriceCandle;
import com.rgv.tradingbot.websocket.PriceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges MarketDataService (fetch + persist) with PriceWebSocketHandler
 * (push to connected clients). Called by TradingScheduler / SchedulerConfig
 * on a fixed interval, or directly by MarketDataController for on-demand refresh.
 *
 * ASSUMPTION: PriceWebSocketHandler exposes a broadcast(Object payload) method
 * that serializes and sends the payload to all connected sessions. Adjust the
 * method name below to match your actual handler once it's shared.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataStreamService {

    private final MarketDataService marketDataService;
    private final PriceWebSocketHandler priceWebSocketHandler;

    /**
     * Fetches the latest candle for a symbol and pushes it to all
     * subscribed WebSocket clients.
     */
    public void streamLatestPrice(String symbol) {
        PriceCandle candle = marketDataService.fetchLatestCandle(symbol);

        if (candle == null) {
            log.warn("Skipping stream for {} - no candle data available", symbol);
            return;
        }

        priceWebSocketHandler.broadcast(candle);
    }

    /**
     * Streams the latest price for multiple symbols in one pass.
     */
    public void streamLatestPrices(List<String> symbols) {
        for (String symbol : symbols) {
            streamLatestPrice(symbol);
        }
    }
}