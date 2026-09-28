package com.rgv.tradingbot.controller;

import com.rgv.tradingbot.model.Transaction;
import com.rgv.tradingbot.model.AppUser;
import com.rgv.tradingbot.repository.PortfolioRepository;
import com.rgv.tradingbot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class TransactionController {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/{portfolioId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable Long portfolioId, @AuthenticationPrincipal AppUser user) {
        if (!portfolioRepository.existsById(portfolioId) || !user.getPortfolioId().equals(portfolioId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transactionRepository.findAllForPortfolio(portfolioId));
    }
}
