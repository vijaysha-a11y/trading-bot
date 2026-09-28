package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.PortfolioSummaryDTO;
import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.model.Trade;
import com.rgv.tradingbot.model.TradeType;
import com.rgv.tradingbot.repository.PortfolioRepository;
import com.rgv.tradingbot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads/updates portfolio balances and builds the aggregated
 * PortfolioSummaryDTO used by PortfolioController and
 * PortfolioWebSocketHandler.
 *
 * Each account's trades are scoped to its portfolio. Pre-authentication
 * trades remain visible to the original portfolio for upgrade compatibility.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final MarketDataService marketDataService;

    @Transactional(readOnly = true)
    public PortfolioSummaryDTO getSummary(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId).orElse(null);
        if (portfolio == null) {
            return null;
        }

        List<Trade> trades = tradeRepository.findAllForPortfolio(portfolioId);

        // Group trades by symbol so sell orders reduce the open position.
        Map<String, List<Trade>> bySymbol = trades.stream()
                .collect(Collectors.groupingBy(Trade::getSymbol));

        List<PortfolioSummaryDTO.HoldingDTO> holdings = bySymbol.entrySet().stream()
                .map(entry -> buildHolding(entry.getKey(), entry.getValue()))
                .filter(holding -> holding != null)
                .collect(Collectors.toList());

        BigDecimal currentMarketValue = holdings.stream()
                .map(h -> h.getCurrentPrice().multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableBalance = BigDecimal.valueOf(portfolio.getCashBalance());
        BigDecimal investedAmount = holdings.stream()
                .map(h -> h.getAvgBuyPrice().multiply(h.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalValue = availableBalance.add(currentMarketValue);
        BigDecimal totalProfitLoss = currentMarketValue.subtract(investedAmount);

        double profitLossPercent = investedAmount.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : totalProfitLoss.doubleValue() / investedAmount.doubleValue() * 100;

        return PortfolioSummaryDTO.builder()
                .portfolioId(portfolio.getId())
                .availableBalance(availableBalance)
                .investedAmount(investedAmount)
                .currentMarketValue(currentMarketValue)
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .profitLossPercent(profitLossPercent)
                .holdings(holdings)
                .asOf(LocalDateTime.now())
                .build();
    }

    private PortfolioSummaryDTO.HoldingDTO buildHolding(String symbol, List<Trade> trades) {
        List<Trade> buyTrades = trades.stream()
                .filter(trade -> trade.getType() == TradeType.BUY)
                .toList();
        List<Trade> sellTrades = trades.stream()
                .filter(trade -> trade.getType() == TradeType.SELL)
                .toList();

        BigDecimal boughtQuantity = buyTrades.stream()
                .map(trade -> BigDecimal.valueOf(trade.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldQuantity = sellTrades.stream()
                .map(trade -> BigDecimal.valueOf(trade.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQuantity = boughtQuantity.subtract(soldQuantity);

        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0 || boughtQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal totalCost = buyTrades.stream()
                .map(t -> BigDecimal.valueOf(t.getPrice())
                        .multiply(BigDecimal.valueOf(t.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgBuyPrice = totalCost.divide(boughtQuantity, 4, java.math.RoundingMode.HALF_UP);

        BigDecimal currentPrice = marketDataService.getRecentCandles(symbol, 1).stream()
                .findFirst()
                .map(candle -> BigDecimal.valueOf(candle.getClose()))
                .orElse(avgBuyPrice);
        BigDecimal profitLoss = currentPrice.subtract(avgBuyPrice).multiply(totalQuantity);

        return PortfolioSummaryDTO.HoldingDTO.builder()
                .symbol(symbol)
                .quantity(totalQuantity)
                .avgBuyPrice(avgBuyPrice)
                .currentPrice(currentPrice)
                .profitLoss(profitLoss)
                .build();
    }

    /**
     * Applies a completed trade's effect on cash/invested balances.
     * Called by OrderExecutionService right after saving the Trade.
     */
    @Transactional
    public void applyTradeToBalances(Long portfolioId, Trade trade) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));

        double cost = trade.getPrice() * trade.getQuantity();

        if (trade.getType() == TradeType.BUY) {
            portfolio.setCashBalance(portfolio.getCashBalance() - cost);
        } else if (trade.getType() == TradeType.SELL) {
            portfolio.setCashBalance(portfolio.getCashBalance() + cost);
        }

        portfolioRepository.save(portfolio);
    }
}