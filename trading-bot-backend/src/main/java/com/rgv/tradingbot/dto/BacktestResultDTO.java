package com.rgv.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Result of running a StrategyConfig against historical PriceCandle data
 * via BacktestService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResultDTO {

    private String strategyName;

    private String symbol;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal startingBalance;

    private BigDecimal endingBalance;

    private BigDecimal netProfitLoss;

    private Double netProfitLossPercent;

    private Integer totalTrades;

    private Integer winningTrades;

    private Integer losingTrades;

    // winningTrades / totalTrades * 100
    private Double winRatePercent;

    // Largest peak-to-trough drop during the backtest window
    private BigDecimal maxDrawdown;

    private Double maxDrawdownPercent;

    private List<TradeSignalDTO> tradeLog;

    private Double sharpeRatio;

    private List<EquityPointDTO> equityCurve;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityPointDTO {
        private LocalDateTime timestamp;
        private BigDecimal value;
    }
}