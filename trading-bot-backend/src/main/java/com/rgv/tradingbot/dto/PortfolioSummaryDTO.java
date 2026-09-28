package com.rgv.tradingbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregated, read-only view of a user's portfolio - sent to the
 * frontend dashboard and pushed live via PortfolioWebSocketHandler.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryDTO {

    private Long portfolioId;

    // Cash available for new trades (after deposits/withdrawals)
    private BigDecimal availableBalance;

    // Total amount currently invested across open holdings
    private BigDecimal investedAmount;

    // Live market value of all holdings combined
    private BigDecimal currentMarketValue;

    // currentMarketValue + availableBalance
    private BigDecimal totalValue;

    private BigDecimal totalProfitLoss;

    // Percentage form of totalProfitLoss relative to invested amount
    private Double profitLossPercent;

    private List<HoldingDTO> holdings;

    private LocalDateTime asOf;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingDTO {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal avgBuyPrice;
        private BigDecimal currentPrice;
        private BigDecimal profitLoss;
    }
}