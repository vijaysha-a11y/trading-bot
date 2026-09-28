package com.rgv.tradingbot.service;

import com.rgv.tradingbot.exception.InsufficientBalanceException;
import com.rgv.tradingbot.exception.InvalidAmountException;
import com.rgv.tradingbot.model.Portfolio;
import com.rgv.tradingbot.model.Transaction;
import com.rgv.tradingbot.repository.PortfolioRepository;
import com.rgv.tradingbot.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Handles fund deposits and withdrawals, keeping Portfolio.cashBalance
 * in sync with the trading logic and recording every movement as a
 * Transaction for audit/history.
 *
 * ASSUMPTIONS (adjust once actual model/repo code is shared):
 * - Transaction has: id, portfolioId, type ("DEPOSIT"/"WITHDRAW"), amount, timestamp
 * - TransactionRepository extends JpaRepository<Transaction, Long>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundService {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Portfolio deposit(Long portfolioId, BigDecimal amount) {
        validateAmount(amount);

        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        portfolio.setCashBalance(portfolio.getCashBalance() + amount.doubleValue());
        Portfolio saved = portfolioRepository.save(portfolio);

        recordTransaction(portfolioId, "DEPOSIT", amount);
        log.info("Deposited {} into portfolio {}", amount, portfolioId);

        return saved;
    }

    @Transactional
    public Portfolio withdraw(Long portfolioId, BigDecimal amount) {
        validateAmount(amount);

        Portfolio portfolio = getPortfolioOrThrow(portfolioId);

        if (portfolio.getCashBalance() < amount.doubleValue()) {
            throw new InsufficientBalanceException(
                    "Cannot withdraw " + amount + " - available balance is "
                            + portfolio.getCashBalance());
        }

        portfolio.setCashBalance(portfolio.getCashBalance() - amount.doubleValue());
        Portfolio saved = portfolioRepository.save(portfolio);

        recordTransaction(portfolioId, "WITHDRAW", amount);
        log.info("Withdrew {} from portfolio {}", amount, portfolioId);

        return saved;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
    }

    private Portfolio getPortfolioOrThrow(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
    }

    private void recordTransaction(Long portfolioId, String type, BigDecimal amount) {
        Portfolio portfolio = getPortfolioOrThrow(portfolioId);
        Transaction transaction = new Transaction(
                com.rgv.tradingbot.model.TransactionType.valueOf(type),
                amount.doubleValue(),
                portfolio.getCashBalance(),
                portfolioId);

        transactionRepository.save(transaction);
    }
}