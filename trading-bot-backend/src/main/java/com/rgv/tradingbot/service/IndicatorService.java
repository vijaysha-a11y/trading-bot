package com.rgv.tradingbot.service;

import com.rgv.tradingbot.model.PriceCandle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes technical indicators from recent price history, for consumption
 * by StrategyEngine. Returns a simple Map<String, Double> keyed by indicator
 * name so StrategyEngine can look up whichever indicators a given
 * StrategyConfig needs, without this service knowing about strategy specifics.
 *
 * Indicators included: SMA(14), EMA(14), RSI(14).
 */
@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final MarketDataService marketDataService;

    private static final int DEFAULT_PERIOD = 14;

    /**
     * Calculates the standard indicator set for a symbol using the last
     * DEFAULT_PERIOD + 1 candles (extra candle needed for RSI's diff calc).
     */
    public Map<String, Double> calculateIndicators(String symbol) {
        List<PriceCandle> candles = marketDataService.getRecentCandles(symbol, DEFAULT_PERIOD + 1);

        Map<String, Double> indicators = new LinkedHashMap<>();

        if (candles.size() < DEFAULT_PERIOD + 1) {
            // Not enough history yet - return empty so StrategyEngine can skip/HOLD
            return indicators;
        }

        indicators.put("SMA", simpleMovingAverage(candles, DEFAULT_PERIOD));
        indicators.put("EMA", exponentialMovingAverage(candles, DEFAULT_PERIOD));
        indicators.put("RSI", relativeStrengthIndex(candles, DEFAULT_PERIOD));

        return indicators;
    }

    private double simpleMovingAverage(List<PriceCandle> candles, int period) {
        List<PriceCandle> window = candles.subList(candles.size() - period, candles.size());

        BigDecimal sum = window.stream()
                .map(candle -> BigDecimal.valueOf(candle.getClose()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP).doubleValue();
    }

    private double exponentialMovingAverage(List<PriceCandle> candles, int period) {
        double multiplier = 2.0 / (period + 1);
        List<PriceCandle> window = candles.subList(candles.size() - period, candles.size());

        double ema = window.get(0).getClose();
        for (int i = 1; i < window.size(); i++) {
            double close = window.get(i).getClose();
            ema = (close - ema) * multiplier + ema;
        }
        return ema;
    }

    private double relativeStrengthIndex(List<PriceCandle> candles, int period) {
        List<PriceCandle> window = candles.subList(candles.size() - (period + 1), candles.size());

        double gainSum = 0;
        double lossSum = 0;

        for (int i = 1; i < window.size(); i++) {
            double diff = window.get(i).getClose() - window.get(i - 1).getClose();
            if (diff >= 0) {
                gainSum += diff;
            } else {
                lossSum += Math.abs(diff);
            }
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}