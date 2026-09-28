package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.model.PriceCandle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core decision-making service: combines IndicatorService output with
 * active StrategyConfig rules to produce TradeSignalDTOs, consumed by
 * TradingScheduler and OrderExecutionService.
 *
 * ASSUMPTIONS (adjust once StrategyConfig's actual fields are shared):
 * - StrategyConfig has: id, name, symbol, type (e.g. "RSI", "SMA_CROSSOVER"),
 *   active (boolean), and threshold fields such as rsiOversold / rsiOverbought
 *   or similar. Placeholder threshold constants are used below where a
 *   dedicated field isn't confirmed yet.
 */
@Service
@RequiredArgsConstructor
public class StrategyEngine {

    private final IndicatorService indicatorService;
    private final MarketDataService marketDataService;

    private static final double RSI_OVERSOLD = 30.0;
    private static final double RSI_OVERBOUGHT = 70.0;
    private static final BigDecimal DEFAULT_ORDER_QUANTITY = BigDecimal.ONE;

    /**
     * Generates trade signals for a symbol based on the latest indicators.
     * Currently implements an RSI mean-reversion strategy plus an
     * SMA/EMA crossover strategy; extend this switch as more
     * StrategyConfig types are added.
     */
    public List<TradeSignalDTO> generateSignals(String symbol) {
        List<TradeSignalDTO> signals = new ArrayList<>();

        Map<String, Double> indicators = indicatorService.calculateIndicators(symbol);
        if (indicators.isEmpty()) {
            return signals; // not enough history yet
        }

        List<PriceCandle> recent = marketDataService.getRecentCandles(symbol, 1);
        if (recent.isEmpty()) {
            return signals;
        }
        BigDecimal latestPrice = BigDecimal.valueOf(recent.get(recent.size() - 1).getClose());

        signals.addAll(evaluateRsiStrategy(symbol, indicators, latestPrice));
        signals.addAll(evaluateSmaEmaCrossover(symbol, indicators, latestPrice));

        return signals;
    }

    private List<TradeSignalDTO> evaluateRsiStrategy(
            String symbol, Map<String, Double> indicators, BigDecimal price) {

        List<TradeSignalDTO> signals = new ArrayList<>();
        Double rsi = indicators.get("RSI");
        if (rsi == null) {
            return signals;
        }

        if (rsi < RSI_OVERSOLD) {
            signals.add(buildSignal(symbol, "BUY", "RSI_MEAN_REVERSION", price,
                    "RSI " + rsi + " below oversold threshold " + RSI_OVERSOLD, indicators));
        } else if (rsi > RSI_OVERBOUGHT) {
            signals.add(buildSignal(symbol, "SELL", "RSI_MEAN_REVERSION", price,
                    "RSI " + rsi + " above overbought threshold " + RSI_OVERBOUGHT, indicators));
        } else {
            signals.add(buildSignal(symbol, "HOLD", "RSI_MEAN_REVERSION", price,
                    "RSI " + String.format("%.1f", rsi) + " is neutral; signals require below 30 or above 70.", indicators));
        }

        return signals;
    }

    private List<TradeSignalDTO> evaluateSmaEmaCrossover(
            String symbol, Map<String, Double> indicators, BigDecimal price) {

        List<TradeSignalDTO> signals = new ArrayList<>();
        Double sma = indicators.get("SMA");
        Double ema = indicators.get("EMA");
        if (sma == null || ema == null) {
            return signals;
        }

        if (ema > sma) {
            signals.add(buildSignal(symbol, "BUY", "SMA_EMA_CROSSOVER", price,
                    "EMA is above SMA, indicating bullish momentum.", indicators));
        } else if (ema < sma) {
            signals.add(buildSignal(symbol, "SELL", "SMA_EMA_CROSSOVER", price,
                    "EMA is below SMA, indicating bearish momentum.", indicators));
        } else {
            signals.add(buildSignal(symbol, "HOLD", "SMA_EMA_CROSSOVER", price,
                    "EMA and SMA are aligned; no directional signal.", indicators));
        }

        return signals;
    }

    private TradeSignalDTO buildSignal(
            String symbol, String action, String strategyName, BigDecimal price, String reason,
            Map<String, Double> indicators) {

        return TradeSignalDTO.builder()
                .symbol(symbol)
                .action(action)
                .strategyName(strategyName)
                .price(price)
                .quantity(DEFAULT_ORDER_QUANTITY)
                .confidence(0.7)
                .reason(reason)
                .generatedAt(LocalDateTime.now())
                .indicators(indicators)
                .build();
    }
}