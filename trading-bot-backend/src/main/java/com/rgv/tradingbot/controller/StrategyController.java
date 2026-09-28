package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.dto.BacktestResultDTO;
import com.rgv.tradingbot.dto.TradeSignalDTO;
import com.rgv.tradingbot.model.StrategyConfig;
import com.rgv.tradingbot.service.BacktestService;
import com.rgv.tradingbot.service.StrategyEngine;
import com.rgv.tradingbot.service.SignalHistoryService;
import com.rgv.tradingbot.model.SignalHistoryEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

/**
 * Exposes StrategyEngine's live signal generation and BacktestService's
 * historical simulation over HTTP.
 *
 * ASSUMPTION: no dedicated StrategyConfigRepository was shared yet, so the
 * backtest endpoint builds a StrategyConfig on the fly from request params
 * rather than loading a saved config by id. Once StrategyConfigRepository
 * exists, swap runBacktest's config-building for a repository lookup.
 */
@RestController
@RequestMapping("/api/strategy")
@RequiredArgsConstructor
public class StrategyController {

    private final StrategyEngine strategyEngine;
    private final BacktestService backtestService;
    private final SignalHistoryService signalHistoryService;

    /**
     * Returns the signals StrategyEngine would generate right now for a
     * given symbol, based on the latest indicators.
     */
    @GetMapping("/{symbol}/signals")
    public ResponseEntity<List<TradeSignalDTO>> getSignals(@PathVariable String symbol) {
        List<TradeSignalDTO> signals = strategyEngine.generateSignals(symbol);
        signalHistoryService.record(signals);
        return ResponseEntity.ok(signals);
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<SignalHistoryEntry>> getSignalHistory(@PathVariable String symbol) {
        return ResponseEntity.ok(signalHistoryService.getRecent(symbol));
    }

    /**
     * Runs a historical backtest for a symbol/strategy over a date range.
     * Example: POST /api/strategy/backtest?symbol=TCS&strategyName=RSI_MEAN_REVERSION
     *          &startDate=2026-01-01&endDate=2026-06-30
     */
    @PostMapping("/backtest")
    public ResponseEntity<BacktestResultDTO> runBacktest(
            @RequestParam String symbol,
            @RequestParam String strategyName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "100000") BigDecimal startingBalance) {

        StrategyConfig config = new StrategyConfig();
        config.setSymbol(symbol);
        config.setName(strategyName);

        BacktestResultDTO result = backtestService.runBacktest(config, startDate, endDate, startingBalance);
        return ResponseEntity.ok(result);
    }
}