package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.service.FundService;
import com.rgv.tradingbot.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Deposit and withdrawal endpoints. Validation (amount > 0) and
 * balance checks happen inside FundService, which throws
 * InvalidAmountException / InsufficientBalanceException - both are
 * caught by GlobalExceptionHandler and turned into clean JSON error responses.
 */
@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @PostMapping("/{portfolioId}/deposit")
    public ResponseEntity<Portfolio> deposit(
            @PathVariable Long portfolioId,
            @RequestBody FundRequest request,
            @AuthenticationPrincipal AppUser user) {

        requireOwnPortfolio(user, portfolioId);
        Portfolio updated = fundService.deposit(portfolioId, request.amount());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{portfolioId}/withdraw")
    public ResponseEntity<Portfolio> withdraw(
            @PathVariable Long portfolioId,
            @RequestBody FundRequest request,
            @AuthenticationPrincipal AppUser user) {

        requireOwnPortfolio(user, portfolioId);
        Portfolio updated = fundService.withdraw(portfolioId, request.amount());
        return ResponseEntity.ok(updated);
    }

    /**
     * Simple request body: { "amount": 5000.00 }
     */
    public record FundRequest(BigDecimal amount) {}

    private void requireOwnPortfolio(AppUser user, Long portfolioId) {
        if (!user.getPortfolioId().equals(portfolioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Portfolio not found.");
        }
    }
}