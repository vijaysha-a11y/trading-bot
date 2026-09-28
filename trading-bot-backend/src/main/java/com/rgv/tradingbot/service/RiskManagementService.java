package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.PortfolioSummaryDTO;
import com.rgv.tradingbot.dto.TradeSignalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Applies basic risk rules to a generated TradeSignalDTO before it's
 * allowed to reach OrderExecutionService. Extend with position-sizing,
 * max-daily-loss, and per-symbol exposure limits as needed.
 *
 * ASSUMPTION: single-portfolio setup for now (portfolioId = 1L). Wire in
 * the actual authenticated user's portfolio id once SecurityConfig /
 * auth context is available here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskManagementService {

    private final PortfolioService portfolioService;

    private static final BigDecimal MIN_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.5);
    private static final Long DEFAULT_PORTFOLIO_ID = 1L;

    public boolean isSafeToExecute(TradeSignalDTO signal) {
        if (signal == null || signal.getAction() == null) {
            return false;
        }

        if (signal.getConfidence() == null
                || BigDecimal.valueOf(signal.getConfidence()).compareTo(MIN_CONFIDENCE_THRESHOLD) < 0) {
            log.info("Signal rejected - confidence too low: {}", signal);
            return false;
        }

        if ("BUY".equalsIgnoreCase(signal.getAction())) {
            return hasSufficientFunds(signal);
        }

        // SELL signals: assume holdings check happens in OrderExecutionService
        // against the actual position, since RiskManagementService doesn't
        // have holding-level detail here.
        return true;
    }

    private boolean hasSufficientFunds(TradeSignalDTO signal) {
        PortfolioSummaryDTO summary = portfolioService.getSummary(DEFAULT_PORTFOLIO_ID);
        if (summary == null || summary.getAvailableBalance() == null) {
            return false;
        }

        BigDecimal orderCost = signal.getPrice().multiply(signal.getQuantity());
        boolean sufficient = summary.getAvailableBalance().compareTo(orderCost) >= 0;

        if (!sufficient) {
            log.info("Signal rejected - insufficient balance for order cost {}", orderCost);
        }

        return sufficient;
    }
}