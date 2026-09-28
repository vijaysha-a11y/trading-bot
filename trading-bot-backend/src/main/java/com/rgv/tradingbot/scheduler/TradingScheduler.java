package com.rgv.tradingbot.scheduler;

import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.service.IndicatorService;
import com.rgv.tradingbot.service.MarketDataService;
import com.rgv.tradingbot.service.OrderExecutionService;
import com.rgv.tradingbot.service.PaperOrderService;
import com.rgv.tradingbot.service.MarketDataStreamService;
import com.rgv.tradingbot.service.RiskManagementService;
import com.rgv.tradingbot.service.StrategyEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "trading.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TradingScheduler {

    private final MarketDataService marketDataService;
    private final IndicatorService indicatorService;
    private final StrategyEngine strategyEngine;
    private final RiskManagementService riskManagementService;
    private final OrderExecutionService orderExecutionService;
    private final PaperOrderService paperOrderService;
    private final MarketDataStreamService marketDataStreamService;

    private final List<String> trackedSymbols;

    public TradingScheduler(
            MarketDataService marketDataService,
            IndicatorService indicatorService,
            StrategyEngine strategyEngine,
            RiskManagementService riskManagementService,
            OrderExecutionService orderExecutionService,
            PaperOrderService paperOrderService,
            MarketDataStreamService marketDataStreamService,
            @Value("${trading.symbol:BTCUSDT}") String symbol) {

        this.marketDataService = marketDataService;
        this.indicatorService = indicatorService;
        this.strategyEngine = strategyEngine;
        this.riskManagementService = riskManagementService;
        this.orderExecutionService = orderExecutionService;
        this.paperOrderService = paperOrderService;
        this.marketDataStreamService = marketDataStreamService;
        this.trackedSymbols = List.of(symbol);
    }

    @Scheduled(fixedDelay = 30_000)
    public void processPaperOrders() {
        try {
            paperOrderService.processOpenOrders();
        } catch (Exception ex) {
            log.error("Failed to process active paper orders: {}", ex.getMessage(), ex);
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void refreshMarketData() {

        for (String symbol : trackedSymbols) {
            try {
                marketDataStreamService.streamLatestPrice(symbol);

            } catch (Exception ex) {
                log.error(
                        "Failed to refresh market data for {}: {}",
                        symbol,
                        ex.getMessage(),
                        ex
                );
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void runTradingCycle() {

        for (String symbol : trackedSymbols) {
            try {
                indicatorService.calculateIndicators(symbol);

                List<TradeSignalDTO> signals =
                        strategyEngine.generateSignals(symbol);

                for (TradeSignalDTO signal : signals) {

                    if ("HOLD".equalsIgnoreCase(signal.getAction())) {
                        continue;
                    }

                    if (riskManagementService.isSafeToExecute(signal)) {

                        orderExecutionService.executeOrder(signal);

                        log.info(
                                "Executed signal: {} {} @ {}",
                                signal.getAction(),
                                signal.getSymbol(),
                                signal.getPrice()
                        );

                    } else {

                        log.warn(
                                "Signal rejected by risk management: {}",
                                signal
                        );
                    }
                }

            } catch (Exception ex) {

                log.error(
                        "Trading cycle failed for {}: {}",
                        symbol,
                        ex.getMessage(),
                        ex
                );
            }
        }
    }
}