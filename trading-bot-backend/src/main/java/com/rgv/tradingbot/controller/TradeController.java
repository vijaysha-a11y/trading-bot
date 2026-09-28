package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.dto.TradeOrderRequest;
import com.rgv.tradingbot.model.Trade;
import com.rgv.tradingbot.model.PaperOrder;
import com.rgv.tradingbot.repository.PaperOrderRepository;
import com.rgv.tradingbot.repository.TradeRepository;
import com.rgv.tradingbot.service.PaperOrderService;
import com.rgv.tradingbot.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manual trade execution (bypassing StrategyEngine) and trade history.
 * Automated trades from TradingScheduler go through OrderExecutionService
 * directly and don't hit this controller.
 */
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final PaperOrderService paperOrderService;
    private final TradeRepository tradeRepository;
    private final PaperOrderRepository paperOrderRepository;

    /**
     * Executes a manual trade (e.g. from the frontend "Buy"/"Sell" button),
     * bypassing StrategyEngine but still going through the same
     * risk/balance checks inside OrderExecutionService.
     */
    @PostMapping("/execute")
    public ResponseEntity<PaperOrder> executeTrade(
            @RequestBody TradeOrderRequest request, @AuthenticationPrincipal AppUser user) {
        PaperOrder order = paperOrderService.submit(request, user.getPortfolioId());
        return ResponseEntity.ok(order);
    }

    /**
     * Returns all trades for a portfolio, most recent logic left to the
     * repository query - adjust ordering there if needed.
     */
    @GetMapping("/portfolio/{portfolioId}")
    public ResponseEntity<List<Trade>> getTradesForPortfolio(
            @PathVariable Long portfolioId, @AuthenticationPrincipal AppUser user) {
        if (!user.getPortfolioId().equals(portfolioId)) {
            return ResponseEntity.notFound().build();
        }
        List<Trade> trades = tradeRepository.findAllForPortfolio(portfolioId);
        return ResponseEntity.ok(trades);
    }

    @GetMapping("/portfolio/{portfolioId}/orders")
    public ResponseEntity<List<PaperOrder>> getPaperOrders(
            @PathVariable Long portfolioId, @AuthenticationPrincipal AppUser user) {
        if (!user.getPortfolioId().equals(portfolioId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(paperOrderRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<PaperOrder> cancelPaperOrder(
            @PathVariable Long orderId, @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(paperOrderService.cancel(orderId, user.getPortfolioId()));
    }
}