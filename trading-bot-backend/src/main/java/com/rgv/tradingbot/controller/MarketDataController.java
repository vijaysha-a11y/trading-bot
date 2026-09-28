package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.model.PriceCandle;
import com.rgv.tradingbot.service.IndicatorService;
import com.rgv.tradingbot.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * On-demand market data endpoints. Live/continuous updates for the same
 * data are pushed separately via MarketDataStreamService + PriceWebSocketHandler.
 */
@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;
    private final IndicatorService indicatorService;

    @GetMapping("/{symbol}/latest")
    public ResponseEntity<PriceCandle> getLatestCandle(@PathVariable String symbol) {
        PriceCandle candle = marketDataService.fetchLatestCandle(symbol);

        if (candle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(candle);
    }

    @GetMapping("/{symbol}/recent")
    public ResponseEntity<List<PriceCandle>> getRecentCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "50") int count) {

        List<PriceCandle> candles = marketDataService.getRecentCandles(symbol, count);
        return ResponseEntity.ok(candles);
    }

    @GetMapping("/{symbol}/indicators")
    public ResponseEntity<Map<String, Double>> getIndicators(@PathVariable String symbol) {
        Map<String, Double> indicators = indicatorService.calculateIndicators(symbol);
        return ResponseEntity.ok(indicators);
    }
}