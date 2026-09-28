package com.rgv.tradingbot.service;

import com.rgv.tradingbot.dto.TradeOrderRequest;
import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.model.PaperOrder;
import com.rgv.tradingbot.model.PriceCandle;
import com.rgv.tradingbot.model.Trade;
import com.rgv.tradingbot.repository.PaperOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperOrderService {
    private static final String PENDING = "PENDING";
    private static final String PROTECTIVE = "PROTECTIVE";
    private static final String FILLED = "FILLED";
    private static final String CANCELLED = "CANCELLED";

    private final PaperOrderRepository orderRepository;
    private final OrderExecutionService orderExecutionService;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;

    @Transactional
    public PaperOrder submit(TradeOrderRequest request, Long portfolioId) {
        String action = normalized(request.getAction());
        String orderType = normalized(request.getOrderType());
        if (!List.of("BUY", "SELL").contains(action)) throw new IllegalArgumentException("Order action must be BUY or SELL.");
        if (!List.of("MARKET", "LIMIT").contains(orderType)) throw new IllegalArgumentException("Order type must be MARKET or LIMIT.");
        if (request.getSymbol() == null || request.getSymbol().isBlank()) throw new IllegalArgumentException("A market symbol is required.");
        requirePositive(request.getQuantity(), "Quantity");
        if (request.getPrice() == null || request.getPrice().signum() <= 0) throw new IllegalArgumentException("A current market price is required.");

        if (orderType.equals("LIMIT")) requirePositive(request.getLimitPrice(), "Limit price");
        boolean marketableLimit = orderType.equals("LIMIT")
                && isLimitReached(action, request.getPrice().doubleValue(), request.getLimitPrice().doubleValue());
        BigDecimal protectiveReferencePrice = marketableLimit ? request.getPrice()
                : orderType.equals("LIMIT") ? request.getLimitPrice() : request.getPrice();
        validateProtectivePrices(request, action, protectiveReferencePrice);
        BigDecimal checkPrice = orderType.equals("LIMIT") ? request.getLimitPrice() : request.getPrice();
        validateCapacity(portfolioId, request.getSymbol(), action, request.getQuantity(), checkPrice);

        PaperOrder order = new PaperOrder();
        order.setSymbol(request.getSymbol().trim().toUpperCase());
        order.setAction(action);
        order.setOrderType(orderType);
        order.setQuantity(request.getQuantity().doubleValue());
        order.setLimitPrice(request.getLimitPrice() == null ? null : request.getLimitPrice().doubleValue());
        order.setStopLoss(request.getStopLoss() == null ? null : request.getStopLoss().doubleValue());
        order.setTakeProfit(request.getTakeProfit() == null ? null : request.getTakeProfit().doubleValue());
        order.setPortfolioId(portfolioId);
        order.setReason(request.getReason());

        if (orderType.equals("LIMIT") && !marketableLimit) {
            order.setStatus(PENDING);
            return orderRepository.save(order);
        }

        order.setStatus(FILLED);
        Trade trade = execute(order, request.getPrice());
        order.setTradeId(trade.getId());
        PaperOrder saved = orderRepository.save(order);
        createProtectiveOrder(saved);
        return saved;
    }

    @Transactional
    public void processOpenOrders() {
        List<PaperOrder> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of(PENDING, PROTECTIVE));
        activeOrders.stream().map(PaperOrder::getSymbol).filter(Objects::nonNull).distinct()
                .forEach(marketDataService::fetchLatestCandle);

        for (PaperOrder order : activeOrders) {
            List<PriceCandle> candles = marketDataService.getRecentCandles(order.getSymbol(), 1);
            if (candles.isEmpty()) continue;
            PriceCandle candle = candles.get(candles.size() - 1);
            if (order.getCreatedAt() != null && !candle.getTimestamp().isAfter(order.getCreatedAt())) continue;
            if (PENDING.equals(order.getStatus())) processLimit(order, candle);
            else processProtective(order, candle);
        }
    }

    @Transactional
    public PaperOrder cancel(Long orderId, Long portfolioId) {
        PaperOrder order = orderRepository.findById(orderId)
                .filter(item -> portfolioId.equals(item.getPortfolioId()))
                .orElseThrow(() -> new IllegalArgumentException("Paper order not found."));
        if (!PENDING.equals(order.getStatus())) throw new IllegalArgumentException("Only pending limit orders can be cancelled.");
        order.setStatus(CANCELLED);
        return orderRepository.save(order);
    }

    private void processLimit(PaperOrder order, PriceCandle candle) {
        double limit = order.getLimitPrice();
        boolean reached = "BUY".equals(order.getAction())
                ? candle.getLow() <= limit
                : candle.getHigh() >= limit;
        if (!reached) return;
        BigDecimal fillPrice = BigDecimal.valueOf(limit);
        Trade trade = execute(order, fillPrice);
        order.setStatus(FILLED);
        order.setTradeId(trade.getId());
        orderRepository.save(order);
        createProtectiveOrder(order);
    }

    private void processProtective(PaperOrder order, PriceCandle candle) {
        var summary = portfolioService.getSummary(order.getPortfolioId());
        var holding = summary == null ? null : summary.getHoldings().stream()
                .filter(item -> order.getSymbol().equals(item.getSymbol())).findFirst().orElse(null);
        if (holding == null || holding.getQuantity().signum() <= 0) {
            order.setStatus(CANCELLED);
            orderRepository.save(order);
            return;
        }

        boolean stopHit = order.getStopLoss() != null && candle.getLow() <= order.getStopLoss();
        boolean targetHit = order.getTakeProfit() != null && candle.getHigh() >= order.getTakeProfit();
        if (!stopHit && !targetHit) return;

        BigDecimal exitQuantity = holding.getQuantity().min(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal triggerPrice = BigDecimal.valueOf(stopHit ? order.getStopLoss() : order.getTakeProfit());
        Trade exit = orderExecutionService.executeOrder(TradeSignalDTO.builder()
                .symbol(order.getSymbol()).action("SELL").strategyName("PAPER_PROTECTIVE_EXIT")
                .price(triggerPrice).quantity(exitQuantity).confidence(1.0)
                .reason(stopHit ? "Paper stop-loss triggered" : "Paper take-profit triggered")
                .generatedAt(LocalDateTime.now()).build(), order.getPortfolioId());
        order.setTradeId(exit.getId());
        order.setStatus(FILLED);
        orderRepository.save(order);
        log.info("Paper protective exit filled for {} at {}", order.getSymbol(), triggerPrice);
    }

    private Trade execute(PaperOrder order, BigDecimal fillPrice) {
        return orderExecutionService.executeOrder(TradeSignalDTO.builder()
                .symbol(order.getSymbol()).action(order.getAction()).strategyName("PAPER_MANUAL")
                .price(fillPrice).quantity(BigDecimal.valueOf(order.getQuantity())).confidence(1.0)
                .reason(order.getReason() == null ? "Paper order" : order.getReason())
                .generatedAt(LocalDateTime.now()).build(), order.getPortfolioId());
    }

    private void createProtectiveOrder(PaperOrder entry) {
        if (!"BUY".equals(entry.getAction()) || (entry.getStopLoss() == null && entry.getTakeProfit() == null)) return;
        PaperOrder bracket = new PaperOrder();
        bracket.setSymbol(entry.getSymbol());
        bracket.setAction("SELL");
        bracket.setOrderType("BRACKET");
        bracket.setQuantity(entry.getQuantity());
        bracket.setStopLoss(entry.getStopLoss());
        bracket.setTakeProfit(entry.getTakeProfit());
        bracket.setPortfolioId(entry.getPortfolioId());
        bracket.setParentOrderId(entry.getId());
        bracket.setStatus(PROTECTIVE);
        bracket.setReason("Protective exit for paper order #" + entry.getId());
        orderRepository.save(bracket);
    }

    private void validateCapacity(Long portfolioId, String symbol, String action, BigDecimal quantity, BigDecimal price) {
        if ("BUY".equals(action)) {
            var summary = portfolioService.getSummary(portfolioId);
            if (summary == null || summary.getAvailableBalance().compareTo(quantity.multiply(price)) < 0)
                throw new IllegalArgumentException("Insufficient paper balance for this order.");
            return;
        }
        var summary = portfolioService.getSummary(portfolioId);
        boolean canSell = summary != null && summary.getHoldings().stream()
                .anyMatch(item -> symbol.equalsIgnoreCase(item.getSymbol()) && item.getQuantity().compareTo(quantity) >= 0);
        if (!canSell) throw new IllegalArgumentException("Insufficient paper holdings for this sell order.");
    }

    private void validateProtectivePrices(TradeOrderRequest request, String action, BigDecimal referencePrice) {
        if (!"BUY".equals(action)) {
            if (request.getStopLoss() != null || request.getTakeProfit() != null)
                throw new IllegalArgumentException("Stop-loss and take-profit are supported for paper buy orders.");
            return;
        }
        if (request.getStopLoss() != null && request.getStopLoss().signum() <= 0)
            throw new IllegalArgumentException("Stop-loss must be greater than zero.");
        if (request.getTakeProfit() != null && request.getTakeProfit().signum() <= 0)
            throw new IllegalArgumentException("Take-profit must be greater than zero.");
        if (request.getStopLoss() != null && request.getStopLoss().compareTo(referencePrice) >= 0)
            throw new IllegalArgumentException("Stop-loss must be below the current price for a buy order.");
        if (request.getTakeProfit() != null && request.getTakeProfit().compareTo(referencePrice) <= 0)
            throw new IllegalArgumentException("Take-profit must be above the current price for a buy order.");
    }

    private void requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(label + " must be greater than zero.");
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isLimitReached(String action, double marketPrice, double limitPrice) {
        return "BUY".equals(action) ? marketPrice <= limitPrice : marketPrice >= limitPrice;
    }
}
