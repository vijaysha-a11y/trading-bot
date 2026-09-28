package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.exception.InsufficientBalanceException;
import com.rgv.tradingbot.model.Trade;
import com.rgv.tradingbot.model.TradeType;
import com.rgv.tradingbot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Executes a risk-approved TradeSignalDTO: persists it as a Trade and
 * updates the portfolio's balances. Called by TradingScheduler (automated
 * signals) and TradeController (manual/override trades).
 *
 * Automated strategy orders still use the default portfolio. Manual orders
 * use the authenticated account's portfolio id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private final TradeRepository tradeRepository;
    private final PortfolioService portfolioService;

    private static final Long DEFAULT_PORTFOLIO_ID = 1L;

    @Transactional
    public Trade executeOrder(TradeSignalDTO signal) {
        return executeOrder(signal, DEFAULT_PORTFOLIO_ID);
    }

    @Transactional
    public Trade executeOrder(TradeSignalDTO signal, Long portfolioId) {
        if (signal.getPrice() == null || signal.getQuantity() == null) {
            throw new IllegalArgumentException("Signal is missing price or quantity: " + signal);
        }

        BigDecimal cost = signal.getPrice().multiply(signal.getQuantity());

        if ("BUY".equalsIgnoreCase(signal.getAction())) {
            var summary = portfolioService.getSummary(portfolioId);
            if (summary == null || summary.getAvailableBalance().compareTo(cost) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance to BUY " + signal.getQuantity() + " " + signal.getSymbol());
            }
        } else if ("SELL".equalsIgnoreCase(signal.getAction())) {
            var summary = portfolioService.getSummary(portfolioId);
            boolean canSell = summary != null && summary.getHoldings().stream()
                    .anyMatch(holding -> holding.getSymbol().equals(signal.getSymbol())
                            && holding.getQuantity().compareTo(signal.getQuantity()) >= 0);
            if (!canSell) {
                throw new IllegalArgumentException(
                        "Insufficient holdings to SELL " + signal.getQuantity() + " " + signal.getSymbol());
            }
        }

        Trade trade = new Trade(
                signal.getSymbol(),
                TradeType.valueOf(signal.getAction().toUpperCase()),
                signal.getQuantity().doubleValue(),
                signal.getPrice().doubleValue(),
                signal.getReason(),
                portfolioId);

        Trade savedTrade = tradeRepository.save(trade);

        portfolioService.applyTradeToBalances(portfolioId, savedTrade);

        log.info("Order executed: {} {} {} @ {}",
                signal.getAction(), signal.getQuantity(), signal.getSymbol(), signal.getPrice());

        return savedTrade;
    }
}