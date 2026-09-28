package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.BacktestResultDTO;
import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.model.PriceCandle;
import com.rgv.tradingbot.model.StrategyConfig;
import com.rgv.tradingbot.repository.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Replays a strategy against historical PriceCandle data to evaluate
 * how it would have performed, without touching the live portfolio.
 *
 * ASSUMPTIONS (adjust once actual model/repo code is shared):
 * - PriceCandleRepository has:
 *     List<PriceCandle> findBySymbolAndTimestampBetweenOrderByTimestampAsc(
 *         String symbol, LocalDateTime start, LocalDateTime end)
 * - StrategyConfig has: symbol, name
 * This is a simplified single-position-at-a-time simulation (no partial
 * fills, no slippage/fees modeled yet).
 */
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final PriceCandleRepository priceCandleRepository;

    private static final BigDecimal DEFAULT_STARTING_BALANCE = BigDecimal.valueOf(100_000);
    private static final double RSI_OVERSOLD = 30.0;
    private static final double RSI_OVERBOUGHT = 70.0;
    private static final int RSI_PERIOD = 14;

    public BacktestResultDTO runBacktest(StrategyConfig config, LocalDate startDate, LocalDate endDate) {
        return runBacktest(config, startDate, endDate, DEFAULT_STARTING_BALANCE);
    }

    public BacktestResultDTO runBacktest(
            StrategyConfig config, LocalDate startDate, LocalDate endDate, BigDecimal startingBalance) {
        if (startingBalance == null || startingBalance.signum() <= 0) {
            throw new IllegalArgumentException("Starting balance must be greater than zero.");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Backtest dates are invalid.");
        }

        List<PriceCandle> candles = priceCandleRepository
                .findBySymbolAndTimestampBetweenOrderByTimestampAsc(
                        config.getSymbol(),
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59));

        BigDecimal balance = startingBalance;
        BigDecimal position = BigDecimal.ZERO; // quantity currently held
        BigDecimal entryPrice = BigDecimal.ZERO;

        int totalTrades = 0;
        int winningTrades = 0;
        int losingTrades = 0;

        BigDecimal peakValue = balance;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        double maxDrawdownPercent = 0;

        List<TradeSignalDTO> tradeLog = new ArrayList<>();
        List<BacktestResultDTO.EquityPointDTO> equityCurve = new ArrayList<>();
        if (!candles.isEmpty()) {
            equityCurve.add(BacktestResultDTO.EquityPointDTO.builder()
                    .timestamp(candles.get(0).getTimestamp()).value(balance).build());
        }
        boolean movingAverageStrategy = config.getName() != null
                && config.getName().toUpperCase().contains("SMA");

        for (int i = RSI_PERIOD; i < candles.size(); i++) {
            List<PriceCandle> window = candles.subList(i - RSI_PERIOD, i + 1);
            double rsi = calculateRsi(window);
            BigDecimal price = BigDecimal.valueOf(candles.get(i).getClose());
            boolean buySignal = movingAverageStrategy
                    ? movingAverageCrossed(candles, i, true)
                    : rsi < RSI_OVERSOLD;
            boolean sellSignal = movingAverageStrategy
                    ? movingAverageCrossed(candles, i, false)
                    : rsi > RSI_OVERBOUGHT;

            if (position.compareTo(BigDecimal.ZERO) == 0 && buySignal) {
                // enter long position with all available balance
                position = balance.divide(price, 4, java.math.RoundingMode.DOWN);
                entryPrice = price;
                balance = balance.subtract(position.multiply(price));
                tradeLog.add(signal(config.getSymbol(), "BUY", config.getName(), price, position,
                        candles.get(i).getTimestamp(), movingAverageStrategy
                                ? "EMA crossed above SMA." : "RSI below the oversold threshold."));

            } else if (position.compareTo(BigDecimal.ZERO) > 0 && sellSignal) {
                // exit position
                BigDecimal proceeds = position.multiply(price);
                balance = balance.add(proceeds);
                totalTrades++;
                if (price.compareTo(entryPrice) > 0) {
                    winningTrades++;
                } else {
                    losingTrades++;
                }

                tradeLog.add(signal(config.getSymbol(), "SELL", config.getName(), price, position,
                        candles.get(i).getTimestamp(), movingAverageStrategy
                                ? "EMA crossed below SMA." : "RSI above the overbought threshold."));
                position = BigDecimal.ZERO;
            }

            BigDecimal currentValue = balance.add(position.multiply(price));
            equityCurve.add(BacktestResultDTO.EquityPointDTO.builder()
                    .timestamp(candles.get(i).getTimestamp()).value(currentValue).build());
            if (currentValue.compareTo(peakValue) > 0) {
                peakValue = currentValue;
            }
            BigDecimal drawdown = peakValue.subtract(currentValue);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
            if (peakValue.signum() > 0) {
                maxDrawdownPercent = Math.max(maxDrawdownPercent,
                        drawdown.doubleValue() / peakValue.doubleValue() * 100);
            }
        }

        // liquidate any open position at the last known price for final accounting
        if (position.compareTo(BigDecimal.ZERO) > 0 && !candles.isEmpty()) {
            BigDecimal lastPrice = BigDecimal.valueOf(candles.get(candles.size() - 1).getClose());
            balance = balance.add(position.multiply(lastPrice));
            totalTrades++;
            if (lastPrice.compareTo(entryPrice) > 0) winningTrades++;
            else losingTrades++;
            if (!equityCurve.isEmpty()) {
                equityCurve.set(equityCurve.size() - 1, BacktestResultDTO.EquityPointDTO.builder()
                        .timestamp(candles.get(candles.size() - 1).getTimestamp()).value(balance).build());
            }
        }

        BigDecimal netProfitLoss = balance.subtract(startingBalance);
        double netProfitLossPercent = netProfitLoss.doubleValue()
                / startingBalance.doubleValue() * 100;
        double winRate = totalTrades == 0 ? 0.0 : (winningTrades * 100.0 / totalTrades);
        Map<LocalDate, BigDecimal> dailyEquity = new TreeMap<>();
        for (BacktestResultDTO.EquityPointDTO point : equityCurve) {
            dailyEquity.put(point.getTimestamp().toLocalDate(), point.getValue());
        }
        List<Double> returns = new ArrayList<>();
        BigDecimal priorDay = null;
        for (BigDecimal dailyValue : dailyEquity.values()) {
            if (priorDay != null && priorDay.signum() > 0)
                returns.add(dailyValue.doubleValue() / priorDay.doubleValue() - 1);
            priorDay = dailyValue;
        }
        double averageReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.size() < 2 ? 0 : returns.stream()
                .mapToDouble(value -> Math.pow(value - averageReturn, 2)).sum() / (returns.size() - 1);
        double sharpeRatio = variance == 0 ? 0 : averageReturn / Math.sqrt(variance) * Math.sqrt(252);

        return BacktestResultDTO.builder()
                .strategyName(config.getName())
                .symbol(config.getSymbol())
                .startDate(startDate)
                .endDate(endDate)
                .startingBalance(startingBalance)
                .endingBalance(balance)
                .netProfitLoss(netProfitLoss)
                .netProfitLossPercent(netProfitLossPercent)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .winRatePercent(winRate)
                .maxDrawdown(maxDrawdown)
                .maxDrawdownPercent(maxDrawdownPercent)
                .tradeLog(tradeLog)
                .equityCurve(equityCurve)
                .sharpeRatio(sharpeRatio)
                .build();
    }

    private boolean movingAverageCrossed(List<PriceCandle> candles, int index, boolean bullish) {
        if (index < RSI_PERIOD + 1) return false;
        double previousSma = averageClose(candles.subList(index - RSI_PERIOD - 1, index));
        double currentSma = averageClose(candles.subList(index - RSI_PERIOD, index + 1));
        double previousEma = exponentialAverage(candles.subList(index - RSI_PERIOD - 1, index));
        double currentEma = exponentialAverage(candles.subList(index - RSI_PERIOD, index + 1));
        return bullish
                ? previousEma <= previousSma && currentEma > currentSma
                : previousEma >= previousSma && currentEma < currentSma;
    }

    private double averageClose(List<PriceCandle> candles) {
        return candles.stream().mapToDouble(PriceCandle::getClose).average().orElse(0);
    }

    private double exponentialAverage(List<PriceCandle> candles) {
        double multiplier = 2.0 / (candles.size() + 1);
        double ema = candles.get(0).getClose();
        for (int i = 1; i < candles.size(); i++)
            ema += (candles.get(i).getClose() - ema) * multiplier;
        return ema;
    }

    private double calculateRsi(List<PriceCandle> window) {
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

        double avgGain = gainSum / RSI_PERIOD;
        double avgLoss = lossSum / RSI_PERIOD;

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private TradeSignalDTO signal(String symbol, String action, String strategyName, BigDecimal price,
                                  BigDecimal quantity, java.time.LocalDateTime timestamp, String reason) {
        return TradeSignalDTO.builder()
                .symbol(symbol)
                .action(action)
                .strategyName(strategyName)
                .price(price)
                .quantity(quantity)
                .confidence(1.0)
                .generatedAt(timestamp)
                .reason(reason)
                .build();
    }
}