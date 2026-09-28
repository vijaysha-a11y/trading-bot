package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.dto.PortfolioSummaryDTO;
import com.rgv.tradingbot.service.PortfolioService;
import com.rgv.tradingbot.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the aggregated portfolio view (balance, invested amount,
 * holdings, P&L) used by the frontend dashboard. Live updates for the
 * same data are pushed separately via PortfolioWebSocketHandler.
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/{portfolioId}/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary(
            @PathVariable Long portfolioId, @AuthenticationPrincipal AppUser user) {
        if (!user.getPortfolioId().equals(portfolioId)) {
            return ResponseEntity.notFound().build();
        }
        PortfolioSummaryDTO summary = portfolioService.getSummary(portfolioId);

        if (summary == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summary);
    }
}